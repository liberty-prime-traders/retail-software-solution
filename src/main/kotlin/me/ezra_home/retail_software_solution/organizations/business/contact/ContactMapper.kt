package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactInsertDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactResponseDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.mapping.FullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class, uses = [ContactQualifier::class])
interface ContactMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "referenceNumber", ignore = true)
    @Mapping(target = "systemDefined", constant = "false")
    fun toEntity(contactInsertDto: ContactInsertDto): ContactEntity

    fun toDomainDto(contactEntity: ContactEntity): ContactDto

    fun toEntity(contactDto: ContactDto): ContactEntity

    @Mapping(source = "createdById", target = "createdBy", qualifiedBy = [FullName::class])
    @Mapping(source = ".", target = "identityType", qualifiedBy = [ToIdentityType::class])
    @Mapping(source = "identity.displayName", target = "fullName")
    fun toResponseDto(contactDto: ContactDto): ContactResponseDto
}
