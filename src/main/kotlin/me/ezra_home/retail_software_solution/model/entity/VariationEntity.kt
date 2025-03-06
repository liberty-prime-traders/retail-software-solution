package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.ColumnDefault
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "variation")
open class VariationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "variation_id_gen")
    @SequenceGenerator(name = "variation_id_gen", sequenceName = "variation_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    open var id: Long? = null

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    open var name: String? = null

    @Column(name = "description", length = Integer.MAX_VALUE)
    open var description: String? = null

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    open var createdAt: OffsetDateTime? = null

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    open var updatedAt: OffsetDateTime? = null

    @NotNull
    @Column(name = "created_by", nullable = false)
    open var createdBy: UUID? = null

    @Column(name = "updated_by")
    open var updatedBy: UUID? = null

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    open var isActive: Boolean? = false
}