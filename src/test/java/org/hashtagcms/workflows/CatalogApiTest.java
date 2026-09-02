package org.hashtagcms.workflows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** GET {prefix}/public/workflows/v1/catalog — the introspected workflow contract. */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogApiTest {

    @Autowired
    MockMvc mvc;

    private static final String CATALOG = "/api/hashtagcms/public/workflows/v1/catalog";

    @Test
    void catalogListsSeededWorkflowsOrderedByAlias() throws Exception {
        // Seeded aliases sort as: BUILDER_DEMO, EXAMPLE_DIRECT, LOAD_PHOTOS, WHOAMI.
        mvc.perform(get(CATALOG).param("site_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.site_id").value(1))
                .andExpect(jsonPath("$.workflows[0].alias").value("WORKFLOW_BUILDER_DEMO"));
    }

    @Test
    void catalogDerivesInputsAndEmittedDirectiveTypes() throws Exception {
        // BUILDER_DEMO: validation rules -> inputs; on_success/on_failure directives -> emits.
        mvc.perform(get(CATALOG).param("site_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflows[0].inputs.code.required").value(true))
                .andExpect(jsonPath("$.workflows[0].inputs.code.rule").value("required|string"))
                .andExpect(jsonPath("$.workflows[0].inputs.quantity.rule").value("required|integer|min:1"))
                .andExpect(jsonPath("$.workflows[0].emits", hasItems("toast", "mutate_cart", "navigate", "haptic")))
                .andExpect(jsonPath("$.workflows[0].target").value("http"));
    }

    @Test
    void catalogReportsAuthRequiredFlag() throws Exception {
        mvc.perform(get(CATALOG).param("site_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflows[?(@.alias=='WORKFLOW_WHOAMI')].auth_required", hasItem(true)));
    }

    @Test
    void catalogForUnknownSiteIsEmpty() throws Exception {
        mvc.perform(get(CATALOG).param("site_id", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.workflows", hasSize(0)));
    }
}
