# Implementation Plan: Kinetic Canvas (Spatial Board & AI Assistant)

Transform **Kinetic Notes** into the exact spatial canvas experience showcased in the reference video and Gemini canvas specification, prioritizing the spatial board home with folder spaces, dashed Bézier connection threads, floating bottom **"Ask Gemini"** AI assistant capsule, and a modern distraction-free executive Focus editor.

---

## 1. Visual & Architectural Alignment with Reference
Based on the screen recording and design reference:
* **Background & Theme:** Architectural dark dot-matrix canvas (`#0A0D14` with sub-pixel dots at 26px/13px grid).
* **Top Navigation Bar:**
  * "KC" avatar pill + "Kinetic Canvas" branding + `v4.0 Pro` version badge.
  * Folder / Spaces switcher pill (e.g., "All Notes", "Architecture", "Sprint", "Research").
  * Action items: Thread Linking icon button + vibrant purple/indigo `+ Create` button.
* **Spatial Board Cards:**
  * Dark glassy surfaces (`#131722` / `rgba(20, 26, 39, 0.94)`) with subtle white borders and neon accents.
  * Card header: category tag pill with glowing indicator dot (Architecture, Product, Code, Priority), connect thread button, and quick delete/archive.
  * Card body: title, live preview (Markdown text, progress bar `4/4 (100%)` for checklists, code block preview).
  * Card footer: relative timestamp (e.g., "2h ago") and `FOCUS ⛶` button.
* **Connection Threads:**
  * Animated/sub-pixel curved dashed Bézier curves connecting linked cards with color matching the source tag.
  * Interactive thread link mode (tap Note A then Note B to create link).
* **Floating Bottom Canvas Controls:**
  * Zoom & reset HUD capsule: `[-]  55%  [+]  •  Reset` located at bottom-left.

---

## 2. Floating "Ask Gemini" Bottom Capsule & AI Capabilities
A floating bottom capsule bar inspired directly by the screen recording:
* **UI Elements:**
  * Expanding bottom pill: `+` action button, text input with placeholder `"Ask Gemini"`, microphone icon, and submit sparkle icon.
* **Core AI Actions (as requested by user):**
  1. **"Auto-Sort & Organize Board":** Gemini analyzes all notes on the canvas and clusters them logically into folder spaces with clean spatial layout coordinates.
  2. **"Tidy & Format Note":** Cleans up messy notes, improves structure with headers, bullet points, and syntax highlighting.
  3. **"Vault Synthesis & Summary":** Summarizes connections and action items across selected notes or the entire active space.
  4. **"Ask Board Anything":** Natural language Q&A about notes on the canvas.

---

## 3. Executive Focus Editor (Distraction-Free)
Replacing the sketch pad with a polished executive focus view:
* **Full-Height Focus Modal:** Smooth slide-in presentation when tapping `FOCUS ⛶` on any card.
* **Top Ribbon:** Folder assignment, accent tag selector, due date picker, pin status, and `Done` action.
* **Editor Tools:**
  * Rich Markdown editing with live `[[WikiLinks]]` linking.
  * One-tap **"AI Beautify"** button to reformat notes into clean, neat executive layouts.
  * Embedded checklists with real-time progress bars.
  * Code blocks with line numbering and syntax copy.

---

## 4. Proposed Implementation Steps

1. **Model & State Updates (`NoteEntity.kt`, `NotesViewModel.kt`):**
   * Add folder/space classification (`spaceId`, `folderName`) and due date fields to Room database.
   * Expand Gemini service with automated sorting, beautify/neatness formatting, and board-wide synthesis prompts.
2. **Kinetic Canvas Viewport (`SpatialCanvasView.kt`):**
   * Update dot-matrix background to match the exact architectural styling.
   * Restyle cards to match the reference: category badge, connect button, timestamp, and `FOCUS ⛶` button.
   * Update dashed Bézier SVG/Canvas link renderer with glowing endpoints.
   * Implement floating `[-] XX% [+] Reset` zoom pill at bottom-left.
3. **Ask Gemini Floating Bar (`AskGeminiBottomBar.kt`):**
   * Create the floating capsule bar at bottom-center with suggestions sheet (Sort board, Tidy note, Summarize vault).
4. **Executive Focus Modal (`NoteDetailSheet.kt`):**
   * Refine into a distraction-free executive workspace with AI-guided neat formatting, Markdown [[WikiLinks]], and task checklist progress.
5. **Compilation & Verification:**
   * Build and verify using `compile_applet` and execute unit tests.
