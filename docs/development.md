# Development Guide

## Project Layout

```text
.
|-- README.md
|-- AGENTS.md
|-- docs/
|-- pom.xml
|-- config/checkstyle/checkstyle.xml
`-- src/main/java/
    |-- moneyunitsconverter/
    |-- outboxsupport/
    |-- processedevent/
    `-- enums/
```

The library source lives under `src/main/java`. Keep this project as a reusable
support library; service-specific Kafka producer and domain
logic should stay in consuming services.

Public package areas:

- `outboxsupport` contains mapped outbox base classes and Kafka send-result
  helpers.
- `processedevent` contains mapped processed-event base classes and repository
  helpers for idempotent consumption.
- `moneyunitsconverter` contains shared major/minor currency conversion helpers.
- `enums` contains common banking enum values grouped by domain.

## Build And Verify

Package the library locally:

```bash
mvn clean package
```

Run the same verification path used by CI:

```bash
mvn --batch-mode verify
```

The `verify` phase runs Checkstyle using
`config/checkstyle/checkstyle.xml`. Generated Maven output belongs in `target/`
and is ignored by Git.

CI runs the same command on pushes and pull requests targeting `main`.

## Formatting

Format Java sources before running Checkstyle:

```bash
mvn spotless:apply
```

Check formatting without modifying files:

```bash
mvn spotless:check
```

Spotless uses `google-java-format`, which matches the two-space indentation and
single non-static import group expected by Checkstyle.

## Versioning

The current artifact version is defined in `pom.xml`:

```xml
<version>0.0.4</version>
```

When changing the published version, update:

- `pom.xml`;
- dependency examples in [README.md](../README.md);
- dependency examples in [docs/usage.md](usage.md).

## Publishing

Publishing is handled by `.github/workflows/publish.yml`. The workflow runs on:

- GitHub release creation;
- manual `workflow_dispatch`.

The publish job uses Java 17 and deploys with:

```bash
mvn --batch-mode clean deploy
```

The package is deployed to GitHub Packages:

```xml
<url>https://maven.pkg.github.com/aburov30/bankingsupport</url>
```

Before creating a release, verify that `pom.xml` contains the intended version
and run:

```bash
mvn --batch-mode verify
```

## Maintenance Notes

- Keep `README.md` short and link deeper details to files in `docs/`.
- Keep `AGENTS.md` focused on agent workflow and repository-specific rules.
- Keep public examples aligned with the actual Java API.
- Avoid broad refactors when changing mapped superclass fields, because
  consuming services may depend on column names and method names.
