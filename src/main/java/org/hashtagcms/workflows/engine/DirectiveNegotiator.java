package org.hashtagcms.workflows.engine;

import org.hashtagcms.workflows.config.WorkflowProperties;
import org.hashtagcms.workflows.model.WorkflowDirective;
import org.hashtagcms.workflows.repository.WorkflowDirectiveRepository;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Capability negotiation: rewrites a workflow's emitted directive list so a
 * client only receives directives it can render — downgrading to a fallback
 * (chased along a chain) or dropping. Fail-safe: unknown types and an empty
 * manifest pass straight through.
 */
@Component
public class DirectiveNegotiator {

    private static final int MAX_FALLBACK_DEPTH = 5;

    private final WorkflowDirectiveRepository repository;
    private final WorkflowProperties properties;

    public DirectiveNegotiator(WorkflowDirectiveRepository repository, WorkflowProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public record Result(List<Map<String, Object>> directives,
                         List<Map<String, String>> downgraded,
                         List<String> dropped) {}

    public Result negotiate(List<Map<String, Object>> directives, long siteId, String platform,
                            String appVersion, List<String> capabilities) {
        Map<String, WorkflowDirective> manifest = resolveManifest(siteId);

        if (manifest.isEmpty()) {
            return new Result(directives, List.of(), List.of());
        }

        List<Map<String, Object>> out = new ArrayList<>();
        List<Map<String, String>> downgraded = new ArrayList<>();
        List<String> dropped = new ArrayList<>();

        for (Map<String, Object> directive : directives) {
            Object typeObj = directive.get("type");
            String type = typeObj == null ? null : typeObj.toString();

            if (type == null || !manifest.containsKey(type)) {
                out.add(directive);
                continue;
            }
            if (isSupported(type, manifest, platform, appVersion, capabilities)) {
                out.add(directive);
                continue;
            }
            String resolved = resolveFallback(type, manifest, platform, appVersion, capabilities);
            if (resolved != null) {
                out.add(new LinkedHashMap<>(Map.of("type", resolved)));
                downgraded.add(Map.of("from", type, "to", resolved));
            } else {
                dropped.add(type);
            }
        }
        return new Result(out, downgraded, dropped);
    }

    public Map<String, WorkflowDirective> resolveManifest(long siteId) {
        long master = properties.getMasterSiteId();
        Set<Long> sites = new LinkedHashSet<>(List.of(siteId, master));
        List<WorkflowDirective> rows = repository.findByPublishStatusIsTrueAndSiteIdIn(sites);

        Map<String, WorkflowDirective> byType = new LinkedHashMap<>();
        for (WorkflowDirective d : rows) {
            WorkflowDirective existing = byType.get(d.getType());
            // site-specific wins over master
            if (existing == null || (d.getSiteId() == siteId && existing.getSiteId() != siteId)) {
                byType.put(d.getType(), d);
            }
        }
        return byType;
    }

    public List<Map<String, Object>> catalog(long siteId, String platform, String appVersion) {
        Map<String, WorkflowDirective> manifest = resolveManifest(siteId);
        boolean filter = platform != null && !platform.isBlank();

        List<Map<String, Object>> out = new ArrayList<>();
        for (WorkflowDirective d : manifest.values()) {
            if (filter && !isSupported(d.getType(), manifest, platform, appVersion, List.of())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", d.getType());
            m.put("label", d.getLabel());
            m.put("category", d.getCategory());
            m.put("description", d.getDescription());
            m.put("platforms", d.getPlatforms());
            m.put("schema", d.getSchema());
            m.put("fallback", d.getFallback());
            out.add(m);
        }
        return out;
    }

    private boolean isSupported(String type, Map<String, WorkflowDirective> manifest, String platform,
                                String appVersion, List<String> capabilities) {
        if (capabilities != null && !capabilities.isEmpty()) {
            return capabilities.contains(type);
        }
        WorkflowDirective d = manifest.get(type);
        if (d == null) return true;

        Map<String, String> platforms = d.getPlatforms();
        if (platforms == null || platforms.isEmpty()) return true;
        if (platform == null || platform.isBlank()) return true;
        if (!platforms.containsKey(platform)) return false;

        String min = platforms.get(platform);
        if (min == null || min.isBlank() || appVersion == null || appVersion.isBlank()) return true;
        return compareVersions(appVersion, min) >= 0;
    }

    private String resolveFallback(String type, Map<String, WorkflowDirective> manifest, String platform,
                                   String appVersion, List<String> capabilities) {
        Set<String> seen = new HashSet<>();
        seen.add(type);
        String current = type;

        for (int depth = 0; depth < MAX_FALLBACK_DEPTH; depth++) {
            WorkflowDirective d = manifest.get(current);
            String next = d == null ? null : d.getFallback();
            if (next == null || next.isBlank() || seen.contains(next)) return null;
            seen.add(next);
            if (!manifest.containsKey(next)) return next;
            if (isSupported(next, manifest, platform, appVersion, capabilities)) return next;
            current = next;
        }
        return null;
    }

    /** Compare dotted numeric versions (e.g. 2.3.0 vs 2.1). */
    static int compareVersions(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            int va = i < pa.length ? parseIntSafe(pa[i]) : 0;
            int vb = i < pb.length ? parseIntSafe(pb[i]) : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.replaceAll("\\D.*$", "")); } catch (Exception e) { return 0; }
    }
}
