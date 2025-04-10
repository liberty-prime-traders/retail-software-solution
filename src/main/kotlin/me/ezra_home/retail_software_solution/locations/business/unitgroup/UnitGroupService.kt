package me.ezra_home.retail_software_solution.locations.business.unitgroup


import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.unitgroup.dto.UnitGroupInsertDto
import me.ezra_home.retail_software_solution.locations.business.unitgroup.dto.UnitGroupResponseDto
import me.ezra_home.retail_software_solution.locations.business.unitgroup.dto.UnitGroupUpdateDto
import me.ezra_home.retail_software_solution.util.exceptions.QueriedByEmptyIdException
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.Optional
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class UnitGroupService(
    private val unitGroupMapper: UnitGroupMapper,
    private val unitGroupCache: UnitGroupCache
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun getAllUnitGroups(): Collection<UnitGroupResponseDto> {
        return unitGroupCache.getAllUnitGroups().map { unitGroupMapper.toResponseDto(it) }
    }

    fun createUnitGroup(unitGroupInsertDto: UnitGroupInsertDto): UnitGroupResponseDto {
        validateNameOnSave(Optional.ofNullable(unitGroupInsertDto.name))
        val entity = unitGroupMapper.toEntity(unitGroupInsertDto)
        unitGroupCache.upsertUnitGroup(entity)
        return unitGroupMapper.toResponseDto(entity)
    }

    private fun validateNameOnSave(name: Optional<String>?, id: UUID? = null) {
        if (name == null || name.isEmpty || name.get().isBlank()) {
            throw RtsGenericException("An UnitGroup must have a name")
        }
        val unitGroupWithMatchingName = unitGroupCache.getAllUnitGroups().find {
            it.name.equals(name.get(), ignoreCase = true) && !Objects.equals(it.id, id)
        }
        if (unitGroupWithMatchingName != null) {
            throw RtsGenericException("An UnitGroup using the name '${name.get()}' already exists")
        }
    }

    fun updateUnitGroup(unitGroupUpdateDto: UnitGroupUpdateDto): UnitGroupResponseDto {
        val id = unitGroupUpdateDto.id ?: throw QueriedByEmptyIdException()
        val entityFromDatabase = unitGroupCache.getAllUnitGroups().find { it.id == id } ?: throw NotFoundException()
        validateNameOnSave(unitGroupUpdateDto.name, unitGroupUpdateDto.id)
        unitGroupMapper.partialUpdate(unitGroupUpdateDto, entityFromDatabase)
        unitGroupCache.upsertUnitGroup(entityFromDatabase)
        return unitGroupMapper.toResponseDto(entityFromDatabase)
    }

    fun deleteUnitGroup(id: UUID?) {
        if (id != null) {
            val entity = unitGroupCache.getAllUnitGroups().find { it.id == id }
            if (entity != null) {
                val usageCount = entity.usageCount
                if (usageCount > 0L) {
                    throw RtsGenericException("UnitGroup ${entity.name} has $usageCount usage(s) and cannot be deleted")
                }
                unitGroupCache.deleteUnitGroup(id)
            }

        }
    }
}
