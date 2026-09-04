package org.hashtagcms.workflows.web;

import org.hashtagcms.workflows.config.WorkflowProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Serves a small human-friendly landing page at {@code /} (instead of Spring
 * Boot's Whitelabel Error Page) plus an in-app renderer for the bundled Markdown
 * docs. This is a headless API service, so the site is purely a signpost to the
 * live endpoints and the documentation.
 *
 * <p><strong>Off by default.</strong> Because this library is embedded in other
 * Spring Boot apps, the site is gated behind
 * {@code hashtagcms.workflows.docs.enabled} — a consumer app never has its
 * {@code /} route claimed unless it opts in. The standalone runner / Docker image
 * turn it on in their {@code application.yml}. Assets live under
 * {@code classpath:/workflows-site/} (never {@code static/}), so nothing is
 * auto-served to consumers.
 */
@Controller
@ConditionalOnProperty(prefix = "hashtagcms.workflows.docs", name = "enabled", havingValue = "true")
public class WorkflowsSiteController {

    /** Doc filenames are whitelisted to plain basenames ending in .md (no path traversal). */
    private static final Pattern DOC_NAME = Pattern.compile("^[A-Za-z0-9._-]+\\.md$");

    private final WorkflowProperties properties;

    public WorkflowsSiteController(WorkflowProperties properties) {
        this.properties = properties;
    }

    /** Landing page. {@code {{ROUTE_PREFIX}}} is substituted with the configured API prefix. */
    @GetMapping(value = {"/", "/index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> index() throws IOException {
        String html = read("workflows-site/index.html")
                .replace("{{ROUTE_PREFIX}}", properties.getRoutePrefix());
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    /** The Markdown docs viewer (renders bundled docs client-side). */
    @GetMapping(value = "/docs.html", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> docsViewer() throws IOException {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(read("workflows-site/docs.html"));
    }

    /** Raw bundled Markdown, fetched by the viewer. Filename is whitelisted. */
    @GetMapping(value = "/docs/{name}")
    @ResponseBody
    public ResponseEntity<byte[]> doc(@PathVariable String name) throws IOException {
        if (!DOC_NAME.matcher(name).matches()) {
            return ResponseEntity.notFound().build();
        }
        ClassPathResource res = new ClassPathResource("workflows-site/docs/" + name);
        if (!res.exists()) {
            return ResponseEntity.notFound().build();
        }
        byte[] body = StreamUtils.copyToByteArray(res.getInputStream());
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(body);
    }

    /** Vendored viewer asset (the Markdown renderer). */
    @GetMapping(value = "/site-assets/marked.min.js")
    @ResponseBody
    public ResponseEntity<byte[]> markedJs() throws IOException {
        ClassPathResource res = new ClassPathResource("workflows-site/assets/marked.min.js");
        if (!res.exists()) {
            return ResponseEntity.notFound().build();
        }
        byte[] body = StreamUtils.copyToByteArray(res.getInputStream());
        return ResponseEntity.ok()
                .contentType(new MediaType("application", "javascript", StandardCharsets.UTF_8))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .body(body);
    }

    private String read(String classpathLocation) throws IOException {
        ClassPathResource res = new ClassPathResource(classpathLocation);
        return StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
    }
}
