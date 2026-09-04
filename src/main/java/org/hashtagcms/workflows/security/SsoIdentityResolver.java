package org.hashtagcms.workflows.security;

import jakarta.servlet.http.HttpServletRequest;
import org.hashtagcms.workflows.config.WorkflowProperties;
import org.hashtagcms.workflows.model.WorkflowSsoProvider;
import org.hashtagcms.workflows.repository.WorkflowSsoProviderRepository;
import org.hashtagcms.workflows.security.sso.SsoDriver;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data-driven identity resolution backed by the SSO provider module — the Java
 * analogue of the PHP {@code SsoIdentityResolver}.
 *
 * <p>Picks the provider for the request's site (or the specific provider a workflow
 * pins via {@code sso_provider_alias}), dispatches to the driver named on the
 * provider row, and applies the provider's {@code on_failure} policy: a rejected
 * credential is downgraded to anonymous under {@code anonymous}, otherwise it is
 * surfaced as a failed identity for the caller to turn into a 401. With no
 * applicable provider — or the {@code @none} pin — it falls back to the local user
 * (the host's {@link WorkflowUserResolver}), so nothing regresses.
 */
@Component
public class SsoIdentityResolver {

    /**
     * Reserved {@code sso_provider_alias} meaning "ignore SSO for this workflow" —
     * resolve via the local user only. Uses '@' (outside the provider alias
     * charset) so it can never collide with a real alias.
     */
    public static final String PROVIDER_NONE = "@none";

    private final WorkflowSsoProviderRepository providers;
    private final WorkflowProperties properties;
    private final Map<String, SsoDriver> drivers;

    public SsoIdentityResolver(WorkflowSsoProviderRepository providers, WorkflowProperties properties,
                               List<SsoDriver> driverList) {
        this.providers = providers;
        this.properties = properties;
        this.drivers = driverList.stream()
                .collect(java.util.stream.Collectors.toMap(SsoDriver::type, d -> d));
    }

    /** Whether the SSO module is live: at least one enabled, published provider exists. */
    public boolean isModuleActive() {
        try {
            return providers.existsByEnabledTrueAndPublishStatusTrue();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param ssoProviderAlias the workflow's pin ({@code null} = site default,
     *                         {@code @none} = ignore SSO, else a specific provider)
     * @param localUser        the host-resolved local user, used as the fallback
     */
    public WorkflowIdentity resolve(HttpServletRequest request, long siteId,
                                    String ssoProviderAlias, Map<String, Object> localUser) {
        // "None": opt out of SSO entirely -> local user only.
        if (PROVIDER_NONE.equals(ssoProviderAlias)) {
            return WorkflowIdentity.localUser(localUser);
        }

        Set<Long> sites = new LinkedHashSet<>(List.of(siteId, properties.getMasterSiteId()));

        WorkflowSsoProvider provider = null;
        if (ssoProviderAlias != null && !ssoProviderAlias.isBlank()) {
            provider = providers.byAlias(ssoProviderAlias, sites, siteId).stream().findFirst().orElse(null);
        }
        if (provider == null) {
            provider = providers.forSite(sites, siteId).stream().findFirst().orElse(null);
        }
        if (provider == null) {
            // No SSO configured for this site — behave exactly as the local guard.
            return WorkflowIdentity.localUser(localUser);
        }

        SsoDriver driver = drivers.get(provider.getDriver());
        if (driver == null) {
            throw new IllegalStateException("Unknown SSO driver '" + provider.getDriver()
                    + "' for provider '" + provider.getAlias() + "'.");
        }

        WorkflowIdentity identity = driver.resolve(provider, request);

        // Rejected credential under an "anonymous" policy runs unauthenticated.
        if (identity.isFailed() && "anonymous".equals(provider.getOnFailure())) {
            return WorkflowIdentity.anonymous();
        }
        // No credential at all: let the local user have a say before settling.
        if (identity.isAnonymous()) {
            return WorkflowIdentity.localUser(localUser);
        }
        return identity;
    }
}
