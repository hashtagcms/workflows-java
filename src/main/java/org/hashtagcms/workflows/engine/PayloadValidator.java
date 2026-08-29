package org.hashtagcms.workflows.engine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A Laravel-style validator for JSON payloads. Supports a broad subset of the
 * Laravel rules that make sense for a headless service (no files, database,
 * network, timezone, or HTTP-request-bound rules).
 *
 * Rule strings look like {@code "required|string|min:3"}; parameters follow a
 * colon and are comma-separated ({@code "in:a,b,c"}, {@code "between:1,10"}).
 */
public final class PayloadValidator {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern UUID = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern ULID = Pattern.compile("^[0-9A-HJKMNP-TV-Z]{26}$");
    private static final Pattern ALPHA = Pattern.compile("^[a-zA-Z]+$");
    private static final Pattern ALPHA_NUM = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern ALPHA_DASH = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern ASCII = Pattern.compile("^[\\x00-\\x7F]*$");
    private static final Pattern MAC = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$");
    private static final Pattern HEX_COLOR = Pattern.compile("^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");
    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");

    private PayloadValidator() {}

    public record Result(boolean passes, Map<String, List<String>> errors, String first) {}

    public static Result validate(Map<String, Object> payload, Map<String, Object> rules, Map<String, Object> messages) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        Map<String, Object> input = payload == null ? Map.of() : payload;
        Map<String, Object> msgs = messages == null ? Map.of() : messages;

        for (Map.Entry<String, Object> entry : rules.entrySet()) {
            String field = entry.getKey();
            List<Rule> ruleList = parse(entry.getValue());
            Set<String> names = ruleNames(ruleList);
            boolean numericField = names.contains("integer") || names.contains("numeric") || names.contains("decimal");
            boolean bail = names.contains("bail");

            boolean exists = input.containsKey(field);
            Object value = input.get(field);
            boolean empty = isEmpty(value);

            // --- presence handling ---
            if (names.contains("sometimes") && !exists) continue;

            boolean requiredNow = isRequired(field, ruleList, input);
            if (requiredNow && empty) {
                add(errors, field, message(msgs, field, "required", "The " + field + " field is required."));
                continue;
            }
            if (names.contains("prohibited") && !empty) {
                add(errors, field, message(msgs, field, "prohibited", "The " + field + " field is prohibited."));
                continue;
            }
            if (names.contains("missing") && exists) {
                add(errors, field, message(msgs, field, "missing", "The " + field + " field must be missing."));
                continue;
            }
            if (names.contains("present") && !exists) {
                add(errors, field, message(msgs, field, "present", "The " + field + " field must be present."));
                continue;
            }
            if (names.contains("filled") && exists && empty) {
                add(errors, field, message(msgs, field, "filled", "The " + field + " field must have a value."));
                continue;
            }
            // Absent optional field, or null+nullable → skip the value rules.
            if (!exists && !requiredNow) continue;
            if (empty && names.contains("nullable")) continue;
            if (empty) continue; // nothing more to check on an empty value

            // --- value rules ---
            for (Rule rule : ruleList) {
                String err = check(rule, field, value, numericField, input);
                if (err != null) {
                    add(errors, field, message(msgs, field, rule.name(), err));
                    if (bail) break;
                }
            }
        }

        String first = errors.values().stream().flatMap(List::stream).findFirst().orElse(null);
        return new Result(errors.isEmpty(), errors, first);
    }

    // ----------------------------------------------------------------- rules

    private static String check(Rule rule, String field, Object value, boolean numericField, Map<String, Object> input) {
        String p0 = rule.param(0);
        return switch (rule.name()) {
            case "string" -> value instanceof String ? null : "The " + field + " must be a string.";
            case "integer" -> isInteger(value) ? null : "The " + field + " must be an integer.";
            case "numeric" -> isNumeric(value) ? null : "The " + field + " must be numeric.";
            case "boolean" -> isBoolean(value) ? null : "The " + field + " field must be true or false.";
            case "array" -> value instanceof List || value instanceof Map ? null : "The " + field + " field must be an array.";
            case "list" -> value instanceof List ? null : "The " + field + " field must be a list.";
            case "json" -> isJson(value) ? null : "The " + field + " field must be a valid JSON string.";
            case "accepted" -> in(value, "yes", "on", "1", "true", "1.0") ? null : "The " + field + " must be accepted.";
            case "declined" -> in(value, "no", "off", "0", "false", "0.0") ? null : "The " + field + " must be declined.";

            case "email" -> EMAIL.matcher(str(value)).matches() ? null : "The " + field + " must be a valid email address.";
            case "url" -> isUrl(str(value)) ? null : "The " + field + " must be a valid URL.";
            case "ip" -> isIp(str(value)) ? null : "The " + field + " must be a valid IP address.";
            case "mac_address" -> MAC.matcher(str(value)).matches() ? null : "The " + field + " must be a valid MAC address.";
            case "uuid" -> UUID.matcher(str(value)).matches() ? null : "The " + field + " must be a valid UUID.";
            case "ulid" -> ULID.matcher(str(value)).matches() ? null : "The " + field + " must be a valid ULID.";
            case "hex_color" -> HEX_COLOR.matcher(str(value)).matches() ? null : "The " + field + " must be a valid hex color.";
            case "alpha" -> ALPHA.matcher(str(value)).matches() ? null : "The " + field + " may only contain letters.";
            case "alpha_num" -> ALPHA_NUM.matcher(str(value)).matches() ? null : "The " + field + " may only contain letters and numbers.";
            case "alpha_dash" -> ALPHA_DASH.matcher(str(value)).matches() ? null : "The " + field + " may only contain letters, numbers, dashes and underscores.";
            case "ascii" -> ASCII.matcher(str(value)).matches() ? null : "The " + field + " must only contain ASCII characters.";
            case "lowercase" -> str(value).equals(str(value).toLowerCase()) ? null : "The " + field + " must be lowercase.";
            case "uppercase" -> str(value).equals(str(value).toUpperCase()) ? null : "The " + field + " must be uppercase.";

            case "starts_with" -> anyMatch(str(value), rule.params(), true, true) ? null : "The " + field + " must start with one of: " + String.join(", ", rule.params()) + ".";
            case "ends_with" -> anyMatch(str(value), rule.params(), false, true) ? null : "The " + field + " must end with one of: " + String.join(", ", rule.params()) + ".";
            case "doesnt_start_with" -> anyMatch(str(value), rule.params(), true, false) ? "The " + field + " may not start with one of: " + String.join(", ", rule.params()) + "." : null;
            case "doesnt_end_with" -> anyMatch(str(value), rule.params(), false, false) ? "The " + field + " may not end with one of: " + String.join(", ", rule.params()) + "." : null;
            case "in" -> rule.params().contains(str(value)) ? null : "The selected " + field + " is invalid.";
            case "not_in" -> rule.params().contains(str(value)) ? "The selected " + field + " is invalid." : null;
            case "regex" -> Pattern.compile(rule.rawParam()).matcher(str(value)).find() ? null : "The " + field + " format is invalid.";
            case "not_regex" -> Pattern.compile(rule.rawParam()).matcher(str(value)).find() ? "The " + field + " format is invalid." : null;

            case "confirmed" -> Objects.equals(str(value), str(input.get(field + "_confirmation"))) ? null : "The " + field + " confirmation does not match.";
            case "same" -> Objects.equals(str(value), str(input.get(p0))) ? null : "The " + field + " and " + p0 + " must match.";
            case "different" -> Objects.equals(str(value), str(input.get(p0))) ? "The " + field + " and " + p0 + " must be different." : null;

            case "min" -> sizeOf(value, numericField) >= num(p0) ? null : "The " + field + " must be at least " + p0 + ".";
            case "max" -> sizeOf(value, numericField) <= num(p0) ? null : "The " + field + " may not be greater than " + p0 + ".";
            case "size" -> sizeOf(value, numericField) == num(p0) ? null : "The " + field + " must be " + p0 + ".";
            case "between" -> {
                double s = sizeOf(value, numericField);
                yield (s >= num(p0) && s <= num(rule.param(1))) ? null : "The " + field + " must be between " + p0 + " and " + rule.param(1) + ".";
            }
            case "gt" -> compareField(value, p0, input, numericField) > 0 ? null : "The " + field + " must be greater than " + p0 + ".";
            case "gte" -> compareField(value, p0, input, numericField) >= 0 ? null : "The " + field + " must be greater than or equal to " + p0 + ".";
            case "lt" -> compareField(value, p0, input, numericField) < 0 ? null : "The " + field + " must be less than " + p0 + ".";
            case "lte" -> compareField(value, p0, input, numericField) <= 0 ? null : "The " + field + " must be less than or equal to " + p0 + ".";
            case "digits" -> (str(value).matches("\\d+") && str(value).length() == (int) num(p0)) ? null : "The " + field + " must be " + p0 + " digits.";
            case "digits_between" -> {
                int len = str(value).length();
                yield (str(value).matches("\\d+") && len >= num(p0) && len <= num(rule.param(1))) ? null : "The " + field + " must be between " + p0 + " and " + rule.param(1) + " digits.";
            }
            case "multiple_of" -> (isNumeric(value) && num(p0) != 0 && Double.parseDouble(str(value)) % num(p0) == 0) ? null : "The " + field + " must be a multiple of " + p0 + ".";
            case "decimal" -> decimalOk(str(value), rule.params()) ? null : "The " + field + " must have " + String.join("-", rule.params()) + " decimal places.";

            case "distinct" -> distinct(value) ? null : "The " + field + " field has a duplicate value.";
            case "in_array" -> inArray(value, p0, input) ? null : "The " + field + " field does not exist in " + p0 + ".";
            case "contains" -> value instanceof List<?> l && l.stream().map(PayloadValidator::str).collect(java.util.stream.Collectors.toSet()).containsAll(rule.params()) ? null : "The " + field + " field must contain: " + String.join(", ", rule.params()) + ".";
            case "doesnt_contain" -> value instanceof List<?> l && l.stream().map(PayloadValidator::str).anyMatch(rule.params()::contains) ? "The " + field + " field must not contain: " + String.join(", ", rule.params()) + "." : null;

            case "date" -> parseDate(str(value)) != null ? null : "The " + field + " is not a valid date.";
            case "date_format" -> dateFormatOk(str(value), p0) ? null : "The " + field + " does not match the format " + p0 + ".";
            case "after" -> dateCompare(value, p0, input) > 0 ? null : "The " + field + " must be a date after " + p0 + ".";
            case "after_or_equal" -> dateCompare(value, p0, input) >= 0 ? null : "The " + field + " must be a date after or equal to " + p0 + ".";
            case "before" -> dateCompare(value, p0, input) < 0 ? null : "The " + field + " must be a date before " + p0 + ".";
            case "before_or_equal" -> dateCompare(value, p0, input) <= 0 ? null : "The " + field + " must be a date before or equal to " + p0 + ".";
            case "date_equals" -> dateCompare(value, p0, input) == 0 ? null : "The " + field + " must be a date equal to " + p0 + ".";

            default -> null; // presence/utility rules handled above, or unsupported → ignored
        };
    }

    // ------------------------------------------------------------- presence

    private static boolean isRequired(String field, List<Rule> rules, Map<String, Object> input) {
        for (Rule r : rules) {
            switch (r.name()) {
                case "required" -> { return true; }
                case "required_if" -> { if (Objects.equals(str(input.get(r.param(0))), r.param(1))) return true; }
                case "required_unless" -> { if (!Objects.equals(str(input.get(r.param(0))), r.param(1))) return true; }
                case "required_with" -> { if (r.params().stream().anyMatch(f -> !isEmpty(input.get(f)))) return true; }
                case "required_with_all" -> { if (r.params().stream().allMatch(f -> !isEmpty(input.get(f)))) return true; }
                case "required_without" -> { if (r.params().stream().anyMatch(f -> isEmpty(input.get(f)))) return true; }
                case "required_without_all" -> { if (r.params().stream().allMatch(f -> isEmpty(input.get(f)))) return true; }
                default -> { /* not a required rule */ }
            }
        }
        return false;
    }

    // ------------------------------------------------------------- helpers

    private record Rule(String name, List<String> params, String rawParam) {
        String param(int i) { return i < params.size() ? params.get(i) : ""; }
    }

    private static List<Rule> parse(Object rules) {
        List<String> tokens = new ArrayList<>();
        if (rules instanceof List<?> list) {
            for (Object o : list) tokens.add(String.valueOf(o));
        } else {
            tokens.addAll(Arrays.asList(String.valueOf(rules).split("\\|")));
        }
        List<Rule> out = new ArrayList<>();
        for (String t : tokens) {
            t = t.trim();
            if (t.isEmpty()) continue;
            int c = t.indexOf(':');
            if (c < 0) {
                out.add(new Rule(t, List.of(), ""));
            } else {
                String name = t.substring(0, c);
                String raw = t.substring(c + 1);
                out.add(new Rule(name, Arrays.stream(raw.split(",")).map(String::trim).toList(), raw));
            }
        }
        return out;
    }

    private static Set<String> ruleNames(List<Rule> rules) {
        Set<String> s = new HashSet<>();
        for (Rule r : rules) s.add(r.name());
        return s;
    }

    private static void add(Map<String, List<String>> errors, String field, String msg) {
        errors.computeIfAbsent(field, k -> new ArrayList<>()).add(msg);
    }

    private static String message(Map<String, Object> msgs, String field, String rule, String def) {
        Object custom = msgs.get(field + "." + rule);
        return custom != null ? custom.toString() : def;
    }

    private static boolean isEmpty(Object v) {
        return v == null
                || (v instanceof String s && s.isEmpty())
                || (v instanceof Collection<?> c && c.isEmpty())
                || (v instanceof Map<?, ?> m && m.isEmpty());
    }

    private static String str(Object v) { return v == null ? "" : String.valueOf(v); }

    private static boolean in(Object v, String... allowed) {
        String s = str(v).toLowerCase();
        for (String a : allowed) if (a.equals(s)) return true;
        return false;
    }

    private static boolean isInteger(Object v) {
        if (v instanceof Integer || v instanceof Long) return true;
        try { Long.parseLong(str(v)); return true; } catch (Exception e) { return false; }
    }

    private static boolean isNumeric(Object v) {
        if (v instanceof Number) return true;
        try { Double.parseDouble(str(v)); return true; } catch (Exception e) { return false; }
    }

    private static boolean isBoolean(Object v) {
        return v instanceof Boolean || in(v, "true", "false", "1", "0");
    }

    private static boolean isJson(Object v) {
        if (!(v instanceof String s)) return false;
        try { new com.fasterxml.jackson.databind.ObjectMapper().readTree(s); return true; } catch (Exception e) { return false; }
    }

    private static boolean isUrl(String s) {
        try { new java.net.URI(s).toURL(); return s.matches("^https?://.+"); } catch (Exception e) { return false; }
    }

    private static boolean isIp(String s) {
        if (IPV4.matcher(s).matches()) {
            for (String part : s.split("\\.")) { int n = Integer.parseInt(part); if (n < 0 || n > 255) return false; }
            return true;
        }
        return s.contains(":"); // loose IPv6
    }

    private static boolean anyMatch(String value, List<String> needles, boolean start, boolean expected) {
        boolean matched = needles.stream().anyMatch(n -> start ? value.startsWith(n) : value.endsWith(n));
        return matched == expected;
    }

    private static double num(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private static double sizeOf(Object v, boolean numericField) {
        if (v instanceof Number n) return n.doubleValue();
        if (numericField && v instanceof String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0; } }
        if (v instanceof String s) return s.length();
        if (v instanceof Collection<?> c) return c.size();
        if (v instanceof Map<?, ?> m) return m.size();
        return 0;
    }

    /** Compare a value against a numeric literal or another field's value. */
    private static int compareField(Object value, String param, Map<String, Object> input, boolean numericField) {
        double a = sizeOf(value, numericField);
        double b = input.containsKey(param) ? sizeOf(input.get(param), true) : num(param);
        return Double.compare(a, b);
    }

    private static boolean decimalOk(String s, List<String> params) {
        int dot = s.indexOf('.');
        int places = dot < 0 ? 0 : s.length() - dot - 1;
        if (params.size() == 1) return places == (int) num(params.get(0));
        if (params.size() >= 2) return places >= num(params.get(0)) && places <= num(params.get(1));
        return places > 0;
    }

    private static boolean distinct(Object v) {
        if (!(v instanceof List<?> list)) return true;
        Set<String> seen = new HashSet<>();
        for (Object o : list) if (!seen.add(str(o))) return false;
        return true;
    }

    private static boolean inArray(Object value, String param, Map<String, Object> input) {
        String key = param.endsWith(".*") ? param.substring(0, param.length() - 2) : param;
        Object target = input.get(key);
        if (target instanceof List<?> list) return list.stream().map(PayloadValidator::str).anyMatch(s -> s.equals(str(value)));
        return false;
    }

    private static java.time.temporal.Temporal parseDate(String s) {
        for (var p : List.of("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd")) {
            try {
                DateTimeFormatter f = DateTimeFormatter.ofPattern(p);
                if (p.contains("X")) return OffsetDateTime.parse(s, f);
                if (p.contains("H")) return LocalDateTime.parse(s, f);
                return LocalDate.parse(s, f);
            } catch (Exception ignored) { }
        }
        return null;
    }

    private static boolean dateFormatOk(String value, String pattern) {
        try { DateTimeFormatter.ofPattern(pattern).parse(value); return true; } catch (Exception e) { return false; }
    }

    /** Compare two dates as epoch-days/seconds; the param may be a literal or a field. */
    private static long dateCompare(Object value, String param, Map<String, Object> input) {
        LocalDate a = toDate(str(value));
        LocalDate b = toDate(input.containsKey(param) ? str(input.get(param)) : param);
        if (a == null || b == null) return Long.MIN_VALUE; // fail comparisons on unparseable input
        return a.toEpochDay() - b.toEpochDay();
    }

    private static LocalDate toDate(String s) {
        var t = parseDate(s);
        if (t instanceof LocalDate d) return d;
        if (t instanceof LocalDateTime dt) return dt.toLocalDate();
        if (t instanceof OffsetDateTime odt) return odt.toLocalDate();
        return null;
    }
}
