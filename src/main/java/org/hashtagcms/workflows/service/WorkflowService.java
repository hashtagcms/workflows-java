package org.hashtagcms.workflows.service;

import org.hashtagcms.workflows.config.WorkflowProperties;
import org.hashtagcms.workflows.engine.*;
import org.hashtagcms.workflows.model.Workflow;
import org.hashtagcms.workflows.model.WorkflowLog;
import org.hashtagcms.workflows.repository.WorkflowLogRepository;
import org.hashtagcms.workflows.repository.WorkflowRepository;
import org.hashtagcms.workflows.security.UnauthorizedException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WorkflowService {

    private final WorkflowRepository workflows;
    private final WorkflowLogRepository logs;
    private final WorkflowEngine engine;
    private final DirectiveNegotiator negotiator;
    private final WorkflowHandlerRegistry handlers;
    private final WorkflowProperties properties;
    private final ApplicationContext context;

    public WorkflowService(WorkflowRepository workflows, WorkflowLogRepository logs, WorkflowEngine engine,
                           DirectiveNegotiator negotiator, WorkflowHandlerRegistry handlers,
                           WorkflowProperties properties, ApplicationContext context) {
        this.workflows = workflows;
        this.logs = logs;
        this.engine = engine;
        this.negotiator = negotiator;
        this.handlers = handlers;
        this.properties = properties;
        this.context = context;
    }

    public WorkflowResponse execute(String alias, Map<String, Object> payload, long siteId,
                                    String platform, String appVersion, List<String> capabilities,
                                    Map<String, Object> user) {
        long start = System.currentTimeMillis();
        long master = properties.getMasterSiteId();

        Workflow workflow = workflows.resolve(alias, new LinkedHashSet<>(List.of(siteId, master)), siteId)
                .stream().findFirst().orElse(null);

        if (workflow == null) {
            workflow = new Workflow();
            workflow.setName(alias);
            workflow.setAlias(alias);
            workflow.setSiteId(siteId);
        }

        // Enforce auth_required (the analogue of a gated Sanctum route).
        if (properties.getAuth().isEnforceRequired() && workflow.isAuthRequired()
                && (user == null || user.isEmpty())) {
            throw new UnauthorizedException("Workflow '" + alias + "' requires an authenticated user.");
        }

        WorkflowContext workflowContext = new WorkflowContext(
                workflow, payload == null ? Map.of() : payload, siteId, platform, appVersion,
                capabilities == null ? List.of() : capabilities, user == null ? Map.of() : user);

        WorkflowResponse response;
        if (isDeclarative(workflow.getConfig())) {
            response = engine.execute(workflowContext);
        } else {
            WorkflowHandler handler = resolveHandler(alias, workflow.getHandler());
            if (handler == null) {
                throw new IllegalStateException("Handler for workflow '" + alias + "' not found.");
            }
            response = handler.handle(workflowContext);
        }

        // Capability negotiation
        DirectiveNegotiator.Result negotiation = null;
        if (properties.getNegotiation().isEnabled()) {
            try {
                negotiation = negotiator.negotiate(response.getDirectives(), siteId, platform, appVersion, capabilities);
                response.setDirectives(negotiation.directives());
            } catch (Exception ignored) {
                negotiation = null;
            }
        }

        long ms = System.currentTimeMillis() - start;
        writeLog(alias, siteId, payload, response, platform, appVersion, negotiation, ms, user);
        return response;
    }

    /**
     * Resolve a code-based handler: by the runtime registry (alias), then by the
     * workflow's `handler` column as a Spring bean name or a class to instantiate
     * (mirrors PHP's `$workflow->handler ?: registry[$alias]`).
     */
    private WorkflowHandler resolveHandler(String alias, String handlerRef) {
        WorkflowHandler h = handlers.get(alias);
        if (h != null) return h;
        if (handlerRef == null || handlerRef.isBlank()) return null;
        try {
            Object bean = context.getBean(handlerRef);
            if (bean instanceof WorkflowHandler wh) return wh;
        } catch (Exception ignored) { }
        try {
            Class<?> clazz = Class.forName(handlerRef);
            if (WorkflowHandler.class.isAssignableFrom(clazz)) {
                return (WorkflowHandler) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (Exception ignored) { }
        return null;
    }

    private Long userId(Map<String, Object> user) {
        if (user == null || user.get("id") == null) return null;
        try { return Long.parseLong(String.valueOf(user.get("id"))); } catch (NumberFormatException e) { return null; }
    }

    private boolean isDeclarative(Map<String, Object> config) {
        return config != null && (config.containsKey("target") || config.containsKey("directives")
                || config.containsKey("validation") || config.containsKey("rules") || config.containsKey("steps"));
    }

    private void writeLog(String alias, long siteId, Map<String, Object> payload, WorkflowResponse response,
                          String platform, String appVersion, DirectiveNegotiator.Result negotiation, long ms,
                          Map<String, Object> user) {
        try {
            WorkflowLog log = new WorkflowLog();
            log.setWorkflowAlias(alias);
            log.setSiteId(siteId);
            log.setUserId(userId(user));
            log.setPayload(payload);
            log.setResponseDirectives(new ArrayList<>(response.getDirectives()));
            log.setSuccess(response.isSuccess());
            log.setErrorMessage(response.isSuccess() ? null : response.getMessage());
            log.setExecutionTimeMs(ms);
            log.setClientPlatform(platform);
            log.setClientAppVersion(appVersion);
            if (negotiation != null && (!negotiation.downgraded().isEmpty() || !negotiation.dropped().isEmpty())) {
                log.setNegotiation(Map.of(
                        "directives_downgraded", negotiation.downgraded(),
                        "directives_dropped", negotiation.dropped()));
            }
            logs.save(log);
        } catch (Exception ignored) {
            // logging must never break execution
        }
    }
}
