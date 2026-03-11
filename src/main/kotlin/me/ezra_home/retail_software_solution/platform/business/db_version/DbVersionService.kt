package me.ezra_home.retail_software_solution.platform.business.db_version

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_version.mapping.DbVersionMapper
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnPlatformSchema(readOnly = true)
class DbVersionService(
    private val dbVersionCache: DbVersionCache,
    private val dbVersionMapper: DbVersionMapper
) {

    fun getAllDbVersions(): Collection<DbVersionResponseDto> {
        return dbVersionCache.getAllDbVersions()
            .map { dbVersionMapper.toResponseDto(it) }
    }

    fun getVersionNumber(versionId: UUID?): String? {
        return versionId?.let {
            dbVersionCache.getAllDbVersions().find { it.id == versionId }?.versionNumber
        }
    }

    @TransactionalOnPlatformSchema
    fun activateDbVersion(versionId: UUID): DbVersionResponseDto {
        val allDbVersions = dbVersionCache.getAllDbVersions()
        val dbVersionToActivate = allDbVersions.find { it.id == versionId} ?: throw UpdatingNonExistingRecordException()
        if (dbVersionToActivate.activatedOn == null) {
            verifyDbVersionForActivation(dbVersionToActivate, allDbVersions)
            val lastSequenceNumber = dbVersionCache.findMaxSequenceNumber() ?: 0L
            dbVersionToActivate.sequenceNumber = lastSequenceNumber.plus(1)
            dbVersionToActivate.activatedOn = OffsetDateTime.now()
            dbVersionCache.upsertDbVersion(dbVersionToActivate)
        }
        return dbVersionMapper.toResponseDto(dbVersionToActivate)
    }

    private fun verifyDbVersionForActivation(dbVersion: DbVersionEntity, allDbVersions: Collection<DbVersionEntity>) {
        dbVersion.prevVersionId?.let { prevVersionId ->
            if (prevVersionId == dbVersion.id) {
                throw RtsGenericException("A DB version cannot point to itself as previous version.")
            }
            val prevVersionEntity = allDbVersions.find { it.id == prevVersionId}
                ?: throw RtsGenericException("Previous DB version does not exist.")
            if (prevVersionEntity.activatedOn == null) {
                throw RtsGenericException("Previous version (${prevVersionEntity.versionNumber}) must be activated first")
            }
        }
    }
}
