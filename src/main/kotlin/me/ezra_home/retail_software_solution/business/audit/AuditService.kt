package me.ezra_home.retail_software_solution.business.audit

import me.ezra_home.retail_software_solution.business.audit.auditmanager.TransientAuditEntity
import me.ezra_home.retail_software_solution.business.sysuser.SysUserCache
import me.ezra_home.retail_software_solution.model.entity.audit.AuditEntity
import me.ezra_home.retail_software_solution.model.entity.audit.AuditId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Objects

@Service
class AuditService(
    private val auditRepository: AuditRepository,
    private val sysUserCache: SysUserCache
) {

    fun getAuditsForTable(tableName: String): Collection<AuditEntity> =
        auditRepository.findByIdEntityName(tableName)

    @Transactional
    fun createAuditRecords(transientAudits: Collection<TransientAuditEntity>) {
        val users = sysUserCache.getSystemUsers()
        val newAuditRecords = transientAudits.map { audit ->
            val newEntity = AuditEntity()
            newEntity.createdOn = audit.auditTime
            newEntity.createdById = users.find { Objects.equals(audit.auditUserOktaId, it.oktaId) }?.id
            newEntity.id = AuditId(audit.entityId, audit.entityName)
            newEntity
        }
        auditRepository.saveAll(newAuditRecords)
    }

    @Transactional
    fun deleteAuditRecords(transientAudits: Collection<TransientAuditEntity>) {
        val auditsToDelete = transientAudits.map { AuditId(it.entityId, it.entityName) }
        auditRepository.deleteByIdIn(auditsToDelete)
    }

    @Transactional
    fun updateAuditRecords(transientAudits: Collection<TransientAuditEntity>) {
        val auditIdsToUpdate = transientAudits.map { AuditId(it.entityId, it.entityName) }
        val auditsToUpdate = auditRepository.findByIdIn(auditIdsToUpdate.toSet())
        val users = sysUserCache.getSystemUsers()
        auditsToUpdate.forEach { audit ->
            val matchingTransientAudit = transientAudits.find { AuditId(it.entityId, it.entityName).isEqual(audit.id) }
            audit.lastUpdatedOn = matchingTransientAudit?.auditTime
            audit.lastUpdatedById = users.find { Objects.equals(matchingTransientAudit?.auditUserOktaId, it.oktaId) }?.id
        }
        auditRepository.saveAll(auditsToUpdate)
    }
}
