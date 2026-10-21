package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface LedgerMapper {

    fun toGroupDto(ledgerEntryGroupEntity: LedgerEntryGroupEntity): LedgerEntryGroupDto

    fun toEntryDto(ledgerEntryEntity: LedgerEntryEntity): LedgerEntryDto

    fun toSubledgerDto(subledgerEntryEntity: SubledgerEntryEntity): SubledgerEntryDto
}
