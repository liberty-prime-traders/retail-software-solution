package me.ezra_home.retail_software_solution.platform.business.db_version.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionCache
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionMapper
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionNumber
import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionDto
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
        val versionNumbersMap = dbVersionCache.getVersionNumbersById()
        return dbVersionCache.getAllDbVersions()
            .map { dbVersionMapper.toResponseDto(it, versionNumbersMap[it.prevVersionId]) }
    }

    @DbVersionNumber
    fun getVersionNumber(versionId: UUID?): String? = versionId?.let { dbVersionCache.getVersionNumbersById()[it] }

    @TransactionalOnPlatformSchema
    fun activateDbVersion(versionId: UUID): DbVersionResponseDto {
        val allDbVersions = dbVersionCache.getAllDbVersions()
        val dbVersionToActivate = allDbVersions.find { it.id == versionId } ?: throw UpdatingNonExistingRecordException()
        if (dbVersionToActivate.activatedOn == null) {
            verifyDbVersionForActivation(dbVersionToActivate, allDbVersions)
            val lastSequenceNumber = dbVersionCache.findMaxSequenceNumber() ?: 0L
            dbVersionToActivate.sequenceNumber = lastSequenceNumber.plus(1)
            dbVersionToActivate.activatedOn = OffsetDateTime.now()
            dbVersionCache.upsertDbVersion(dbVersionToActivate)
        }
        val prevVersionNumber = dbVersionCache.getVersionNumbersById()[dbVersionToActivate.prevVersionId]
        return dbVersionMapper.toResponseDto(dbVersionToActivate, prevVersionNumber)
    }

    private fun verifyDbVersionForActivation(dbVersion: DbVersionDto, allDbVersions: Collection<DbVersionDto>) {
        dbVersion.prevVersionId?.let { prevVersionId ->
            if (prevVersionId == dbVersion.id) {
                throw RtsGenericException("A DB version cannot point to itself as previous version.")
            }
            val prevVersionDto = allDbVersions.find { it.id == prevVersionId }
                ?: throw RtsGenericException("Previous DB version does not exist.")
            if (prevVersionDto.activatedOn == null) {
                throw RtsGenericException("Previous version (${prevVersionDto.versionNumber}) must be activated first")
            }
        }
    }
}
