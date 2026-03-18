package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ExpirableDateAssignmentEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.time.LocalDate
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.ORG_JURISDICTION_TAX_TYPE)
@HasReference(tableName = TableName.ORG_JURISDICTION_TAX_TYPE)
class OrgJurisdictionTaxTypeEntity(

    @Column(name = "jurisdiction_tax_type_id", nullable = false, updatable = false)
    var jurisdictionTaxTypeId: UUID,

    startDate: LocalDate,
    endDate: LocalDate? = null

) : ExpirableDateAssignmentEntity(startDate, endDate)
