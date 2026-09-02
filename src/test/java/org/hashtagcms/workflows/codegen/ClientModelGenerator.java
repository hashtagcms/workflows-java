package org.hashtagcms.workflows.codegen;

import org.hashtagcms.workflows.service.DirectiveManifest;
import org.hashtagcms.workflows.service.DirectiveManifest.DirectiveDef;

import java.util.List;
import java.util.Locale;

/**
 * Emits compile-time-safe client models for the directive contract from the
 * single source of truth ({@link DirectiveManifest#core()}), so server-driven-UI
 * clients (Kotlin/KMP, TypeScript, Swift) can exhaustively handle directive types
 * and know each one's category and fallback instead of matching on raw strings.
 *
 * Pure string builders — driven by {@code ClientModelGeneratorTest}, which both
 * (re)writes the files under {@code clients/} and guards that the committed copies
 * stay in sync with the manifest.
 */
public final class ClientModelGenerator {

    private ClientModelGenerator() {}

    private static final String NOTE =
            "// Generated from the HashtagCMS Workflows directive manifest — DO NOT EDIT BY HAND.\n"
          + "// Regenerate: ./mvnw test -Dtest=ClientModelGeneratorTest -Dcodegen.write=true\n";

    // ---- Kotlin (KMP) -------------------------------------------------------

    public static String kotlin(List<DirectiveDef> defs) {
        StringBuilder b = new StringBuilder();
        b.append(NOTE)
         .append("package org.hashtagcms.workflows.client\n\n")
         .append("/** Every directive type the server can emit, with its category and negotiation fallback. */\n")
         .append("enum class WorkflowDirectiveType(val wire: String, val category: String, val fallback: String?) {\n");
        for (int i = 0; i < defs.size(); i++) {
            DirectiveDef d = defs.get(i);
            b.append("    ").append(kotlinConst(d.type())).append("(\"").append(d.type()).append("\", \"")
             .append(d.category()).append("\", ").append(nullableKt(d.fallback())).append(")")
             .append(i < defs.size() - 1 ? "," : ";").append("  // ").append(d.label()).append("\n");
        }
        b.append("\n    companion object {\n")
         .append("        private val byWire = entries.associateBy { it.wire }\n")
         .append("        fun fromWire(wire: String): WorkflowDirectiveType? = byWire[wire]\n")
         .append("    }\n}\n");
        return b.toString();
    }

    // ---- TypeScript ---------------------------------------------------------

    public static String typescript(List<DirectiveDef> defs) {
        StringBuilder b = new StringBuilder();
        b.append(NOTE).append("\n");
        b.append("/** Every directive type the server can emit. */\n");
        b.append("export type WorkflowDirectiveType =\n");
        for (int i = 0; i < defs.size(); i++) {
            b.append("  | \"").append(defs.get(i).type()).append("\"")
             .append(i < defs.size() - 1 ? "\n" : ";\n");
        }
        b.append("\nexport interface DirectiveMeta {\n")
         .append("  category: string;\n  fallback: WorkflowDirectiveType | null;\n}\n\n");
        b.append("export const DIRECTIVE_META: Record<WorkflowDirectiveType, DirectiveMeta> = {\n");
        for (DirectiveDef d : defs) {
            b.append("  ").append(d.type()).append(": { category: \"").append(d.category())
             .append("\", fallback: ").append(nullableTs(d.fallback())).append(" },\n");
        }
        b.append("};\n");
        return b.toString();
    }

    // ---- Swift --------------------------------------------------------------

    public static String swift(List<DirectiveDef> defs) {
        StringBuilder b = new StringBuilder();
        b.append(NOTE).append("\n");
        b.append("/// Every directive type the server can emit, with its category and negotiation fallback.\n");
        b.append("public enum WorkflowDirectiveType: String, CaseIterable {\n");
        for (DirectiveDef d : defs) {
            b.append("    case ").append(swiftCase(d.type())).append(" = \"").append(d.type()).append("\"\n");
        }
        b.append("\n    public var category: String {\n        switch self {\n");
        for (DirectiveDef d : defs) {
            b.append("        case .").append(swiftCase(d.type())).append(": return \"").append(d.category()).append("\"\n");
        }
        b.append("        }\n    }\n\n");
        b.append("    public var fallback: WorkflowDirectiveType? {\n        switch self {\n");
        for (DirectiveDef d : defs) {
            String fb = d.fallback() == null ? "nil" : "." + swiftCase(d.fallback());
            b.append("        case .").append(swiftCase(d.type())).append(": return ").append(fb).append("\n");
        }
        b.append("        }\n    }\n}\n");
        return b.toString();
    }

    // ---- name helpers -------------------------------------------------------

    static String kotlinConst(String wire) {
        return wire.toUpperCase(Locale.ROOT);
    }

    static String swiftCase(String wire) {
        String[] parts = wire.split("_");
        StringBuilder s = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            s.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return s.toString();
    }

    private static String nullableKt(String v) { return v == null ? "null" : "\"" + v + "\""; }

    private static String nullableTs(String v) { return v == null ? "null" : "\"" + v + "\""; }

    public static List<DirectiveDef> manifest() {
        return DirectiveManifest.core();
    }
}
