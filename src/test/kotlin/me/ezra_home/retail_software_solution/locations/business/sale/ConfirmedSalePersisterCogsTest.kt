package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.ConfirmedSalePersister
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineSaveRequest
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSaveRequest
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleLineConsumedCost
import me.ezra_home.retail_software_solution.locations.business.stock.api.SaleStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockReserver
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class ConfirmedSalePersisterCogsTest {

    private val saleId = UUID.randomUUID()
    private val saleLineId = UUID.randomUUID()
    private val saleLineClientKey = UUID.randomUUID()
    private val saleRepository = mock(SaleRepository::class.java)
    private val saleLineRepository = mock(SaleLineRepository::class.java)
    private val saleStockUpdater = mock(SaleStockUpdater::class.java)
    private val saleDataFetcher = mock(SaleDataFetcher::class.java)
    private val saleSaveFinalizer = mock(SaleSaveFinalizer::class.java)

    private val confirmedSalePersister = ConfirmedSalePersister(
        saleRepository = saleRepository,
        saleLineRepository = saleLineRepository,
        stockReserver = mock(StockReserver::class.java),
        saleStockUpdater = saleStockUpdater,
        saleConfirmedHandlerForKafka = mock(SaleConfirmedHandlerForKafka::class.java),
        saleDataFetcher = saleDataFetcher,
        fiscalPeriodService = mock(FiscalPeriodService::class.java),
        saleSaveFinalizer = saleSaveFinalizer,
        saleValidator = mock(SaleValidator::class.java),
        locationProductDataFetcher = mock(LocationProductDataFetcher::class.java),
        saleTaxFinalizer = mock(SaleTaxFinalizer::class.java),
    )

    private val saleLineEntity = SaleLineEntity(
        saleId = saleId,
        locationProductId = UUID.randomUUID(),
        userProvidedQuantity = BigDecimal("2.0000"),
        unitId = UUID.randomUUID(),
        lineTotal = BigDecimal("20.0000"),
        defaultSalePrice = BigDecimal("10.0000"),
        storageUnitsPerUserProvidedUnit = 1L,
    ).also {
        it.id = saleLineId
        it.referenceNumber = "SALN000001"
    }

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1")
            )
        )
        val draftSale = SaleEntity(contactId = UUID.randomUUID(), status = SaleStatus.DRAFT)
            .also {
                it.id = saleId
                it.referenceNumber = "SALE-1"
            }
        `when`(saleDataFetcher.loadDraftAtVersion(saleId, 0L)).thenReturn(draftSale)
        `when`(saleLineRepository.findBySaleId(saleId)).thenReturn(listOf(saleLineEntity))
        `when`(saleLineRepository.saveAll(anyList<SaleLineEntity>())).thenReturn(listOf(saleLineEntity))
        `when`(
            saleSaveFinalizer.finalize(
                draftSale,
                saleSaveRequest,
                SaleLineSync.Result(mapOf(saleLineClientKey to saleLineId), listOf(saleLineEntity))
            )
        ).thenReturn(SaleSaveFinalizer.SaleSaveFinalizeResult(emptyMap(), emptyMap()))
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a draft line has no cost until confirm stores the consumed cost on it`() {
        `when`(saleStockUpdater.consumeStock(anyList(), anyString()))
            .thenReturn(listOf(SaleLineConsumedCost(saleLineId, BigDecimal("16.0000"))))

        assertNull(saleLineEntity.cogsTotal)

        confirmedSalePersister.confirm(saleSaveRequest)

        assertEquals(BigDecimal("16.0000"), saleLineEntity.cogsTotal)
        verify(saleLineRepository, times(2)).saveAll(listOf(saleLineEntity))
    }

    private val saleSaveRequest = SaleSaveRequest(
        saleId = saleId,
        expectedVersion = 0L,
        contactId = UUID.randomUUID(),
        soldById = UUID.randomUUID(),
        dateSold = null,
        notes = null,
        saleLines = listOf(
            SaleLineSaveRequest(
                clientKey = saleLineClientKey,
                existingId = saleLineId,
                locationProductId = saleLineEntity.locationProductId,
                userProvidedQuantity = saleLineEntity.userProvidedQuantity,
                unitId = saleLineEntity.unitId,
                storageUnitsPerUserProvidedUnit = saleLineEntity.storageUnitsPerUserProvidedUnit,
                lineTotal = saleLineEntity.lineTotal,
                defaultSalePrice = saleLineEntity.defaultSalePrice,
            )
        ),
        saleAdjustments = emptyList(),
        salePayments = emptyList(),
    )
}
