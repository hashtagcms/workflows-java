package org.hashtagcms.workflows.service;

import org.hashtagcms.workflows.model.Workflow;
import org.hashtagcms.workflows.repository.WorkflowRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Derives a machine-readable contract for the workflows configured on a site:
 * for each workflow, its alias, the payload keys it expects (from
 * {@code config.validation.rules}), and the directive types it can emit (from
 * {@code config.on_success} / {@code on_failure} / top-level {@code directives}).
 *
 * The workflow analogue of {@link DirectiveManifest}; introspected from stored
 * configs so nothing is hand-maintained.
 */
@Service
public class WorkflowCatalog {

    private final WorkflowRepository workflows;

    public WorkflowCatalog(WorkflowRepository workflows) {
        this.workflows = workflows;
    }

    public List<Map<String, Object>> forSite(long siteId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Workflow w : workflows.findBySiteIdAndPublishStatusTrueOrderByAliasAsc(siteId)) {
            out.add(describe(w));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> describe(Workflow w) {
        Map<String, Object> config = w.getConfig() == null ? Map.of() : w.getConfig();

        // Expected payload keys, from validation rules.
        Map<String, Object> rules = Map.of();
        Object validation = config.get("validation");
        if (validation instanceof Map<?, ?> vm && vm.get("rules") instanceof Map<?, ?> r) {
            rules = (Map<String, Object>) r;
        } else if (config.get("rules") instanceof Map<?, ?> r) {
            rules = (Map<String, Object>) r;
        }

        Map<String, Object> inputs = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : rules.entrySet()) {
            String ruleStr = e.getValue() instanceof List<?> list
                    ? String.join("|", list.stream().map(String::valueOf).toList())
                    : String.valueOf(e.getValue());
            Map<String, Object> spec = new LinkedHashMap<>();
            spec.put("required", ruleStr.contains("required"));
            spec.put("rule", ruleStr);
            inputs.put(e.getKey(), spec);
        }

        // Directive types this workflow can emit.
        LinkedHashSet<String> emits = new LinkedHashSet<>();
        for (String branch : List.of("on_success", "on_failure")) {
            if (config.get(branch) instanceof Map<?, ?> bm && bm.get("directives") instanceof List<?> dl) {
                for (Object d : dl) {
                    if (d instanceof Map<?, ?> dm && dm.get("type") != null) emits.add(String.valueOf(dm.get("type")));
                }
            }
        }
        if (config.get("directives") instanceof List<?> dl) {
            for (Object d : dl) {
                if (d instanceof Map<?, ?> dm && dm.get("type") != null) emits.add(String.valueOf(dm.get("type")));
            }
        }

        String target = null;
        if (config.get("target") instanceof Map<?, ?> tm && tm.get("type") != null) {
            target = String.valueOf(tm.get("type"));
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("alias", w.getAlias());
        row.put("name", w.getName());
        row.put("description", w.getDescription());
        row.put("auth_required", w.isAuthRequired());
        row.put("inputs", inputs);
        row.put("emits", new ArrayList<>(emits));
        row.put("target", target);
        return row;
    }
}
