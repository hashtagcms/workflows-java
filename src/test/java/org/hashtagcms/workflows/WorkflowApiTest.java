package org.hashtagcms.workflows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WorkflowApiTest {

    @Autowired
    MockMvc mvc;

    private static final String BASE = "/api/hashtagcms/public/workflows/v1";

    @Test
    void healthReportsOk() throws Exception {
        mvc.perform(get(BASE + "/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void directivesManifestIsSeeded() throws Exception {
        mvc.perform(get(BASE + "/directives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.directives", hasSize(72)));
    }

    @Test
    void directivesAreFilteredPerPlatform() throws Exception {
        // haptic is native-only, so it must be absent from the web catalogue.
        mvc.perform(get(BASE + "/directives").param("platform", "web").param("app_version", "9.9.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directives[?(@.type=='haptic')]", hasSize(0)))
                .andExpect(jsonPath("$.directives[?(@.type=='toast')]", hasSize(1)));
    }

    @Test
    void executeInterpolatesAndNegotiatesOnWeb() throws Exception {
        String body = """
            { "workflow": "WORKFLOW_EXAMPLE_DIRECT",
              "payload": { "name": "Sam" },
              "client": { "platform": "web", "app_version": "1.0.0" } }
            """;

        mvc.perform(post(BASE + "/execute").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // interpolation
                .andExpect(jsonPath("$.directives[0].message").value("Welcome, Sam!"))
                // negotiation: native-only haptic dropped on web
                .andExpect(jsonPath("$.directives[?(@.type=='haptic')]", hasSize(0)));
    }

    @Test
    void missingWorkflowReturns400() throws Exception {
        mvc.perform(post(BASE + "/execute").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
