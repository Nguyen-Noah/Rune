package runestone.panels

enum class ContentBrowserAction(type: Int) {
    None(0),
    Refresh(1),
    ClearSelections(2),
    Selected(3),
    Deselected(4),
    Hovered(5),
    Reload(7),
    Copy(8),
    Duplicate(9),
    Activated(10)
}
