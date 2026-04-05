package me.ezra_home.retail_software_solution.platform.business.db_migration

data class OrganizationLocationsMigration(
    val organizationMigration: DbMigrationDto,
    val locationMigrations: List<DbMigrationDto>
)
