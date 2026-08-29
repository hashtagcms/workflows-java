# Publishing to Maven Central

[← Docs index](README.md)

This library is released to **Maven Central** through the
[Sonatype Central Portal](https://central.sonatype.com). The `pom.xml` already
carries the required metadata (name, description, URL, license, developers, SCM)
and a `release` profile that builds signed sources + Javadoc jars and publishes.

## One-time setup

1. **Central Portal account** — sign in at
   [central.sonatype.com](https://central.sonatype.com) and register (verify) the
   `org.hashtagcms` namespace. Namespace verification proves you control the
   matching domain/GitHub org; until it's approved you cannot publish under that
   group id. (Alternatively publish under `io.github.<user>`, which verifies via a
   GitHub repo — then change `<groupId>` accordingly.)

2. **Generate a user token** in the portal (Account → *Generate User Token*) and
   add it to `~/.m2/settings.xml`:

   ```xml
   <settings>
     <servers>
       <server>
         <id>central</id>
         <username>YOUR_TOKEN_USERNAME</username>
         <password>YOUR_TOKEN_PASSWORD</password>
       </server>
     </servers>
   </settings>
   ```

   The `<id>central</id>` matches `publishingServerId` in the `release` profile.

3. **A GPG key** for signing, published to a public keyserver:

   ```bash
   gpg --gen-key
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   ```

   Maven picks the key up automatically; for CI, pass
   `-Dgpg.passphrase=...` (and import the key first).

## Cut a release

1. Set a release version (drop `-SNAPSHOT`):

   ```bash
   ./mvnw versions:set -DnewVersion=1.0.0
   ```

2. Build, sign, and publish:

   ```bash
   ./mvnw -Prelease clean deploy
   ```

   The `release` profile attaches the sources and Javadoc jars, GPG-signs all
   artifacts, and uploads the bundle via the `central-publishing-maven-plugin`.
   With `autoPublish=true` it releases automatically once validation passes;
   otherwise approve it in the portal UI. Central sync typically takes a few
   minutes to a couple of hours.

3. Tag and prepare the next iteration:

   ```bash
   git tag v1.0.0 && git push --tags
   ./mvnw versions:set -DnewVersion=1.1.0-SNAPSHOT
   ```

4. Move the `CHANGELOG.md` `[Unreleased]` entries under the new version heading.

## Verifying the artifact

The **main** artifact is the plain library jar
(`target/workflows-<version>.jar`) — that's what consumers depend on. The runnable
Spring Boot jar is attached separately under the `exec` classifier
(`target/workflows-<version>-exec.jar`) and is **not** what gets consumed. Confirm
the main jar has no `BOOT-INF/` (i.e. it is a normal library, not a repackaged
executable):

```bash
unzip -l target/workflows-*.jar | grep -c BOOT-INF   # expect 0
```
