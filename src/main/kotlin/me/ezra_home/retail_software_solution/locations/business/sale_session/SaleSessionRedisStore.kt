package me.ezra_home.retail_software_solution.locations.business.sale_session

import com.fasterxml.jackson.databind.ObjectMapper
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
@Profile("!sessionInMemory")
class SaleSessionRedisStore(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${sale-session.ttl-seconds:7200}") private val ttlSeconds: Long,
) : SaleSessionStore {

    override fun save(session: SaleSession) {
        val key = sessionKey(session.sessionId)
        val json = objectMapper.writeValueAsString(session)
        val ttl = Duration.ofSeconds(ttlSeconds)
        redisTemplate.opsForValue().set(key, json, ttl)
        session.saleId?.let { saleId ->
            redisTemplate.opsForValue().set(bySaleKey(saleId), session.sessionId, ttl)
        }
        redisTemplate.opsForSet().add(OPEN_INDEX_KEY, session.sessionId)
        redisTemplate.expire(OPEN_INDEX_KEY, ttl)
    }

    override fun load(sessionId: String): SaleSession =
        loadOrNull(sessionId) ?: throw RtsGenericException("Sale session $sessionId not found or expired")

    override fun loadOrNull(sessionId: String): SaleSession? {
        val json = redisTemplate.opsForValue().get(sessionKey(sessionId)) ?: return null
        return objectMapper.readValue(json, SaleSession::class.java)
    }

    override fun delete(sessionId: String) {
        val existing = loadOrNull(sessionId)
        redisTemplate.delete(sessionKey(sessionId))
        redisTemplate.opsForSet().remove(OPEN_INDEX_KEY, sessionId)
        existing?.saleId?.let { redisTemplate.delete(bySaleKey(it)) }
    }

    override fun findOpenSessionForSale(saleId: UUID): SaleSession? {
        val sessionId = redisTemplate.opsForValue().get(bySaleKey(saleId)) ?: return null
        return loadOrNull(sessionId)
    }

    override fun listOpen(): List<SaleSession> {
        val ids = redisTemplate.opsForSet().members(OPEN_INDEX_KEY) ?: return emptyList()
        return ids.mapNotNull { loadOrNull(it) }
    }

    private fun sessionKey(sessionId: String) = "$SESSION_PREFIX$sessionId"
    private fun bySaleKey(saleId: UUID) = "$BY_SALE_PREFIX$saleId"

    companion object {
        private const val SESSION_PREFIX = "sale-session:"
        private const val BY_SALE_PREFIX = "sale-session:by-sale:"
        private const val OPEN_INDEX_KEY = "sale-session:open"
    }
}
