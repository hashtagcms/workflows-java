package org.hashtagcms.workflows.service;

import org.hashtagcms.workflows.config.WorkflowProperties;
import org.hashtagcms.workflows.model.Workflow;
import org.hashtagcms.workflows.model.WorkflowDirective;
import org.hashtagcms.workflows.repository.WorkflowDirectiveRepository;
import org.hashtagcms.workflows.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Seeds the directive manifest and bundled example workflows (idempotent). */
@Service
public class SeedService {

    private final WorkflowDirectiveRepository directives;
    private final WorkflowRepository workflows;
    private final WorkflowProperties properties;

    public SeedService(WorkflowDirectiveRepository directives, WorkflowRepository workflows, WorkflowProperties properties) {
        this.directives = directives;
        this.workflows = workflows;
        this.properties = properties;
    }

    @Transactional
    public int seedDirectives() {
        long site = properties.getMasterSiteId();
        int n = 0;
        for (DirectiveManifest.DirectiveDef def : DirectiveManifest.core()) {
            WorkflowDirective d = directives.findBySiteIdAndType(site, def.type()).orElseGet(WorkflowDirective::new);
            d.setSiteId(site);
            d.setType(def.type());
            d.setLabel(def.label());
            d.setCategory(def.category());
            d.setDescription(def.description());
            d.setPlatforms(def.platforms());
            d.setSchema(def.schema());
            d.setFallback(def.fallback());
            d.setCore(true);
            d.setPublishStatus(true);
            directives.save(d);
            n++;
        }
        return n;
    }

    @Transactional
    public int seedExamples() {
        int n = 0;
        n += upsert("WORKFLOW_LOAD_PHOTOS", "Load Photos",
                "Fetches a list of photos from picsum.photos and returns them in `data`.", loadPhotos(), false);
        n += upsert("WORKFLOW_EXAMPLE_DIRECT", "Example: Direct Directives",
                "No external call — returns client directives directly.", directDirectives(), false);
        n += upsert("WORKFLOW_BUILDER_DEMO", "Builder Demo — Apply Coupon",
                "Validation + HTTP target + interpolated data + multi-category directives.", builderDemo(), false);
        n += upsert("WORKFLOW_WHOAMI", "Who Am I",
                "Auth-required example that greets the authenticated user via {{ user.* }}.", whoami(), true);
        return n;
    }

    private int upsert(String alias, String name, String description, Map<String, Object> config, boolean authRequired) {
        Workflow wf = workflows.findBySiteIdAndAlias(1L, alias).orElseGet(Workflow::new);
        wf.setSiteId(1L);
        wf.setAlias(alias);
        wf.setName(name);
        wf.setDescription(description);
        wf.setAuthRequired(authRequired);
        wf.setPublishStatus(true);
        wf.setConfig(config);
        workflows.save(wf);
        return 1;
    }

    private Map<String, Object> whoami() {
        return map(
            "version", "1.0",
            "target", map("type", "none"),
            "on_success", map(
                "message", "Hello {{ user.email | default: 'guest' }}",
                "data", map("userId", "{{ user.id }}"),
                "directives", list(
                    map("type", "toast", "level", "success", "message", "Signed in as {{ user.email }}")
                ))
        );
    }

    // ---- example configs (flat directive envelope) ----

    private Map<String, Object> loadPhotos() {
        return map(
            "version", "1.0",
            "target", map("type", "http", "method", "GET", "url", "https://picsum.photos/v2/list",
                "headers", map("Accept", "application/json"), "timeout", 15),
            "on_success", map(
                "message", "Photos loaded.",
                "data", map("photos", "{{ response.body }}"),
                "directives", list(
                    map("type", "render_photos", "action", "render", "items", "{{ response.body }}"),
                    map("type", "toast", "level", "success", "message", "Photos loaded successfully.")
                )),
            "on_failure", map("directives", list(
                map("type", "toast", "level", "error", "message", "Could not load photos.")
            ))
        );
    }

    private Map<String, Object> directDirectives() {
        return map(
            "version", "1.0",
            "target", map("type", "none"),
            "on_success", map(
                "message", "Welcome, {{ payload.name | default: 'guest' }}!",
                "directives", list(
                    map("type", "toast", "level", "success", "message", "Welcome, {{ payload.name | default: 'guest' }}!"),
                    map("type", "navigate", "target", "/dashboard", "params", map("ref", "{{ payload.name | default: 'guest' }}")),
                    map("type", "haptic", "intensity", "success")
                ))
        );
    }

    private Map<String, Object> builderDemo() {
        return map(
            "version", "1.0",
            "validation", map("rules", map("code", "required|string", "quantity", "required|integer|min:1")),
            "target", map("type", "http", "method", "GET", "url", "https://picsum.photos/v2/list",
                "query", map("page", "{{ payload.page | default: 1 }}"), "timeout", 10),
            "on_success", map(
                "message", "Applied {{ payload.code }}",
                "data", map("photos", "{{ response.body }}", "appliedCode", "{{ payload.code }}"),
                "directives", list(
                    map("type", "toast", "level", "success", "message", "Coupon {{ payload.code }} applied!"),
                    map("type", "mutate_cart", "action", "apply_coupon", "couponCode", "{{ payload.code }}", "discountPercent", 10),
                    map("type", "navigate", "target", "cart", "params", map()),
                    map("type", "haptic", "intensity", "success")
                )),
            "on_failure", map("directives", list(
                map("type", "toast", "level", "error", "message", "Could not apply {{ payload.code }}"),
                map("type", "haptic", "intensity", "error")
            ))
        );
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static List<Object> list(Object... items) {
        return new ArrayList<>(Arrays.asList(items));
    }
}
