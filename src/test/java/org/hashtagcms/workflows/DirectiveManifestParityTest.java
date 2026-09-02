package org.hashtagcms.workflows;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hashtagcms.workflows.service.DirectiveManifest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against the Java port drifting from the reference PHP implementation's
 * directive manifest. The fixture {@code php-directive-manifest.json} is generated
 * from PHP's {@code DirectiveManifest::core()} by {@code scripts/dump-php-manifest.php};
 * regenerate it whenever the PHP manifest changes. If the Java
 * {@link DirectiveManifest} diverges (a directive added/removed/renamed, or its
 * category / per-platform support / fallback changed), this test fails and names
 * exactly what drifted.
 */
class DirectiveManifestParityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void javaManifestMatchesPhpReference() throws Exception {
        Map<String, Map<String, Object>> php = byType(loadPhpFixture());
        Map<String, Map<String, Object>> java = byType(javaManifestAsMaps());

        // 1. Same set of directive types — catches added / removed / renamed directives.
        assertThat(java.keySet())
                .as("directive types (Java vs PHP reference)")
                .isEqualTo(php.keySet());

        // 2. Field-by-field per directive — catches category / platform / fallback / label drift.
        for (String type : php.keySet()) {
            assertThat(java.get(type))
                    .as("directive '%s' drifted from the PHP reference", type)
                    .isEqualTo(php.get(type));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadPhpFixture() throws Exception {
        try (var in = getClass().getResourceAsStream("/php-directive-manifest.json")) {
            assertThat(in).as("fixture /php-directive-manifest.json on the test classpath").isNotNull();
            return MAPPER.readValue(in, List.class);
        }
    }

    /** Project the Java manifest into the same canonical shape the fixture uses. */
    private List<Map<String, Object>> javaManifestAsMaps() {
        return DirectiveManifest.core().stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", d.type());
            m.put("label", d.label());
            m.put("category", d.category());
            m.put("description", d.description());
            m.put("platforms", d.platforms()); // null when unrestricted, matching the fixture
            m.put("fallback", d.fallback());
            return m;
        }).collect(Collectors.toList());
    }

    private Map<String, Map<String, Object>> byType(List<Map<String, Object>> defs) {
        Map<String, Map<String, Object>> out = new TreeMap<>();
        for (Map<String, Object> d : defs) {
            out.put(String.valueOf(d.get("type")), new LinkedHashMap<>(d));
        }
        return out;
    }
}
