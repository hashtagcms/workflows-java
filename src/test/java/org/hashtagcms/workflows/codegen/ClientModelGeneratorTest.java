package org.hashtagcms.workflows.codegen;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generates the client directive models and guards that the committed copies under
 * {@code clients/} stay in sync with the directive manifest.
 *
 * <ul>
 *   <li>Regenerate:  {@code ./mvnw test -Dtest=ClientModelGeneratorTest -Dcodegen.write=true}</li>
 *   <li>Otherwise the test fails if a committed file has drifted from the manifest,
 *       pointing you at the regenerate command.</li>
 * </ul>
 */
class ClientModelGeneratorTest {

    private Map<String, String> expectedFiles() {
        var defs = ClientModelGenerator.manifest();
        return Map.of(
                "clients/kotlin/WorkflowDirectiveType.kt", ClientModelGenerator.kotlin(defs),
                "clients/typescript/workflowDirectiveType.ts", ClientModelGenerator.typescript(defs),
                "clients/swift/WorkflowDirectiveType.swift", ClientModelGenerator.swift(defs));
    }

    @Test
    void clientModelsAreInSyncWithTheManifest() throws Exception {
        boolean write = Boolean.getBoolean("codegen.write");
        for (var e : expectedFiles().entrySet()) {
            Path path = Path.of(e.getKey());
            String expected = e.getValue();

            if (write) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, expected);
                continue;
            }

            assertThat(Files.exists(path))
                    .as("%s missing — run: ./mvnw test -Dtest=ClientModelGeneratorTest -Dcodegen.write=true", path)
                    .isTrue();
            assertThat(Files.readString(path))
                    .as("%s is out of sync with the directive manifest — regenerate with "
                            + "./mvnw test -Dtest=ClientModelGeneratorTest -Dcodegen.write=true", path)
                    .isEqualTo(expected);
        }
    }
}
