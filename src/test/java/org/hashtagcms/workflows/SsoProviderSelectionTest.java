package org.hashtagcms.workflows;

import org.hashtagcms.workflows.model.WorkflowSsoProvider;
import org.hashtagcms.workflows.repository.WorkflowSsoProviderRepository;
import org.hashtagcms.workflows.security.SsoIdentityResolver;
import org.hashtagcms.workflows.security.WorkflowIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provider-selection and the local-fallback paths that don't hit the network —
 * the Java analogue of the PHP repository + resolver fallback tests.
 */
@SpringBootTest
@Transactional
class SsoProviderSelectionTest {

    @Autowired WorkflowSsoProviderRepository repository;
    @Autowired SsoIdentityResolver resolver;

    private WorkflowSsoProvider provider(long siteId, String alias, boolean enabled) {
        WorkflowSsoProvider p = new WorkflowSsoProvider();
        p.setSiteId(siteId);
        p.setName(alias);
        p.setAlias(alias);
        p.setDriver("opaque");
        p.setEnabled(enabled);
        p.setPublishStatus(true);
        p.setOnFailure("reject");
        p.setConfig(Map.of(
                "verify", Map.of("url", "http://localhost:1/none"),
                "identity", Map.of("user_id", "{{response.body.id}}")));
        return repository.save(p);
    }

    @Test
    void forSitePrefersSiteOverMasterAndSkipsDisabled() {
        provider(1, "master-sso", true);   // master (site 1)
        provider(2, "site2-sso", true);
        provider(2, "site2-disabled", false);

        assertThat(repository.forSite(List.of(2L, 1L), 2L).get(0).getAlias()).isEqualTo("site2-sso");
        assertThat(repository.forSite(List.of(999L, 1L), 999L).get(0).getAlias()).isEqualTo("master-sso");
        assertThat(repository.existsByEnabledTrueAndPublishStatusTrue()).isTrue();
    }

    @Test
    void forSiteIsDeterministicWithMultipleProvidersAndByAliasPicksOne() {
        WorkflowSsoProvider first = provider(1, "first-sso", true);
        provider(1, "second-sso", true);

        // Deterministic default: site-first then lowest id.
        assertThat(repository.forSite(List.of(1L, 1L), 1L).get(0).getAlias()).isEqualTo(first.getAlias());
        // A specific pin resolves that provider.
        assertThat(repository.byAlias("second-sso", List.of(1L, 1L), 1L).get(0).getAlias()).isEqualTo("second-sso");
        // A disabled/unknown pin resolves to nothing.
        assertThat(repository.byAlias("nope", List.of(1L, 1L), 1L)).isEmpty();
    }

    @Test
    void noneSentinelIgnoresSsoAndUsesLocalUser() {
        provider(1, "active-sso", true); // a provider IS enabled

        WorkflowIdentity id = resolver.resolve(new MockHttpServletRequest(), 1L,
                SsoIdentityResolver.PROVIDER_NONE, Map.of("id", "501"));

        assertThat(id.isAuthenticated()).isTrue();
        assertThat(id.getUserId()).isEqualTo(501L);
        assertThat(id.getExternalUserId()).isNull();
        assertThat(id.getProvider()).isNull();
    }

    @Test
    void noProviderForSiteFallsBackToLocalUser() {
        // No providers on site 3 (nor master) -> local user.
        WorkflowIdentity id = resolver.resolve(new MockHttpServletRequest(), 3L, null, Map.of("id", "7"));
        assertThat(id.getUserId()).isEqualTo(7L);
        assertThat(id.isAuthenticated()).isTrue();
    }
}
