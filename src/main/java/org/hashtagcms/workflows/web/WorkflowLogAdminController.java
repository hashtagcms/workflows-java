package org.hashtagcms.workflows.web;

import org.hashtagcms.workflows.model.WorkflowLog;
import org.hashtagcms.workflows.repository.WorkflowLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Read (and prune) the workflow audit log — the REST replacement for PHP's
 * {@code WorkflowLogController}. Logs are written by {@code WorkflowService} on
 * every execution; this exposes them for inspection. Listing is paginated and
 * newest-first, optionally filtered by workflow alias.
 */
@RestController
@RequestMapping("${hashtagcms.workflows.route-prefix:/api/hashtagcms}/admin/logs")
public class WorkflowLogAdminController {

    private final WorkflowLogRepository repository;

    public WorkflowLogAdminController(WorkflowLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Page<WorkflowLog> list(@RequestParam(name = "alias", required = false) String alias,
                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                  @RequestParam(name = "size", defaultValue = "25") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 200));
        return (alias == null || alias.isBlank())
                ? repository.findAllByOrderByIdDesc(pageable)
                : repository.findByWorkflowAliasOrderByIdDesc(alias, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowLog> get(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
