package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountUsageType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class LoanAccountUsageProviderTest {

    private val loanRepository = mock(LoanRepository::class.java)
    private val loanRepaymentRepository = mock(LoanRepaymentRepository::class.java)
    private val loanAccountUsageProvider = LoanAccountUsageProvider(loanRepository, loanRepaymentRepository)

    @Test
    fun `reports the loans and repayments that use the account as their asset account`() {
        val loanEntity = LoanEntity(UUID.randomUUID(), "001.001", BigDecimal("10.0000"), LocalDate.of(2026, 3, 1), null)
            .also { it.referenceNumber = "LOAN000001" }
        val repaymentEntity = LoanRepaymentEntity(UUID.randomUUID(), "001.001", BigDecimal("5.0000"), LocalDate.of(2026, 3, 2), null)
            .also { it.referenceNumber = "LNRP000001" }
        `when`(loanRepository.findTop10ByAssetAccountCodeOrderByCreatedOnDesc("001.001")).thenReturn(listOf(loanEntity))
        `when`(loanRepaymentRepository.findTop10ByAssetAccountCodeOrderByCreatedOnDesc("001.001")).thenReturn(listOf(repaymentEntity))

        assertEquals(AccountUsageType.LOAN, loanAccountUsageProvider.usageType)
        assertEquals(listOf("LOAN000001", "LNRP000001"), loanAccountUsageProvider.getReferences("001.001"))
        assertEquals(emptyList<String>(), loanAccountUsageProvider.getReferences("001.002"))
    }
}
