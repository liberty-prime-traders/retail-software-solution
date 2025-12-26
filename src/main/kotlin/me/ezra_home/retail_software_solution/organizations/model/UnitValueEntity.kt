package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.listeners.ReferenceNumberEntityListener
import me.ezra_home.retail_software_solution.util.model.AuditableEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID
import me.ezra_home.retail_software_solution.util.model.TableName

@Entity
@Table(name = TableNames.UNIT_VALUE)
@HasReference(tableName = TableName.UNIT_VALUE)
@EntityListeners(ReferenceNumberEntityListener::class)
class UnitValueEntity(

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "code", nullable = false)
    var code: String,

    @Column(name = "unit_group_id", nullable = false)
    var unitGroupId: UUID,

    @Column(name = "base_unit")
    var baseUnit: UUID? = null,

    @Column(name = "conversion_factor")
    var conversionFactor: Double? = null,

    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null

): AuditableEntity()
