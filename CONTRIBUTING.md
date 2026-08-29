# Contributing

Thanks for your interest in improving HashtagCMS Workflows (Java). This is an
open-source library published to Maven Central — contributions of all sizes are
welcome.

## Getting set up

You need **JDK 21+**. Maven is not required — the project ships the Maven Wrapper.

```bash
git clone https://github.com/hashtagcms/workflows-java.git
cd workflows-java
./mvnw test          # build + run the suite (in-memory H2, no external services)
./mvnw spring-boot:run   # optional: start the standalone runner on :8080
```

## Development workflow

1. Open an issue first for anything substantial, so we can agree on the approach.
2. Branch from `main`.
3. Keep changes focused; match the surrounding code style (constructor injection,
   small classes, Javadoc on public types).
4. **Add or update tests.** Every behavioural change needs coverage — see
   `src/test/java` for the existing patterns (`@SpringBootTest` + `MockMvc` for
   the API, plain unit tests for the engine).
5. Run `./mvnw test` and make sure the suite is green.
6. Update `CHANGELOG.md` under `[Unreleased]` and the relevant docs.
7. Open a pull request describing the change and the reasoning.

## Design principles

- **API only, server-driven.** The library returns directives; it renders nothing.
- **Parity with the PHP package.** Behaviour should match `hashtagcms/workflows`
  unless there is a documented, deliberate reason to differ.
- **Drop-in.** It must auto-configure cleanly and never disable a host
  application's own beans — every library bean is `@ConditionalOnMissingBean`.
- **No surprises for consumers.** JDBC drivers, auth providers, and the like are
  the host's choice; keep such dependencies `optional`.

## Reporting bugs / security

- Bugs and features: open a GitHub issue with a minimal reproduction.
- Security issues: please report privately rather than in a public issue.

By contributing you agree that your contributions are licensed under the
project's [MIT License](LICENSE).
