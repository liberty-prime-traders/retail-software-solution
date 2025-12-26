package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SourceType
import java.time.OffsetDateTime
import java.util.UUID

@MappedSuperclass
abstract class HasCreatorEntity(
    @Column(name = "created_by_id", nullable = false, updatable = false)
    var createdById: UUID? = null,

    @CreationTimestamp(source = SourceType.VM)
    @Column(name = "created_on", updatable = false)
    var createdOn: OffsetDateTime? = null
): BaseEntity()
