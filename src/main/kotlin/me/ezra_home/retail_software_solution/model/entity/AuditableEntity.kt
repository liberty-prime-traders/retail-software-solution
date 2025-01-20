package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.validation.constraints.NotNull
import me.ezra_home.retail_software_solution.business.util.usagecount.HasUsageCount
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SourceType
import java.time.OffsetDateTime
import java.util.UUID

@MappedSuperclass
abstract class AuditableEntity(
    @NotNull
    @Column(name = "created_by_id", nullable = false, updatable = false)
    var createdById: UUID? = null,

    @CreationTimestamp(source = SourceType.VM)
    @Column(name = "created_on", updatable = false)
    var createdOn: OffsetDateTime? = null,

    @Column(name = "predecessor_of_id")
    var predecessorOfId: UUID? = null,

    @Column(name = "usage_count")
    override var usageCount: Long = 0

): BaseEntity(), HasUsageCount
