package me.ezra_home.retail_software_solution.platform.business.prefix_sequence_tracker

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.BaseEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORGANIZATION_PREFIX_SEQUENCE_TRACKER)
class OrganizationPrefixSequenceTrackerEntity(
    @Column(name = "table_registry_id", nullable = false)
    val tableRegistryId: UUID,

    @Column(name = "prefix", nullable = false, length = 100)
    val prefix: String,

    @Column(name = "next_number", nullable = false)
    var nextNumber: Long = 1
) : BaseEntity()
