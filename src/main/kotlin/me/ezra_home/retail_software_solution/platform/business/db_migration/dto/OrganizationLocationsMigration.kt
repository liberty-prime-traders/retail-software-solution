package me.ezra_home.retail_software_solution.platform.business.db_migration.dto

import me.ezra_home.retail_software_solution.platform.model.DbMigrationEntity

data class OrganizationLocationsMigration(
    val organizationMigration: DbMigrationEntity,
    val locationMigrations: List<DbMigrationEntity>
)
