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

## Supported installed database versions

- **Version 3:** current schema; opens directly.
- **Version 2:** supported through the explicit `NoteDatabase.MIGRATION_2_3` path and the migration instrumentation.
- **Version 1 and earlier:** no migration is registered; these versions are not supported by the current app and must be reported as a compatibility blocker rather than silently upgraded.

## Verification status

- Room schema generation is configured with `room.schemaLocation`.
- Generated schemas are supplied to Android-test assets from `app/schemas` during CI.
- The migration test is mandatory in the CI workflow.
- The previous hosted run `37239195565` built and uploaded the APK, then its emulator action failed before its script started. Its diagnostic `emulator-runner-status.log` records `runner_outcome=failure` and `phase_logs=absent`; the Android-test compilation and migration assertions did not execute. Migration correctness therefore remains **Not verified**, not silently waived.
- The bounded follow-up workflow separates Android-test compilation, emulator image preparation, emulator boot, migration execution, logcat collection, and report upload. It must complete a fresh run before AC4 can change status.

A genuine historical v2 schema can only be promoted to verified evidence if it is recovered from a trusted repository artifact, a known-good user database/export, or a successful test run using the actual historical schema.
