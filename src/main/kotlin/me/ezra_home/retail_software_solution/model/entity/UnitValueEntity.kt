package me.ezra_home.retail_software_solution.model.entity

import java.util.UUID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import me.ezra_home.retail_software_solution.model.util.TableNames

@Entity
@Table(name = TableNames.UNITVALUE)
class UnitValueEntity(

    @Column(name = "name", nullable = false)
    var name: String? = null,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "code", nullable = false)
    var code: String? = null,

    @Column(name = "unit_group_id", nullable = false)
    var unitGroupId: UUID? = null,

    @Column(name = "base_unit")
    var baseUnit: UUID? = null,

    @Column(name = "conversion_factor")
    var conversionFactor: Double? = null,

): AuditableEntity()