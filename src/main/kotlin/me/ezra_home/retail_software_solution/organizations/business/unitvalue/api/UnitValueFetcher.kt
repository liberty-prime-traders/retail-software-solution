package me.ezra_home.retail_software_solution.organizations.business.unitvalue.api

import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitName
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueCache
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueMapper
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UnitValueFetcher(
    private val unitValueCache: UnitValueCache,
    private val unitValueMapper: UnitValueMapper
) {

    @UnitName
    fun getUnitName(unitValueId: UUID?): String? = unitValueId?.let { getUnitNamesById()[it] }

    fun getUnitNamesById(): Map<UUID, String> = unitValueCache.getUnitNamesById()

    fun getAllUnitValues(): Collection<UnitValueResponseDto> {
        val unitNamesById = unitValueCache.getUnitNamesById()
        return unitValueCache.getAllUnitValues().map {
            unitValueMapper.toResponseDto(it, unitNamesById[it.baseUnit])
        }
    }

    fun getUnitValuesForUnitGroup(unitGroupId: UUID): Collection<UnitValueResponseDto> {
        val unitNamesById = unitValueCache.getUnitNamesById()
        return unitValueCache.getByUnitGroupId(unitGroupId).map {
            unitValueMapper.toResponseDto(it, unitNamesById[it.baseUnit])
        }
    }
}
