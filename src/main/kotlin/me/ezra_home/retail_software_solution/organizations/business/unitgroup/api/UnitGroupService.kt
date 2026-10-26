package me.ezra_home.retail_software_solution.organizations.business.unitgroup.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupCache
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupMapper
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupNameRules
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueService
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Optional
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class UnitGroupService(
    private val unitGroupMapper: UnitGroupMapper,
    private val unitGroupCache: UnitGroupCache,
    private val unitValueService: UnitValueService,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock,
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllUnitGroupDtos(): Collection<UnitGroupDto> = unitGroupCache.getAllUnitGroups()

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllUnitGroups(): Collection<UnitGroupResponseDto> {
        return unitGroupCache.getAllUnitGroups().map { unitGroupMapper.toResponseDto(it) }
    }

    fun createUnitGroup(unitGroupInsertDto: UnitGroupInsertDto): UnitGroupResponseDto {
        validateNameOnSave(Optional.ofNullable(unitGroupInsertDto.name))
        unitValueService.validateRootUnitCodeBeforeGroupCreate(unitGroupInsertDto.name, unitGroupInsertDto.baseUnitCode)
        val dto = unitGroupCache.create(unitGroupInsertDto)
        val response = unitGroupMapper.toResponseDto(dto)
        unitValueService.ensureBaseUnitExists(
            unitGroupId = response.id,
            unitGroupName = response.name,
            baseUnitName = unitGroupInsertDto.baseUnitName,
            baseUnitCode = unitGroupInsertDto.baseUnitCode
        )
        return response
    }

    fun bulkCreateValidatedList(insertDtos: List<UnitGroupInsertDto>): List<UnitGroupResponseDto> {
        if (insertDtos.isEmpty()) return emptyList()
        requireNamesAvailable(insertDtos.map { it.name })
        val entities = insertDtos.map { unitGroupMapper.toEntity(it) }
        val saved = unitGroupCache.saveAll(entities)
        return saved.map { unitGroupMapper.toDomainDto(it) }.map { unitGroupMapper.toResponseDto(it) }
    }

    private fun validateNameOnSave(optionalName: Optional<String>?, id: UUID? = null) {
        val name = StringUtils.getValueOrException(optionalName, "An UnitGroup must have a name")
        if (name.none { it.isLetterOrDigit() }) {
            throw RtsGenericException("A unit group name must contain at least one letter or digit")
        }
        requireNamesAvailable(listOf(name), id)
    }

    private fun requireNamesAvailable(names: List<String>, excludedUnitGroupId: UUID? = null) {
        orgEntityAdvisoryLock.acquire(LockNamespaces.UNIT_GROUP_NAME, UnitGroupNameRules.LOCK_KEY)
        UnitGroupNameRules.requireAvailable(unitGroupCache.getAllUnitGroups(), names, excludedUnitGroupId)
    }

    fun updateUnitGroup(unitGroupUpdateDto: UnitGroupUpdateDto): UnitGroupResponseDto {
        val existing = unitGroupCache.getAllUnitGroups().find { it.id == unitGroupUpdateDto.id }
            ?: throw UpdatingNonExistingRecordException()
        if (existing.systemDefined) throw RtsGenericException("System-defined unit groups cannot be modified")
        validateNameOnSave(unitGroupUpdateDto.name, unitGroupUpdateDto.id)
        val updated = unitGroupUpdateDto.applyTo(existing)
        val saved = unitGroupCache.save(updated)
        return unitGroupMapper.toResponseDto(saved)
    }

    fun deleteUnitGroup(id: UUID) {
        unitGroupCache.getAllUnitGroups().find { it.id == id }
            ?.apply {
                if (this.systemDefined) throw RtsGenericException("System-defined unit groups cannot be deleted")
                unitGroupCache.deleteUnitGroup(id)
            }
    }
}
