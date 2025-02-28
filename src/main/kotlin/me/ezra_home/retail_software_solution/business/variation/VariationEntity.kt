package me.ezra_home.retail_software_solution.business.variation

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "variation")
data class VariationEntity(
    @Id
    @Column(name = "variation_id")
    val variationId: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,

    @Column(name = "created_by", nullable = false)
    val createdBy: UUID,

    @Column(name = "updated_by")
    var updatedBy: UUID? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
)