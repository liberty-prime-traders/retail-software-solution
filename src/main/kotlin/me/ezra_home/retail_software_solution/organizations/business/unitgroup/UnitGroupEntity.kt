package me.ezra_home.retail_software_solution.organizations.business.unitgroup

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.UNIT_GROUP)
@HasReference(tableName = TableName.UNIT_GROUP)
class UnitGroupEntity(

    @Column(name = "code", length = 5, updatable = false)
    var code: String? = null,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "system_defined", nullable = false, updatable = false)
    var systemDefined: Boolean = false

): HasReferenceEntity()
