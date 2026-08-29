package org.hashtagcms.workflows;

import org.hashtagcms.workflows.engine.VariableInterpolator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class VariableInterpolatorTest {

    private Map<String, Object> ctx() {
        return Map.of(
                "payload", Map.of("name", "Sam", "code", "DOM50"),
                "response", Map.of("body", List.of("a", "b", "c")),
                "user", Map.of("id", 14));
    }

    @Test
    void substitutesTokensInStrings() {
        assertThat(VariableInterpolator.interpolate("Hi {{ payload.name }}!", ctx())).isEqualTo("Hi Sam!");
    }

    @Test
    void wholeTokenReturnsTheActualValue() {
        Object out = VariableInterpolator.interpolate("{{ response.body }}", ctx());
        assertThat(out).isInstanceOf(List.class);
        assertThat((List<Object>) out).containsExactly("a", "b", "c");
    }

    @Test
    void defaultFilterAppliesWhenMissing() {
        assertThat(VariableInterpolator.interpolate("{{ payload.missing | default: 'guest' }}", ctx())).isEqualTo("guest");
    }

    @Test
    void envUsesTheConfiguredResolver() {
        VariableInterpolator.setEnvResolver(k -> "API_TOKEN".equals(k) ? "secret-123" : null);
        try {
            assertThat(VariableInterpolator.interpolate("Bearer {{ env.API_TOKEN }}", ctx())).isEqualTo("Bearer secret-123");
        } finally {
            VariableInterpolator.setEnvResolver(System::getenv);
        }
    }

    @Test
    void recursesIntoMapsAndLists() {
        Object out = VariableInterpolator.interpolate(
                Map.of("greet", "Hi {{ payload.name }}", "items", List.of("{{ payload.code }}")), ctx());
        assertThat(out).isInstanceOf(Map.class);
        Map<?, ?> m = (Map<?, ?>) out;
        assertThat(m.get("greet")).isEqualTo("Hi Sam");
        assertThat((List<Object>) m.get("items")).containsExactly("DOM50");
    }
}
