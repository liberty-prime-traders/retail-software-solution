package me.ezra_home.retail_software_solution.platform.business.db_version

import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

internal interface DbVersionRepository : JpaRepository<DbVersionEntity, UUID> {

    @Query("SELECT MAX(d.sequenceNumber) FROM DbVersionEntity d")
    fun findMaxSequenceNumber(): Long?
}
