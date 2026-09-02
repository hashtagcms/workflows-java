<?php
/**
 * Regenerate the PHP↔Java parity fixture from the reference PHP implementation.
 *
 * Usage (from the Java repo root):
 *   php scripts/dump-php-manifest.php \
 *     ../../hashtagcms-workflows/src/Support/DirectiveManifest.php \
 *     > src/test/resources/php-directive-manifest.json
 *
 * The default path assumes the sibling PHP package `hashtagcms/workflows` is
 * checked out at ../../hashtagcms-workflows relative to this repo. Run this
 * whenever the PHP directive manifest changes; DirectiveManifestParityTest then
 * fails if the Java DirectiveManifest has drifted from it.
 */
$src = $argv[1] ?? __DIR__ . '/../../../hashtagcms-workflows/src/Support/DirectiveManifest.php';
if (!is_file($src)) { fwrite(STDERR, "PHP manifest not found: $src\n"); exit(1); }
require $src;
$out = [];
foreach (\HashtagCms\Workflows\Support\DirectiveManifest::core() as $d) {
    $out[] = [
        'type'        => $d['type'] ?? null,
        'label'       => $d['label'] ?? null,
        'category'    => $d['category'] ?? null,
        'description' => $d['description'] ?? null,
        'platforms'   => $d['platforms'] ?? null,
        'fallback'    => $d['fallback'] ?? null,
    ];
}
echo json_encode($out, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE), "\n";
