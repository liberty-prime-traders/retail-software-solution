package me.ezra_home.retail_software_solution.util.enums

enum class RtsRole(
    override val code: String,
    val tier: SchemaLevel,
    val requiredRoles: List<RtsRole> = emptyList()
) : HasCode {
    ADD_USER_TO_LOCATION("AUTL", SchemaLevel.ORGANIZATION),
    CREATE_ORGANIZATION("CRGO", SchemaLevel.PLATFORM),
    PLATFORM_ADMIN("PLAD", SchemaLevel.PLATFORM),
    REMOVE_USER_FROM_LOCATION("RUFL", SchemaLevel.ORGANIZATION),
    REMOVE_USER_FROM_ORG("RUFO", SchemaLevel.ORGANIZATION),
    VIEW_USERS_OF_LOCATION("VUOL", SchemaLevel.ORGANIZATION),
    VIEW_USERS_OF_ORG("VUOO", SchemaLevel.ORGANIZATION)
}
