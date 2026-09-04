package org.hashtagcms.workflows.web;

import org.hashtagcms.workflows.model.WorkflowSsoProvider;
import org.hashtagcms.workflows.repository.WorkflowSsoProviderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Management API for SSO / external-login providers (the API-only replacement for
 * the PHP admin UI). A provider row tells the {@code SsoIdentityResolver} how to
 * verify a client credential and map it to a workflow identity.
 */
@RestController
@RequestMapping("${hashtagcms.workflows.route-prefix:/api/hashtagcms}/admin/sso-providers")
public class WorkflowSsoProviderAdminController {

    private static final Pattern ALIAS = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final Set<String> DRIVERS = Set.of("opaque", "jwt");
    private static final Set<String> ON_FAILURE = Set.of("reject", "anonymous");

    private final WorkflowSsoProviderRepository repository;

    public WorkflowSsoProviderAdminController(WorkflowSsoProviderRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<WorkflowSsoProvider> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowSsoProvider> get(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody WorkflowSsoProvider provider) {
        String error = validate(provider, null);
        if (error != null) return unprocessable(error);
        provider.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(provider));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody WorkflowSsoProvider provider) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        String error = validate(provider, id);
        if (error != null) return unprocessable(error);
        provider.setId(id);
        return ResponseEntity.ok(repository.save(provider));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** @return an error message, or null when valid. Alias is unique per site. */
    private String validate(WorkflowSsoProvider p, Long selfId) {
        if (p.getName() == null || p.getName().isBlank()) return "name is required.";
        if (p.getAlias() == null || !ALIAS.matcher(p.getAlias()).matches()) {
            return "alias is required and may contain only letters, digits, '.', '_' and '-'.";
        }
        if (!DRIVERS.contains(p.getDriver())) return "driver must be one of " + DRIVERS + ".";
        if (!ON_FAILURE.contains(p.getOnFailure())) return "on_failure must be one of " + ON_FAILURE + ".";
        Long siteId = p.getSiteId() == null ? 1L : p.getSiteId();
        if (repository.existsBySiteIdAndAlias(siteId, p.getAlias().trim())) {
            // Allow the row to keep its own alias on update.
            if (selfId == null || repository.findById(selfId)
                    .map(existing -> !p.getAlias().trim().equals(existing.getAlias())
                            || !siteId.equals(existing.getSiteId()))
                    .orElse(true)) {
                return "alias '" + p.getAlias().trim() + "' is already used on this site.";
            }
        }
        return null;
    }

    private ResponseEntity<?> unprocessable(String message) {
        return ResponseEntity.unprocessableEntity().body(Map.of("success", false, "message", message));
    }
}
