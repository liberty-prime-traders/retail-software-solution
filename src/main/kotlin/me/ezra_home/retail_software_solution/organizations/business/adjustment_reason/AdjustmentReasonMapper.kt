package me.ezra_home.retail_software_solution.organizations.business.adjustment_reason

import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonInsertDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
abstract class AdjustmentReasonMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "systemDefined", constant = "false")
    abstract fun toEntity(insertDto: AdjustmentReasonInsertDto): AdjustmentReasonEntity

    abstract fun toDomainDto(entity: AdjustmentReasonEntity): AdjustmentReasonDto

    abstract fun toEntity(dto: AdjustmentReasonDto): AdjustmentReasonEntity
}
