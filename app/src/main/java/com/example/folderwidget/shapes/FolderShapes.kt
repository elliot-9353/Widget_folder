package com.example.folderwidget.shapes

/** Registry of all available shapes. Add new shapes to [all]. */
object FolderShapes {
    val all: List<FolderShape> = listOf(
        FlowerShape,
        HorizontalShape,
        GridShape,
    )

    val default: FolderShape get() = all.first()

    fun byId(id: String?): FolderShape = all.firstOrNull { it.id == id } ?: default
}
