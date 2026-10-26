package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactInsertDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.IdentityType
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.mappers.MoneyQualifier
import me.ezra_home.retail_software_solution.util.business.mappers.StringQualifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@WithDefaultOrganizationSession
class ContactMapperMoneyTest {

    private val contactMapper = ContactMapperImpl(StringQualifier, MoneyQualifier)

    @Test
    fun `the credit limit reaches the entity as the typed value`() {
        val contactInsertDto = ContactInsertDto(
            contactTypes = emptySet(),
            identityType = IdentityType.INDIVIDUAL,
            firstName = "Wanjiku",
            creditLimit = Money.of(BigDecimal("150.50")),
        )
        assertEquals(BigDecimal("150.50"), contactMapper.toEntity(contactInsertDto).creditLimit)
    }
}
