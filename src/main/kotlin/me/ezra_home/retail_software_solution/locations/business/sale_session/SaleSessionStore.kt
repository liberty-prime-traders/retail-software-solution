package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import java.util.UUID

interface SaleSessionStore {

    fun save(session: SaleSession)

    fun load(sessionId: String): SaleSession

    fun loadOrNull(sessionId: String): SaleSession?

    fun delete(sessionId: String)

    fun findOpenSessionForSale(saleId: UUID): SaleSession?

    fun listOpen(): List<SaleSession>
}
