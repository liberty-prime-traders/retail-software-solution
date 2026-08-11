package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeader
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionTotals
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class SaleSessionTest {

    @Test
    fun `no warning when productsVersion matches productsReservedAtVersion`() {
        val saleSession = buildSaleSession(productsVersion = 3, productsReservedAtVersion = 3)
        assertFalse(saleSession.showUnreservedChangesWarning)
    }

    @Test
    fun `warns when productsVersion is ahead of productsReservedAtVersion`() {
        val saleSession = buildSaleSession(productsVersion = 4, productsReservedAtVersion = 3)
        assertTrue(saleSession.showUnreservedChangesWarning)
    }

    private fun buildSaleSession(productsVersion: Long, productsReservedAtVersion: Long): SaleSession {
        val now = OffsetDateTime.now()
        val userId = UUID.randomUUID()
        return SaleSession(
            sessionId = UUID.randomUUID(),
            locationId = UUID.randomUUID(),
            saleId = UUID.randomUUID(),
            saleVersion = 1,
            productsVersion = productsVersion,
            productsReservedAtVersion = productsReservedAtVersion,
            originalStatus = SaleStatus.DRAFT,
            createdById = userId,
            createdAt = now,
            lastUpdatedAt = now,
            lastAccessedById = userId,
            lastAccessedAt = now,
            header = SaleSessionHeader(
                contactId = SystemContact.WALK_IN.id,
                soldById = null,
                dateSold = null,
                notes = null,
                referenceNumber = null,
            ),
            saleLines = emptyList(),
            saleAdjustments = emptyList(),
            salePayments = emptyList(),
            totals = SaleSessionTotals.ZERO,
        )
    }
}
