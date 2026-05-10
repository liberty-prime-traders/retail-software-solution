package me.ezra_home.retail_software_solution.locations.business.tax_entry

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.TAX_ENTRY)
@HasReference(tableName = TableName.TAX_ENTRY)
class TaxEntryEntity(

    @Column(name = "source_reference_number", nullable = false, updatable = false)
    var sourceReferenceNumber: String,

    @Column(name = "source_type", nullable = false, length = 5, updatable = false)
    var sourceType: TaxSourceType,

    @Column(name = "tax_type_id", nullable = false, updatable = false)
    var taxTypeId: UUID,

    @Column(name = "fiscal_period_id", nullable = false, updatable = false)
    var fiscalPeriodId: UUID,

    @Column(name = "calculation_method", nullable = false, length = 5, updatable = false)
    var calculationMethod: CalculationMethod,

    @Column(name = "rate", nullable = false, precision = 19, scale = 4, updatable = false)
    var rate: BigDecimal,

    @Column(name = "tax_inclusive", nullable = false, updatable = false)
    var taxInclusive: Boolean,

    @Column(name = "taxable_amount", nullable = false, precision = 19, scale = 4, updatable = false)
    var taxableAmount: BigDecimal,

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4, updatable = false)
    var taxAmount: BigDecimal

) : ImmutableEntity()
