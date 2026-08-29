package org.hashtagcms.workflows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthTest {

    @Autowired
    MockMvc mvc;

    private static final String EXECUTE = "/api/hashtagcms/public/workflows/v1/execute";

    @Test
    void authRequiredWorkflowRejectedWithoutUser() throws Exception {
        mvc.perform(post(EXECUTE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflow\":\"WORKFLOW_WHOAMI\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void authRequiredWorkflowRunsWithForwardedUserHeaders() throws Exception {
        mvc.perform(post(EXECUTE).contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "14")
                        .header("X-User-Email", "sam@example.com")
                        .content("{\"workflow\":\"WORKFLOW_WHOAMI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // {{ user.* }} interpolation from the resolved user
                .andExpect(jsonPath("$.message").value("Hello sam@example.com"))
                .andExpect(jsonPath("$.directives[0].message").value("Signed in as sam@example.com"))
                .andExpect(jsonPath("$.data.userId").value("14"));
    }

    @Test
    void nonAuthWorkflowUnaffected() throws Exception {
        mvc.perform(post(EXECUTE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflow\":\"WORKFLOW_EXAMPLE_DIRECT\",\"payload\":{\"name\":\"Sam\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
