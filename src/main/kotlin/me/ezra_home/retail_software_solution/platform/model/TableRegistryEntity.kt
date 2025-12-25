package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevelConverter
import me.ezra_home.retail_software_solution.util.model.BaseEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.TABLE_REGISTRY)
class TableRegistryEntity(
    @Column(name = "table_name", insertable = false, updatable = false)
    var tableName: String,

    @Column(name = "default_prefix", nullable = false)
    var defaultPrefix: String,

    @Column(name = "minimum_version_id", insertable = false, updatable = false)
    var minimumVersionId: UUID,

    @Column(name = "schema_level", insertable = false, updatable = false)
    @Convert(converter = SchemaLevelConverter::class)
    var schemaLevel: SchemaLevel,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

    @Column(name = "description", nullable = false)
    var description: String,

    @Column(name = "user_facing", nullable = false)
    var userFacing: Boolean = false,

    @Column(name = "validated", nullable = false)
    var validated: Boolean = false,

    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null
): BaseEntity()
