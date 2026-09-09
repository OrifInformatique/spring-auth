# API documentation generation (Spring REST Docs)

Companion guide to the [README](../README.md). It describes **how** spring-auth HTTP documentation is produced, verified, and published.

See also:

- [process-documentation.md](process-documentation.md): application structure, security, database, test execution (environment profiles, Docker)
- [index.html](index.html): consumer-facing API reference (HTML)
- [src/asciidoc/index.adoc](../src/asciidoc/index.adoc): AsciiDoc template (structure and snippet includes)

## Pipeline overview

Library: **Spring REST Docs** (`spring-restdocs-mockmvc` 3.0.1).

Source: [process/restdocs-pipeline.drawio](process/restdocs-pipeline.drawio)

![REST Docs pipeline](process/export/restdocs-pipeline.png)

Tests **do not** generate `index.adoc`: only the AsciiDoc template is maintained by hand. HTTP examples come from the tests.

## Diagram 1: parent process

Source: [restdocs-generation.drawio](process/restdocs-generation.drawio) (page **"1. Generation documentation"**)

![Parent process: documentation generation](process/export/restdocs-generation-1.-Generation-documentation.png)

Summary:

1. Run integration tests with `document(...)` and, on happy paths, JSON contracts (`RestDocsSnippets`).
2. If a JSON field no longer matches the contract, the test fails: no HTML rebuilt from stale examples.
3. If tests pass, Asciidoctor assembles `index.adoc` and the snippets.
4. Maven writes HTML to `target/generated-snippets-html/`.
5. Depending on the execution context, `docs/index.html` is updated or not (see below).

## Diagram 2: tests and contracts

Source: [restdocs-generation.drawio](process/restdocs-generation.drawio) (page **"2. Tests and contracts"**)

![MockMvc tests and JSON contracts](process/export/restdocs-generation-2.-Tests-and-contracts.png)

Classes involved:

- `AuthControllerIntegrationTest`
- `UserControllerIntegrationTest`
- `RestDocsSnippets` (centralized `requestFields` / `responseFields` contracts)
- `RestDocsSensitiveDataMasking` (masks JWT and refresh-token values in snippets)

Configuration:

```java
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
```

Each `document("auth/…" or "users/…", …, snippets)` call produces a folder under `target/generated-snippets/`, for example:

```
target/generated-snippets/auth/login/http-request.adoc
target/generated-snippets/auth/login/request-fields.adoc
target/generated-snippets/auth/login/response-fields.adoc
```

Each `document(...)` call also applies `maskSensitiveData()` before `prettyPrint()` so that snippets and `index.html` never contain real JWT or cookie values (see below).

## Sensitive data masking

Integration tests use real JWT tokens (signed with the test secret from `application-test.properties`). Without masking, those values would be copied verbatim into `target/generated-snippets/` and `docs/index.html`.

`RestDocsSensitiveDataMasking` is an `OperationPreprocessor` applied in both integration test helpers:

```java
preprocessRequest(maskSensitiveData(), prettyPrint())
preprocessResponse(maskSensitiveData(), prettyPrint())
```

| Location | Example before | Placeholder after |
|---|---|---|
| `Authorization` header | `Bearer eyJhbGci...` | `Bearer <access-token>` |
| `Cookie` / `Set-Cookie` | `refresh_token=eyJhbGci...` | `refresh_token=<refresh-token>` |
| JSON `token` / `accessToken` | `"eyJhbGci..."` | `"<jwt-access-token>"` |

Malformed examples used in 401 tests (for example `this.is.not.a.valid.token`) are left unchanged.

Unit tests: `RestDocsSensitiveDataMaskingTest`.

After changing masking rules, regenerate snippets and HTML (`verify` then `package`) and commit `docs/index.html` if it is versioned.

## Diagram 3: Asciidoctor assembly

Source: [restdocs-generation.drawio](process/restdocs-generation.drawio) (page **"3. Asciidoctor assembly"**)

![Asciidoctor assembly](process/export/restdocs-generation-3.-Asciidoctor-assembly.png)

At the top of `index.adoc`:

```adoc
ifndef::snippets[]
:snippets: ../../target/generated-snippets
endif::[]
```

Then includes such as:

```adoc
include::{snippets}/auth/login/http-request.adoc[]
```

The Maven plugin `asciidoctor-maven-plugin` (phase `prepare-package`) reads `src/asciidoc/index.adoc` and writes HTML to `target/generated-snippets-html/` (`pom.xml`, Maven attribute `<snippets>`).

## Diagram 4: test isolation (401)

Source: [restdocs-generation.drawio](process/restdocs-generation.drawio) (page **"4. Isolation tests 401"**)

![Test isolation 401](process/export/restdocs-generation-4.-Isolation-tests-401.png)

Separate process, related to test suite reliability (and therefore documentation), not HTML generation itself.

Related fixes in `SecurityConfig` and tests:

- `@AfterEach`: `SecurityContextHolder.clearContext()`
- Two `SecurityFilterChain` beans (stateless JWT API vs OAuth2 with session)
- `requestCache` disabled on the API chain
- `JwtAuthFilter` not registered as a servlet filter (`FilterRegistrationBean` disabled)

## Where does the HTML land?

| Command / context | Snippets | HTML output | `docs/index.html` updated? |
|---|---|---|---|
| `scripts/java-env.sh mvn test` | Yes | No | No |
| `scripts/java-env.sh mvn package` | Yes | `target/generated-snippets-html/` | No (unless copied manually) |
| `mvn clean package` on the host | Yes | `target/` | No (unless copied / committed) |
| `docker compose up` (`app` service, `./docs` volume) | Yes | mapped to `./docs/` | **Yes** (direct volume write) |

Compose volume on the `app` service:

```yaml
- ./docs:/app/target/generated-snippets-html
```

Without this volume (`java` container, `workspace` profile), regenerating docs only changes `target/`. To version `docs/index.html`, you must **commit** the file afterward.

## Common commands

Recommended build environment (JDK 21 + Maven 3.9 + MariaDB, no Java on the host):

```bash
scripts/java-env.sh up
scripts/java-env.sh mvn -Dspring.profiles.active=test verify
scripts/java-env.sh mvn clean package
```

Inspect snippets:

```bash
ls target/generated-snippets/auth/login/
```

Open HTML locally:

```bash
# generated by Maven
xdg-open target/generated-snippets-html/index.html

# committed copy in the repo
xdg-open docs/index.html
```

## Regenerate PNG exports

From the project root (Docker required):

```bash
for f in restdocs-pipeline restdocs-generation; do
  docker run --rm \
    -v "$PWD/docs/process:/data" \
    rlespinasse/drawio-export:v4.6.0 \
    -f png -t -s 2 -o /data/export /data/${f}.drawio
done
cp docs/process/export/restdocs-pipeline-Pipeline-REST-Docs.png docs/process/export/restdocs-pipeline.png
```

Options: `-t` transparent background, `-s 2` scale 2×. Edit `.drawio` files with [diagrams.net](https://app.diagrams.net/) or the Draw.io Integration extension.

## Maintenance rule

Any change to the pipeline or detailed processes must update the Draw.io files (`restdocs-pipeline.drawio`, `restdocs-generation.drawio`) **and** the PNGs in `process/export/` **in the same commit** as the code or text documentation.

After an API change:

1. Update tests and `RestDocsSnippets` if JSON payloads change.
2. Regenerate snippets and HTML (`verify` then `package`).
3. Update `docs/index.html` if published documentation must follow.
4. Update this document or the Draw.io diagrams if the flow changes.

## Keeping documentation in sync manually

| File | Role | When to edit |
|---|---|---|
| `src/asciidoc/index.adoc` | Structure and snippet includes | New documented endpoint, new section |
| Integration tests + `document(...)` | HTTP snippets | New endpoint, request/response change |
| `RestDocsSnippets` | JSON field contracts | Field added, removed, or renamed |
| `RestDocsSensitiveDataMasking` | Token/cookie placeholders in snippets | New sensitive header or JSON field to mask |
