# Extending

[← Docs index](README.md)

Everything the library exposes is a Spring bean, and every bean it defines is
`@ConditionalOnMissingBean`. So you extend it the ordinary Spring way — declare a
bean — and override it by declaring one of the same type.

## Code-based workflows (Java handlers)

Most workflows are declarative JSON. When you need real logic, implement
[`WorkflowHandler`](../src/main/java/org/hashtagcms/workflows/engine/WorkflowHandler.java)
and register it by alias. A workflow row whose `handler` matches the alias (or a
config with `target.type: custom_class`) runs your code.

```java
@Component
class ReorderHandler implements WorkflowHandler {
    @Override
    public WorkflowResponse handle(WorkflowContext ctx) {
        Object userId = ctx.getUser().get("id");
        // ...do work using ctx.getPayload(), ctx.getSiteId(), etc...
        return WorkflowResponse.make()
                .setMessage("Re-ordered your last basket")
                .toast("Added 6 items", "success")
                .withData(Map.of("orderId", 12345));
    }
}

@Configuration
class WorkflowHandlers {
    WorkflowHandlers(WorkflowHandlerRegistry registry, ReorderHandler reorder) {
        registry.register("WORKFLOW_QUICK_REORDER", reorder);
    }
}
```

`WorkflowResponse.make()` is a fluent builder: `setSuccess`, `setMessage`,
`toast`, `haptic`, `navigate`, `mutateCart`, `addDirective`, `withData`.

## Custom target adapters

A target is how a declarative workflow reaches the outside world. The built-ins
are `http`, `service`, `event`, and `custom_class`. Add your own by implementing
[`TargetAdapter`](../src/main/java/org/hashtagcms/workflows/engine/target/TargetAdapter.java)
and exposing it as a bean — the engine auto-collects every `TargetAdapter` on the
context and routes by the `types()` it declares.

```java
@Component
class GraphQlTargetAdapter implements TargetAdapter {
    @Override public Set<String> types() { return Set.of("graphql"); }

    @Override
    public TargetResult execute(Map<String, Object> target, Map<String, Object> context) {
        // target is already fully interpolated ({{ }} resolved)
        Map<String, Object> body = callGraphQl(target);
        return new TargetResult(true, 200, body, Map.of(), null);
    }
}
```

A workflow then uses `"target": { "type": "graphql", ... }`.

## Custom directives

The directive **manifest** (the 72 built-ins) governs capability negotiation —
which directives a given client platform / app version can render. Add your own
directive types by inserting rows into the `workflow_directives` table (over the
management API, `POST /api/hashtagcms/admin/directives`, or a seeder), giving each
a `type`, supported `platforms`, an optional `fallback`, and a min app version.
Directives you emit that are not in the manifest are passed through untouched.

## Custom authentication

Declare a [`WorkflowUserResolver`](../src/main/java/org/hashtagcms/workflows/security/WorkflowUserResolver.java)
bean to integrate any identity source — Spring Security, mTLS, remote token
introspection, a bespoke session store. It backs off the built-in driver, so
`auth.driver` no longer applies.

```java
@Bean
WorkflowUserResolver workflowUserResolver(MyAuthService auth) {
    return request -> {
        var principal = auth.authenticate(request);   // your logic
        if (principal == null) return null;            // -> 401 for auth_required
        return Map.of("id", principal.id(), "email", principal.email());
    };
}
```

See [Authentication & SSO](authentication.md) for the built-in drivers.

## Overriding anything else

The same pattern applies throughout. Want a different interpolation env source,
negotiator behaviour, or logging? Declare a bean of that type and it replaces the
default. Because the library is plain Spring beans wired by auto-configuration,
there is nothing special to unregister first.
