package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleHeaderDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineDto
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentSnapshot
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeader
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPayment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import java.util.UUID

@Mapper(config = RtsMapperConfig::class, uses = [SaleSessionQualifierUtil::class])
interface DomainToSessionMapper {

    fun toSaleSessionHeader(saleHeaderDto: SaleHeaderDto): SaleSessionHeader

    @Mapping(source = "id", target = "identity", qualifiedBy = [PersistedSessionIdentity::class])
    @Mapping(source = ".", target = "defaultSalePrice", qualifiedBy = [SaleLineDefaultSalePrice::class])
    @Mapping(source = "locationProductId", target = "baseUnitId", qualifiedBy = [SaleLineBaseUnitId::class])
    fun toSaleSessionLine(
        saleLineDto: SaleLineDto,
        @Context baseUnitIdsByLocationProductId: Map<UUID, UUID>,
    ): SaleSessionLine

    @Mapping(source = "id", target = "identity", qualifiedBy = [PersistedSessionIdentity::class])
    @Mapping(
        source = "saleLineId",
        target = "relatedSaleLineIdentity",
        qualifiedBy = [RelatedSaleLineSessionIdentity::class]
    )
    fun toSaleSessionAdjustment(
        saleAdjustmentDto: SaleAdjustmentDto,
        @Context relatedSaleLineIdentityBySaleLineId: Map<UUID, SessionIdentity>,
    ): SaleSessionAdjustment

    @Mapping(source = "id", target = "identity", qualifiedBy = [PersistedSessionIdentity::class])
    fun toSaleSessionPayment(salePaymentSnapshot: SalePaymentSnapshot): SaleSessionPayment
}
