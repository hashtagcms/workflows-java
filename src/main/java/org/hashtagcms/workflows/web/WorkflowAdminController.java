package org.hashtagcms.workflows.web;

import org.hashtagcms.workflows.config.WorkflowProperties;
import org.hashtagcms.workflows.engine.DirectiveNegotiator;
import org.hashtagcms.workflows.engine.WorkflowContext;
import org.hashtagcms.workflows.engine.WorkflowEngine;
import org.hashtagcms.workflows.engine.WorkflowResponse;
import org.hashtagcms.workflows.model.Workflow;
import org.hashtagcms.workflows.repository.WorkflowRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Management API for workflows (the API-only replacement for the admin UI). */
@RestController
@RequestMapping("${hashtagcms.workflows.route-prefix:/api/hashtagcms}/admin/workflows")
public class WorkflowAdminController {

    private final WorkflowRepository repository;
    private final WorkflowEngine engine;
    private final DirectiveNegotiator negotiator;
    private final WorkflowProperties properties;

    public WorkflowAdminController(WorkflowRepository repository, WorkflowEngine engine,
                                   DirectiveNegotiator negotiator, WorkflowProperties properties) {
        this.repository = repository;
        this.engine = engine;
        this.negotiator = negotiator;
        this.properties = properties;
    }

    @GetMapping
    public List<Workflow> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workflow> get(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Workflow create(@RequestBody Workflow workflow) {
        workflow.setId(null);
        return repository.save(workflow);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Workflow> update(@PathVariable Long id, @RequestBody Workflow workflow) {
        return repository.findById(id).map(existing -> {
            workflow.setId(id);
            return ResponseEntity.ok(repository.save(workflow));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Dry-run an unsaved config through the engine — no persistence. Mirrors the
     * real execute path (declarative engine + capability negotiation) on a
     * transient workflow, so a client can validate a config before saving it.
     * The PHP analogue is {@code WorkflowBuilderController@preview}.
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/preview")
    public Map<String, Object> preview(@RequestBody Map<String, Object> body) {
        Object rawConfig = body.get("config");
        Map<String, Object> config = rawConfig instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        Map<String, Object> payload = body.get("payload") instanceof Map<?, ?> p ? (Map<String, Object>) p : Map.of();
        String platform = body.get("platform") == null ? null : String.valueOf(body.get("platform"));
        String appVersion = body.get("app_version") == null ? null : String.valueOf(body.get("app_version"));
        long siteId = body.get("site_id") == null ? 1L : Long.parseLong(String.valueOf(body.get("site_id")));
        List<String> capabilities = body.get("capabilities") instanceof List<?> c
                ? c.stream().map(String::valueOf).toList() : List.of();

        try {
            Workflow workflow = new Workflow();
            workflow.setName("Preview");
            workflow.setAlias("WORKFLOW_PREVIEW");
            workflow.setSiteId(siteId);
            workflow.setConfig(config);

            WorkflowContext context = new WorkflowContext(
                    workflow, payload, siteId, platform, appVersion, capabilities, Map.of());
            WorkflowResponse response = engine.execute(context);

            if (properties.getNegotiation().isEnabled()) {
                DirectiveNegotiator.Result result =
                        negotiator.negotiate(response.getDirectives(), siteId, platform, appVersion, capabilities);
                response.setDirectives(result.directives());
            }
            return response.toMap();
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage(), "directives", List.of());
        }
    }
}
