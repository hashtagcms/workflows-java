package org.hashtagcms.workflows.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;

/**
 * Turns Hibernate's cryptic schema-validation failure into an actionable message.
 *
 * <p>In {@code shared} mode the library runs with {@code ddl-auto=validate} against
 * the PHP-owned tables. If those tables are absent or drifted, Hibernate aborts
 * startup with a {@code SchemaManagementException} buried in a long stack trace.
 * This listener detects that specific failure and logs a clear explanation of the
 * likely cause and the fix (registered on the standalone runner in
 * {@link org.hashtagcms.workflows.WorkflowsApplication}).
 */
public class SchemaCompatibilityListener implements ApplicationListener<ApplicationFailedEvent> {

    private static final Logger log = LoggerFactory.getLogger(SchemaCompatibilityListener.class);

    /** Tables the library maps and therefore validates in shared mode. */
    static final String REQUIRED_TABLES =
            "workflows, workflow_logs, workflow_directives, workflow_sso_providers, users, personal_access_tokens";

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        String message = diagnose(event.getException());
        if (message != null) {
            log.error("\n{}", message);
        }
    }

    /**
     * @return an actionable message if the failure is a schema-validation mismatch,
     *         otherwise {@code null} (leave other failures untouched).
     */
    public static String diagnose(Throwable failure) {
        if (!isSchemaValidationFailure(failure)) {
            return null;
        }
        String original = rootMessage(failure);
        return """
            ================================================================================
             HashtagCMS Workflows — database schema validation FAILED
            ================================================================================
            The app is validating the schema (Hibernate ddl-auto=validate — e.g. the
            'shared' profile), but the database does not match the expected mapping.

            Most likely one of:
              * The workflow tables are not present in this database. In shared mode PHP
                owns the schema — run the PHP package's migrations first.
              * DB_URL / SPRING_DATASOURCE_URL points at the wrong database. The target
                must contain: %s.
              * The schema has drifted from the version this library expects.

            Fixes:
              * Point at the correct database, or
              * Run the PHP migrations so the tables exist, or
              * For a standalone, Java-owned database do NOT use the shared profile —
                set SPRING_JPA_HIBERNATE_DDL_AUTO=update so this app creates and seeds
                its own schema.

            Original error: %s
            ================================================================================"""
                .formatted(REQUIRED_TABLES, original);
    }

    private static boolean isSchemaValidationFailure(Throwable t) {
        for (Throwable c = t; c != null && c != c.getCause(); c = c.getCause()) {
            String cls = c.getClass().getName();
            String msg = c.getMessage() == null ? "" : c.getMessage().toLowerCase();
            if (cls.endsWith("SchemaManagementException")
                    || msg.contains("schema-validation")
                    || (msg.contains("missing table") || (msg.contains("missing") && msg.contains("table")))) {
                return true;
            }
        }
        return false;
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        return c.getMessage() == null ? c.toString() : c.getMessage();
    }
}
