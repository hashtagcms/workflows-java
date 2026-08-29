package org.hashtagcms.workflows;

import org.hashtagcms.workflows.engine.VariableInterpolator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Proves {@code {{ env.* }}} resolves through Spring's Environment (like Laravel env()). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "demo.token=abc123")
class EnvInterpolationTest {

    @Test
    void envResolvesSpringProperty() {
        assertThat(VariableInterpolator.interpolate("Bearer {{ env.demo.token }}", Map.of()))
                .isEqualTo("Bearer abc123");
    }

    @Test
    void envSupportsDefaultFilterWhenMissing() {
        assertThat(VariableInterpolator.interpolate("{{ env.missing.key | default: 'fallback' }}", Map.of()))
                .isEqualTo("fallback");
    }
}
