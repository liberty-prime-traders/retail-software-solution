package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime
import java.util.UUID

@MappedSuperclass
abstract class AuditableEntity(
    @NotNull
    @Column(name = "created_by_id", nullable = false)
    open var createdById: UUID? = null,

    @Column(name = "created_on", insertable = false, updatable = false)
    open var createdOn: OffsetDateTime? = null

): BaseEntity()
