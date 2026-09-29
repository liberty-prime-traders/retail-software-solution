package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.STOCK_TRANSFER_RECEIPT_LINE)
@HasReference(tableName = TableName.STOCK_TRANSFER_RECEIPT_LINE)
class StockTransferReceiptLineEntity(

    @Column(name = "stock_transfer_receipt_id", nullable = false, updatable = false)
    var stockTransferReceiptId: UUID,

    @Column(name = "stock_transfer_dispatch_line_ref", nullable = false, updatable = false, length = 30)
    var stockTransferDispatchLineRef: String,

    @Column(name = "location_product_id", nullable = false, updatable = false)
    var locationProductId: UUID,

    @Column(name = "quantity_received", nullable = false, precision = 15, scale = 4)
    var quantityReceived: BigDecimal

) : HasReferenceEntity()
