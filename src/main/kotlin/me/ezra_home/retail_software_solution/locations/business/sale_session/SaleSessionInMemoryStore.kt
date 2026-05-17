package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
@Profile("sessionInMemory")
class SaleSessionInMemoryStore : SaleSessionStore {

    private val sessions = ConcurrentHashMap<String, SaleSession>()
    private val bySale = ConcurrentHashMap<UUID, String>()

    override fun save(session: SaleSession) {
        sessions[session.sessionId] = session
        session.saleId?.let { bySale[it] = session.sessionId }
    }

    override fun load(sessionId: String): SaleSession =
        loadOrNull(sessionId) ?: throw RtsGenericException("Sale session $sessionId not found or expired")

    override fun loadOrNull(sessionId: String): SaleSession? = sessions[sessionId]

    override fun delete(sessionId: String) {
        val removed = sessions.remove(sessionId)
        removed?.saleId?.let { bySale.remove(it) }
    }

    override fun findOpenSessionForSale(saleId: UUID): SaleSession? {
        val sessionId = bySale[saleId] ?: return null
        return sessions[sessionId]
    }

    override fun listOpen(): List<SaleSession> = sessions.values.toList()
}
