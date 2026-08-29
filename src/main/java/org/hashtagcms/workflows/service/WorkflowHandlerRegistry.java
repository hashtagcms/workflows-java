package org.hashtagcms.workflows.service;

import org.hashtagcms.workflows.engine.WorkflowHandler;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of code-based workflow handlers keyed by alias. The package ships no
 * built-in handlers — applications register their own (e.g. in a
 * {@code @PostConstruct} or config bean).
 */
@Component
public class WorkflowHandlerRegistry {

    private final Map<String, WorkflowHandler> handlers = new ConcurrentHashMap<>();

    public WorkflowHandlerRegistry register(String alias, WorkflowHandler handler) {
        handlers.put(alias, handler);
        return this;
    }

    public WorkflowHandler get(String alias) {
        return handlers.get(alias);
    }

    public Map<String, WorkflowHandler> all() {
        return Map.copyOf(handlers);
    }
}
