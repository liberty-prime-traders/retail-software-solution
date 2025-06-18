package me.ezra_home.retail_software_solution.platform.business.db_version

import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DbVersionRepository : JpaRepository<DbVersionEntity, UUID> {
    fun findByVersionNumber(versionNumber: String): DbVersionEntity?
    fun findTopByOrderBySequenceNumberDesc(): DbVersionEntity?
    fun findTopByActivatedOnIsNotNullOrderBySequenceNumberDesc(): DbVersionEntity?
}
