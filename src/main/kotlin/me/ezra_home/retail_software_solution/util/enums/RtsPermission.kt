package me.ezra_home.retail_software_solution.util.enums

enum class RtsPermission(
    override val code: String,
    val tier: SchemaLevel
) : HasCode {
    ADD_USER_TO_LOCATION("AUTL", SchemaLevel.ORGANIZATION),
    CREATE_ORGANIZATION("CRGO", SchemaLevel.PLATFORM),
    LIBERTY_ADMIN("LBAD", SchemaLevel.ORGANIZATION),
    MANAGE_LOCATION_ACCESS("MLA", SchemaLevel.LOCATION),
    CREATE_MANUAL_JOURNAL("CMJ", SchemaLevel.ORGANIZATION),
    REVERSE_MANUAL_JOURNAL("RMJ", SchemaLevel.ORGANIZATION),
    VIEW_MANUAL_JOURNAL("VMJ", SchemaLevel.ORGANIZATION),
    MANAGE_ORGANIZATION_ACCESS("MOA", SchemaLevel.ORGANIZATION),
    REMOVE_USER_FROM_LOCATION("RUFL", SchemaLevel.ORGANIZATION),
    REMOVE_USER_FROM_ORG("RUFO", SchemaLevel.ORGANIZATION),
    VIEW_USERS_OF_LOCATION("VUOL", SchemaLevel.ORGANIZATION),
    VIEW_USERS_OF_ORG("VUOO", SchemaLevel.ORGANIZATION)
}
