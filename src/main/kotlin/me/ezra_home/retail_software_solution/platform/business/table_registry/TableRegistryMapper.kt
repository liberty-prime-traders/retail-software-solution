package me.ezra_home.retail_software_solution.platform.business.table_registry

import me.ezra_home.retail_software_solution.platform.business.db_version.DbVersionNumber
import me.ezra_home.retail_software_solution.platform.business.db_version.api.DbVersionService
import me.ezra_home.retail_software_solution.platform.business.table_registry.api.TableRegistryDto
import me.ezra_home.retail_software_solution.platform.business.table_registry.api.TableRegistryResponseDto
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(
    config = RtsMapperConfig::class,
    uses = [DbVersionService::class]
)
interface TableRegistryMapper {

    fun toDomainDto(entity: TableRegistryEntity): TableRegistryDto

    fun toEntity(dto: TableRegistryDto): TableRegistryEntity

    @Mapping(source = "minimumVersionId", target = "minimumVersion", qualifiedBy = [DbVersionNumber::class])
    fun toDto(dto: TableRegistryDto): TableRegistryResponseDto
}
