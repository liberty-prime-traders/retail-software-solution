package me.ezra_home.retail_software_solution.util.enums

enum class RtsRole(
    override val code: String,
    val tier: SchemaLevel,
    val requiredRoles: List<RtsRole> = emptyList()
) : HasCode {
    PLATFORM_ADMIN("PLAD", SchemaLevel.PLATFORM),
    CREATE_ORGANIZATION("CRGO", SchemaLevel.PLATFORM),
    ORG_ADMIN("ORAD", SchemaLevel.ORGANIZATION),
    LOCATION_ADMIN("LCAD", SchemaLevel.LOCATION)
}
