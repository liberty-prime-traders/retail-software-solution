package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeader
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPayment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionTotals
import me.ezra_home.retail_software_solution.util.business.DateTimes
import java.util.UUID

class SaleSessionBuilder(private val sessionId: UUID) {

    private val now = DateTimes.Offset.Now.organization()
    private val userId = SessionContextProvider.getUserId()
    private val locationId = SessionContextProvider.getLocationId()

    private var saleId: UUID? = null
    private var saleVersion: Long? = null
    private var originalStatus = SaleStatus.DRAFT
    private var productsVersion = 0L
    private var header: SaleSessionHeader? = null
    private var saleLines: List<SaleSessionLine> = emptyList()
    private var saleAdjustments: List<SaleSessionAdjustment> = emptyList()
    private var salePayments: List<SaleSessionPayment> = emptyList()

    fun linkedToPersistedSale(saleId: UUID, saleVersion: Long?, originalStatus: SaleStatus): SaleSessionBuilder = apply {
        this.saleId = saleId
        this.saleVersion = saleVersion
        this.originalStatus = originalStatus
    }

    fun withHeader(header: SaleSessionHeader): SaleSessionBuilder = apply { this.header = header }

    fun withSaleLines(saleLines: List<SaleSessionLine>): SaleSessionBuilder = apply { this.saleLines = saleLines }

    fun withSaleAdjustments(saleAdjustments: List<SaleSessionAdjustment>): SaleSessionBuilder =
        apply { this.saleAdjustments = saleAdjustments }

    fun withSalePayments(salePayments: List<SaleSessionPayment>): SaleSessionBuilder =
        apply { this.salePayments = salePayments }

    fun withLinesAwaitingReservation(): SaleSessionBuilder = apply { this.productsVersion = 1L }

    fun build(): SaleSession = SaleSession(
        sessionId = sessionId,
        locationId = locationId,
        saleId = saleId,
        saleVersion = saleVersion,
        productsVersion = productsVersion,
        productsReservedAtVersion = 0L,
        originalStatus = originalStatus,
        createdById = userId,
        createdAt = now,
        lastUpdatedAt = now,
        lastAccessedById = userId,
        lastAccessedAt = now,
        header = header ?: throw IllegalStateException("A sale session needs a header before it is built"),
        saleLines = saleLines,
        saleAdjustments = saleAdjustments,
        salePayments = salePayments,
        totals = SaleSessionTotals.ZERO,
    )
}
