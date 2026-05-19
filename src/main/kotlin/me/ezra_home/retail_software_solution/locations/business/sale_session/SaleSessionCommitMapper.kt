package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitInput
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitLine
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitPayment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPayment
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = RtsMapperConfig::class)
interface SaleSessionCommitMapper {

    @Mapping(source = "saleVersion", target = "expectedVersion")
    @Mapping(source = "header.contactId", target = "contactId")
    @Mapping(source = "header.soldById", target = "soldById")
    @Mapping(source = "header.dateSold", target = "dateSold")
    @Mapping(source = "header.notes", target = "notes")
    fun toCommitInput(session: SaleSession): SaleCommitInput

    @Mapping(target = "clientKey", expression = "java(line.getIdentity().key())")
    @Mapping(target = "existingId", expression = "java(line.getIdentity().getId())")
    fun toCommitLine(line: SaleSessionLine): SaleCommitLine

    fun toCommitLines(lines: List<SaleSessionLine>): List<SaleCommitLine>

    @Mapping(target = "clientKey", expression = "java(adjustment.getIdentity().key())")
    @Mapping(target = "existingId", expression = "java(adjustment.getIdentity().getId())")
    @Mapping(
        target = "lineClientKey",
        expression = "java(adjustment.getLineIdentity() != null ? adjustment.getLineIdentity().key() : null)",
    )
    fun toCommitAdjustment(adjustment: SaleSessionAdjustment): SaleCommitAdjustment

    fun toCommitAdjustments(adjustments: List<SaleSessionAdjustment>): List<SaleCommitAdjustment>

    @Mapping(target = "clientKey", expression = "java(payment.getIdentity().key())")
    @Mapping(target = "existingId", expression = "java(payment.getIdentity().getId())")
    fun toCommitPayment(payment: SaleSessionPayment): SaleCommitPayment

    fun toCommitPayments(payments: List<SaleSessionPayment>): List<SaleCommitPayment>
}
