package me.ezra_home.retail_software_solution.organizations.business.org_table_registry

import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.api.OrgTableRegistryDto
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.api.OrgTableRegistryResponseDto
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.mapping.TableNameQualifier
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.mapping.TableNameResolver
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(
    config = RtsMapperConfig::class,
    uses = [TableNameResolver::class]
)
interface OrgTableRegistryMapper {

    fun toDomainDto(entity: OrgTableRegistryEntity): OrgTableRegistryDto

    fun toEntity(dto: OrgTableRegistryDto): OrgTableRegistryEntity

    @Mapping(source = "registryId", target = "tableName", qualifiedBy = [TableNameQualifier::class])
    fun toDto(dto: OrgTableRegistryDto): OrgTableRegistryResponseDto
}
