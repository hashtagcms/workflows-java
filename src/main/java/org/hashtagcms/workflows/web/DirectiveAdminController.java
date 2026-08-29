package org.hashtagcms.workflows.web;

import org.hashtagcms.workflows.model.WorkflowDirective;
import org.hashtagcms.workflows.repository.WorkflowDirectiveRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Management API for the directive capability manifest. */
@RestController
@RequestMapping("${hashtagcms.workflows.route-prefix:/api/hashtagcms}/admin/directives")
public class DirectiveAdminController {

    private final WorkflowDirectiveRepository repository;

    public DirectiveAdminController(WorkflowDirectiveRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<WorkflowDirective> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDirective> get(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public WorkflowDirective create(@RequestBody WorkflowDirective directive) {
        directive.setId(null);
        return repository.save(directive);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowDirective> update(@PathVariable Long id, @RequestBody WorkflowDirective directive) {
        return repository.findById(id).map(existing -> {
            directive.setId(id);
            return ResponseEntity.ok(repository.save(directive));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
