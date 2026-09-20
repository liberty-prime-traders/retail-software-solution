package me.ezra_home.retail_software_solution.util.enums

enum class RtsRole(
    override val code: String,
    val tier: SchemaLevel,
    val permissions: Set<RtsPermission> = emptySet(),
    val requiredRoles: List<RtsRole> = emptyList()
) : HasCode {

    MANAGE_ORGANIZATION_USERS(
        "MOGU",
        SchemaLevel.ORGANIZATION,
        permissions = setOf(
            RtsPermission.ADD_USER_TO_LOCATION,
            RtsPermission.REMOVE_USER_FROM_LOCATION,
            RtsPermission.REMOVE_USER_FROM_ORG,
            RtsPermission.VIEW_USERS_OF_LOCATION,
            RtsPermission.VIEW_USERS_OF_ORG
        )
    ),

    VIEW_ORGANIZATION_USERS(
        "VOGU",
        SchemaLevel.ORGANIZATION,
        permissions = setOf(
            RtsPermission.VIEW_USERS_OF_LOCATION,
            RtsPermission.VIEW_USERS_OF_ORG
        )
    ),

    PLATFORM_ADMIN("PLAD", SchemaLevel.PLATFORM, permissions = setOf(RtsPermission.CREATE_ORGANIZATION))
}
