package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionAssembler
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionLoader
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionValidator
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleSessionHandler(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionLoader: SaleSessionLoader,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionValidator: SaleSessionValidator,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun start(sessionStartDto: SaleSessionStartDto): SaleSessionResponseDto {
        sessionStartDto.saleId?.let { saleId ->
            saleSessionStore.findOpenSessionForSale(saleId)?.let { existingSession ->
                return saleSessionAssembler.buildResponse(existingSession)
            }
        }
        val sessionId = UUID.randomUUID()
        val session = if (sessionStartDto.saleId != null) {
            saleSessionLoader.loadFromSale(sessionId, sessionStartDto.saleId)
        } else {
            val locationId = SessionContextProvider.getLocationId()
            val userId = SessionContextProvider.getUserId()
            saleSessionLoader.newSession(sessionId, locationId, sessionStartDto.contactId, userId)
        }
        saleSessionValidator.validate(session)
        saleSessionStore.save(session)
        return saleSessionAssembler.buildResponse(session)
    }

    fun abandon(sessionId: UUID) {
        saleSessionStore.delete(sessionId)
    }

    fun listOpenSessions(mineOnly: Boolean): List<SaleSessionSummaryDto> {
        return saleSessionStore.listOpenSessions()
            .filter { session ->
                if (mineOnly) {
                    session.createdById == SessionContextProvider.getUserId()
                } else {
                    true
                }
            }
            .let { saleSessionAssembler.buildSummaries(it) }
    }

    fun acquireSession(sessionId: UUID): SaleSessionResponseDto {
        val session = saleSessionStore.load(sessionId)
        val visitedSession = session.markVisited(SessionContextProvider.getUserId())
        saleSessionStore.save(visitedSession)
        return saleSessionAssembler.buildResponse(visitedSession)
    }
}
