package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevelConverter
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.TABLE_REGISTRY)
class TableRegistryEntity(
    @Column(name = "table_name", nullable = false)
    var tableName: String? = null,

    @Column(name = "default_prefix", nullable = false)
    var defaultPrefix: String? = null,

    @Column(name = "minimum_version_id", nullable = false)
    var minimumVersionId: UUID? = null,

    @Column(name = "schema_level", nullable = false)
    @Convert(converter = SchemaLevelConverter::class)
    var schemaLevel: SchemaLevel? = null,

    @Column(name = "display_name", nullable = false)
    var displayName: String? = null,

    @Column(name = "description", nullable = false)
    var description: String? = null,

    @Column(name = "user_facing", nullable = false)
    var userFacing: Boolean? = false,

    @Column(name = "next_number", nullable = false, insertable = false)
    var nextNumber: Long = 1L
): HasCreatorEntity()
