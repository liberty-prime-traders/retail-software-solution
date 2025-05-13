package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.util.UUID

@MappedSuperclass
abstract class AuditableEntity(
    @Column(name = "predecessor_of_id")
    var predecessorOfId: UUID? = null,

    @Column(name = "usage_count")
    var usageCount: Long = 0

): HasCreatorEntity()
