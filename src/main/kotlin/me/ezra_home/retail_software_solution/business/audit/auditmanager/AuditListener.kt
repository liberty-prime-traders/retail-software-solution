package me.ezra_home.retail_software_solution.business.audit.auditmanager

import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import me.ezra_home.retail_software_solution.model.entity.BaseEntity
import me.ezra_home.retail_software_solution.model.enums.DatabaseAction
import me.ezra_home.retail_software_solution.rest.session.SessionContextProvider
import org.springframework.stereotype.Component

@Component
class AuditListener(
    private val auditBatchingContext: AuditBatchingContext,
    private val sessionContextProvider: SessionContextProvider
) {

    @PostPersist
    fun onPostPersist(entity: BaseEntity) {
        auditBatchingContext.logEntityActivity(entity, DatabaseAction.CREATE, sessionContextProvider.getSession())
    }

    @PostUpdate
    fun onPostUpdate(entity: BaseEntity) {
        auditBatchingContext.logEntityActivity(entity, DatabaseAction.UPDATE, sessionContextProvider.getSession())
    }

    @PostRemove
    fun onPostRemove(entity: BaseEntity) {
        auditBatchingContext.logEntityActivity(entity, DatabaseAction.DELETE, sessionContextProvider.getSession())
    }
}
