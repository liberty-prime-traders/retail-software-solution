package me.ezra_home.retail_software_solution.locations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.PREFIX_CONFIGURATION)
class LocationPrefixConfigurationEntity(
    @Column(name = "table_registry_id", updatable = false)
    val tableRegistryId: UUID? = null,

    @Column(name = "prefix", length = 100)
    var prefix: String? = null,

    @Column(name = "updated_on")
    var updatedOn: OffsetDateTime? = null,

    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null
) : HasCreatorEntity()
