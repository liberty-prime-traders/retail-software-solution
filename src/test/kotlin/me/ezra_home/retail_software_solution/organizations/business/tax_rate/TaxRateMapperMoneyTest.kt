package me.ezra_home.retail_software_solution.organizations.business.tax_rate

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.organizations.business.tax_rate.api.TaxRateInsertDto
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.mappers.MoneyQualifier
import me.ezra_home.retail_software_solution.util.business.mappers.StringQualifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@WithDefaultOrganizationSession
class TaxRateMapperMoneyTest {

    private val taxRateMapper = TaxRateMapperImpl(StringQualifier, MoneyQualifier)

    @Test
    fun `the flat tax amount reaches the entity as the typed value`() {
        val taxRateInsertDto = TaxRateInsertDto(
            orgJurisdictionTaxTypeId = UUID.randomUUID(),
            name = "Levy",
            rateFlatAmount = Money.of(BigDecimal("12.75")),
            taxIsBilledToCustomerSeparately = false,
            taxIsIncludedInTaxableAmount = false,
            startDate = LocalDate.of(2026, 1, 1),
        )
        assertEquals(BigDecimal("12.75"), taxRateMapper.toEntity(taxRateInsertDto).rateFlatAmount)
    }
}
