package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.platform.business.db_migration.MigrationStatus
import me.ezra_home.retail_software_solution.platform.business.db_migration.MigrationStatusConverter
import me.ezra_home.retail_software_solution.platform.business.db_migration.MigrationType
import me.ezra_home.retail_software_solution.platform.business.db_migration.MigrationTypeConverter
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerType
import me.ezra_home.retail_software_solution.util.enums.SchemaOwnerTypeConverter
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.DB_MIGRATION)
@HasReference(tableName = TableName.DB_MIGRATION)
internal class DbMigrationEntity(

    @Column(name = "db_version_id", nullable = false, updatable = false)
    val dbVersionId: UUID,

    @Column(name = "schema_owner_id", nullable = false, updatable = false)
    val schemaOwnerId: UUID,

    @Column(name = "schema_owner_type", nullable = false, length = 5)
    @Convert(converter = SchemaOwnerTypeConverter::class)
    val schemaOwnerType: SchemaOwnerType,

    @Column(name = "start_on", nullable = false)
    val startOn: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "end_on")
    var endOn: OffsetDateTime? = null,

    @Column(name = "status", nullable = false, length = 10)
    @Convert(converter = MigrationStatusConverter::class)
    var status: MigrationStatus,

    @Column(name = "type", length = 20)
    @Convert(converter = MigrationTypeConverter::class)
    var migrationType: MigrationType? = null,

    @Column(name = "message", length = 100)
    var message: String? = null,

    @Column(name = "migration_parent_id", updatable = false)
    var migrationParentId: UUID? = null

) : HasReferenceEntity()
