package ir.kitgroup.formula.model


enum class MaterialType(val value: String) {
    MATERIAL("material"),
    PACKAGING("packaging")
}

enum class MaterialNature(val value: String) {
    PHYSICAL("physical"),
    VIRTUAL("virtual")
}
