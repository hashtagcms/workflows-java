package org.hashtagcms.workflows.security.sso;

import jakarta.servlet.http.HttpServletRequest;
import org.hashtagcms.workflows.model.WorkflowSsoProvider;
import org.hashtagcms.workflows.security.WorkflowIdentity;

/**
 * Verifies a client credential against an external login service and maps it to a
 * {@link WorkflowIdentity}. Implementations MUST NOT throw for an absent or invalid
 * credential — return {@link WorkflowIdentity#anonymous()} (no credential) or
 * {@link WorkflowIdentity#rejected(String)} (bad/expired). Throw only on
 * misconfiguration.
 */
public interface SsoDriver {

    /** The {@code driver} column value(s) this implementation handles. */
    String type();

    WorkflowIdentity resolve(WorkflowSsoProvider provider, HttpServletRequest request);
}
