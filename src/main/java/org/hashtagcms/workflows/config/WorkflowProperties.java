package org.hashtagcms.workflows.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds `hashtagcms.workflows.*` configuration. */
@ConfigurationProperties(prefix = "hashtagcms.workflows")
public class WorkflowProperties {

    /** Master site id used as a fallback when a site-specific row is missing. */
    private long masterSiteId = 1;

    /**
     * Base API route prefix (mirrors PHP's `hashtagcmsapi.route_prefix`). The
     * public endpoints live under `{route-prefix}/public/workflows/v1` and the
     * management endpoints under `{route-prefix}/admin/...`.
     */
    private String routePrefix = "/api/hashtagcms";

    /**
     * When true, workflow execution failures return the raw exception message to
     * the API client (mirrors PHP's `expose_error_details`). Keep false in prod.
     */
    private boolean exposeErrorDetails = false;

    private final Negotiation negotiation = new Negotiation();
    private final Install install = new Install();
    private final Auth auth = new Auth();

    public Auth getAuth() { return auth; }

    public long getMasterSiteId() { return masterSiteId; }
    public void setMasterSiteId(long masterSiteId) { this.masterSiteId = masterSiteId; }
    public String getRoutePrefix() { return routePrefix; }
    public void setRoutePrefix(String routePrefix) { this.routePrefix = routePrefix; }
    public boolean isExposeErrorDetails() { return exposeErrorDetails; }
    public void setExposeErrorDetails(boolean exposeErrorDetails) { this.exposeErrorDetails = exposeErrorDetails; }
    public Negotiation getNegotiation() { return negotiation; }
    public Install getInstall() { return install; }

    public static class Negotiation {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Install {
        private boolean seedDirectives = true;
        private boolean seedExamples = true;
        public boolean isSeedDirectives() { return seedDirectives; }
        public void setSeedDirectives(boolean v) { this.seedDirectives = v; }
        public boolean isSeedExamples() { return seedExamples; }
        public void setSeedExamples(boolean v) { this.seedExamples = v; }
    }

    public static class Auth {
        /**
         * User-resolution driver:
         *   `header`  — trusted gateway forwards the identity as headers (default);
         *   `sanctum` — validate a Laravel Sanctum bearer token against the shared DB;
         *   `jwt`     — verify an external IdP's JWT (OIDC/SSO) via JWKS or a shared secret.
         */
        private String driver = "header";
        /** Reject execution of an `auth_required` workflow when no user resolves. */
        private boolean enforceRequired = true;
        /** Trusted headers used by the default (gateway) user resolver. */
        private String idHeader = "X-User-Id";
        private String emailHeader = "X-User-Email";
        private String nameHeader = "X-User-Name";

        private final Jwt jwt = new Jwt();

        public String getDriver() { return driver; }
        public void setDriver(String driver) { this.driver = driver; }
        public boolean isEnforceRequired() { return enforceRequired; }
        public void setEnforceRequired(boolean v) { this.enforceRequired = v; }
        public String getIdHeader() { return idHeader; }
        public void setIdHeader(String v) { this.idHeader = v; }
        public String getEmailHeader() { return emailHeader; }
        public void setEmailHeader(String v) { this.emailHeader = v; }
        public String getNameHeader() { return nameHeader; }
        public void setNameHeader(String v) { this.nameHeader = v; }
        public Jwt getJwt() { return jwt; }
    }

    /**
     * Settings for the `jwt` driver. Configure exactly one key source:
     * `jwks-uri` (asymmetric RS/ES/PS — the usual OIDC case) or `secret` (symmetric HS*).
     */
    public static class Jwt {
        /** JWKS endpoint of the IdP, e.g. https://YOUR_DOMAIN/.well-known/jwks.json (RS/ES/PS tokens). */
        private String jwksUri;
        /** Shared secret for HS* tokens (use instead of jwks-uri). */
        private String secret;
        /** Signature algorithm. Defaults: RS256 when jwks-uri is set, HS256 when secret is set. */
        private String algorithm;
        /** Expected `iss` claim (rejected if it does not match). Optional but recommended. */
        private String issuer;
        /** Expected `aud` claim (rejected if the token's audience does not contain it). Optional. */
        private String audience;
        /** Allowed clock skew, in seconds, for exp/nbf validation. */
        private long clockSkewSeconds = 60;
        /** Claim → user-field mapping. */
        private String idClaim = "sub";
        private String emailClaim = "email";
        private String nameClaim = "name";
        /**
         * When true, look up the local `users` row by the email claim and adopt its
         * numeric id / canonical email+name — so `{{ user.id }}` and workflow_logs
         * carry the real HashtagCMS id. When false (or no match), the JWT claims are
         * used as-is (id may be the non-numeric `sub`).
         */
        private boolean linkByEmail = false;

        public String getJwksUri() { return jwksUri; }
        public void setJwksUri(String v) { this.jwksUri = v; }
        public String getSecret() { return secret; }
        public void setSecret(String v) { this.secret = v; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String v) { this.algorithm = v; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String v) { this.issuer = v; }
        public String getAudience() { return audience; }
        public void setAudience(String v) { this.audience = v; }
        public long getClockSkewSeconds() { return clockSkewSeconds; }
        public void setClockSkewSeconds(long v) { this.clockSkewSeconds = v; }
        public String getIdClaim() { return idClaim; }
        public void setIdClaim(String v) { this.idClaim = v; }
        public String getEmailClaim() { return emailClaim; }
        public void setEmailClaim(String v) { this.emailClaim = v; }
        public String getNameClaim() { return nameClaim; }
        public void setNameClaim(String v) { this.nameClaim = v; }
        public boolean isLinkByEmail() { return linkByEmail; }
        public void setLinkByEmail(boolean v) { this.linkByEmail = v; }
    }
}
