package me.ezra_home.retail_software_solution.organizations.business.fund_movement

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface FundMovementMapper {

    fun toDto(fundMovementEntity: FundMovementEntity): FundMovementDto

    fun toVoidDto(fundMovementVoidEntity: FundMovementVoidEntity): FundMovementVoidDto
}
