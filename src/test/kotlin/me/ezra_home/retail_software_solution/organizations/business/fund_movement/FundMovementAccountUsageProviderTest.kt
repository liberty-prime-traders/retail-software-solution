package me.ezra_home.retail_software_solution.organizations.business.fund_movement

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountUsageType
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate

class FundMovementAccountUsageProviderTest {

    private val fundMovementRepository = mock(FundMovementRepository::class.java)
    private val fundMovementAccountUsageProvider = FundMovementAccountUsageProvider(fundMovementRepository)

    @Test
    fun `reports the references of movements that debit or credit the account`() {
        val fundMovementEntity = FundMovementEntity(
            FundMovementType.TRANSFER_BETWEEN_ACCOUNTS, "001.001", "001.002", BigDecimal("10.0000"), LocalDate.of(2026, 3, 1), null
        ).also { it.referenceNumber = "FM000001" }
        `when`(fundMovementRepository.findTop10ByDebitAccountCodeOrCreditAccountCodeOrderByCreatedOnDesc("001.002", "001.002"))
            .thenReturn(listOf(fundMovementEntity))

        assertEquals(AccountUsageType.FUND_MOVEMENT, fundMovementAccountUsageProvider.usageType)
        assertEquals(listOf("FM000001"), fundMovementAccountUsageProvider.getReferences("001.002"))
        assertEquals(emptyList<String>(), fundMovementAccountUsageProvider.getReferences("001.003"))
    }
}
