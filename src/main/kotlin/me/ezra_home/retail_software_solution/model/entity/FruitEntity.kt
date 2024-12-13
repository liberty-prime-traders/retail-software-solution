package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.ColumnDefault
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@Entity(name = "FruitEntity")
@Table(name = "fruit", schema = "public")
open class FruitEntity {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    open var id: UUID? = null

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    open var name: String? = null

    @Size(max = 255)
    @Column(name = "alternate_name")
    open var alternateName: String? = null

    @Size(max = 50)
    @NotNull
    @Column(name = "color", nullable = false, length = 50)
    open var color: String? = null

    @NotNull
    @Column(name = "cost", nullable = false, precision = 10, scale = 2)
    open var cost: BigDecimal? = null

    @NotNull
    @Column(name = "edible_ind", nullable = false)
    open var edibleInd: Boolean? = false

    @NotNull
    @Column(name = "created_by_id", nullable = false)
    open var createdById: UUID? = null

    @ColumnDefault("now()")
    @Column(name = "created_on")
    open var createdOn: OffsetDateTime? = null
}