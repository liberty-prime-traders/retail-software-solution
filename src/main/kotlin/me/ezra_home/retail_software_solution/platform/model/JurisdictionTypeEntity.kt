package me.ezra_home.retail_software_solution.platform.model

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
@Table(name = TableNames.JURISDICTION_TYPE)
@HasReference(tableName = TableName.JURISDICTION_TYPE)
internal class JurisdictionTypeEntity(

    @Column(name = "name", length = 100, nullable = false)
    var name: String,

    @Column(name = "description", length = 500)
    var description: String? = null

) : HasReferenceEntity()
