package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import java.util.UUID

interface SaleSessionStore {

    fun save(session: SaleSession)

    fun load(sessionId: UUID): SaleSession

    fun loadOrNull(sessionId: UUID): SaleSession?

    fun delete(sessionId: UUID)

    fun findOpenSessionForSale(saleId: UUID): SaleSession?

    fun listOpenSessions(): List<SaleSession>
}
