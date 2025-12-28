package me.ezra_home.retail_software_solution.organizations.business.org_table_registry

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.dto.OrgTableRegistryResponseDto
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.dto.OrgTableRegistryUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.mapping.TableNameQualifier
import me.ezra_home.retail_software_solution.organizations.business.org_table_registry.mapping.TableNameResolver
import me.ezra_home.retail_software_solution.organizations.model.OrgTableRegistryEntity
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

    @Mapping(source = "registryId", target = "tableName", qualifiedBy = [TableNameQualifier::class])
    fun toDto(entity: OrgTableRegistryEntity): OrgTableRegistryResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registryId", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun patchEntity(dto: OrgTableRegistryUpdateDto, @MappingTarget entity: OrgTableRegistryEntity): OrgTableRegistryEntity
}
