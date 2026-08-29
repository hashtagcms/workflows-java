package org.hashtagcms.workflows.web;

import org.hashtagcms.workflows.model.Workflow;
import org.hashtagcms.workflows.repository.WorkflowRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Management API for workflows (the API-only replacement for the admin UI). */
@RestController
@RequestMapping("${hashtagcms.workflows.route-prefix:/api/hashtagcms}/admin/workflows")
public class WorkflowAdminController {

    private final WorkflowRepository repository;

    public WorkflowAdminController(WorkflowRepository repository) {
        this.repository = repository;
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
}
