package me.ezra_home.retail_software_solution.platform.business.db_version

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionResponseDto
import me.ezra_home.retail_software_solution.platform.business.db_version.dto.DbVersionCreationDto
import me.ezra_home.retail_software_solution.platform.model.DbVersionEntity
import me.ezra_home.retail_software_solution.platform.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class DbVersionService(
    private val dbVersionRepository: DbVersionRepository,
    private val dbVersionMapper: DbVersionMapper
) {
    fun createDbVersion(dbVersionCreationDto: DbVersionCreationDto): DbVersionResponseDto {
        if (dbVersionRepository.findByVersionNumber(dbVersionCreationDto.versionNumber) != null) {
            throw RtsGenericException("DB Version with number '${dbVersionCreationDto.versionNumber}' already exists.")
        }

        val prevVersionEntity = dbVersionCreationDto.prevVersionId?.let {
            dbVersionRepository.findById(it).orElseThrow { RtsGenericException("Previous version record not found") }
        }

        val lastSequenceNumber = dbVersionRepository.findTopByOrderBySequenceNumberDesc()?.sequenceNumber ?: 0L
        val newSequenceNumber = lastSequenceNumber.plus(1)

        val newVersion = DbVersionEntity(
            versionNumber = dbVersionCreationDto.versionNumber,
            sequenceNumber = newSequenceNumber,
            prevVersionId = prevVersionEntity?.id,
        ).apply {
            createdById = SessionContextProvider.getUserId()
        }

        return dbVersionMapper.toResponseDto(dbVersionRepository.save(newVersion))
    }

    fun activateDbVersion(versionId: UUID): DbVersionResponseDto {
        val versionToActivate =
            dbVersionRepository.findById(versionId).orElseThrow { RtsGenericException("Version not found") }

        if (versionToActivate.prevVersionId != null) {
            val prevVersion = dbVersionRepository.findById(versionToActivate.prevVersionId!!).orElse(null)
            if (prevVersion?.activatedOn == null) {
                throw RtsGenericException("Previous version must be activated first")
            }
        }

        if (versionToActivate.activatedOn != null) {
            throw RtsGenericException("The DB version was already activated")
        }

        versionToActivate.activatedOn = OffsetDateTime.now()
        return dbVersionMapper.toResponseDto(dbVersionRepository.save(versionToActivate))
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getLatestActivatedDbVersion(): DbVersionEntity? {
        return dbVersionRepository.findTopByActivatedOnIsNotNullOrderBySequenceNumberDesc()
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllDbVersions(): Collection<DbVersionResponseDto> {
        return dbVersionRepository.findAll()
            .sortedByDescending { it.sequenceNumber }
            .map { dbVersionMapper.toResponseDto(it) }
    }
}
