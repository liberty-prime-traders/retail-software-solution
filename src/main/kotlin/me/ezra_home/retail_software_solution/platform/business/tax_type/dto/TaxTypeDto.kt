package me.ezra_home.retail_software_solution.platform.business.tax_type.dto

import me.ezra_home.retail_software_solution.platform.business.tax_type.CalculationMethod
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxApplicationLevel
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxRecoveryType
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTrigger
import me.ezra_home.retail_software_solution.util.model.HasId
import java.time.OffsetDateTime
import java.util.UUID

data class TaxTypeDto(
    override var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var name: String,
    var description: String? = null,
    var calculationMethod: CalculationMethod,
    var taxRecoveryType: TaxRecoveryType,
    var taxApplicationLevel: TaxApplicationLevel,
    var taxTriggers: List<TaxTrigger>
) : HasId
