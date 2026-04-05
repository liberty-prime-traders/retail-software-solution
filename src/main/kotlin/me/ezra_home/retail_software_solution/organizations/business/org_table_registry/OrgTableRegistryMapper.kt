package me.ezra_home.retail_software_solution.organizations.business.org_table_registry

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.OrgTableRegistryDto
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.api.OrgTableRegistryResponseDto
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.api.OrgTableRegistryUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.mapping.TableNameQualifier
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.mapping.TableNameResolver
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.OrgTableRegistryEntity
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(
    config = RtsMapperConfig::class,
    uses = [TableNameResolver::class]
)
interface OrgTableRegistryMapper {

    fun toDomainDto(entity: OrgTableRegistryEntity): OrgTableRegistryDto

    fun toEntity(dto: OrgTableRegistryDto): OrgTableRegistryEntity

    @Mapping(source = "registryId", target = "tableName", qualifiedBy = [TableNameQualifier::class])
    fun toDto(dto: OrgTableRegistryDto): OrgTableRegistryResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registryId", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(updateDto: OrgTableRegistryUpdateDto, @MappingTarget dto: OrgTableRegistryDto)
}
