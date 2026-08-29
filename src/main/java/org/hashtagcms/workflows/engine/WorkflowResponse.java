package org.hashtagcms.workflows.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A fluent builder for the server-driven response: success flag, message, an
 * ordered list of client directives, and an optional data payload.
 */
public class WorkflowResponse {

    private boolean success = true;
    private String message;
    private List<Map<String, Object>> directives = new ArrayList<>();
    private Map<String, Object> data = new LinkedHashMap<>();

    public static WorkflowResponse make() { return new WorkflowResponse(); }

    public WorkflowResponse toast(String message, String level) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("type", "toast");
        d.put("message", message);
        d.put("level", level);
        directives.add(d);
        return this;
    }

    public WorkflowResponse addToast(String message, String level) { return toast(message, level); }

    public WorkflowResponse haptic(String intensity) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("type", "haptic");
        d.put("intensity", intensity);
        directives.add(d);
        return this;
    }

    public WorkflowResponse navigate(String target, Map<String, Object> params) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("type", "navigate");
        d.put("target", target);
        d.put("params", params == null ? new LinkedHashMap<>() : params);
        directives.add(d);
        return this;
    }

    public WorkflowResponse mutateCart(Map<String, Object> cartData) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("type", "mutate_cart");
        if (cartData != null) d.putAll(cartData);
        directives.add(d);
        return this;
    }

    public WorkflowResponse addDirective(Map<String, Object> directive) {
        if (directive != null) directives.add(directive);
        return this;
    }

    public WorkflowResponse setSuccess(boolean success) { this.success = success; return this; }

    public WorkflowResponse setMessage(String message) { this.message = message; return this; }

    public WorkflowResponse withData(Map<String, Object> data) {
        if (data != null) this.data.putAll(data);
        return this;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<Map<String, Object>> getDirectives() { return directives; }

    public WorkflowResponse setDirectives(List<Map<String, Object>> directives) {
        this.directives = new ArrayList<>(directives);
        return this;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", success);
        out.put("message", message);
        out.put("directives", directives);
        out.put("data", data);
        return out;
    }
}
