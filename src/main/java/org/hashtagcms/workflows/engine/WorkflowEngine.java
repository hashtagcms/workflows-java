package org.hashtagcms.workflows.engine;

import org.hashtagcms.workflows.engine.target.TargetAdapter;
import org.hashtagcms.workflows.engine.target.TargetResult;
import org.springframework.stereotype.Component;

import java.util.*;

/** Executes a workflow declaratively from its `config` map. Mirrors the PHP engine. */
@Component
public class WorkflowEngine {

    private final Map<String, TargetAdapter> adapters = new HashMap<>();

    public WorkflowEngine(List<TargetAdapter> adapterList) {
        for (TargetAdapter a : adapterList) {
            for (String type : a.types()) adapters.put(type, a);
        }
    }

    @SuppressWarnings("unchecked")
    public WorkflowResponse execute(WorkflowContext context) {
        Map<String, Object> config = context.getWorkflow().getConfig() == null ? Map.of() : context.getWorkflow().getConfig();
        Map<String, Object> payload = context.getPayload() == null ? Map.of() : context.getPayload();

        // 1. Validation
        Map<String, Object> validation = asMap(config.get("validation"));
        Map<String, Object> rules = validation != null ? asMap(validation.get("rules")) : asMap(config.get("rules"));
        if (rules != null && !rules.isEmpty()) {
            Map<String, Object> messages = validation != null ? asMap(validation.get("messages")) : asMap(config.get("messages"));
            PayloadValidator.Result result = PayloadValidator.validate(payload, rules, messages);
            if (!result.passes()) {
                WorkflowResponse response = WorkflowResponse.make().setSuccess(false).setMessage(result.first());
                Object onError = validation != null ? nested(validation, "on_error", "directives") : nested(config, "on_error", "directives");
                Map<String, Object> errCtx = new LinkedHashMap<>();
                errCtx.put("payload", payload);
                errCtx.put("errors", result.errors());
                if (onError instanceof List<?> list) {
                    Object interpolated = VariableInterpolator.interpolate(list, errCtx);
                    for (Object d : (List<Object>) interpolated) {
                        if (d instanceof Map) response.addDirective((Map<String, Object>) d);
                    }
                } else {
                    response.addToast(result.first(), "error");
                }
                return response;
            }
        }

        // 2. Interpolation context
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("payload", payload);
        ctx.put("site", Map.of("id", context.getSiteId()));
        ctx.put("platform", context.getPlatform());
        ctx.put("user", context.getUser() == null ? Map.of() : context.getUser());
        ctx.put("claims", context.getClaims() == null ? Map.of() : context.getClaims());
        ctx.put("identity", context.getIdentity() == null ? Map.of() : context.getIdentity());
        ctx.put("config", config);
        ctx.put("workflow", Map.of(
                "id", context.getWorkflow().getId() == null ? 0 : context.getWorkflow().getId(),
                "alias", context.getWorkflow().getAlias() == null ? "" : context.getWorkflow().getAlias(),
                "name", context.getWorkflow().getName() == null ? "" : context.getWorkflow().getName()));
        ctx.put("workflow_context", context); // for custom_class/handler targets

        // 3. Target
        TargetResult targetResult = TargetResult.skipped();
        Map<String, Object> target = asMap(config.get("target"));
        if (target != null && !target.isEmpty()) {
            String type = String.valueOf(target.getOrDefault("type", "http_request")).toLowerCase();
            if (!type.equals("none") && !type.equals("direct")) {
                TargetAdapter adapter = adapters.get(type);
                if (adapter == null) {
                    throw new IllegalArgumentException("Unsupported workflow target type '" + type + "'.");
                }
                Map<String, Object> interpolatedTarget = (Map<String, Object>) VariableInterpolator.interpolate(target, ctx);
                targetResult = adapter.execute(interpolatedTarget, ctx);

                // A custom_class/handler target may return a compiled response.
                if (targetResult.workflowResponse() != null) {
                    return targetResult.workflowResponse();
                }
            }
        }

        // 4. Add response to context
        ctx.put("response", Map.of(
                "success", targetResult.success(),
                "status", targetResult.status(),
                "body", targetResult.body() == null ? "" : targetResult.body(),
                "headers", targetResult.headers()));
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("message", targetResult.error());
        ctx.put("error", error);

        boolean success = targetResult.success() && targetResult.status() < 400;
        String branchKey = success ? "on_success" : "on_failure";
        Map<String, Object> branch = asMap(config.get(branchKey));
        if (branch == null) branch = Map.of();

        Object rawDirectives = branch.get("directives");
        if (rawDirectives == null && success) rawDirectives = config.get("directives");

        Object rawMessage = branch.get("message");
        if (rawMessage == null) rawMessage = config.get("message");
        if (rawMessage == null) {
            rawMessage = success ? "Workflow executed successfully."
                    : (targetResult.error() != null ? targetResult.error() : "Workflow execution failed.");
        }

        WorkflowResponse response = WorkflowResponse.make()
                .setSuccess(success)
                .setMessage(String.valueOf(VariableInterpolator.interpolate(rawMessage, ctx)));

        boolean hasDirectives = false;
        if (rawDirectives instanceof List<?> list) {
            Object interpolated = VariableInterpolator.interpolate(list, ctx);
            for (Object d : (List<Object>) interpolated) {
                if (d instanceof Map) { response.addDirective((Map<String, Object>) d); hasDirectives = true; }
            }
        }
        if (!success && !hasDirectives) {
            response.addToast(response.getMessage(), "error");
        }

        // 5. Optional data payload
        Object rawData = branch.get("data");
        if (rawData == null) rawData = config.get("data");
        if (rawData != null) {
            Object interpolated = VariableInterpolator.interpolate(rawData, ctx);
            if (interpolated instanceof Map) response.withData((Map<String, Object>) interpolated);
        }

        return response;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    private static Object nested(Map<String, Object> map, String a, String b) {
        Map<String, Object> inner = asMap(map.get(a));
        return inner == null ? null : inner.get(b);
    }
}
