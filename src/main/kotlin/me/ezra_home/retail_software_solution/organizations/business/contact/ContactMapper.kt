package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactInsertDto
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactResponseDto
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ContactQualifier
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.ToIdentityType
import me.ezra_home.retail_software_solution.organizations.model.ContactEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(config = RtsMapperConfig::class, uses = [ContactQualifier::class])
interface ContactMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    fun toEntity(contactInsertDto: ContactInsertDto): ContactEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = ".", target = "identityType", qualifiedBy = [ToIdentityType::class])
    @Mapping(source = "identity.displayName", target = "fullName")
    fun toResponseDto(contactEntity: ContactEntity): ContactResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(contactUpdateDto: ContactUpdateDto, @MappingTarget contactEntity: ContactEntity)
}
