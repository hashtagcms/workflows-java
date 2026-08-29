package org.hashtagcms.workflows;

import org.hashtagcms.workflows.engine.WorkflowContext;
import org.hashtagcms.workflows.engine.WorkflowEngine;
import org.hashtagcms.workflows.engine.WorkflowResponse;
import org.hashtagcms.workflows.model.Workflow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowEngineTest {

    private final WorkflowEngine engine = new WorkflowEngine(List.of());

    private WorkflowResponse run(Map<String, Object> config, Map<String, Object> payload) {
        Workflow wf = new Workflow();
        wf.setAlias("TEST");
        wf.setName("Test");
        wf.setConfig(config);
        WorkflowContext ctx = new WorkflowContext(wf, payload, 1, "web", "1.0.0", List.of(), Map.of());
        return engine.execute(ctx);
    }

    @Test
    void directDirectivesInterpolateAndReturnData() {
        Map<String, Object> config = Map.of(
                "target", Map.of("type", "none"),
                "on_success", Map.of(
                        "message", "Hi {{ payload.name }}",
                        "data", Map.of("echo", "{{ payload.name }}"),
                        "directives", List.of(Map.of("type", "toast", "level", "success", "message", "Hi {{ payload.name }}"))));

        WorkflowResponse r = run(config, Map.of("name", "Sam"));

        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getMessage()).isEqualTo("Hi Sam");
        assertThat(r.getDirectives().get(0).get("message")).isEqualTo("Hi Sam");
        assertThat(r.toMap().get("data")).isEqualTo(Map.of("echo", "Sam"));
    }

    @Test
    void validationFailureShortCircuits() {
        Map<String, Object> config = Map.of("validation", Map.of("rules", Map.of("code", "required|string")));
        WorkflowResponse r = run(config, Map.of());
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getDirectives().get(0).get("type")).isEqualTo("toast");
    }
}
