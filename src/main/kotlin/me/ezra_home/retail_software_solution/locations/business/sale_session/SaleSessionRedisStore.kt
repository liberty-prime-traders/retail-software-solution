package me.ezra_home.retail_software_solution.locations.business.sale_session

import com.fasterxml.jackson.databind.ObjectMapper
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class SaleSessionRedisStore(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : SaleSessionStore {

    private val ttlSeconds: Long = 7200

    override fun save(session: SaleSession) {
        val key = sessionKey(session.sessionId)
        val json = objectMapper.writeValueAsString(session)
        val ttl = Duration.ofSeconds(ttlSeconds)
        val sessionIdText = session.sessionId.toString()
        redisTemplate.opsForValue().set(key, json, ttl)
        session.saleId?.let { saleId ->
            redisTemplate.opsForValue().set(bySaleKey(saleId), sessionIdText, ttl)
        }
        redisTemplate.opsForSet().add(OPEN_INDEX_KEY, sessionIdText)
        redisTemplate.expire(OPEN_INDEX_KEY, ttl)
    }

    override fun load(sessionId: UUID): SaleSession =
        loadOrNull(sessionId) ?: throw RtsGenericException("Sale session $sessionId not found or expired")

    override fun loadOrNull(sessionId: UUID): SaleSession? {
        val json = redisTemplate.opsForValue().get(sessionKey(sessionId)) ?: return null
        return objectMapper.readValue(json, SaleSession::class.java)
    }

    override fun delete(sessionId: UUID) {
        val existing = loadOrNull(sessionId)
        redisTemplate.delete(sessionKey(sessionId))
        redisTemplate.opsForSet().remove(OPEN_INDEX_KEY, sessionId.toString())
        existing?.saleId?.let { redisTemplate.delete(bySaleKey(it)) }
    }

    override fun findOpenSessionForSale(saleId: UUID): SaleSession? {
        val sessionIdText = redisTemplate.opsForValue().get(bySaleKey(saleId)) ?: return null
        return loadOrNull(UUID.fromString(sessionIdText))
    }

    override fun listOpenSessions(): List<SaleSession> {
        val ids = redisTemplate.opsForSet().members(OPEN_INDEX_KEY) ?: return emptyList()
        return ids.mapNotNull { loadOrNull(UUID.fromString(it)) }
    }

    private fun sessionKey(sessionId: UUID) = "$SESSION_PREFIX$sessionId"
    private fun bySaleKey(saleId: UUID) = "$BY_SALE_PREFIX$saleId"

    companion object {
        private const val SESSION_PREFIX = "sale-session:"
        private const val BY_SALE_PREFIX = "sale-session:by-sale:"
        private const val OPEN_INDEX_KEY = "sale-session:open"
    }
}
