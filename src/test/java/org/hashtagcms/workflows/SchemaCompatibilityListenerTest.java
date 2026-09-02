package org.hashtagcms.workflows;

import org.hashtagcms.workflows.config.SchemaCompatibilityListener;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The shared-mode schema-validation failure is turned into an actionable message. */
class SchemaCompatibilityListenerTest {

    @Test
    void diagnosesHibernateSchemaValidationFailure() {
        Throwable failure = new IllegalStateException("context failed",
                new RuntimeException(new SchemaManagementException(
                        "Schema-validation: missing table [workflows]")));

        String msg = SchemaCompatibilityListener.diagnose(failure);

        assertThat(msg).isNotNull();
        assertThat(msg)
                .contains("schema validation FAILED")
                .contains("ddl-auto=validate")
                .contains("workflows, workflow_logs, workflow_directives, users, personal_access_tokens")
                .contains("SPRING_JPA_HIBERNATE_DDL_AUTO=update")
                .contains("missing table [workflows]"); // original error preserved
    }

    @Test
    void detectsMissingTableByMessageEvenWithoutTheHibernateType() {
        Throwable failure = new RuntimeException("Schema-validation: missing table [workflow_logs]");
        assertThat(SchemaCompatibilityListener.diagnose(failure)).isNotNull();
    }

    @Test
    void leavesUnrelatedFailuresUntouched() {
        assertThat(SchemaCompatibilityListener.diagnose(new RuntimeException("port 8080 already in use")))
                .isNull();
        assertThat(SchemaCompatibilityListener.diagnose(new NullPointerException()))
                .isNull();
    }
}
