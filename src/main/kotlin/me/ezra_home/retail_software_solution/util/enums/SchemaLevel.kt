package me.ezra_home.retail_software_solution.util.enums

enum class SchemaLevel(override val code: String) : HasCode {
    PLATFORM("PLT"),
    ORGANIZATION("ORG"),
    LOCATION("LOC")
}
