# Kinetic Notes — Audited Masterplan
## Version 1.0 · 4 October 2026

**Product:** personal, local-first Android notes app with a spatial Board, searchable Library, structured editor, and optional BYOK assistant.  
**Primary user:** Trent; phone-first, with a future PC companion.  
**Design:** Dark Editorial Utility — quiet surfaces, muted note colors, readable content, precise manipulation.  
**Technical direction:** evolve Kotlin / Compose / Room in bounded slices.  
**Branch:** arena/01a10467-kinetic-notes2  
**Audited commit:** a8e193c295317cf84c2ea4717f300e9b2aa842ea  
**Repository:** https://github.com/drinkwaterdrink/Kinetic-notes2  
**Status:** audit and implementation blueprint. Changes described here are proposed, not already implemented.

## How to use this document

Read the audit and decisions, then implement one dependency-ready I-packet at a time. Feature contracts, data invariants, screen specifications and acceptance tests apply together.

Existing F-001, F-003, F-004, F-010, F-011, F-014–F-021 and F-023 retain their meanings. Missing feature numbers remain reserved; additions start at F-024. Existing S-001/S-002/S-004/S-005/S-006 and J-001/J-002/J-003/J-005–J-008 are preserved. No master “implement everything” prompt is intended.

Recommendations use the user's explicit permission to improve the plan. Assumptions are labeled rather than presented as previously confirmed preferences.

## 1. Executive assessment

Keep the native foundation and improve it incrementally. A framework rewrite would discard useful work without solving the most important risks.

Priority issues:

1. AI failures become successful canned answers.
2. “Encrypted” notes are plaintext and open without authentication.
3. Editor drafts and global save state can disagree during interruptions and concurrent saves.
4. Board geometry mixes raw pixels and dp dimensions.
5. Migration instrumentation is absent from the green CI job and lacks historical schema assets.
6. Deletion is permanent and complete portable backup/recovery is missing.

The north-star experience: capture immediately; trust committed work; retrieve anything through Library/search; arrange meaningful work spatially; use AI with explicit scope and reversible results.

### Preserve

- Kotlin, Compose, Room, coroutines/Flow and the single app module.
- Stable note/group IDs, explicit 2→3 migration and no destructive fallback.
- Targeted text writes, safe editing upserts and persistence regression tests.
- Durable groups and “delete group but keep notes.”
- Transient drag coordinates, boundary persistence, anchored zoom and Fit Notes.
- Library foundation, manual links and useful preview-before-apply intent.
- Separate EXP application ID, with corrected signing continuity.

### Boundaries

No accounts, monetization, collaboration server, enterprise layers, always-running AI, autonomous deletion or cloud requirement. Semantic retrieval, actual PC sync, PDF annotation and a new voice recorder are later work.

## 2. Audit evidence and limitations

### Checks actually performed

- Read the exact branch's persistence/network/state code, active Board/Library/editor wiring, settings/theme, graph engine, tests, manifest, backup rules and CI.
- Inspected legacy/audio/editor components to distinguish active features from disconnected prototypes.
- Confirmed GitHub Actions run 37221246524 succeeded for this SHA. Its combined step ran testDebugUnitTest, assembleDebug and lintDebug.
- Attempted ./gradlew testDebugUnitTest assembleDebug --no-daemon locally. It stopped before running: gradlew is absent. No gradle, adb or kotlinc was available through PATH. Local Java is 17; CI provisions Java 21.
- Checked current provider/platform contracts against official documentation.
- No APK was installed/rendered, no user API key used and no live generation executed.

CI: https://github.com/drinkwaterdrink/Kinetic-notes2/actions/runs/37221246524

A green configured CI job does not establish migration instrumentation, on-device UX, encryption or live AI functionality. Visual findings are source observations, not screenshot verification.

### Findings register

Critical = privacy/destructive-result risk; High = data integrity/core functionality; Medium = quality/performance/maintainability. “Confirmed” means established from source; “risk” needs the specified execution test.

| ID | Severity / confidence | Evidence at audited commit | Finding | Remedy |
|---|---|---|---|---|
| A-001 | Critical, confirmed | GeminiService.kt:56–95, 125–166 | Missing key, HTTP error, empty result and exceptions return Result.success with canned prose. Apply can replace real notes with unrelated demo content. | Honest errors, I-001/I-007/I-008. |
| A-002 | Critical, confirmed | NoteEntity; NoteDao.updateLockStatus; NotesViewModel.openNote/toggleLock; MainActivity editor wiring | Lock is a Boolean; no auth/encryption protects opening. | Remove false claim, then real protection, I-001/I-015. |
| A-003 | Critical, confirmed | SpatialCanvasView:641–735; KeepGridView title/mask; filteredNotes; repository AI methods | Titles remain visible; checklist/code previews bypass mask; search/AI include protected plaintext. | Shared privacy projection, scope exclusion, I-001/I-015. |
| A-004 | High, confirmed risk path | FullScreenNoteEditorScreen:127–187; saveEditorDraft | remember-owned drafts; dirty cleared before DB acknowledgment; old completion can mark newer work SAVED. | Per-note session/generation writer, I-002. |
| A-005 | High, confirmed gap | Editor:154–179, 563; MainActivity:498–519 | No background flush; WikiLink can change notes before debounce; Back does not await already-in-flight save. | Flush barriers and durable draft recovery, I-002. |
| A-006 | High, confirmed risk path | updateSelectedNote:436–439; repository.saveNote | Metadata changes still save whole stale entities, risking geometry/metadata overwrites. | Field-scoped commands, I-002. |
| A-007 | High, confirmed | NotesViewModel.loadChecklist | Every note starts a collector without canceling old collectors; old-note items can replace current checklist state. | flatMapLatest/canceled scoped job, I-002. |
| A-008 | High, confirmed | NotesViewModel:754–835 | AI reads saved snapshots; Apply/Undo lack revision checks; layout writes separately; cancel does not stop request job. | Request IDs, live scope, revision/transaction guard, I-008/I-017. |
| A-009 | High, confirmed | autoSortBoard/handleAskGeminiQuery | Sort is fixed two-column placement over allNotes; custom prompts are keyword routes or stock synthesis space names. | Honest local arrange; real prompts/tools, I-001/I-008/I-017. |
| A-010 | High, confirmed mismatch | SpatialCanvasView:540–548; SpatialCoordinates:16–21 | Raw IntOffset pixels versus 210.dp width; math assumes 210×140 world bounds while rendered height is content-driven. | Single geometry contract/migration, I-009. |
| A-011 | High, confirmed gap | NotesUiState; NoteEntity | Viewport only in memory; no dimensions, independent placement or Stack tables. | Board model, I-009–I-011. |
| A-012 | High, confirmed | deleteNoteById/deleteNotesInGroup; editor Delete | Permanent deletion; editor lacks comparable confirmation/recovery. | Trash and history, I-003. |
| A-013 | High, confirmed config | .gitignore, migration test, app Gradle, CI | /app/schemas ignored; historical schemas absent; androidTest assets unwired; connectedAndroidTest not run. Manual CREATE TABLE follows helper.createDatabase, which already needs the schema. | Genuine schemas and instrumentation, I-001. |
| A-014 | High, confirmed update risk | CI debug-keystore generation; debugConfig | Fresh signing certificate each CI build can make same-package upgrades fail, encouraging data-losing uninstall. | Stable private signing + upgrade tests, I-001. |
| A-015 | High, confirmed gap | Manifest and backup XML | Auto backup enabled with template rules; no deliberate credential/data policy or portable backup. | Explicit rules + portable recovery, I-001/I-016. |
| A-016 | Medium, confirmed | Editor:733–800 | Formatting appends literal placeholders instead of applying to selection; no content undo contract. | Structured selection commands, I-003/I-004. |
| A-017 | Medium, confirmed | wordCount; tag; filteredNotes | Body word count only; one tag; missing favorites/filters/FTS. Filtering depends on entire UI state, including pan/zoom. | Counter/query model, I-004/I-005. |
| A-018 | Medium, confirmed | Graph engine; ViewModel init | Offscreen pairwise physics; mutable objects shared across dispatchers; skips clearing zero notes; one-node loop never advances step count. | Lifecycle-scoped immutable graph, I-018. |
| A-019 | Medium, confirmed | Board notes.forEach; root allNotes/allLinks | Every card composed; whole entities observed widely, increasing large-corpus invalidation/memory risk. | Preview DTOs/culling/state isolation, I-009/I-019. |
| A-020 | Medium, confirmed | group repository methods; groupId | Related mutations nontransactional; no group FK; readable folder mirror can diverge. | Constraints and transactions, I-003/I-005. |
| A-021 | Medium, confirmed | active editor and CODE/checklist previews | Editor writes content but CODE previews codeSnippet/sample; checklist preview shows content, not actual tasks. | One document projection, I-003/I-004. |
| A-022 | Medium, confirmed | VoiceMemoRecorder:72–117; transcribeAudioMemo | Timer/random waveform only; “transcription” simulates from duration without audio. Not connected to active editor. | Preserve data; no fake feature, I-001. |
| A-023 | Medium, confirmed source | Theme.kt and UI | darkTheme ignored; hard-coded dark colors; tiny labels/controls; crowded editor top row. | Semantic themes and layout/a11y gate, I-004/I-006. |
| A-024 | Medium, confirmed | getNoteByTitle/createLink/acceptAiLink | Duplicate titles resolve arbitrarily; duplicate edges allowed; unknown AI suggestion can create a note implicitly. | Stable-ID links, explicit creation, I-003/I-008/I-018. |
| A-025 | Medium, confirmed | repository fallbacks; GeminiService | Errors erased; unsuccessful response not use-closed; cancellation not coupled to Call.cancel. | Typed outcomes/resource cleanup, I-001/I-007. |
| A-026 | Medium, confirmed | tracked files and wrapper properties | Pinned wrapper properties but no launcher scripts/JAR. | Reproducible wrapper, I-001. |
| A-027 | Medium, confirmed | position and checklist mutations | Board move changes note updatedAt; checklist mutation may not. “Recently edited” misrepresents content changes. | Separate document/metadata/layout clocks, I-003/I-009. |

This is not a count of independently reproduced device bugs.

### Priority reproduction cases

- Missing key/HTTP 401 → Format: must show failure, not canned replacement.
- Delay save A; type B; complete A: B remains pending and cannot be overwritten.
- Start save; Back while SAVING; fail DB: preserve draft and surface failure.
- Type then immediately open WikiLink: commit or block navigation with recovery.
- Open checklist A then B; mutate A: B must never show A's items.
- Generate from revision r; edit r+1; Apply: conflict/regenerate/new-note option, no overwrite.
- Compare rendered card corners/Fit/link anchors at density 1 and high-density phone.
- Lock note; search unique phrase; open Graph; request synthesis; inspect caches/export: no protected plaintext leakage.

## 3. Decisions and scope

| ID | Status | Decision/source | Implication |
|---|---|---|---|
| D-001 | LOCKED | Offline/local-first, user | Room + owned files authoritative; AI cannot block notes. |
| D-002 | LOCKED | Keep native stack, delegated choice grounded in code | No framework rewrite/server. |
| D-003 | LOCKED | Board/Library share notes, user | Remove placement ≠ delete note. |
| D-004 | LOCKED | Free overlap and deliberate Stacks, user | No collision-driven grouping/reflow. |
| D-005 | LOCKED | One visible Home Board initially, user | Multi-Board schema only. |
| D-006 | LOCKED | Existing groups become “Folders,” delegated terminology | Preserve IDs; no duplicate concept. |
| D-007 | ASSUMPTION | Stack scoped to one folder or Unfiled, from “within groups” | Cross-folder stacking requires confirmed moves. |
| D-008 | LOCKED | Continuous resize, user | Classes derived; exact dimensions stored. |
| D-009 | LOCKED | Dark Editorial Utility with light/system support | Muted content surfaces; restrained controls. |
| D-010 | LOCKED | BYOK/note assistance brought forward, current request | Current desire outranks draft “Later.” |
| D-011 | LOCKED | AI proposes; user applies, user | No autonomous organization/deletion. |
| D-012 | LOCKED | Dynamic models/manual fallback | No hard-coded best model. |
| D-013 | LOCKED | Characters ↔ estimated tokens, user | Local default; estimate labeled. |
| D-014 | LOCKED | Preserve existing links/checklists/sketch/audio data | No destructive “cleanup” of prototypes. |
| D-015 | LEANING | Use device PIN/password/pattern and optional biometric | Avoid separate app PIN cryptosystem; confirm if separate app passcode desired before security packet. |
| D-016 | LOCKED | Manual backup before sync, user | Real restore gate before portability claim. |
| D-017 | DEFERRED | Vector engine, sync transport, PC framework, PDF annotation | Decide in later bounded packets. |
| D-018 | LOCKED | Preserve observed minSdk 24, targetSdk 36 and IDs initially | “Choose SDK at bootstrap” superseded by existing code. |
| D-019 | ASSUMPTION | Board capture near viewport center; Library capture unplaced | Avoid teleport to offscreen Inbox; provide Inbox collection. |
| D-020 | LOCKED | No production mock AI or automatic upload | New-install examples optional; existing samples not auto-deleted. |
| D-021 | LOCKED | Separate content/metadata/layout revisions | Dragging does not count as content edit. |
| D-022 | LEANING | Trash/history retained until explicit cleanup initially | No surprise purge before recovery is established. |

Consequential defaults to revisit: Stack folder scope before I-011; device credential versus separate app passcode before I-015.

### Added recommendations

| Addition | Purpose / location | Complexity / tradeoff | Scope |
|---|---|---|---|
| Recoverable draft inbox | Storage/interruption recovery; launch/editor | Medium; save acknowledgments/journal | Core |
| Trash + layered undo | Prevent accidental loss; Library/menus/history | Medium; retains file references | Core |
| AI scope + change review | Prevent hidden upload/mutation; Assistant | Medium–High; validation/revisions | Core AI |
| Statistics detail | Useful for prompt/lore writing; editor chip | Low; estimates are not bills | Core |
| True task/code previews | Board accurately represents documents | Medium; unified projection | Core |
| Android share capture | Save text/files from other apps | Medium; untrusted URI/import handling | Important |
| Redacted diagnostics | Troubleshoot save/provider errors | Low–Medium; limited logs | Important |
| Templates/reusable AI prompts | Reduce repeated work | Medium; depends on stable editor | Deferred |
| Passive whole-vault AI monitoring | Suggestions without invocation | High; upload/battery/quota cost | Deferred |
| OCR/voice/handwriting recognition | Access more content | High; separate fidelity/privacy contracts | Deferred |

## 4. Feature contracts

Each numbered acceptance item below is F-ID.AC-number. Packet criteria supplement these contracts.

### F-001 — Local-first Notes Core · Core

Create durable note identity before editor opens; if creation fails, retain a draft and offer Retry. Keystrokes update a lifecycle-owned EditorSession immediately. Coalesce writes and serialize per note. Acknowledgment includes note/session/generation; “Saved” refers only to the current durable generation.

Back, note switches, WikiLinks, AI submission and document replacement pass through a flush barrier. Background flush is best-effort, not a guarantee Android always calls back before killing a process. Persist recoverable draft snapshots separately; large drafts never go into Bundle/SavedStateHandle. No explicit Save required; failures show Retry, Copy draft, Export draft.

**Acceptance**
1. Committed new/edit notes survive force-stop/relaunch and offline use.
2. Older completion cannot mark a newer draft saved or overwrite it.
3. Failed saves retain the draft and block explicit navigation until recovery/explicit discard.
4. Configuration change preserves text/caret/scroll; kill restores latest durable document/draft.
5. No promise claims unsaved, never-persisted keystrokes survive arbitrary termination.

### F-003 — Adaptive Resizable Cards · Core / Signature

Continuous world-dp width/height, exact final bounds stored. Micro shows title/type/privacy; Compact short excerpt; Standard meaningful text/tasks/media; Expanded more content and selected quick actions. Thresholds derive from usable content area and font scale, with hysteresis. Presets are magnetic landmarks, not restrictions. Zoom LOD additionally removes unreadable text.

Selected resize handle has a subtle visual with at least platform-minimum screen-space target. Provide menu presets and accessible move/resize alternatives.

**Acceptance**
1. Continuous sizes persist exactly after restart.
2. Growing checklist reveals actual rows without changing data.
3. At most one haptic per snap-landmark entry; settings respected.
4. Cancel restores start; failed commit offers retry/revert; zoom does not change dimensions.
5. Non-finite/zero bounds rejected; minimum sizes verified at supported font scales.

### F-004 — Stacks · Core

A Stack groups notes within a folder context; it does not own their lifetime. One Stack membership per note per initial Board; no nested Stacks.

Drag hover arms only over a deliberate target region with low movement and visible progress. “Stack with this” appears before release can group. Moving away, cancel, second-pointer transition or deleted target disarms. Release before activation is ordinary overlap. Menu/multi-selection creation is an equal alternative.

Collapsed Stack occupies one Board item; member placements remain hidden with original dimensions/restoration anchors. Focus overlay opens/reorders/adds/removes notes without scattering. Dissolve restores retained positions; “Arrange members here” is separate previewed action. Moving Stack translates retained anchors by the same delta. Drag-out places member at release; menu-remove restores its retained location.

Zero members deletes empty Stack; one member stays named until dissolved. Create folder from Stack previews all moves and offers Keep or Dissolve. Keep changes member folder IDs and Stack context; dissolve also restores placements. Existing source folders remain. Moving an individual member to another folder confirms removal from Stack; moving entire Stack moves all member folder relationships.

**Acceptance**
1. Brief overlap never creates Stack.
2. Armed drop creates/updates membership; same-folder operation leaves folders unchanged.
3. Cross-folder operation explicitly previews moves and cancels without mutation.
4. Focus/order/dissolve survive restart; no note duplicated/deleted.
5. Folder conversion applies all or none; Undo restores folders, membership and geometry.

### F-010 — Images & File Attachments · Core

Photo Picker for images; SAF for PDF/files. Copy to owned storage before document references it. Stream to generated staging path, enforce import limits while counting bytes, hash and validate. Retain original filename, MIME, actual size, importedAt and optional source URI. Never use user filename as storage path.

Commit files/metadata/document using the journal protocol in §10. Downsample/cache previews; protected media cannot use plaintext disk thumbnails. Missing file is a recoverable block state, not crash.

**Acceptance**
1. Picker cancel changes nothing.
2. Completed import remains offline-valid after URI revocation/source removal.
3. Interrupted copy creates no committed broken block; restart reconciles staging.
4. Board image preview never requires full-resolution decode.
5. Duplicate names, malformed MIME, low space and provider failure preserve note.

### F-011 — PDF Experience · Important

First-page preview; internal viewer with page count, next/previous, direct page choice, zoom and remembered page; share/export original. Baseline platform PdfRenderer off main thread, bounded cache, closed pages/descriptors. Review parser isolation in packet. Corrupt/unsupported/password-protected files show explicit fallback, not crash. Use read-granted content URI for sharing. Annotation/OCR/full-PDF text search deferred.

**Acceptance**
1. Multi-page PDF navigates offline after restart.
2. Corrupt/password/unsupported file has recoverable state.
3. Export preserves original bytes.
4. Cancel/exit releases resources; low-memory reopen works.

### F-014 — Smart Collections · Important

Versioned saved FilterSpec/SortSpec, not containers. Built-ins: PDFs, Images, Locked, Recently edited, Unfiled, Pinned, Has checklist, Favorites. Create from current filters; rename/reorder/duplicate/edit/delete. Same query engine as Library. Relative dates stay relative. Deleted folder/tag references cause visible invalid filter, never silently broaden query.

**Acceptance**
1. Deleting collection never deletes/moves notes.
2. Saved results equal equivalent live query after restart.
3. Relative dates update across midnight/timezone changes.
4. Invalid references are visible and repairable.

### F-015 — Customization · Important

Appearance: System/Dark/Light, accent, note palette, Board background, texture intensity/off, corners, depth. Board: default dimensions, guides, snapping/strength, haptics, Board Lock, zoom, density. Editor: font/line spacing, default capture type/template, toolbar groups. Motion: Reduced/Subtle/Balanced; system reduction is respected.

Preferences in DataStore; note styles/geometry in Room. Mini previews and per-category reset; custom palette combinations remain readable.

**Acceptance**
1. Preferences persist; changing theme/font/motion preserves draft/viewport.
2. Reduced motion removes traveling/scaling while retaining status.
3. Board Lock disables move/resize/Stack mutations, permits navigation/read.
4. Portable settings never overwrite credentials/device auth.

### F-016 — Backup / Export / Restore · Important

Versioned package includes manifest, notes/documents, folders/tags, links, Boards/items, Stacks, attachments, collections and portable preferences. Default includes Trash and history with visible size option. Exclude credentials, keys, logs, thumbnails and embedding caches.

Export consistent domain snapshot, not live SQLite file copy; pin immutable referenced media during export. Validate version/integrity/references/path safety/space/count and encryption before mutation. Initial modes: Import as copies with remapped IDs or Replace workspace with confirmation/recovery snapshot. No implicit timestamp merge.

Individual export: Markdown/text + assets + structured sidecar when needed for fidelity.

**Acceptance**
1. Round-trip restores content, structure, geometry, tasks/media/private notes after authentication.
2. Bad checksum/schema/path/space leaves current workspace untouched.
3. Import-as-copies cannot overwrite existing same-ID records.
4. Replace can recover after each crash boundary.
5. No key, protected plaintext or absolute device path in package.

### F-017 — History & Recovery · Important

Snapshot meaningful editing bursts/leaving editor and before AI/restore/transforms; deduplicate identical content. Not every keystroke/autosave becomes visible history. Record source/reason, parent, timestamp, schema and media refs. Restore appends a new current revision.

Separate content Undo, Board operation Undo, Trash, draft recovery and long-term history. Initial history retained until explicit cleanup; show storage use. Retained revisions pin their media.

**Acceptance**
1. Restore keeps intervening history.
2. AI apply has recoverable before-state.
3. Revision references prevent premature media deletion.
4. Private history stays encrypted/unavailable while locked.

### F-018 — Provider Framework · Important, brought forward

Profiles: OpenRouter, NanoGPT, Google AI Studio, Custom OpenAI-compatible. Profile stores ID/name/type/normalized endpoint/credential reference/model/options; individual model holds capabilities. Add/edit/delete, masked/reveal key, clear key, Test connection/generation, model refresh/search/sort/favorites and manual ID.

Separate catalog reachable, credential verified and generation verified. Cached list labeled stale offline. Filters: text generation, tools, structured output, vision, free/included where known; unknown stays unknown. Sort name/recent/favorite/context/known price. Nano subscription mode uses corresponding catalog AND generation route with no paid fallback.

No cleartext key in BuildConfig/preferences/backups/logs. Typed errors, cancellation, visible provider/model.

**Acceptance**
1. Built-ins discover live models and perform explicitly triggered test generation; no AI setup required for notes.
2. Auth/permission/quota/429/timeout/malformed/refusal/model missing distinguished.
3. Offline cached list usable; removed model not silently replaced.
4. Endpoint/key changes invalidate incompatible cache/test status.
5. Requests/logs/backups pass key-leak checks.

### F-019 — AI Note Assist · Important, brought forward

Presets: improve, spell check, grammar, format/organize, tidy, shorten, expand, tone, headings, checklist, summarize, action items, title; custom instructions.

Selection defaults to selected text, else current note. Read actual current draft after successful flush. Show scope/provider/model/included attachments before Send. Grammar/spelling preserve meaning; formatting preserves facts/links/tasks. Expansion needs review; summaries default separate output.

Validated document/range proposal, never arbitrary executable instructions. Before/after review, edit proposal, Copy, Save as new note, Cancel, Apply. Respect structured blocks/marks. Base revision + selection anchors guard apply; stale means regenerate or save separately.

Attachments excluded by default; explicit supported media inclusion required. Private notes excluded even during unlock unless per-request authorized. Foreground cancellable request; one active per scope; late old-session result ignored.

**Acceptance**
1. “Fix spelling only” is passed with actual current content, not routed to Board sort.
2. Cancel/error leaves original unchanged.
3. Apply is one undoable operation with history.
4. Stale selection/revision blocks Apply; double tap applies once.
5. Oversized scope explained; no silent truncation/whole-vault upload.

### F-020 — AI Organization · Important after core

Suggest folder/tags/Stack, related notes, inbox filing, duplicate/outdated review, natural-language organization and AI Arrange. Scope selected notes/current folder/current Board filter or deliberately all eligible notes.

Bounded read/propose tool registry. Local application validates and executes approved mutations. Filing review shows note/current/proposed/reason/checkbox. Board model proposes semantic grouping/order; local solver uses actual dimensions and fixed items, rendering ghost layout before Apply. Deterministic Align/Distribute/Tidy is not branded AI. Duplicate/outdated is suggestion, never autonomous deletion.

**Acceptance**
1. Scoped action cannot mutate out-of-scope note.
2. Ghost changes no durable state; Apply transactional and undoable.
3. Changed/deleted/moved targets invalidate affected proposals.
4. Hallucinated IDs/tools/arguments rejected.
5. Prompt-injection note cannot widen scope, access secrets or approve itself.

### F-021 — Semantic Search / Ask My Notes · Later

Separate embedding model/profile/capability. Consent to index eligible unlocked text; track model/dimensions/chunk hash/source revision/index version. Private content excluded including vectors/answer caches. Answers cite note/block context and admit insufficient evidence. Invalidate edited/deleted/locked sources immediately; async rebuild later. FTS remains functional. Engine selected in I-020 from current support, licensing and measured corpus behavior.

**Acceptance**
1. Citation opens exact source or reports deletion.
2. No stale/ineligible source used as current evidence.
3. Different embedding dimensions/models never mixed.
4. Index/network failure leaves local search intact.

### F-023 — Android ↔ PC Sync · Later

Replaceable engine; no server selected now. Stable UUIDs/revisions/tombstones/attachment hashes and portable schema prepared now. Future conflict handling cannot use timestamp alone: preserve concurrent versions. Device-only keys not sync format. PC uses portable model, not Room internals. Add outbox/cursors only when implementing sync.

**Acceptance**
1. Offline edits/open remain usable.
2. Repeated delivery idempotent.
3. Delete/edit conflicts preserve recovery without silent resurrection.
4. Attachments/private-note portability proven before claiming sync.

### F-024 — Character / Estimated Token Counter · Core addition

Footer chip toggles Characters ↔ Estimated tokens, preference persists. Detail shows words, selected text, title and totals. Default count is visible document text excluding title: task labels/code/captions included; formatting syntax/binary media/metadata excluded.

Characters = extended grapheme clusters including spaces/newlines; test emoji families/combining marks. Initial offline estimate may use a documented versioned heuristic such as ceil(Unicode code points / 4), labeled “≈ N tokens · estimate” with English-prose limitation; validate multilingual/code fixtures. Exact local tokenizer only when genuinely available for chosen model. Separate document count from full request count and provider-reported billable usage.

Remote countTokens is explicit because it transmits text; never per keystroke.

**Acceptance**
1. Toggle immediate and persistent.
2. Empty = zero; grapheme/selection fixtures correct.
3. No network during typing/counting.
4. Document/checklist/code/selection scope consistent; binary attachments excluded.
5. Estimate cannot be mistaken for actual request usage.

### F-025 — Folders, Tags, Favorites, Filters & Sort · Core addition

Preserve group IDs as folders; parent IDs cycle-checked. Multiple normalized tags; migrate entire legacy tag as one tag, never guess separators. Pin=list prominence; Favorite=bookmark; neither changes Board geometry.

Filters: folder/subfolders, tag ANY/ALL, pinned/favorite yes/no/any, attachment type, lock state, created/edited date/recency, checklist, kind, Unfiled. Across categories AND; within choices OR except explicit tag ALL. Sort edited/created newest/oldest and title A–Z/Z–A, optional pin-first, stable ID ties. UTC timestamps; calendar ranges use local timezone half-open intervals. Edited means contentEditedAt.

Bulk selection count explicit; mutations transactional or offer clear exclusions before apply.

**Acceptance**
1. All minimum filters combine/clear.
2. Normalized uniqueness/cycle rules enforced.
3. Filters/sort/pin/favorite persist; filters never delete placements.
4. Saved/cleared filter alters no note.
5. Task edit updates recency; card movement does not.

### F-026 — Board Placement & Navigation · Core addition

Separate BoardItem; notes can be unplaced. Persist x/y/width/height/z-order and settled camera. Touch slop distinguishes tap/drag; long press selects; multi-touch/cancel policy explicit. Guides nearby edges/centers; optional gentle snap. Front/back, Lock, Fit, Show on Board and accessible movement menus. Session manipulation undo. Never DB writes per pointer frame.

**Acceptance**
1. Overlap remains independent.
2. Fit/link anchors match actual bounds at density/zoom/font changes.
3. Restart restores geometry/camera.
4. Remove from Board keeps Library note.
5. Far zoom offers usable Open/Fit without unreadable mini text.

### F-027 — Structured Editor & Fidelity · Core addition

Versioned atomic NoteDocument with stable block IDs/marks. Text, heading, tasks/list/quote/code/divider/image/PDF/file plus preserved legacy sketch/audio. Selection formatting, IME composition, undo/redo, insertion, paste and portable import/export. No executable HTML. Unknown block retained raw with safe placeholder. Stable-ID WikiLinks; title disambiguation; missing target explicit creation. Retain legacy source until fidelity verified.

**Acceptance**
1. Bold acts on selection/caret, not appended placeholder.
2. CJK IME/emoji/paste/undo/keyboard preserve content/selection.
3. Tasks/code agree across editor/preview/export/search.
4. Unknown block round-trips.
5. No operation silently changes type or loses legacy fields.

### F-028 — Private Notes · Core security addition

Encrypt title/document/history/drafts/media; authenticated editor session, safe DTOs. Legacy isLocked is not proof of encryption; immediate honest labeling and later explicit upgrade. Board/Library generic “Locked note”; no title/snippet/thumbnail. Exclude from FTS/AI/persistent vectors/notifications/app-switcher previews. Folder association/existence remain limited visible metadata; this is not whole-vault anonymity.

**Acceptance**
1. Open requires supported authentication.
2. Background/relock clears access and previews.
3. Authentication/tamper failure renders no partial plaintext.
4. Lock transition handles old indexes/caches/history/drafts.
5. Portable encrypted backup works with new device keys.

### F-029 — Trash & Destructive Recovery · Core addition

Soft-delete hides notes from active Board/search/AI, retaining media/history/placements. Restore prior folder when valid else Unfiled with notice. No blindly resurrected deleted Stack. Default folder deletion keeps notes; destructive variant moves notes to Trash. Permanent deletion distinct and count-aware; media removed only with zero retained references.

**Acceptance**
1. Board/editor/bulk delete all enter Trash with Undo.
2. Restart then Restore retains document/media.
3. Delete/restore idempotent.
4. Permanent delete cannot remove shared retained media.

### F-030 — Links & Graph · Important addition

Keep manual/WikiLinks; stable-ID target and duplicate-edge rules. Initial manual undirected edges canonicalize pair; source-specific inline references separate. Graph secondary, lifecycle-paused, immutable snapshots, eligible scope. Graph positions never overwrite Board. Duplicate titles disambiguated.

**Acceptance**
1. Rename retains links.
2. Hidden/background Graph has no active simulation.
3. Delete all clears graph; one node does not spin.
4. Protected titles excluded.

### F-031 — Android Capture/Sharing · Important addition

Sharesheet text/URL/files → draft review → target folder/Board option → owned import. Resume/idempotency keyed by import session, not globally deduplicating identical text. Widgets/tile later. Outbound temporary content grants.

**Acceptance**
1. Shared text becomes editable offline-persistent note.
2. Canceled import leaves no partial attachment.
3. URI/HTML cannot execute/escape paths.
4. Process recreation offers resume/retry without duplicate import.

### F-032 — Diagnostics & Honest Status · Important addition

Settings: build/schema, last successful backup, provider status, storage. Redacted report contains operation/error category/provider/model/time/version, no keys/body/title/media or credential URLs. Actual AI input/output/total tokens when returned; unknown not zero. No fabricated price. Clear caches without deleting canonical content.

**Acceptance**
1. Report contains no secrets/note content.
2. Cache clear preserves notes.
3. Failures actionable, never fake success.

## 5. User journeys

| Journey | Flow / success | Failure/recovery |
|---|---|---|
| J-001 Capture | Board + → durable ID → editor → type → acknowledgment → compact placement at viewport. Library capture stays unplaced unless Add to Board chosen. | Create/save failure retains draft with Copy/Export/Retry. No type chooser required for ordinary note. |
| J-002 Arrange | Touch → slop → lift → direct tracking/guides → release → one committed operation. | Brief overlap stays overlap; armed target follows J-009; cancel/error restores or retries. |
| J-003 Resize | Select → handle/menu → continuous resize/adaptive preview → release. | Cancel restores start; bounds remain usable at large font. |
| J-005 Find | Search focuses → FTS results → filters → Open/Show on Board/Show in Folder. | Filter-aware zero results; safe index-rebuild state; no protected snippets. |
| J-006 Attach | Insert → system picker → staging progress → owned file/document commit → inline block. | Cancel, cloud source offline, permission loss and low space preserve original. |
| J-007 Lock | Menu → auth setup/check → flush → encrypt protected artifacts → generic card. Open → authenticate → session → leave/background → relock. | Missing device security offers setup; interrupted encryption has honest state/recovery. |
| J-008 AI edit | Selection/preset → scope/provider/model → request → before/after → edit proposal → Apply/Cancel. | Timeout/cancel unchanged; stale proposal blocks; save as new offered. |
| J-009 Stack | Same-folder notes → deliberate hover/menu → transaction → collapsed Stack → focus/reorder/drag-out. | Cross-folder moves explicit; folder conversion preview; full Undo. |
| J-010 Provider setup | Settings → type/key → catalog → compatible model → explicit test → profile. | Catalog ≠ key validity; manual model/cache offline fallback. |
| J-011 Recover | Trash/history/draft inbox → inspect → Restore. | Missing folder becomes Unfiled; missing media repair; private history authenticates. |
| J-012 Backup | Export options/destination → consistent package → completion. Import → validate → counts/mode → Apply → integrity report. | Precommit failure leaves workspace unchanged; swap journal recovers crash. |
| J-013 AI organize | Scope → instructions → proposal rows/ghost layout → choose → atomic Apply. | Conflict requires deselection/regeneration; no autonomous delete. |
| J-014 Share capture | Sharesheet → draft review → folder/placement → owned import/editor. | Lost URI reselect; canceled session cleans staging. |
| J-015 Ask notes, later | Scope → retrieve eligible chunks → grounded answer/source links. | Insufficient evidence stated; stale/private sources excluded. |
| J-016 Sync, later | Configure → initial comparison → conflict review → incremental exchange. | Offline editing continues; concurrent versions preserved. |

## 6. Navigation and screen specifications

Primary destinations: **Board** and **Library**. Search is global and opens a dedicated route. Capture is persistently reachable. Graph is secondary under Library/overflow. Settings is a conventional destination. Assistant is a contextual sheet/panel and dedicated review flow, not a permanent large input bar occupying Board space.

Routes use stable IDs. Save barriers live in controllers so Back, gesture back, WikiLink, results and deep links share behavior. Compact phone uses bottom navigation/full-screen editor; wider windows use rail and optional Library/editor split without replacing the active note session.

| Screen | Purpose/data | Anatomy, actions, state | Acceptance/accessibility |
|---|---|---|---|
| S-001 Home Board | North-star spatial view; BoardViewModel + lightweight previews | Compact folder/title/Search; canvas gets remaining space; New note; conditional viewport controls. Content at rest; border/context/resize on selection; lift during drag. Empty, filtered-empty, offscreen, protected, multiselect, ghost and commit-error states. | Fit actual bounds; no decorative dashboard header; accessible Open/Move/Resize/Stack/Folder/Pin/Lock/Trash; insets and screen-space targets at all zooms. |
| S-002 Library | Dense browsing; shared query engine | Folder breadcrumb/count, List/Grid, Search, filter/sort controls, relevant chips; long press bulk selection. Loading differs from empty. | Remember query/sort/scroll; large font reduces density; counts/checked state announced. |
| S-004 Editor | Quiet writing; EditorSession | Back, compact state, Undo/Redo/More; title as content; body; IME-aware format/insert toolbar + statistics. Metadata/history/export in More. Assistant from selection/action. | No redundant Save/Done; toolbar preserves caret; failure strip persists; protected session clears on leave. |
| S-005 Search | Immediate retrieval | Focused field/clear, active chips, context snippets, Open/Board/Folder result actions. | Zero results explains filters/Clear; escaped query syntax; keyboard/TalkBack flow; returning preserves scroll. |
| S-006 Stack Focus | Ordered subgroup without scattering | Phone sheet/wide panel, name/count, members, drag reorder + Move up/down, Add/Rename/Convert/Dissolve. | Return same viewport; Remove from Stack distinct from Trash; empty/one-member policy explicit. |
| S-007 Settings | Preferences/maintenance | Appearance, Board, Editor, Motion, AI, Privacy, Backup, Storage/Diagnostics; previews/reset. | Keeps draft; theme works; colors labeled. |
| S-008 Provider/Models | Profile setup | Type/name/endpoint, masked key, model list/search/filter/sort/favorites, catalog/test status, advanced supported options. | Offline/stale/error states distinct; manual ID; editing doesn't dismiss keyboard. |
| S-009 Assistant | Scoped request | Scope chips/provider/model, presets/free prompt, included-data disclosure, Send/Cancel/result. | No lost prompt on failure or silent scope expansion; progress/partial/canceled states explicit. |
| S-010 Change Review | Proposal inspection | Phone Before/After tabs + summary; wide side-by-side. Structured changes per note. Ghost Board with Apply/Cancel tray. | Stale/invalid Apply disabled; diff not color-only. Initial apply whole validated selection/document; partial-hunk apply deferred to avoid invalid blocks. |
| S-011 Media/PDF Viewer | Read owned attachments | Image fit/zoom; PDF page navigation; filename/share/export/open externally. Loading/corrupt/missing/unsupported/protected states. | Page announced; bounded cache/descriptors released; no private preview outside session. |
| S-012 Trash/History | Recovery | Trash Restore/Permanent delete; note revision timeline/reason/compare; separate draft inbox. | Explicit destructive counts; protected data authenticates; fallback Unfiled. |
| S-013 Backup/Restore | Portable recovery | Export scope/history/encryption/destination; validation progress; counts/warnings/mode; Apply/summary. | Cancel safe before commit; explain noncancelable commit interval; prior workspace recoverable. |
| S-014 Graph | Optional relationships | Scoped graph, focus/Fit and accessible equivalent list; settled/hidden pause. | No private labels or Board coordinate writes; empty/one-node stable. |
| S-015 Incoming Capture | Share session | Text/files preview, target folder, Add to Board, Save/Open. | Cancel cleans staging; duplicate delivery/resume idempotent. |

All screens render immutable UI state and emit typed events. Composables do not own encryption, canonical file writes, credentials or multi-entity transactions.

## 7. Visual system: Dark Editorial Utility

Choose an editorial workspace: charcoal canvas, warm readable text, muted paper-like note surfaces, sage action accent, restrained stone/ochre/sage/terracotta/blue/plum variants. Existing purple/cyan can remain optional accents.

Considered alternatives: bright paper desk (less aligned with dark identity) and dense technical cockpit (repeats current small-label/control overload). Chosen direction keeps spatial personality while prioritizing writing.

### Semantic starting tokens

These are a coherent design starting set, not a claim of final measured contrast.

| Role | Dark | Light |
|---|---|---|
| Canvas | #111416 | #F4F3EE |
| Surface | #1A1F21 | #FFFFFF |
| Elevated | #252B2D | #EAEDE7 |
| Primary text | #F1F1E9 | #202621 |
| Secondary text | #BBC3BD | #505C54 |
| Muted text | #8D9891 | #677269 |
| Border | #39413C | #CED5CC |
| Accent/selection | #B6C9AA | #405A3C |
| Danger | #F1AAA3 | #9C2F2A |

Create matching on-color tokens; contrast-test every combination, including note palettes/customization. State uses icon/label/border as well as color.

- **Typography:** verified licensed, bundled Manrope if suitable; local Android sans until verified. Starting roles: body 16sp, compact preview 14sp, metadata 12sp. These are design defaults; respect scaling and editor preference. No pervasive 8–10sp text. Code uses local monospace.
- **Density:** small 4dp unit with common 8/12/16/24 spacing; compact icons inside screen-space 48dp interactive targets. Optical alignment matters more than rigid spacing arithmetic.
- **Shape:** medium card rounding, smaller control rounding, pills for tags/filters/switches only.
- **Depth:** resting low, selected medium, dragged above surroundings, modal highest. Remove permanent heavy shadows.
- **Texture:** subtle locally available grain, off until contrast/performance reviewed; adjustable; no network font/texture dependency.
- **States:** focused, pressed, selected, disabled and busy explicitly designed; disabled action explains prerequisite.
- **Motion:** direct finger following; no theatrical bounce. Optional card→editor continuity cannot delay typing. Reduced mode removes travel/morphs but retains feedback.
- **Haptics:** pickup, entered snap landmark, Stack completion, confirmed destructive action; not every tap. Timing values centralized and tuned on device rather than arbitrary scattered constants.
- **Copy:** neutral new-note labels; no automatic “Architecture” tag or fake code. Empty Board explains capture; filtered empty explains filters. “Assistant” generic name, provider/model visible at request boundary.

## 8. State ownership and save correctness

| State | Owner/persistence |
|---|---|
| Notes/documents, folders/tags, history, placements, membership | Room canonical |
| Media and immutable revision files | Managed files with DB references |
| Preferences | DataStore |
| Draft, selection, IME, text undo | Per-note EditorSession; durable draft journal separately |
| Routes/IDs/restoration hints | Navigation/SavedStateHandle, small values only |
| Gesture/hover/ghost positions | Board session, boundary commit |
| Decrypted content/key access | Authenticated memory-only session; never SavedStateHandle |
| Catalogs, FTS, thumbnails/previews/vectors | Rebuildable derived state with explicit invalidation |
| AI request/proposal | Request ID/scope/base revisions; only approval mutates domain |

### Save algorithm

1. Emit immutable draft generation; mark pending.
2. Coalesce newest generation per note without discarding newer work while an older write runs.
3. Single ordered writer uses expected document revision where external restore/AI could race.
4. Acknowledge the exact generation. Clean only if it is current.
5. On failure retain current draft; never replace it with old Room emission.
6. Navigation awaits current-generation durability, not coroutine launch/global enum.
7. Lock/delete/restore/AI apply share coordinator and cannot bypass pending edits.
8. Propagate cancellation; do not convert it to success.
9. Best-effort background flush supplements durable journal; it is not a force-kill guarantee.

States: Clean → Editing → Saving → Clean only after current acknowledgment. New edits during Saving retain a newer pending generation; failure → SaveFailed → Retry. A delayed previous result cannot regress current session.

Selection-aware commands operate on block/range, preserve marks/link IDs/composition and group one undo operation. Formatting must not collapse caret/steal focus. Back dismisses transient UI/keyboard consistently before route exit. AI Undo requires expected post-apply revision; otherwise offer compare/history instead of overwriting later work.

## 9. Domain model and invariants

Conceptual entities; do not create a class/repository/module for every row without a job.

| Entity | Fields/relationships | Invariants |
|---|---|---|
| Note | UUID, schema/payload or encrypted envelope, title or protected title, createdAt/contentEditedAt/metadataEditedAt, deletedAt?/archivedAt?, folderId?, pin/favorite/style, documentRevision/metadataRevision/securityState | One canonical document; protected cleartext fields empty; IDs never reused. |
| NoteDocument | Schema version, ordered stable block IDs/types/payloads, spans/marks, media IDs/link targets | Unknown types preserved; atomic serialization; no HTML execution. |
| Folder | UUID, display/normalized name, parent?, order/appearance/timestamps/revision | No cycles; normalized name unique per parent. All Notes=query; Unfiled=null. |
| Tag/NoteTag | Stable tag ID/display/normalized name/appearance; unique note/tag join | Normalized global tag uniqueness. |
| Attachment | ID, noteId, managed relative key, filename/MIME/actual bytes/hash/import time/encryption version | READY files only referenced; untrusted names never paths. |
| Board | ID/name/isHome, cameraCenterWorldX/Y, zoom/timestamps | One Home initially; center+zoom restores across viewport changes. |
| BoardItem | ID/boardId, exactly one noteId or stackId, world-dp bounds/z-order/layoutRevision/time/removedAt? | FK-backed typed target; finite positive bounds; unique active note placement per Board. |
| Stack | ID/boardId/folderId?, name/presentation/revision | Not note owner; no nesting initially. |
| StackMember | Stack/note/order/retained placement | Unique membership per Board; same-folder invariant. |
| NoteLink | ID/endpoints/kind/label/style/revision | Valid IDs, edge uniqueness; inline source-block refs distinct from manual edge. |
| SmartCollection | ID/name/versioned FilterSpec/SortSpec/order | Query only; validated references. |
| NoteRevision | ID/note/parent/time/source/reason/schema/snapshot/media refs/encryption | Restore appends; retained refs pin media. |
| DraftJournal | Note/session generation/base revision/payload or ciphertext/writtenAt | Recover if newer than acknowledged canonical; no private plaintext. |
| SearchDocument/FTS | Integer rowid↔note UUID, derived title/body/tags/filenames | Only eligible nondeleted nonprotected notes. |
| ProviderProfile | ID/type/name/endpoint/credentialRef/model/options | Key absent; no silent model/endpoint switch. |
| OperationRecord | ID/kind/targets/base revisions/before-after or inverse/applied state/time | Idempotent atomic apply/undo; no executable payload. |
| ImportSession | ID/phase/staged paths/validation status | Restart reconciliation idempotent. |
| Preferences | Theme/editor/Board defaults/counter/query options | Portable and device-only subsets distinguished. |

Additional rules:

- UUID portable identity; integer FTS rowid internal.
- Use mutually exclusive noteId/stackId rather than unenforceable naked polymorphic targetId.
- Board move changes only layout revision; task toggle changes document revision/contentEditedAt.
- Queries check security/deletion eligibility even if an index updater lags.
- Soft deletion hides links/membership/placement without destroying recovery references.
- No cascade destroys media/history for soft delete.
- Notes exist independently of Board and viewport.
- Tombstones prepare sync; they do not imply current sync.

### Naming, import and relationship details

Normalize user-entered folder/tag names by trimming outside whitespace, Unicode NFC, and locale-independent case comparison; preserve original display spelling. Reject blank names. Folder uniqueness is among siblings, including root; tag uniqueness is global. Detect legacy normalized-name collisions during migration and present/record a deterministic disambiguating suffix rather than merging unrelated folders or changing note ownership silently. For Import as copies, preview disambiguated names and remap every note/tag/folder/link/Stack reference consistently.

Attachment type filters use explicit categories (image, PDF, audio, other file); multiple categories mean any matching attachment. Do not infer MIME from filename alone. Date/recency filters specify whether created or content-edited time is used. “Unfiled” includes no folder, independent of Board placement.

### Legacy document conversion

Preserve exact source first. Parse Markdown only losslessly; otherwise LegacyMarkdown block stores source with export path.

- DOC: preserve whitespace/body; recognize structure without silent rewriting.
- CODE: preserve both content and codeSnippet if different, as labeled separate imported blocks.
- CHECKLIST: preserve item IDs/order/checked state; note.content remains introduction.
- SKETCH: retain strokeData and read-only renderer/export.
- AUDIO: preserve duration/path and copy actual reachable bytes; missing bytes visibly unavailable, never fabricated audio.
- Links keep IDs; ambiguous title references remain unresolved pending choice.
- Existing content resembling sample data is not automatically deleted.

## 10. Migration and file protocols

### Incremental migrations

Before any schema/content migration, take a consistent private recovery checkpoint of the database and referenced files, with WAL-aware capture and a tested restore path. This is an internal migration safeguard available before the complete user-facing I-016 exporter. Preserve encryption for already protected content, exclude credentials from portable copies, and keep the checkpoint out of automatic cloud backup. Do not assume the user has a backup simply because the later roadmap contains one.

Use next actual schema number at implementation time; branch may have advanced beyond v3.

1. M-01: recover genuine v2/v3 schemas from source history/verified builds; track/wire fixtures. Establish whether v1 ever shipped; current source registers only 2→3.
2. M-02: add revisions/content-vs-metadata clocks/tombstones; introduce payload while retaining recovery source.
3. M-03: idempotent staged document conversion; validate before canonical switch. Do not maintain dual canonical writers indefinitely.
4. M-04: normalize tags/folders preserving group IDs; retire legacy mirror after all readers migrate.
5. M-05: Home Board + one item per existing note; normalize old pixels using upgrading device density; set intended dp dimensions; retain raw values/conversion density for recovery.
6. M-06: new entities as feature packets land, each with upgrade fixtures.
7. M-07: remove obsolete readers/columns only after fidelity/backup/upgrade gates. Never destructive fallback.

Old x/y stores no source-device density. Promise preservation on upgrading device, with before/after recovery snapshot; do not claim exact original cross-device appearance is reconstructible.

### Attachment import

1. Journal generated attachment ID; existing note unchanged.
2. Stream URI to private staging, count/hash/validate, cancel/low-space checks.
3. Finalize immutable file through same-filesystem atomic rename.
4. One DB transaction adds READY metadata, document block, history refs and completion.
5. DB failure after rename leaves unreferenced journaled file for retry/cleanup. Crash before rename cleans/restarts partial.
6. Garbage collection excludes refs from current notes, Trash, history, export pins and incomplete operations.

SQLite and filesystem do not share a transaction; a Room transaction alone cannot guarantee this flow.

### Backup/restore

- Consistent logical DB snapshot; immutable media pinned while copying; success only after completed package.
- Manifest: format/app/schema/document versions, timestamp, entry paths/hashes/sizes/counts and encryption envelope parameters.
- Validate path traversal, absolute/symlink/duplicate entries, decompression/count/size bounds, hashes, references, schema and finite geometry.
- Stage isolated DB/files; verify integrity; Import copies remaps ALL IDs/relationships; Replace uses recoverable workspace-swap journal.
- Keep old complete workspace until new opens/validates. Restart completes or rolls back journal.
- Never raw-copy open SQLite without WAL consistency.
- Incomplete destination export may need deletion/retry; never call it a successful backup; source remains unchanged.

## 11. Board engineering

### Coordinate contract

Logical world units are dp:

~~~
screenPx = (worldDp - cameraCenterDp) * density * zoom + viewportCenterPx
worldDp = (screenPx - viewportCenterPx) / (density * zoom) + cameraCenterDp
~~~

Layout, stored dimensions, guides, hit tests, resize, links and Fit share geometry service. Convert at input/render boundaries only.

Compose may already transform pointer deltas to local coordinates. Every handler documents local pixels versus screen pixels; do not divide by zoom twice. Instrumented render/gesture tests complement pure math.

States: Idle → PressCandidate → Dragging/Resizing → CommitPending → Idle. HoverCandidate/StackArmed are drag substates. Cancel restores initial geometry. Commit carries original layout revisions and final bounds. Failed persistence offers retry/revert. Target deletion, Board Lock changes, navigation/multi-touch cancel consistently. Persist z-order.

Pin prominence ≠ fixed spatial item. AI Arrange explicitly respects fixed-position option/Board Lock.

### Performance

Use compact preview DTOs, not full bodies/media. Cull outside viewport with measured prefetch margin; retain active dragged item. Draw relevant links using actual dimensions. Start with simple bounding-box metadata filtering; add spatial index only if measurement demands it. Separate state flows so gesture updates do not rerun Library filtering or editor parsing.

Stacks retain hidden member bounds/z/restoration anchors; moving translates anchors without resizing members. Same-folder scope avoids ambiguous partial Stacks under folder filters.

## 12. AI architecture and current service contracts

### Components

- ProviderAdapter: model discovery, serialization, response/error normalization and cancel.
- CredentialStore: device-encrypted secrets, profile references only.
- ModelCatalogRepository: profile-scoped cache/capabilities/manual IDs.
- AssistantController: scope, flush, request ID/progress/proposals.
- ContextBuilder: only eligible scoped fields/media within context budget.
- ToolRegistry/ProposalValidator: bounded reads and validated mutation proposals.
- ApplyCoordinator: authorization/revisions/transaction/history/undo; only it touches mutations.

Small packages/classes within existing module suffice. Reuse existing OkHttp/Moshi. No agent server/framework/MCP layer required.

### Endpoints verified 4 October 2026

| Provider | Catalog | Generate/auth | Notes |
|---|---|---|---|
| OpenRouter | GET https://openrouter.ai/api/v1/models | POST https://openrouter.ai/api/v1/chat/completions; Bearer | Modalities/context/pricing/supported_parameters inform capabilities; not universal routed-provider guarantees. |
| Nano standard | GET https://nano-gpt.com/api/v1/models?detailed=true | POST https://nano-gpt.com/api/v1/chat/completions; Bearer | Visibility can follow account settings; pricing units normalized. |
| Nano subscription | GET https://nano-gpt.com/api/subscription/v1/models?detailed=true | POST https://nano-gpt.com/api/subscription/v1/chat/completions | Both routes required; no paid/provider-selection overrides. |
| AI Studio/Gemini | GET https://generativelanguage.googleapis.com/v1beta/models with pagination | POST https://generativelanguage.googleapis.com/v1beta/{modelName}:generateContent; x-goog-api-key | Preserve returned models/... name once; supportedGenerationMethods filter; parse relevant text parts/finish/block/usage. |
| Custom compatible | Configured HTTPS base + models if supported | Base + chat/completions; configured key | Manual ID fallback; no claim of universal compatibility. |

Public catalogs may not validate credentials. Nano documents returning a list even with invalid keys; profile must distinguish “Models loaded” from “Generation test passed.” Explicit provider selection can invoke pay-as-you-go, so subscription mode must exclude it.

No model IDs/prices frozen into product defaults; select during setup from current list.

### Adapter interface and errors

~~~
discoverModels(profile, pageToken?) -> CatalogPage | ProviderError
generate(profile, model, messages, supportedOptions, requestId)
  -> Flow<Started | TextDelta | ToolProposal | Usage | Completed | Failed>
cancel(requestId)
countTokens(profile, model, request) -> Optional capability
~~~

Errors: NotConfigured, AuthInvalid, PermissionDenied, QuotaExceeded, RateLimited(retryAfter), ModelUnavailable, UnsupportedCapability, ContextTooLarge, SafetyRefusal, NetworkUnavailable, Timeout, MalformedResponse, Canceled.

- Close bodies via use; coroutine cancel calls Call.cancel; propagate CancellationException.
- Separate bounded catalog/generation timeouts, tuned by provider tests. Do not treat long reasoning like quick catalog fetch.
- Catalog GET can bounded-retry transient errors. Generation is not automatically replayed after uncertain transmission; user retry notes usage may already have occurred.
- Interrupted stream remains Incomplete/copyable, not automatically applicable.
- Send only supported/configured options. Empty/reasoning-only/refused output cannot be replacement success.
- Bound response size. HTML/error page becomes typed error.
- Prevent credential forwarding to changed-origin redirects; HTTPS, no embedded userinfo/keys in URLs. Cleartext local endpoints would require explicit future opt-in, not blanket permission.

### Scope/privacy/context

Scope carries allowed IDs/fields/ranges/media, base revisions and operation type. All notes requires deliberate choice; folder filters never ignored. Deleted/private/inaccessible notes excluded.

Do not silently take first 150/500 characters and claim complete analysis. If scope exceeds budget, show omitted/oversized notes and offer narrowing or explicit multi-request summary. No auto chunk-upload whole vault.

Only displayed selected content leaves device, directly to configured provider. BYOK does not promise zero provider retention. Show policy links; no developer-operated backend. Profile key only to configured origin.

### Tool registry

| Tool | Kind | Allowed result |
|---|---|---|
| search_notes | Read | Bounded eligible scoped IDs/snippets |
| read_notes | Read | Allowed fields of whitelisted IDs |
| propose_edit | Propose | Authorized document/range replacement + base revision |
| propose_title | Propose | One authorized title |
| propose_tags | Propose | Existing IDs/explicit new names |
| propose_folder_moves | Propose | Scoped IDs/allowed folder or proposed creation |
| propose_stack | Propose | Same-folder membership/Board |
| propose_links | Propose | Existing IDs/reasons; no implicit creation |
| propose_layout | Propose | Semantic groups/order; local geometry solver |

No deletion, credentials export, arbitrary network/shell/filesystem/SQL tool. “Sort” may mean content, Library order or Board arrangement; ask focused clarification when scope is ambiguous. Do not substring-dispatch.

Use native tool/structured output when supported; otherwise bounded JSON parsing + same validation, or text-only fallback. Invalid output is never an action.

### Proposal lifecycle

Created → Validated → Review → ApplyRequested → Revalidated → Applied / Conflict / Rejected.

Freeze request/profile/model/scope/base revisions. Ignore late results after cancel/session/scope change. Re-read targets before apply: authorization, existence, security state, revisions, folder invariants and schema.

Default checked-row application all-or-nothing. Conflict blocks until deselected/regenerated; no silent partial apply. Store inverse/history and changes in same transaction; operation ID makes double tap idempotent. Undo checks expected post-apply revisions.

Treat note/model text as untrusted. Delimiters help clarity but enforcement is local authorization/validator. Prompt injection cannot expand tool scope or approve proposals.

### Agent experience

A contextual assistant can inspect an explicitly selected set, ask bounded read tools and propose changes conversationally. Begin with one request/proposal cycle. Later iterative tool rounds need visible progress, call limits and Cancel. No background surveillance agent required.

## 13. Privacy and encryption

### Transition

Immediately replace false “Encrypted” claims with “Hide preview” and explain incomplete legacy protection. Apply uniform redaction/exclude from AI/search. Finished private notes encrypt title/document/history/drafts/media and sensitive attachment metadata; safe DTOs prevent raw entities reaching previews.

### Authentication

Recommended initial passcode is Android device PIN/password/pattern, optionally biometric. First lock checks secure setup. A separate app passcode changes recovery/KDF design and must be settled before I-015. API 24–29 versus newer authenticator combinations need compatibility tests.

### Cryptographic boundary

- Random per-note data key and authenticated encryption such as AES-256-GCM with fresh nonces and authenticated note/version context.
- Device wrapping key protects local data keys under supported auth/session policy.
- No protected plaintext in DB/search/thumbnail/journal/log/Bundle/backups.
- Large files need reviewed streaming encryption format/library; do not invent chunk-nonce protocol.
- Provider secrets use separate key and never enter note backup.
- Lock: pause writer → flush → encrypt current/history/media → atomically install references → clear plaintext projections/index/cache/journal → publish protected state. Journal interruptions.
- Prior SQLite/WAL/deleted pages/backups may contain historical plaintext. Rebuild/compact/checkpoint and review backup policy; do not promise forensic secure flash erasure or removal from prior remote backups.
- Relock on background/explicit action by default; auth activity transition handled as part of unlock state machine. Decrypted memory kept short-lived; managed runtime cannot promise perfect zeroization.
- Secure window/app-switcher for private editor. Copy/export is explicit disclosure.
- Folder association/existence may remain visible; explain that boundary.

### Portable backup security

Keystore keys are not portable recovery. For protected-note export, authenticate and rewrap data keys under a separate backup passphrase/recovery envelope.

I-015 must select/review a maintained password-KDF and streaming-encryption implementation, record algorithm/salt/work parameters/nonces/auth format and independent test vectors. A cipher name is not a complete password backup format. No custom crypto.

Reject protected backups lacking portable recovery envelope rather than reporting success. Explain lost passphrase/key invalidation honestly; no invented recovery backdoor.

Exclude keys/secret stores/protected working data/plaintext indexes from OS backup and transfer, including both legacy and Android 12+ rules. Recommended ordinary-note portability is explicit manual backup until full restore verified; do not rely on allowBackup alone.

## 14. Search/filter implementation

Start with FTS4 compatible with current Room baseline. Current docs also discuss newer support; no upgrade solely for FTS5 required. Confirm pinned API compatibility.

Internal integer rowid↔stable note ID projection; index eligible title/body/tags/attachment names. Bound parameters; compile/escape tokens rather than exposing raw MATCH syntax.

- Independent of Board viewport/placement.
- FilterSpec schema shared by Library/Search/Collections/AI scope.
- Narrow distinct query flow; pan/zoom/progress cannot rerun full search.
- Phrase/prefix/symbol/quote/non-Latin tests.
- Snippets map to actual block/range where possible.
- Protected notes absent from content search even when one editor is unlocked; Locked filter returns generic records.
- PDF body/OCR/embeddings not advertised after filename indexing only.
- Rebuild from eligible canonical data with visible state and safe title fallback.

## 15. Failure/edge-case matrix

| Condition | Required behavior |
|---|---|
| First launch | Empty + optional examples; no required AI setup. |
| Filtered-empty Board | Explain active filters/Clear, not “all notes gone.” |
| Huge note/corpus | Preview derived/truncated, source complete; query/decode off hot path. |
| Disk full save | Retain draft; Retry/Copy/Export; no Saved acknowledgment. |
| Rotate/background/kill | Latest durable recovery; relock; no automatic AI replay. |
| Stale Room emission | Cannot replace newer draft/gesture. |
| Note deleted during AI | Proposal conflicts; no resurrection. |
| Key/model invalid | Actionable profile error; prompt preserved. |
| Quota/429 | Explain retry boundary; no paid/provider fallback. |
| Network lost midstream | Incomplete proposal, no auto-apply. |
| Malicious file/archive | Staged validation, no path escape/partial mutation. |
| Missing file | Repair/remove block, keep document. |
| Future/malformed document | Safe unsupported read/export; no lossy overwrite. |
| Duplicate title | Target picker with folder/snippet; stable ID thereafter. |
| Deleted filter reference | Repair state, not query broadening. |
| Large text/TalkBack | Adaptive layout/gesture alternatives/focus. |
| Timezone/DST | Recompute calendar boundaries, UTC data unchanged. |
| Batch failure | Roll back; reconcile UI. |
| Restore crash | Journal recovery keeps old workspace. |
| APK signature mismatch | Stable-signed update/export migration; never uninstall first. |
| Credential/key change | Recovery explanation; no false decrypt success. |

## 16. Test/performance strategy

Planned coverage, not a claim these tests already exist:

- Unit: document codecs/unknown blocks, graphemes, filters/date boundaries, Stack invariants, geometry, proposal validation, revision conflict rules.
- Room integration: field-scoped concurrency, Trash/history, FTS eligibility, folder cycles, Stack conversion, operation idempotency.
- Migration instrumentation: genuine supported old schemas, content/checklist/link/media fidelity, malformed legacy reporting, no wipe.
- Compose: selection formatting, save barrier/error, checklist collection isolation, filter clear, safe private semantics, stale AI apply, adaptive keyboard behavior.
- MockWebServer/equivalent: pagination, wrong key/model, price units, unknown fields, SSE fragments/refusal/malformed/cancel/redaction.
- Files: interrupted import, corrupt PDF, missing media, archive traversal/decompression limits, checksum failure, staged restore rollback, repeated delivery.
- Security: no protected plaintext in managed stores, tamper failure, relock/invalidation, new-device encrypted restore.
- Device manual: S25-class phone plus min-supported/emulator, gesture navigation/keyboards, TalkBack/large font, dark/light, offline/low space, interruption.

### Measurements

Record launch, long-note typing/paste, Library scroll/search, Board pan/pinch/resize, media import/PDF paging and graph traces. Use synthetic 100/1,000/5,000-note fixtures as scale probes, not assumed user volume or universal guarantees. Include large documents/media/Stacks.

Measure frame deadlines at actual refresh rate, main-thread I/O, allocations, memory and write frequency. Required invariants: no DB write per pointer frame, no full image decode per card, hidden graph idle, search driven by query/data rather than pan. Set numeric budgets from baseline rather than unsupported “60fps” promises.

Visual gate: empty/populated/selected/error, keyboard, large text, both themes; inspect alignment/wrapping/contrast/insets/sheets/focus/filenames/targets. Passing build is not a visual review.

## 17. Implementation packets

Every packet must compile/run, preserve completed flows, document schema changes, and report acceptance as Passed / Failed / Not verified. Packet may be split into focused commits; do not expand into later features.

**G-ALL completion gate:** relevant build/unit/static checks, required instrumentation/manual flows, regression review and reviewable checkpoint. Never claim unrun checks passed. Stop scope expansion when repository reality materially contradicts the blueprint.

### I-001 — Truthful, reproducible baseline

**Outcome:** reproducible build and honest existing functionality.  
**Features/screens:** F-001/F-018/F-028/F-032; S-001/S-004/S-007/S-009. **Depends:** none.

**Work:** inspect HEAD/instructions versus audited SHA; restore official wrapper scripts/JAR/checksum and document CI toolchain without upgrading working pins. Track genuine historical schemas, wire androidTest assets and run migration instrumentation. Add PR validation; separate validation read permissions from release publishing. Remove automatic APK-to-branch commits/failure-issue spam from ordinary CI; preserve controlled artifact delivery. Establish stable private EXP signing and inspect existing-install certificate compatibility; if old key unavailable, plan explicit export/migration without uninstall. Remove production simulated-success fallbacks; typed visible errors. Rename deterministic arrange and restrict scope. Replace false encryption claims, uniform legacy-private redaction/AI/search exclusion. Explicit backup rules. Document/disconnect misleading prototype entry points without deleting content.

**Areas:** CI/Gradle/schema assets, GeminiService/repository, preview UI, backup XML.  
**Non-goals:** new document schema, full provider system, encryption or visual overhaul.

**Acceptance/tests**
- I-001.AC1: clean checkout builds through documented wrapper.
- AC2: missing key/401/network/empty response yields failure, never canned success.
- AC3: no false encryption claim or legacy-private outgoing AI/search content.
- AC4: genuine 2→3 instrumentation runs; other supported installed versions enumerated.
- AC5: stable-signed consecutive EXP builds upgrade without uninstall; old mismatched certificate reported as migration blocker.
- Focused network/privacy tests, migration instrumentation, offline launch/capture; G-ALL.

### I-002 — Editor save and navigation reliability

**Outcome:** trustworthy drafts and navigation.  
**Features/screens:** F-001/F-027; S-004. **Depends:** I-001.

Move draft/selection to EditorSession; generation acknowledgment/serialized writer; dirty until acknowledgment; Back/WikiLink/switch barriers; background flush/journal. Field-scoped metadata writes. Cancel old checklist stream; visible save errors.

**Areas:** active editor, EditorViewModel extraction, repository/DAO/lifecycle/draft store. Add the internal pre-migration checkpoint/restore safeguard before any new draft-journal schema migration (§10).  
**Non-goals:** rich block UI or AI redesign.

**Acceptance:** I-002.AC1 delayed A/new B race passes; AC2 failure during Back retains draft; AC3 WikiLink flush; AC4 configuration/background durable recovery; AC5 checklist isolation; AC6 metadata preserves geometry. Controlled-delay/storage-failure tests and device interruptions; G-ALL.

### I-003 — Versioned content, history and Trash

**Outcome:** unified content with recovery.  
**Features/screens:** F-017/F-027/F-029/F-030 preservation; S-004/S-012. **Depends:** I-002.

Versioned payload/stable blocks/lossless legacy wrapper; migrate tasks/code/sketch/audio/link refs; separate revisions/clocks; snapshot history; tombstones/Trash/restore/draft inbox. Transactions preserve children; legacy recovery fields retained until validated.

**Areas:** entities/DAO/database/codecs/repository/recovery UI.  
**Non-goals:** importer, embedding, sync.

**Acceptance:** I-003.AC1 mixed legacy fixture fidelity; AC2 delete/restore keeps refs; AC3 restore appends; AC4 unknown block round-trip; AC5 content and layout revisions independent. File-backed upgrade/relaunch tests; G-ALL.

### I-004 — Quiet editor, formatting and counters

**Outcome:** usable writing and requested statistics.  
**Features/screens:** F-024/F-027/editor F-015; S-004. **Depends:** I-003.

Block editing, selection-aware formatting/undo, WikiLinks, native IME, statistics chip/detail/DataStore, metadata More menu, remove Save/Done duplication. Semantic styling aligned with §7.

**Areas:** editor/command engine/counters/preferences/legacy adapter.  
**Non-goals:** complex rich paste or partial AI hunk apply.

**Acceptance:** I-004.AC1 formatting at selection; AC2 IME/caret stable; AC3 grapheme/counter preference; AC4 no network counting; AC5 code/tasks preview/export consistent; AC6 actionable save failure. Large-font/keyboard visual review; G-ALL.

### I-005 — Library organization, filters and FTS

**Outcome:** requested retrieval/filter/sort behavior.  
**Features/screens:** F-025/search F-001/F-030; S-002/S-005. **Depends:** I-003.

Folders/tag normalization/favorites; FilterSpec/SortSpec/shared Room queries; eligible FTS/snippets; bulk operations/query persistence; narrow state flows; link disambiguation. Search result Board action temporarily delegates to current Fit/open behavior until I-009, clearly labeled; do not pretend precise Show on Board exists early.

**Areas:** metadata schema/DAO/LibraryViewModel/KeepGridView/Search/filter sheet.  
**Non-goals:** semantic or saved collections.

**Acceptance:** I-005.AC1 filter truth table; AC2 unplaced/offscreen content found; AC3 privacy; AC4 duplicate-title targets; AC5 folder cycle rejection; AC6 proper content recency. Query/UI/offline corpus tests; G-ALL.

### I-006 — Navigation and design coherence

**Outcome:** consistent compact native shell.  
**Features/screens:** F-015/F-027; S-001/S-002/S-004/S-005/S-007. **Depends:** I-004/I-005.

Stable routes/save-aware navigation, measured insets, Board/Library destinations, contextual Assistant, secondary Graph. Semantic tokens, actual dark/light/system, muted palettes, neutral copy, reduced motion. Adaptive rail/split only preserving same session.

**Areas:** MainActivity decomposition/theme/shared components/navigation/preferences.  
**Non-goals:** Board geometry rewrite.

**Acceptance:** I-006.AC1 route state preservation; AC2 theme behavior; AC3 large-font/no clipped controls; AC4 motion preference; AC5 compact Board retains capture/search. Screenshot/TalkBack pass; G-ALL.

### I-007 — BYOK and live models

**Outcome:** working runtime provider setup.  
**Features/screens:** F-018/F-032; S-007/S-008. **Depends:** I-001/I-006.

Profiles/secret store, catalog cache/adapters/capabilities/manual ID; replace compiled key/model; Nano subscription route parity; typed errors/cancel/redaction/usage; explicit test generation.

**Areas:** remote adapters/profile store/settings/model picker.  
**Non-goals:** embeddings or organization agent.

**Acceptance:** I-007.AC1 fixtures for every provider; AC2 catalog≠auth; AC3 removed/offline/manual model; AC4 subscription no paid override; AC5 logs/backups key-free. Live tests only through user-provided profiles; report if not run. G-ALL.

### I-008 — Safe note assistant/custom prompts

**Outcome:** real useful writing assistance.  
**Features/screens:** F-019/F-017/F-032; S-004/S-009/S-010. **Depends:** I-003/I-004/I-007.

Presets/free instructions/scope/current draft context; request IDs/cancel/progress; before/after/Copy/New note/Apply; stale guards and history/undo. Remove keyword dispatcher.

**Areas:** AssistantController/ContextBuilder/validator/apply/review/editor.  
**Non-goals:** structural multi-note tools/vector search.

**Acceptance:** I-008.AC1 exact custom instructions; AC2 draft flush before request; AC3 timeout/cancel/refusal no mutation; AC4 stale apply blocked; AC5 idempotent apply/history; AC6 hidden media/private content excluded. Mock request/editor race tests; G-ALL.

### I-009 — Correct geometry and persistent Board

**Outcome:** dependable placement across density/relaunch.  
**Features/screens:** F-026/F-003 groundwork; S-001/S-005. **Depends:** I-003/I-006.

Board/items/camera schema, measured legacy migration, dp geometry/actual bounds/anchored zoom, z-order/Lock/move undo, unplaced behavior and real Show on Board, preview DTO/culling.

**Areas:** SpatialCoordinates/SpatialCanvasView/BoardViewModel/Room/search actions.  
**Non-goals:** resize/Stacks.

**Acceptance:** I-009.AC1 render/gesture agreement across densities/zoom; AC2 camera/item relaunch; AC3 remove placement≠delete; AC4 no pointer DB writes; AC5 filters retain positions; AC6 migration visual equivalence on upgrading device. Instrumentation/traces; G-ALL.

### I-010 — Adaptive resize

**Outcome:** signature resizable content cards.  
**Features/screens:** F-003/F-015; S-001. **Depends:** I-009/I-004.

Continuous bounds, landmark hysteresis/LOD, screen-space handle and accessible presets, haptic gating, task/code previews and dimension-aware links. Commit/undo/cancel.

**Acceptance:** I-010.AC1 exact dimensions persist; AC2 handles correct across density/zoom; AC3 previews adapt without source change; AC4 large-font bounds; AC5 exact geometry undo. G-ALL.

### I-011 — Intentional Stacks

**Outcome:** folder subgroups with predictable conversion.  
**Features/screens:** F-004; S-001/S-006. **Depends:** I-005/I-009/I-010.

Membership/focus, hover target/menu creation, reorder/add/remove/drag-out, retained anchors, dissolve/group movement, cross-folder confirmation and keep/dissolve folder conversion transaction.

**Acceptance:** I-011.AC1 overlap versus intent; AC2 cancel/multi-touch disarm; AC3 relaunch membership/order; AC4 conversion/Undo restores all relations; AC5 zero/one policy. G-ALL.

### I-012 — Owned attachments

**Outcome:** real offline images/files.  
**Features/screens:** F-010/F-027; S-004/S-011. **Depends:** I-003/I-004.

Picker/SAF/import journal/stream/hash/finalization, media blocks, bounded image cache, FileProvider sharing, history-aware GC.

**Acceptance:** I-012.AC1 picker cancel; AC2 source revoked after success; AC3 kill/low space no broken block; AC4 safe duplicate names; AC5 bounded decode; AC6 removed attachment retained by history. G-ALL.

### I-013 — PDF viewing

**Outcome:** practical offline PDFs.  
**Features/screens:** F-011; S-004/S-011. **Depends:** I-012.

First page, resource-managed page renderer/cache, page/zoom restore, export/external fallback, corrupt/password/unsupported states, parser isolation/minSdk review.

**Acceptance:** I-013.AC1 offline multi-page; AC2 corrupt safe failure; AC3 resources released; AC4 original byte export; AC5 low-memory reopen. G-ALL.

### I-014 — Collections and customization

**Outcome:** saved views and complete preferences.  
**Features/screens:** F-014/F-015; S-002/S-005/S-007. **Depends:** I-005/I-006/I-010/I-012.

Saved queries/built-ins/reference repair/date semantics; remaining Board/editor/texture/depth/motion controls/previews/category resets. Defaults do not rewrite existing card geometry.

**Acceptance:** I-014.AC1 query equivalence; AC2 collection deletion no notes lost; AC3 date rollover; AC4 preference portability; AC5 existing dimensions unchanged. G-ALL.

### I-015 — Private notes and portable crypto format

**Outcome:** real encrypted/authenticated notes and specified backup envelope.  
**Features/screens:** F-028/F-016 security prerequisite; S-004/S-007/S-011/S-012. **Depends:** I-003/I-007/I-012/I-013.

Settle auth model D-015. Select maintained crypto/KDF/streaming library using compatibility/license/security evidence; write envelope/test vectors before shipping. Encrypt document/history/media/draft, auth session, safe projections, index/cache purge, relock/key invalidation; explicit legacy hidden-note protection upgrade.

**Non-goals:** custom cryptography or sync server.

**Acceptance:** I-015.AC1 no private plaintext in managed stores after transition cleanup; AC2 auth/tamper/relock; AC3 per-request private AI consent; AC4 independent-device envelope decrypt; AC5 interrupted transition recovery. Security review/device auth required; unresolved primitive/compatibility choice blocks shipment, not papered over. G-ALL.

### I-016 — Backup/export/restore

**Outcome:** verified portable recovery before sync.  
**Features/screens:** F-016/F-017/F-032; S-007/S-012/S-013. **Depends:** I-003/I-005/I-011/I-012/I-014/I-015.

Snapshot/package/checksums/encryption, Markdown/text/assets/sidecar, staged validation, copy-ID mapping, replacement journal. Exclude keys/cache/plaintext protected data. Backup date only after success.

**Acceptance:** I-016.AC1 full fixture round-trip incl Stack/Trash/private media; AC2 precommit failure unchanged DB/files; AC3 swap crash recovery; AC4 malicious ZIP rejected; AC5 clean-install restore with new local keys. G-ALL.

### I-017 — AI organization/ghost layout

**Outcome:** controlled agent-like organization.  
**Features/screens:** F-020; S-001/S-002/S-006/S-009/S-010. **Depends:** I-005/I-008/I-011/I-015/I-016.

Bounded tools/scopes/limits/stable-ID validation, per-row changes/local layout solver/ghost rendering, transactional Apply and guarded Undo. Separate deterministic arrange.

**Acceptance:** I-017.AC1 injection cannot expand scope; AC2 schema/ID/revision validation; AC3 all-or-none checked changes; AC4 geometry respects dimensions/fixed items; AC5 secrets/tools remain inaccessible; AC6 no auto duplicate deletion. G-ALL.

### I-018 — Graph/link preservation and performance

**Outcome:** useful graph without background cost/leaks.  
**Features/screens:** F-030; S-014. **Depends:** I-005/I-009/I-015.

Reversible link dedup, inline-reference preservation, immutable graph state, visibility/lifecycle stop/start, zero/one-node fix, safe scope and profile focused subgraph instead of unbounded pairwise work.

**Acceptance:** I-018.AC1 rename retains link; AC2 hidden simulation idle; AC3 empty/one-node stable; AC4 privacy/scope; AC5 no Board mutations. G-ALL.

### I-019 — Native capture, diagnostics and polish gate

**Outcome:** daily-use release candidate.  
**Features/screens:** F-031/F-032 and cross-feature QA; S-001–S-015 excluding reserved S-003. **Depends:** I-013/I-014/I-016/I-017/I-018.

Sharesheet intake/outbound grants, redacted diagnostics/storage, accessibility/keyboard/adaptive visual states and performance traces. Remove demonstrably unused dependencies (e.g. Firebase) only after checking code/resources; no broad upgrades in polish packet.

**Acceptance:** I-019.AC1 capture interruption/idempotency; AC2 core journeys offline; AC3 a11y/theme/font/motion visual pass; AC4 representative performance regression comparison; AC5 upgrade-install + restore; AC6 Critical/High audit issues resolved or release-blocking. G-ALL.

### I-020 — Semantic retrieval · Later

**Features/screens:** F-021; S-005/S-009. **Depends:** I-005/I-007/I-015/I-016/I-019.

Current engine/provider/license/corpus evaluation; consent/chunks/index version, hybrid retrieval/citations, lock/delete invalidation and rebuild. Choose concrete engine here.

**Acceptance:** I-020.AC1 source-ID fidelity; AC2 no stale/private vectors; AC3 dimension/model rebuild; AC4 offline FTS; AC5 insufficient-evidence answer. G-ALL.

### I-021 — PC and sync · Later

**Features:** F-023; new PC/sync screens defined then. **Depends:** I-016/I-019, not I-020.

Choose PC/transport from actual needs; protocol/conflicts/encryption; outbox/cursors only now. First vertical slice create/edit/delete/media exchange.

**Acceptance:** I-021.AC1 concurrent versions preserved; AC2 duplicate/reordered delivery; AC3 tombstone/media integrity; AC4 private portability; AC5 sync disabled still usable. G-ALL.

### Release checkpoints

A: I-001–I-006 trustworthy writing.  
B: add I-007/I-008 useful BYOK.  
C: add I-009–I-011 spatial identity.  
D: add I-012–I-016 complete core.  
E: add I-017–I-019 daily-use polish.  
I-020/I-021 are later projects, not blockers for core. This is dependency sequencing, not a calendar estimate.

## 18. Traceability

Planned suite names, not claims of existing coverage.

| Feature | Journey | Screen | State | Packet | Acceptance → test |
|---|---|---|---|---|---|
| F-001 | J-001/J-005 | S-001/S-004/S-005 | Note/draft/journal | I-001–I-003 | AC1–5 → SaveGenerationRace, OfflineRelaunch, DraftRecovery |
| F-003 | J-003 | S-001 | Bounds/LOD | I-009/I-010 | AC1–5 → ResizePersistence, DensityGesture, FontScale |
| F-004 | J-002/J-009 | S-001/S-006 | Stack/member/folder | I-011 | AC1–5 → HoverVsOverlap, ConversionUndo |
| F-010 | J-006 | S-004/S-011 | Attachment/journal | I-012 | AC1–5 → ImportCrashMatrix, SourceRevocation |
| F-011 | J-006 | S-011 | PDF/view state | I-013 | AC1–4 → PdfLifecycle, CorruptPdf, OfflinePages |
| F-014 | J-005 | S-002/S-005/S-007 | Collection/query | I-014 | AC1–4 → QueryEquivalence, InvalidRef, Rollover |
| F-015 | J-001–J-003 | S-001/S-004/S-007 | Preferences | I-004/I-006/I-014 | AC1–4 → PreferenceRestore, BoardLock, Theme |
| F-016 | J-012 | S-013 | Package/staging | I-015/I-016 | AC1–5 → FullRoundTrip, ArchiveAttack, RestoreCrash |
| F-017 | J-008/J-011 | S-004/S-012 | History/media refs | I-003/I-008/I-015 | AC1–4 → HistoryAppend, MediaRetention, PrivateHistory |
| F-018 | J-010 | S-007/S-008 | Profile/key/catalog | I-007 | AC1–5 → ProviderContract, Redaction, CatalogVsAuth |
| F-019 | J-008 | S-004/S-009/S-010 | Scope/proposal/revision | I-008 | AC1–5 → ExactPrompt, StaleApply, CancelNoMutation |
| F-020 | J-013 | S-001/S-009/S-010 | Operation/layout | I-017 | AC1–5 → ScopeInjection, GhostApply, BatchRollback |
| F-021 | J-015 | S-005/S-009 | Embedding index | I-020 | AC1–4 → Citations, Invalidation, FtsFallback |
| F-023 | J-016 | Future PC/sync | Revisions/tombstones | I-021 | AC1–4 → Conflicts, IdempotentDelivery |
| F-024 | J-001/J-008 | S-004 | Text projection/prefs | I-004 | AC1–5 → Graphemes, CounterScope, NoNetworkTyping |
| F-025 | J-005/J-013 | S-002/S-005 | Metadata/query | I-005 | AC1–5 → FilterTruthTable, Cycles, Recency |
| F-026 | J-002/J-005 | S-001/S-005 | Board/item/camera | I-009 | AC1–5 → WorldDensity, PlacementVsNote, Relaunch |
| F-027 | J-001/J-006/J-008 | S-004 | Document/selection | I-003/I-004 | AC1–5 → LegacyFidelity, Selection, IME |
| F-028 | J-007 | S-001/S-004/S-011 | Encryption/session | I-001/I-015 | AC1–5 → LeakageMatrix, Relock, CrossDevice |
| F-029 | J-011 | S-002/S-012 | Trash/media refs | I-003 | AC1–4 → Restore, Idempotency, SafeGC |
| F-030 | J-005 | S-004/S-014 | Link IDs/graph | I-003/I-005/I-018 | AC1–4 → RenameLink, HiddenIdle, EmptyGraph |
| F-031 | J-014 | S-015/S-004 | Share session | I-019 | AC1–4 → ShareResume, URIValidation |
| F-032 | J-010/J-012 | S-007/S-008 | Redacted metadata | I-001/I-007/I-016/I-019 | AC1–3 → DiagnosticRedaction, CacheIntegrity |

**Supplied scenarios:** A free placement → I-009/I-011; B intentional Stack → I-011; C resize checklist → I-004/I-010; D search independent → I-005; E geometry/camera restart → I-009; F offline → I-002/I-012/I-015/I-019; G protected privacy → I-015 with interim I-001 honesty; H failed attachment → I-012.

## 19. Blueprint integrity review

Cold-reading review checked requirement survival, repository versus proposed behavior, migration safety, dependency order, privacy and traceability. This is a planning self-review, not security certification.

### Resolved contradictions

- AI “Later” versus current need: profiles/editing moved earlier; semantics/sync later.
- Groups/folders/Stacks: preserve existing IDs; Stacks separate, same-folder assumption explicit.
- Keep Stack after conversion: migrate members AND Stack context.
- Overlap versus grouping: visible armed target/menu.
- Continuous versus preset sizes: real dimensions, derived magnetic classes.
- Pixel versus dp: unified contract with one-time measured conversion.
- Autosave versus kill: durability guaranteed only for committed/journaled state.
- Current Boolean lock versus encryption: honest interim label; gated genuine protection.
- Lock versus FTS/history/backup: cover all derived stores and portable keys.
- Whole entity versus concurrent writes: scoped commands and revisions.
- File versus DB transactions: journal protocol required.
- Catalog versus auth: separate verified states.
- Sort versus arrangement: distinguish Library/content/Board.
- Pin versus position lock: distinct semantics.
- Prototype media versus preservation: keep data, no fake recording.
- Green CI versus full verification: instrumentation/device/provider limits explicit.
- Bootstrap SDK versus existing app: retain observed min 24 initially.
- Future sync versus device key: portable envelope/protocol required.

### Remaining bounded gates

1. Actual device visual/gesture/lifecycle verification remains unperformed here.
2. Confirm separate app passcode if desired before I-015.
3. Confirm different Stack folder scope if desired before I-011.
4. Select/review crypto/KDF/streaming library and minSdk auth behavior in I-015.
5. Historical installed schema versions/signature continuity in I-001.
6. Font license/assets and measured contrast before design completion.
7. Live user-key provider tests during I-007.
8. Vector engine/PC/transport only in later packets.

These gates are not permission for coding agents to invent security or silently change product behavior.

## 20. Official references

Checked 4 October 2026. Sources ground external API/platform facts; product design elsewhere is recommended behavior. Recheck relevant moving contracts during their packets. No fixed model availability/pricing promise.

| Ref | URL | Supports |
|---|---|---|
| R-01 | https://openrouter.ai/docs/api/api-reference/models/get-models | Model metadata/capabilities |
| R-02 | https://docs.nano-gpt.com/api-reference/endpoint/models | Catalog details/subscription/auth caveat |
| R-03 | https://docs.nano-gpt.com/api-reference/endpoint/chat-completion | Generation/subscription/provider-routing distinction |
| R-04 | https://ai.google.dev/api/models | Pagination/method support |
| R-05 | https://ai.google.dev/gemini-api/docs/api-key | Key handling/client extraction risk |
| R-06 | https://ai.google.dev/api/tokens | Explicit remote token counting |
| R-07 | https://developer.android.com/training/data-storage/shared/photo-picker | Selected-media access/fallback |
| R-08 | https://developer.android.com/training/data-storage/shared/documents-files | SAF access |
| R-09 | https://developer.android.com/training/data-storage/room/defining-data | Entities/FTS rowid/indexes |
| R-10 | https://developer.android.com/privacy-and-security/keystore | Device key storage/auth constraints |
| R-11 | https://developer.android.com/identity/sign-in/biometric-auth | Authentication compatibility |
| R-12 | https://developer.android.com/reference/android/graphics/pdf/PdfRenderer | PDF resource/render contract |
| R-13 | https://developer.android.com/identity/data/autobackup | Backup/extraction configuration |
| R-14 | https://github.com/drinkwaterdrink/Kinetic-notes2/actions/runs/37221246524 | Exact audited SHA CI success |
| R-15 | https://openrouter.ai/docs/api/api-reference/chat/create-a-chat-completion | Generation endpoint/request shape |

Personal runtime BYOK is different from embedding a developer-funded shared key. Local encrypted storage reduces at-rest exposure but cannot make a compromised device a server-side secret boundary. A public app with shared funded credentials requires different architecture.


### Immutable code evidence index

These links pin the audit to its inspected commit; symbols in §2 identify the relevant paths.

- [AI response/fallbacks](https://github.com/drinkwaterdrink/Kinetic-notes2/blob/a8e193c295317cf84c2ea4717f300e9b2aa842ea/app/src/main/java/com/example/data/remote/GeminiService.kt)
- [State, save, AI dispatch/apply](https://github.com/drinkwaterdrink/Kinetic-notes2/blob/a8e193c295317cf84c2ea4717f300e9b2aa842ea/app/src/main/java/com/example/ui/viewmodel/NotesViewModel.kt)
- [Active editor and toolbar](https://github.com/drinkwaterdrink/Kinetic-notes2/blob/a8e193c295317cf84c2ea4717f300e9b2aa842ea/app/src/main/java/com/example/ui/editor/FullScreenNoteEditorScreen.kt)
- [Board rendering/gesture/privacy](https://github.com/drinkwaterdrink/Kinetic-notes2/blob/a8e193c295317cf84c2ea4717f300e9b2aa842ea/app/src/main/java/com/example/ui/canvas/SpatialCanvasView.kt)
- [Geometry math](https://github.com/drinkwaterdrink/Kinetic-notes2/blob/a8e193c295317cf84c2ea4717f300e9b2aa842ea/app/src/main/java/com/example/ui/canvas/SpatialCoordinates.kt)
- [Persistence and AI repository](https://github.com/drinkwaterdrink/Kinetic-notes2/blob/a8e193c295317cf84c2ea4717f300e9b2aa842ea/app/src/main/java/com/example/data/repository/NoteRepository.kt)
- [DAO queries](https://github.com/drinkwaterdrink/Kinetic-notes2/blob/a8e193c295317cf84c2ea4717f300e9b2aa842ea/app/src/main/java/com/example/data/local/NoteDao.kt)
- [Database migration](https://github.com/drinkwaterdrink/Kinetic-notes2/blob/a8e193c295317cf84c2ea4717f300e9b2aa842ea/app/src/main/java/com/example/data/local/NoteDatabase.kt)
- [Migration instrumentation](https://github.com/drinkwaterdrink/Kinetic-notes2/blob/a8e193c295317cf84c2ea4717f300e9b2aa842ea/app/src/androidTest/java/com/example/NoteDatabaseMigrationTest.kt)
- [Graph physics](https://github.com/drinkwaterdrink/Kinetic-notes2/blob/a8e193c295317cf84c2ea4717f300e9b2aa842ea/app/src/main/java/com/example/domain/physics/ForceDirectedGraphEngine.kt)
- [Voice prototype](https://github.com/drinkwaterdrink/Kinetic-notes2/blob/a8e193c295317cf84c2ea4717f300e9b2aa842ea/app/src/main/java/com/example/ui/editor/VoiceMemoRecorder.kt)
- [Build and test configuration](https://github.com/drinkwaterdrink/Kinetic-notes2/blob/a8e193c295317cf84c2ea4717f300e9b2aa842ea/app/build.gradle.kts)
- [CI and signing](https://github.com/drinkwaterdrink/Kinetic-notes2/blob/a8e193c295317cf84c2ea4717f300e9b2aa842ea/.github/workflows/android-ci.yml)

## 21. Coding-agent kickoff — I-001 only

Attach this masterplan and copy:

~~~text
Work on drinkwaterdrink/Kinetic-notes2, branch:
arena/01a10467-kinetic-notes2

Audit baseline:
a8e193c295317cf84c2ea4717f300e9b2aa842ea

Read Kinetic_Notes_Masterplan_v1.md fully, then inspect current HEAD,
applicable AGENTS.md, Gradle/CI, active UI routes, persistence/network
paths and tests before editing.

Implement I-001 ONLY: Truthful, reproducible baseline.
Do not rewrite the app, implement later packets, add BYOK profiles,
migrate document content, or claim genuine encrypted-note protection.

First report:
1. Relevant differences from audited HEAD and already-fixed findings.
2. Relevant files and smallest I-001 change set.
3. Historical schema/build/signing evidence available.
4. Material blueprint/repository contradictions.
5. Exact tests/build/instrumentation/manual verification planned.

Required outcomes:
- Official pinned/verifiable Gradle wrapper and clean-checkout instructions;
  preserve current working version set unless evidence requires change.
- Genuine historical schemas, androidTest asset wiring and supported
  migration verification. Never invent historical schema fixtures.
- PR validation and separate controlled publishing. Do not push APK
  commits or send issue/comment messages.
- Safe stable EXP signing plan; no secret exposure, casual identity change
  or uninstall of a data-bearing app.
- No production AI fake success. Missing key, HTTP/network, empty response
  and parse failures become honest actionable states.
- Deterministic arrangement labeled honestly and scope-limited.
- Remove false "Encrypted" claim, retain legacy data, uniformly redact
  previews and exclude legacy-private content from search/AI.
- Intentional backup exclusions for credentials/protected working data.
- Document disconnected simulated features without deleting stored data.

Use focused regression tests. Preserve notes, links, checklist children,
groups and existing safe text updates. Keep mocks in tests.

After changes:
- Run build/unit/lint and relevant instrumentation.
- Report I-001.AC1–AC5 as Passed / Failed / Not verified individually.
- Explain environment-blocked checks; never imply they passed.
- Report changed files, data/upgrade impact, deviations and remaining risks.
- Leave a reviewable diff/checkpoint. Do not merge, publish, deploy or
  proceed to I-002 without a separate instruction.
~~~
