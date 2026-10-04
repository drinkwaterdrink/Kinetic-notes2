# Room migration evidence

## Scope

This record supports I-001 migration review. It is evidence, not a replacement for a generated historical Room schema.

## Repository evidence

- Audited base commit: `fde9c773f9dbf543a5b238e7cec0304cd5851eff`.
- The base tree contains `gradle/wrapper/gradle-wrapper.properties`, but no wrapper launcher, wrapper JAR, or `app/schemas` files.
- Local Git history and the repository's reachable GitHub tree contain no historical Room JSON schema files.
- The existing migration is `NoteDatabase.MIGRATION_2_3`, which adds durable groups and `notes.groupId` without a destructive fallback.

## Current test evidence

`app/src/androidTest/java/com/example/NoteDatabaseMigrationTest.kt` creates a documented v2 fixture because no recoverable exported v2 JSON exists. The fixture exercises:

- two legacy notes and their metadata;
- a legacy folder name and group backfill;
- a link row;
- a checklist row;
- the v2-to-v3 migration and post-migration preservation assertions.

This fixture is deliberately labeled as reconstructed evidence. It does not prove that the fixture is byte-for-byte identical to an historical production schema.

## Verification status

- Room schema generation is configured with `room.schemaLocation`.
- Generated schemas are supplied to Android-test assets from `app/schemas` during CI.
- The migration test is mandatory in the CI workflow and is now run with phase-separated emulator diagnostics.
- The latest hosted connected-test run before this evidence update failed in the emulator stage; the raw stage log was not retrievable from the Arena sandbox. Migration correctness therefore remains **Not verified**, not silently waived.

A genuine historical v2 schema can only be promoted to verified evidence if it is recovered from a trusted repository artifact, a known-good user database/export, or a successful test run using the actual historical schema.
