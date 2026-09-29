package me.ezra_home.retail_software_solution.locations.business.stock

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = TableNames.SALE_LINE_STOCK_RESERVATION)
class StockReservationEntity(

    @Column(name = "sale_id", nullable = false)
    var saleId: UUID,

    @Column(name = "sale_line_id", nullable = false)
    var saleLineId: UUID,

    @Column(name = "location_product_id", nullable = false)
    var locationProductId: UUID,

    @Column(name = "quantity_reserved", nullable = false, precision = 15, scale = 4)
    var quantityReserved: BigDecimal,

) : HasCreatorEntity()
