package me.ezra_home.retail_software_solution.locations.business.sync

import me.ezra_home.retail_software_solution.locations.business.sync.dto.SyncLogResponseDto
import me.ezra_home.retail_software_solution.locations.model.SyncLogEntity
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface SyncLogMapper {
  fun toDto(entity: SyncLogEntity): SyncLogResponseDto
}
