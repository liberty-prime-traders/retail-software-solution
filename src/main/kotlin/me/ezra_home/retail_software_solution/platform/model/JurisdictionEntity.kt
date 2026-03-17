package me.ezra_home.retail_software_solution.platform.model

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
@Table(name = TableNames.JURISDICTION)
@HasReference(tableName = TableName.JURISDICTION)
class JurisdictionEntity(

    @Column(name = "name", length = 200, nullable = false)
    var name: String,

    @Column(name = "jurisdiction_type_id", nullable = false)
    var jurisdictionTypeId: UUID,

    @Column(name = "parent_jurisdiction_id")
    var parentJurisdictionId: UUID? = null

) : HasReferenceEntity()
