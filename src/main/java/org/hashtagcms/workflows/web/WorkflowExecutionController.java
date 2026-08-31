package org.hashtagcms.workflows.web;

import jakarta.servlet.http.HttpServletRequest;
import org.hashtagcms.workflows.config.WorkflowProperties;
import org.hashtagcms.workflows.engine.DirectiveNegotiator;
import org.hashtagcms.workflows.security.UnauthorizedException;
import org.hashtagcms.workflows.security.WorkflowUserResolver;
import org.hashtagcms.workflows.service.WorkflowCatalog;
import org.hashtagcms.workflows.service.WorkflowHandlerRegistry;
import org.hashtagcms.workflows.service.WorkflowService;
import org.hashtagcms.workflows.web.dto.ExecuteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Public headless API: execute a workflow, health, and the directive manifest. */
@RestController
@RequestMapping("${hashtagcms.workflows.route-prefix:/api/hashtagcms}/public/workflows/v1")
public class WorkflowExecutionController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionController.class);

    private final WorkflowService workflowService;
    private final DirectiveNegotiator negotiator;
    private final WorkflowHandlerRegistry handlers;
    private final WorkflowProperties properties;
    private final WorkflowUserResolver userResolver;
    private final WorkflowCatalog catalog;

    public WorkflowExecutionController(WorkflowService workflowService, DirectiveNegotiator negotiator,
                                       WorkflowHandlerRegistry handlers, WorkflowProperties properties,
                                       WorkflowUserResolver userResolver, WorkflowCatalog catalog) {
        this.workflowService = workflowService;
        this.negotiator = negotiator;
        this.handlers = handlers;
        this.properties = properties;
        this.userResolver = userResolver;
        this.catalog = catalog;
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute(@RequestBody ExecuteRequest request,
                                                       HttpServletRequest httpRequest) {
        if (request.getWorkflow() == null || request.getWorkflow().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing required parameter: workflow",
                    "directives", List.of()));
        }

        String platform = "android";
        String appVersion = null;
        if (request.getClient() != null) {
            if (request.getClient().getPlatform() != null) platform = request.getClient().getPlatform();
            appVersion = request.getClient().getAppVersion();
        } else if (request.getPlatform() != null) {
            platform = request.getPlatform();
        }
        long siteId = request.getSiteId() == null ? 1L : request.getSiteId();
        List<String> capabilities = request.getCapabilities() == null ? List.of() : request.getCapabilities();
        Map<String, Object> user = userResolver.resolveUser(httpRequest);

        try {
            var response = workflowService.execute(
                    request.getWorkflow(),
                    request.getPayload() == null ? Map.of() : request.getPayload(),
                    siteId, platform, appVersion, capabilities, user);
            return ResponseEntity.ok(response.toMap());
        } catch (UnauthorizedException e) {
            Map<String, Object> toast = new LinkedHashMap<>();
            toast.put("type", "toast");
            toast.put("message", "Authentication required.");
            toast.put("level", "error");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "directives", List.of(toast)));
        } catch (Exception e) {
            log.error("Workflow execution failed for {}", request.getWorkflow(), e);
            String clientMessage = properties.isExposeErrorDetails()
                    ? e.getMessage()
                    : "Workflow execution failed. Please try again later.";
            Map<String, Object> toast = new LinkedHashMap<>();
            toast.put("type", "toast");
            toast.put("message", "Workflow failed: " + clientMessage);
            toast.put("level", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", clientMessage,
                    "directives", List.of(toast)));
        }
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "module", "hashtagcms/workflows-java",
                "registered_handlers", new ArrayList<>(handlers.all().keySet()));
    }

    @GetMapping("/directives")
    public Map<String, Object> directives(@RequestParam(name = "site_id", defaultValue = "1") long siteId,
                                          @RequestParam(name = "platform", required = false) String platform,
                                          @RequestParam(name = "app_version", required = false) String appVersion) {
        return Map.of("success", true, "directives", negotiator.catalog(siteId, platform, appVersion));
    }

    /** The workflow contract for a site: each workflow's alias, expected payload keys, and emitted directive types. */
    @GetMapping("/catalog")
    public Map<String, Object> catalog(@RequestParam(name = "site_id", defaultValue = "1") long siteId) {
        return Map.of("success", true, "site_id", siteId, "workflows", catalog.forSite(siteId));
    }
}
