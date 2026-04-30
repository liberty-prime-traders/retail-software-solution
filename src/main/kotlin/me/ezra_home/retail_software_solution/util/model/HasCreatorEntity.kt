package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.SourceType
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime
import java.util.UUID

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class HasCreatorEntity(
    @CreatedBy
    @Column(name = "created_by_id", updatable = false)
    var createdById: UUID? = null,

    @CreationTimestamp(source = SourceType.VM)
    @Column(name = "created_on", updatable = false)
    var createdOn: OffsetDateTime? = null

): BaseEntity()
