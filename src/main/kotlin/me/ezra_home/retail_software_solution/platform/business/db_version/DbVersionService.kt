package me.ezra_home.retail_software_solution.platform.business.db_version

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionCreationDto
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class DbVersionService(
    private val dbVersionCache: DbVersionCache,
    private val dbVersionMapper: DbVersionMapper
) {
    fun createDbVersion(dbVersionCreationDto: DbVersionCreationDto): DbVersionResponseDto {
        if (dbVersionCache.getDbVersionByVersionNumber(dbVersionCreationDto.versionNumber) != null) {
            throw RtsGenericException("DB Version with number '${dbVersionCreationDto.versionNumber}' already exists.")
        }
        val prevVersionEntity = dbVersionCreationDto.prevVersionId?.let {
            val prevDbVersion = dbVersionCache.getAllDbVersions().find { dbVersion -> dbVersion.id == it}
                ?: throw NotFoundException()
            prevDbVersion
        }
        val lastSequenceNumber = dbVersionCache.getLatestDbVersion()?.sequenceNumber ?: 0L
        val newSequenceNumber = lastSequenceNumber.plus(1)
        val dbVersionEntity = DbVersionEntity(
            versionNumber = dbVersionCreationDto.versionNumber,
            sequenceNumber = newSequenceNumber,
            prevVersionId = prevVersionEntity?.id,
        ).apply {
            createdById = SessionContextProvider.getUserId()
        }
        dbVersionCache.upsertDbVersion(dbVersionEntity)
        return dbVersionMapper.toResponseDto(dbVersionEntity)
    }

    fun activateDbVersion(versionId: UUID): DbVersionResponseDto {
        val allDbVersions = dbVersionCache.getAllDbVersions()
        val dbVersionToActivate = allDbVersions.find { it.id == versionId}
            ?: throw NotFoundException()
        if (dbVersionToActivate.prevVersionId != null) {
            val prevDBVersionEntity = allDbVersions.find { it.id == dbVersionToActivate.prevVersionId!!}
                ?: throw RtsGenericException("Previous DB version does not exist.")
            if (prevDBVersionEntity.activatedOn == null) {
                throw RtsGenericException("Previous version must be activated first")
            }
        }
        if (dbVersionToActivate.activatedOn != null) {
            throw RtsGenericException("The DB version was already activated")
        }
        dbVersionToActivate.activatedOn = OffsetDateTime.now()
        dbVersionCache.upsertDbVersion(dbVersionToActivate)
        return dbVersionMapper.toResponseDto(dbVersionToActivate)
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllDbVersions(): Collection<DbVersionResponseDto> {
        return dbVersionCache.getAllDbVersions()
            .sortedByDescending { it.sequenceNumber }
            .map { dbVersionMapper.toResponseDto(it) }
    }
}
