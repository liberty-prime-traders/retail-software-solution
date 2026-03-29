package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ExpirableDateAssignmentEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.TAX_RATE)
@HasReference(tableName = TableName.TAX_RATE)
class TaxRateEntity(

    @Column(name = "org_jurisdiction_tax_type_id", nullable = false, updatable = false)
    var orgJurisdictionTaxTypeId: UUID,

    @Column(name = "name", length = 100, nullable = false)
    var name: String,

    @Column(name = "rate_percentage", precision = 5, scale = 2)
    var ratePercentage: BigDecimal? = null,

    @Column(name = "rate_flat_amount", precision = 15, scale = 4)
    var rateFlatAmount: BigDecimal? = null,

    startDate: LocalDate,
    endDate: LocalDate? = null

) : ExpirableDateAssignmentEntity(startDate, endDate)
