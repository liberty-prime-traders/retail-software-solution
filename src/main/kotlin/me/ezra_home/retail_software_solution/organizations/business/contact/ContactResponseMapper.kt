package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactResponseDto
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerBalanceDto
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.CreatorFullName
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface ContactResponseMapper {

    @Mapping(source = "contactDto.createdById", target = "createdBy", qualifiedBy = [CreatorFullName::class])
    @Mapping(source = "contactDto.identity.identityType", target = "identityType")
    @Mapping(source = "contactDto.identity.displayName", target = "fullName")
    @Mapping(source = "subledgerBalanceDto.receivable", target = "receivableBalance")
    @Mapping(source = "subledgerBalanceDto.payable", target = "payableBalance")
    @Mapping(source = "subledgerBalanceDto.netBalance", target = "netBalance")
    @Mapping(source = "subledgerBalanceDto.balanceEffect", target = "balanceEffect")
    @Mapping(source = "openingBalanceDto.netBalance", target = "netOpeningBalance")
    @Mapping(source = "openingBalanceDto.balanceEffect", target = "netOpeningBalanceEffect")
    fun toResponseDto(
        contactDto: ContactDto,
        subledgerBalanceDto: SubledgerBalanceDto,
        openingBalanceDto: SubledgerBalanceDto
    ): ContactResponseDto
}
