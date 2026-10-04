# Kinetic Notes — Implementation Progress

## Tracking scope

- **Plan:** `docs/plans/Kinetic_Notes_Masterplan_v1.md`
- **Branch:** `arena/01a10467-kinetic-notes2`
- **Requested packet:** **I-001 — Truthful, reproducible baseline only**
- **Audited commit named by the plan:** `a8e193c295317cf84c2ea4717f300e9b2aa842ea`
- **Actual checkout at start of this packet:** `fde9c77` with 30 modified/untracked paths containing the prior implementation work.
- **Policy:** preserve the existing uncommitted work; do not reset, stash, uninstall, destructively migrate, merge, publish, or begin I-002.

## Plan reconciliation

The supplied raw text contains the audit, decisions, feature contracts, journeys, screens, visual system, state ownership, domain invariants, migration/file protocols, and the beginning of Board engineering. It does **not** include a separately enumerated I-001 packet section or packet-specific acceptance table. The I-001 scope below is therefore mapped conservatively from the explicit user request (“Truthful, reproducible baseline”) and the plan's I-001 remedies:

- A-001 / A-009 / A-025: AI/provider failures must be honest; no canned success or simulated transcription.
- A-002 / A-003: the existing Boolean lock must not be represented as encryption; protected text must not enter search/Graph/AI previews when not unlocked.
- A-013: expose Room schema generation to migration instrumentation and run the migration check in CI.
- A-014: do not publish an APK unless a stable signing key is configured; ordinary validation builds are explicitly identified as ephemeral when that secret is absent.
- A-015: opt out of Android automatic backup until a tested portable backup/restore package exists.
- A-026: preserve the current CI build evidence and record that the repository still lacks a local Gradle launcher; no fake claim of local reproducibility is made.

The plan explicitly defers encryption/authentication, durable draft generations, trash/history, structured documents, attachments, provider profiles, backup/restore, BoardItem/Stack schema, and other later packets. Those are not implemented here.

## Existing work reconciled and preserved

### Already present before I-001 changes

- Kotlin / Compose / Room / Flow single-module foundation.
- Explicit Room v2→v3 migration with no destructive fallback.
- Durable groups, stable IDs, group deletion that keeps notes, safe Room upserts, targeted text writes, and persistence tests.
- Canvas viewport measurement, pan/zoom, drag persistence, Fit Notes, linking, and prior Canvas/editor/Graph/UI refinement.
- Search title/content/tag/folder/type matching, focus/keyboard behavior, recent search/tag chips, contextual Canvas actions, editor save state, and reversible AI preview scaffolding from the prior implementation worktree.
- EXP application ID suffix and schema-location KSP configuration.

### Findings still open after reconciliation

- `NoteEntity.isLocked` remains a Boolean and is not encryption/authentication. I-001 only makes the copy honest and suppresses protected text from sensitive derived views; real protection belongs to F-028/I-015.
- Room v2 history is reconstructed in the instrumented fixture rather than recovered as a generated historical schema asset. This is recorded as a deviation; no destructive migration is introduced.
- The local repository still has no `gradlew` launcher/JAR and the sandbox has no Java/Gradle/adb. CI remains the executable verification environment until a wrapper can be added without fabricating a bootstrap.
- Stable EXP signing secret `KX_EXP_KEYSTORE_B64` is not available in this environment. CI continues to make ephemeral validation APKs but now refuses release publication without the stable secret.
- Automatic backup is intentionally disabled, not replaced by a portable export. F-016/I-016 remains open.
- AI provider profiles, cancellation/request IDs, revision guards, structured proposal validation, and real audio capture remain later work.

## I-001 change set

| Change | Packet rationale | Status |
|---|---|---|
| Save this plan as `docs/plans/Kinetic_Notes_Masterplan_v1.md` | Required reference source | Done |
| Add this progress ledger | Required packet tracking/evidence | Done |
| Make `GeminiService` fail on missing key, HTTP error, empty response, exceptions; close responses and rethrow cancellation | A-001/A-025; truthful status | Done |
| Remove repository canned AI fallbacks and propagate `Result` failures to UI notices/previews | A-001/A-009/A-025 | Done |
| Mark voice transcription unavailable while the prototype stores no audio bytes | A-022; no fake feature | Done |
| Use “Locked note” rather than “Encrypted Note”; exclude locked text from search/Graph/AI eligibility | A-002/A-003; honest baseline | Done |
| Expose `app/schemas` as Android-test assets and add connected migration/persistence CI stage | A-013 | Done; CI stage attempted but not green |
| Opt out of automatic cloud/device backup until portable recovery is implemented | A-015 | Done |
| Use configured stable EXP signing secret when present and block publication without it | A-014 | Done; secret unavailable here |
| Add genuine Gradle launcher/JAR | A-026 | Not implemented; tooling unavailable and no safe wrapper artifact is present |

## I-001 acceptance and verification ledger

These are the acceptance checks available from the request and plan mapping. Packet-specific criteria were not supplied in the raw text, so they are not invented here.

| Criterion | Status | Evidence / remaining issue |
|---|---|---|
| No missing-key/provider/empty-response path reports fabricated AI success | **Passed (CI/source)** | `GeminiService` returns explicit failures and the build/unit/lint stage passed in CI runs `37230585818` and `37233108051`; no live provider call was made. |
| Failed AI actions leave note content unchanged and expose an error | **Passed (CI/source)** | Repository/ViewModel failure paths preserve the existing note and set a visible `userNotice`; build/unit/lint passed, but no device/manual run was performed. |
| No fake audio transcription is offered by the timer-only prototype | **Passed (source)** | `VoiceMemoRecorder` transcription control is disabled and says audio bytes are not stored. |
| Locked notes are not called encrypted and protected text is excluded from sensitive derived views | **Passed (source)** | UI copy changed to “Locked note”; search/Graph/AI eligibility checks exclude locked notes unless in the existing temporary-unlock set. This is not encryption. |
| No destructive database migration or data-bearing uninstall | **Passed (source)** | No database schema version bump or destructive fallback added in I-001; current explicit 2→3 migration remains. |
| Room migration assets/instrumentation are configured | **Passed (source)** | `androidTest` assets source points at generated `app/schemas`; CI includes `connectedDebugAndroidTest`; historical v2 schema asset remains a documented deviation. |
| Build, unit tests, and lint complete | **Passed** | `testDebugUnitTest assembleDebug lintDebug` passed in CI before the connected stage in run `37233108051` (and the preceding corrected-label runs). |
| Migration test runs in the green CI job | **Failed** | The connected stage exits nonzero in `37229480548`, `37230585818`, `37231028979`, `37231487318`, `37231909132`, `37232246940`, `37232697677`, and `37233108051`; build/unit/lint passes first. CI reports a KSP AWT `ApplicationManager.getApplication()` null exception, while the raw emulator log is unavailable from the sandbox (GitHub log/artifact retrieval returns EOF), so this is recorded as a CI/tooling blocker rather than a claimed migration-data failure. |
| Automatic Android backup does not silently copy current plaintext workspace | **Passed (source)** | `backup_rules.xml` and `data_extraction_rules.xml` exclude database/files/preferences until I-016 export/restore exists. |
| APK release publication cannot use a fresh ephemeral signing certificate | **Passed (source)** | CI uses `KX_EXP_KEYSTORE_B64` when configured and fails the publish path if it is absent. Ordinary validation APKs remain explicitly ephemeral. |
| Stable EXP signing continuity is verified | **Not verified** | `KX_EXP_KEYSTORE_B64` is unavailable here; the stable-key release path was correctly not invoked. |
| Installable APK packaging completes | **Not verified** | The connected stage failed first, so the package/upload steps were skipped in the hosted runs. |
| A reproducible local Gradle command is available | **Failed** | `gradlew` and `gradle-wrapper.jar` are absent; local `java`, `gradle`, `adb`, and `kotlinc` are not on PATH. This is an infrastructure gap, not an app test failure. |
| Existing notes, links, checklists, folders, and installed-app data are preserved | **Passed (scope/source)** | I-001 introduced no destructive migration, delete, reset, or uninstall operation. Device data preservation cannot be manually exercised in this environment. |
| No later packet was implemented | **Passed (scope review)** | No encryption, Trash/history, backup/restore package, structured-document conversion, attachments, stacks, provider framework, or sync packet was added. |

## Verification commands/evidence

### Local

- `git status --short --branch` at start: branch was `arena/01a10467-kinetic-notes2`; `HEAD` was `fde9c77`; prior implementation remained uncommitted.
- `git cat-file -t a8e193c...`: audited commit object was not present locally.
- `find /home/user/uploads`: attachment mount was unavailable; plan was saved from the raw text supplied in chat.
- Local build was not run: repository has no `./gradlew`, and the sandbox has no `java`, `gradle`, `adb`, or `kotlinc` on PATH.

### CI evidence for this checkpoint

- `gradle testDebugUnitTest assembleDebug lintDebug` passed before the connected stage in runs `37230585818`, `37231028979`, `37231487318`, `37231909132`, `37232246940`, `37232697677`, and `37233108051`.
- `gradle connectedDebugAndroidTest --stacktrace --no-configuration-cache --no-daemon` was attempted inside `reactivecircus/android-emulator-runner@v2` in those same runs and did not produce a green job.
- The original baseline run `37229480548` also passed build/unit/lint and failed in connected verification. Later runs provide the KSP `ApplicationManager.getApplication()` null annotation, but the raw emulator output could not be retrieved from this sandbox because GitHub log/artifact downloads returned EOF.
- APK packaging, stable-signing continuity, and release publication were not reached after the connected-stage failure. Release publication remains guarded by `KX_EXP_KEYSTORE_B64` and was not invoked.
- Local verification remains unavailable: the repository has no `gradlew`/wrapper JAR and the sandbox has no Java, Gradle, adb, or kotlinc.

## Deviations and remaining issues

1. The supplied text ended at the beginning of §11 and did not contain a dedicated I-packet/acceptance section. The mapping above is explicit and conservative rather than silently pretending packet details were supplied.
2. Genuine historical Room schema JSON files could not be recovered from the current shallow checkout or the unavailable prior commit object. The migration test's v2 schema is still created in its test fixture; I-001 does not change user data or claim cross-device migration fidelity.
3. A standard Gradle wrapper cannot be generated locally because the wrapper JAR/distribution and Java are unavailable. CI's pinned Gradle 9.3.1 remains the reproducible remote command; adding a hand-written non-wrapper launcher would be misleading.
4. No emulator, installed APK, live provider call, API key, or manual UX verification was performed. The hosted connected-test stage was attempted repeatedly, but it did not reach a green result; its raw emulator log is not available from this sandbox, so migration correctness is not claimed.

## Stop boundary

After the I-001 checkpoint is verified and committed, stop. Do not merge, publish, or begin I-002 without a new instruction.
