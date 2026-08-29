package org.hashtagcms.workflows.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code {{ path }}} tokens (with an optional {@code | default: 'x'}
 * filter) recursively over strings, maps and lists — mirroring the PHP engine.
 *
 * If a string is exactly one token, the resolved VALUE is returned (so
 * {@code "{{ response.body }}"} becomes the actual list/map). Otherwise tokens
 * are substituted as strings.
 */
public final class VariableInterpolator {

    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*(.*?)\\s*}}");
    private static final Pattern WHOLE = Pattern.compile("^\\{\\{\\s*(.*?)\\s*}}$");

    /**
     * Resolver for {@code {{ env.KEY }}} tokens — the Laravel {@code env()}
     * analogue. Defaults to OS environment variables; a Spring config swaps in
     * the full {@code Environment} (application.yml + system properties + env).
     */
    private static Function<String, String> envResolver = System::getenv;

    private VariableInterpolator() {}

    public static void setEnvResolver(Function<String, String> resolver) {
        envResolver = resolver != null ? resolver : System::getenv;
    }

    @SuppressWarnings("unchecked")
    public static Object interpolate(Object template, Map<String, Object> context) {
        if (template instanceof String s) {
            return interpolateString(s, context);
        }
        if (template instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(e.getKey()), interpolate(e.getValue(), context));
            }
            return out;
        }
        if (template instanceof List<?> list) {
            List<Object> out = new ArrayList<>();
            for (Object item : list) out.add(interpolate(item, context));
            return out;
        }
        return template;
    }

    private static Object interpolateString(String s, Map<String, Object> context) {
        Matcher whole = WHOLE.matcher(s);
        if (whole.matches()) {
            return resolveExpression(whole.group(1), context);
        }
        Matcher m = TOKEN.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Object value = resolveExpression(m.group(1), context);
            m.appendReplacement(sb, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static Object resolveExpression(String expr, Map<String, Object> context) {
        String[] parts = expr.split("\\|");
        String path = parts[0].trim();
        Object value = resolvePath(path, context);

        for (int i = 1; i < parts.length; i++) {
            String filter = parts[i].trim();
            if (filter.startsWith("default:")) {
                if (isEmpty(value)) {
                    value = stripQuotes(filter.substring("default:".length()).trim());
                }
            }
        }
        return value;
    }

    private static Object resolvePath(String path, Map<String, Object> context) {
        String[] segs = path.split("\\.");
        if (segs.length == 0) return null;

        if ("env".equals(segs[0])) {
            String key = path.substring(4); // after "env."
            return envResolver.apply(key);
        }

        Object current = context;
        for (String seg : segs) {
            if (current == null) return null;
            if (current instanceof Map<?, ?> map) {
                current = map.get(seg);
            } else if (current instanceof List<?> list) {
                try {
                    int idx = Integer.parseInt(seg);
                    current = (idx >= 0 && idx < list.size()) ? list.get(idx) : null;
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return current;
    }

    private static boolean isEmpty(Object v) {
        if (v == null) return true;
        if (v instanceof String s) return s.isEmpty();
        return false;
    }

    private static String stripQuotes(String s) {
        if (s.length() >= 2 && ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\"")))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
