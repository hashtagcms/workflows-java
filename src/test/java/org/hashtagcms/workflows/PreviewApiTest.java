package org.hashtagcms.workflows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** POST {prefix}/admin/workflows/preview — dry-run a config with no persistence. */
@SpringBootTest
@AutoConfigureMockMvc
class PreviewApiTest {

    @Autowired
    MockMvc mvc;

    private static final String PREVIEW = "/api/hashtagcms/admin/workflows/preview";
    private static final String CATALOG = "/api/hashtagcms/public/workflows/v1/catalog";

    @Test
    void previewInterpolatesAndNegotiatesWithoutPersisting() throws Exception {
        String body = """
            { "config": {
                "version": "1.0",
                "target": { "type": "none" },
                "on_success": {
                  "message": "Hi {{ payload.name }}",
                  "directives": [
                    { "type": "toast", "level": "success", "message": "Hi {{ payload.name }}" },
                    { "type": "haptic", "intensity": "success" }
                  ] } },
              "payload": { "name": "Ann" },
              "platform": "web", "app_version": "1.0.0" }
            """;

        mvc.perform(post(PREVIEW).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // interpolation
                .andExpect(jsonPath("$.message").value("Hi Ann"))
                .andExpect(jsonPath("$.directives[0].message").value("Hi Ann"))
                // negotiation: native-only haptic dropped on web
                .andExpect(jsonPath("$.directives[?(@.type=='haptic')]", hasSize(0)));

        // ...and nothing was written: no WORKFLOW_PREVIEW row appears in the catalog.
        mvc.perform(get(CATALOG).param("site_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflows[?(@.alias=='WORKFLOW_PREVIEW')]", hasSize(0)));
    }

    @Test
    void previewRunsValidationAndFailsOnMissingInput() throws Exception {
        String body = """
            { "config": {
                "validation": { "rules": { "code": "required|string" } },
                "target": { "type": "none" },
                "on_success": { "directives": [ { "type": "toast", "message": "ok" } ] } },
              "payload": {},
              "platform": "web" }
            """;

        mvc.perform(post(PREVIEW).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }
}
