package me.ezra_home.retail_software_solution.business.audit

import me.ezra_home.retail_software_solution.model.entity.audit.AuditEntity
import me.ezra_home.retail_software_solution.model.entity.audit.AuditId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AuditRepository : JpaRepository<AuditEntity, UUID> {
    fun deleteByIdIn(ids: Collection<AuditId>)
    fun findByIdIn(ids: Set<AuditId>): Collection<AuditEntity>
    fun findByIdEntityName(entityName: String): Collection<AuditEntity>
}
