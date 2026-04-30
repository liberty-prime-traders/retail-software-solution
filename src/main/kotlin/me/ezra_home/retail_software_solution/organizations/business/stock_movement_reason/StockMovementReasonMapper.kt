package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonInsertDto
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
abstract class StockMovementReasonMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "systemDefined", constant = "false")
    abstract fun toEntity(insertDto: StockMovementReasonInsertDto): StockMovementReasonEntity

    abstract fun toDomainDto(entity: StockMovementReasonEntity): StockMovementReasonDto

    abstract fun toEntity(dto: StockMovementReasonDto): StockMovementReasonEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    abstract fun toResponseDto(dto: StockMovementReasonDto): StockMovementReasonResponseDto
}
