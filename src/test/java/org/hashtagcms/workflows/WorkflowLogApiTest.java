package org.hashtagcms.workflows;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** GET/DELETE {prefix}/admin/logs — the audit-log read API. */
@SpringBootTest
@AutoConfigureMockMvc
class WorkflowLogApiTest {

    @Autowired
    MockMvc mvc;

    private static final String EXECUTE = "/api/hashtagcms/public/workflows/v1/execute";
    private static final String LOGS = "/api/hashtagcms/admin/logs";

    /** Run a workflow so at least one log row exists. */
    private void executeOnce() throws Exception {
        String body = """
            { "workflow": "WORKFLOW_EXAMPLE_DIRECT",
              "payload": { "name": "Logger" },
              "client": { "platform": "web", "app_version": "1.0.0" } }
            """;
        mvc.perform(post(EXECUTE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void listReturnsExecutionLogsNewestFirst() throws Exception {
        executeOnce();
        mvc.perform(get(LOGS).param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[?(@.workflowAlias=='WORKFLOW_EXAMPLE_DIRECT')]", not(empty())));
    }

    @Test
    void listCanFilterByAlias() throws Exception {
        executeOnce();
        // Every returned row must be for the requested alias (none of another alias present).
        mvc.perform(get(LOGS).param("alias", "WORKFLOW_EXAMPLE_DIRECT").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.workflowAlias!='WORKFLOW_EXAMPLE_DIRECT')]", empty()));
    }

    @Test
    void getByIdReturnsTheLogThenDeleteRemovesIt() throws Exception {
        executeOnce();

        String json = mvc.perform(get(LOGS).param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int id = ((Number) JsonPath.read(json, "$.content[0].id")).intValue();

        mvc.perform(get(LOGS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.workflowAlias", not(emptyOrNullString())));

        mvc.perform(delete(LOGS + "/" + id))
                .andExpect(status().isNoContent());

        mvc.perform(get(LOGS + "/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMissingLogReturns404() throws Exception {
        mvc.perform(get(LOGS + "/99999999"))
                .andExpect(status().isNotFound());
    }
}
