package me.ezra_home.retail_software_solution.platform.business.table_registry

import me.ezra_home.retail_software_solution.configuration.mapping.RtsMapperConfig
import me.ezra_home.retail_software_solution.platform.business.db_version.mapping.DbVersionNumber
import me.ezra_home.retail_software_solution.platform.business.db_version.mapping.DbVersionQualifier
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryInsertDto
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryResponseDto
import me.ezra_home.retail_software_solution.platform.business.table_registry.dto.TableRegistryUpdateDto
import me.ezra_home.retail_software_solution.platform.model.TableRegistryEntity
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(
    config = RtsMapperConfig::class,
    uses = [DbVersionQualifier::class]
)
interface TableRegistryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "nextNumber", ignore = true)
    fun toEntity(dto: TableRegistryInsertDto): TableRegistryEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = "minimumVersionId", target = "minimumVersion", qualifiedBy = [DbVersionNumber::class])
    fun toDto(entity: TableRegistryEntity): TableRegistryResponseDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "nextNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun updateEntity(dto: TableRegistryUpdateDto, @MappingTarget entity: TableRegistryEntity): TableRegistryEntity
}
