package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatusConverter
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.STOCK_TRANSFER_DISPATCH)
@HasReference(tableName = TableName.STOCK_TRANSFER_DISPATCH)
class StockTransferDispatchEntity(

    @Column(name = "stock_transfer_order_ref", nullable = false, updatable = false, length = 30)
    var stockTransferOrderRef: String,

    @Convert(converter = StockTransferStatusConverter::class)
    @Column(name = "status", nullable = false, length = 5)
    var status: StockTransferStatus = StockTransferStatus.DRAFT,

    @Column(name = "dispatched_by_id")
    var dispatchedById: UUID? = null,

    @Column(name = "dispatched_at")
    var dispatchedAt: OffsetDateTime? = null,

    @Column(name = "notes")
    var notes: String? = null

) : HasReferenceEntity()
