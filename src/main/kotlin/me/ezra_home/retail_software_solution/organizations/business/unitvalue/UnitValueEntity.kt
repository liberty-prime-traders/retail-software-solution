package me.ezra_home.retail_software_solution.organizations.business.unitvalue

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.UNIT_VALUE)
@HasReference(tableName = TableName.UNIT_VALUE)
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

    @Column(name = "units_of_base_per_unit")
    var unitsOfBasePerUnit: Long? = null,

    @Column(name = "system_defined", nullable = false, updatable = false)
    var systemDefined: Boolean = false

): HasReferenceEntity()
