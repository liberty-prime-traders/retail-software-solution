package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationDto
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

class SubledgerBalanceReconciliationJobTest {

    private val organizationService = mock(OrganizationService::class.java)
    private val contactService = mock(ContactService::class.java)
    private val subledgerBalanceReconciliationJob = SubledgerBalanceReconciliationJob(organizationService, contactService)

    private val failingOrganization = organization("org-a")
    private val healthyOrganization = organization("org-b")
    private val unprovisionedOrganization = organization(null)

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `every provisioned organization is reconciled under its own session even when an earlier one fails`() {
        `when`(organizationService.getAllOrganizationDtos())
            .thenReturn(listOf(failingOrganization, unprovisionedOrganization, healthyOrganization))
        val reconciledOrganizationIds = mutableListOf<UUID?>()
        doAnswer {
            reconciledOrganizationIds.add(SessionContextProvider.getOrganizationIdOrNull())
            if (reconciledOrganizationIds.size == 1) throw IllegalStateException("boom")
            null
        }.`when`(contactService).reconcileSubledgerBalances()

        subledgerBalanceReconciliationJob.run()

        assertEquals(listOf(failingOrganization.id, healthyOrganization.id), reconciledOrganizationIds)
        assertNull(SessionContextProvider.getOrganizationIdOrNull())
    }

    private fun organization(schemaName: String?) = OrganizationDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
        referenceNumber = "OR000001",
        name = "Org ${schemaName ?: "pending"}",
        schemaName = schemaName,
        timezone = "UTC"
    )
}
