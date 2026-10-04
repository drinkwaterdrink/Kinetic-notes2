package com.example.data.local

object SampleData {
    const val GROUP_ID_ARCHITECTURE = "group_architecture"
    const val GROUP_ID_CODE = "group_code"
    const val GROUP_ID_RESEARCH = "group_research"
    const val GROUP_ID_PRODUCT = "group_product"

    /** Demo groups, only ever inserted into a brand new database. */
    fun getInitialGroups(): List<NoteGroupEntity> = listOf(
        NoteGroupEntity(id = GROUP_ID_ARCHITECTURE, name = "Architecture", colorHex = "#6366F1", icon = "🏛️", orderIndex = 0),
        NoteGroupEntity(id = GROUP_ID_PRODUCT, name = "Product", colorHex = "#10B981", icon = "🚀", orderIndex = 1),
        NoteGroupEntity(id = GROUP_ID_CODE, name = "Code", colorHex = "#38BDF8", icon = "💻", orderIndex = 2),
        NoteGroupEntity(id = GROUP_ID_RESEARCH, name = "Research", colorHex = "#F59E0B", icon = "🔬", orderIndex = 3)
    )

    val NOTE_ID_ARCH = "node_1"
    val NOTE_ID_CODE = "node_2"
    val NOTE_ID_SPRINT = "node_3"
    val NOTE_ID_DESIGN = "node_4"
    val NOTE_ID_RESEARCH = "node_5"

    fun getInitialNotes(): List<NoteEntity> = listOf(
        NoteEntity(
            id = NOTE_ID_ARCH,
            title = "Workspace Architecture Spec",
            content = "Core framework operates on spatial coordinates with sub-pixel bezier routing. Targeting 60fps card transforms with kinetic card physics and [[Sprint Deliverables]].",
            type = NoteType.DOC,
            colorHex = "#6366F1", // Indigo
            tag = "Architecture",
            groupId = GROUP_ID_ARCHITECTURE,
            folder = "Architecture",
            x = 40f,
            y = 50f,
            isPinned = true,
            isLocked = false,
            dueDateText = "10-02",
            zIndex = 10,
            updatedAt = System.currentTimeMillis() - 3600000L
        ),
        NoteEntity(
            id = NOTE_ID_CODE,
            title = "Vector Bezier Routing",
            content = "Sub-pixel cubic bezier pathing engine connecting linked cards.",
            type = NoteType.CODE,
            colorHex = "#38BDF8", // Cyan
            tag = "Code",
            groupId = GROUP_ID_CODE,
            folder = "Code",
            x = 300f,
            y = 50f,
            codeSnippet = "const midX = (x1 + x2) / 2;\nconst c1 = `\${midX},\${y1}`;\nconst c2 = `\${midX},\${y2}`;\nreturn `M \${x1} \${y1} C \${c1} \${c2} \${x2} \${y2}`;",
            isPinned = false,
            isLocked = false,
            zIndex = 12,
            updatedAt = System.currentTimeMillis() - 1800000L
        ),
        NoteEntity(
            id = NOTE_ID_SPRINT,
            title = "Sprint Deliverables",
            content = "Production deliverables for spatial canvas milestone.",
            type = NoteType.CHECKLIST,
            colorHex = "#10B981", // Emerald
            tag = "Product",
            groupId = GROUP_ID_PRODUCT,
            folder = "Product",
            x = 40f,
            y = 260f,
            dueDateText = "09-28",
            isPinned = false,
            isLocked = false,
            zIndex = 14,
            updatedAt = System.currentTimeMillis() - 7200000L
        ),
        NoteEntity(
            id = NOTE_ID_DESIGN,
            title = "Design Critique Sync",
            content = "Reviewing spatial radar sync, interactive fluid gestures, and bottom Ask Gemini capsule layout with team.",
            type = NoteType.DOC,
            colorHex = "#EC4899", // Rose
            tag = "Priority",
            folder = "All Notes",
            x = 300f,
            y = 260f,
            dueDateText = "09-30",
            isPinned = false,
            isLocked = false,
            zIndex = 15,
            updatedAt = System.currentTimeMillis() - 5400000L
        ),
        NoteEntity(
            id = NOTE_ID_RESEARCH,
            title = "Sub-pixel Spatial Coordinates",
            content = "Investigating matrix transformation math for infinite zoom levels between 0.4x and 2.0x with zero jitter.\n\nConnected to [[Workspace Architecture Spec]].",
            type = NoteType.DOC,
            colorHex = "#F59E0B", // Amber
            tag = "Research",
            groupId = GROUP_ID_RESEARCH,
            folder = "Research",
            x = 170f,
            y = 470f,
            isPinned = false,
            isLocked = false,
            zIndex = 16,
            updatedAt = System.currentTimeMillis() - 14400000L
        )
    )

    fun getInitialLinks(): List<NoteLinkEntity> = listOf(
        NoteLinkEntity(sourceId = NOTE_ID_ARCH, targetId = NOTE_ID_CODE, colorHex = "#6366F1"),
        NoteLinkEntity(sourceId = NOTE_ID_ARCH, targetId = NOTE_ID_SPRINT, colorHex = "#38BDF8"),
        NoteLinkEntity(sourceId = NOTE_ID_SPRINT, targetId = NOTE_ID_DESIGN, colorHex = "#10B981")
    )

    fun getInitialChecklist(): List<ChecklistItemEntity> = listOf(
        ChecklistItemEntity(noteId = NOTE_ID_SPRINT, text = "Obsidian Knowledge Graph View (⌘G)", isChecked = true, orderIndex = 0),
        ChecklistItemEntity(noteId = NOTE_ID_SPRINT, text = "Samsung Notes S-Pen Inking Studio", isChecked = true, orderIndex = 1),
        ChecklistItemEntity(noteId = NOTE_ID_SPRINT, text = "Google Keep Due Date Badges", isChecked = true, orderIndex = 2),
        ChecklistItemEntity(noteId = NOTE_ID_SPRINT, text = "Bi-directional [[WikiLinks]] parser", isChecked = true, orderIndex = 3)
    )
}
