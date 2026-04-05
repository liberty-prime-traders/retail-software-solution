package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.organizations.business.contact.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactQualifier
import me.ezra_home.retail_software_solution.organizations.business.contact.ToIdentityType
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactInsertDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactResponseDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactUpdateDto
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
    fun toDomainDto(contactInsertDto: ContactInsertDto): ContactDto

    fun toDomainDto(contactEntity: ContactEntity): ContactDto

    fun toEntity(contactDto: ContactDto): ContactEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = ".", target = "identityType", qualifiedBy = [ToIdentityType::class])
    @Mapping(source = "identity.displayName", target = "fullName")
    fun toResponseDto(contactDto: ContactDto): ContactResponseDto

    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun partialUpdate(contactUpdateDto: ContactUpdateDto, @MappingTarget contactDto: ContactDto)
}
