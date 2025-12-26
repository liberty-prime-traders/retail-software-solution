package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.listeners.ReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.model.BaseEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.model.TableName
import java.util.UUID

@Entity
@Table(name = TableNames.ORG_TABLE_REGISTRY)
@HasReference(tableName = TableName.ORG_TABLE_REGISTRY)
@EntityListeners(ReferenceNumberEntityListener::class)
class OrgTableRegistryEntity(
    @Column(name = "registry_id", nullable = false, insertable = false, updatable = false)
    var registryId: UUID,

    @Column(name = "default_prefix", nullable = false, unique = true)
    var defaultPrefix: String,

    @Column(name = "display_name", nullable = false, unique = true)
    var displayName: String,

    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null
): BaseEntity()
