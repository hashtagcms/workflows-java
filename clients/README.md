# Generated client models

Compile-time-safe models of the **directive contract**, generated from the single
source of truth — the server's directive manifest (`DirectiveManifest.core()`, the
same 72 directives served at `/directives` and described by `/catalog`). Instead of
matching directives on raw strings, a server-driven-UI client switches over a typed
enum and knows each directive's **category** and negotiation **fallback**.

| Language | File | Shape |
|---|---|---|
| Kotlin (KMP) | [`kotlin/WorkflowDirectiveType.kt`](kotlin/WorkflowDirectiveType.kt) | `enum class WorkflowDirectiveType(wire, category, fallback)` + `fromWire()` |
| TypeScript | [`typescript/workflowDirectiveType.ts`](typescript/workflowDirectiveType.ts) | `type WorkflowDirectiveType` union + `DIRECTIVE_META` |
| Swift | [`swift/WorkflowDirectiveType.swift`](swift/WorkflowDirectiveType.swift) | `enum WorkflowDirectiveType: String, CaseIterable` + `category` / `fallback` |

## Use it

**Kotlin / KMP** (e.g. DMB Mobile v2) — copy the file into your shared module (adjust
the `package`), then:

```kotlin
when (WorkflowDirectiveType.fromWire(directive.type)) {
    WorkflowDirectiveType.TOAST    -> showToast(directive)
    WorkflowDirectiveType.NAVIGATE -> navigate(directive)
    null                           -> Unit // unknown type — ignore or log
    else                           -> renderFallback(directive)
}
```

**TypeScript** (web):

```ts
import { WorkflowDirectiveType, DIRECTIVE_META } from "./workflowDirectiveType";
function handle(type: WorkflowDirectiveType) { /* exhaustive — tsc errors on a missing case */ }
```

**Swift** (iOS): `WorkflowDirectiveType(rawValue: directive.type)` then `switch` over it.

## Regenerate

These files are generated — **do not edit by hand**. When the directive manifest
changes, regenerate and commit:

```bash
./mvnw test -Dtest=ClientModelGeneratorTest -Dcodegen.write=true
```

`ClientModelGeneratorTest` (run in the normal suite) fails if a committed file has
drifted from the manifest, so they can't silently rot.
