package me.ezra_home.retail_software_solution.platform.business.feature

import me.ezra_home.retail_software_solution.platform.business.feature.api.FeatureDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface FeatureMapper {
    fun toDomainDto(entity: FeatureEntity): FeatureDto

    fun toEntity(dto: FeatureDto): FeatureEntity
}
