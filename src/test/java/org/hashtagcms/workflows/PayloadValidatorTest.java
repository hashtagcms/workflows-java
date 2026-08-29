package org.hashtagcms.workflows;

import org.hashtagcms.workflows.engine.PayloadValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadValidatorTest {

    private boolean passes(Map<String, Object> payload, Map<String, Object> rules) {
        return PayloadValidator.validate(payload, rules, Map.of()).passes();
    }

    @Test
    void requiredAndSometimes() {
        assertThat(passes(Map.of(), Map.of("code", "required"))).isFalse();
        assertThat(passes(Map.of("code", "X"), Map.of("code", "required"))).isTrue();
        // absent optional field is fine
        assertThat(passes(Map.of(), Map.of("code", "sometimes|string|min:3"))).isTrue();
    }

    @Test
    void stringRules() {
        assertThat(passes(Map.of("e", "a@b.com"), Map.of("e", "email"))).isTrue();
        assertThat(passes(Map.of("e", "nope"), Map.of("e", "email"))).isFalse();
        assertThat(passes(Map.of("u", "https://x.com/y"), Map.of("u", "url"))).isTrue();
        assertThat(passes(Map.of("s", "abc"), Map.of("s", "alpha"))).isTrue();
        assertThat(passes(Map.of("s", "ab3"), Map.of("s", "alpha"))).isFalse();
        assertThat(passes(Map.of("s", "a-b_1"), Map.of("s", "alpha_dash"))).isTrue();
        assertThat(passes(Map.of("c", "#fff"), Map.of("c", "hex_color"))).isTrue();
        assertThat(passes(Map.of("id", "018f4d0e-9d7a-7c1b-9b0a-2f2b8b9c1234"), Map.of("id", "uuid"))).isTrue();
        assertThat(passes(Map.of("s", "WORKFLOW_X"), Map.of("s", "starts_with:WORKFLOW_"))).isTrue();
        assertThat(passes(Map.of("s", "ORDER_1"), Map.of("s", "starts_with:WORKFLOW_"))).isFalse();
    }

    @Test
    void inAndNotIn() {
        assertThat(passes(Map.of("level", "success"), Map.of("level", "in:success,error,info"))).isTrue();
        assertThat(passes(Map.of("level", "boom"), Map.of("level", "in:success,error,info"))).isFalse();
        assertThat(passes(Map.of("x", "a"), Map.of("x", "not_in:a,b"))).isFalse();
    }

    @Test
    void numericSizes() {
        assertThat(passes(Map.of("q", 5), Map.of("q", "integer|min:1|max:10"))).isTrue();
        assertThat(passes(Map.of("q", 0), Map.of("q", "integer|min:1"))).isFalse();
        assertThat(passes(Map.of("q", 5), Map.of("q", "integer|between:1,10"))).isTrue();
        assertThat(passes(Map.of("q", 4), Map.of("q", "numeric|multiple_of:2"))).isTrue();
        assertThat(passes(Map.of("q", 5), Map.of("q", "numeric|multiple_of:2"))).isFalse();
        assertThat(passes(Map.of("pin", "1234"), Map.of("pin", "digits:4"))).isTrue();
        assertThat(passes(Map.of("pin", "123"), Map.of("pin", "digits:4"))).isFalse();
        // string min is length, not numeric value
        assertThat(passes(Map.of("name", "ab"), Map.of("name", "string|min:3"))).isFalse();
        assertThat(passes(Map.of("name", "abcd"), Map.of("name", "string|min:3"))).isTrue();
    }

    @Test
    void crossFieldRules() {
        assertThat(passes(Map.of("a", "x", "a_confirmation", "x"), Map.of("a", "confirmed"))).isTrue();
        assertThat(passes(Map.of("a", "x", "a_confirmation", "y"), Map.of("a", "confirmed"))).isFalse();
        assertThat(passes(Map.of("a", "1", "b", "1"), Map.of("a", "same:b"))).isTrue();
        assertThat(passes(Map.of("a", "1", "b", "2"), Map.of("a", "different:b"))).isTrue();
        // required_if
        assertThat(passes(Map.of("type", "card"), Map.of("card", "required_if:type,card"))).isFalse();
        assertThat(passes(Map.of("type", "cash"), Map.of("card", "required_if:type,card"))).isTrue();
    }

    @Test
    void booleansAndArrays() {
        assertThat(passes(Map.of("agree", "yes"), Map.of("agree", "accepted"))).isTrue();
        assertThat(passes(Map.of("agree", "no"), Map.of("agree", "accepted"))).isFalse();
        assertThat(passes(Map.of("tags", List.of("a", "b")), Map.of("tags", "array|distinct"))).isTrue();
        assertThat(passes(Map.of("tags", List.of("a", "a")), Map.of("tags", "array|distinct"))).isFalse();
        assertThat(passes(Map.of("tags", List.of("a", "b")), Map.of("tags", "list|min:1|max:5"))).isTrue();
    }

    @Test
    void dateRules() {
        assertThat(passes(Map.of("d", "2026-01-15"), Map.of("d", "date"))).isTrue();
        assertThat(passes(Map.of("d", "nope"), Map.of("d", "date"))).isFalse();
        assertThat(passes(Map.of("d", "2026-06-01"), Map.of("d", "date|after:2026-01-01"))).isTrue();
        assertThat(passes(Map.of("d", "2025-06-01"), Map.of("d", "date|after:2026-01-01"))).isFalse();
    }

    @Test
    void nullableSkipsValueRules() {
        assertThat(passes(Map.of("note", ""), Map.of("note", "nullable|string|min:5"))).isTrue();
    }
}
