package me.ezra_home.retail_software_solution.organizations.business.stock_transfer

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
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.STOCK_TRANSFER_ORDER)
@HasReference(tableName = TableName.STOCK_TRANSFER_ORDER)
class StockTransferOrderEntity(

    @Column(name = "source_location_id", nullable = false, updatable = false)
    var sourceLocationId: UUID,

    @Column(name = "destination_location_id", nullable = false, updatable = false)
    var destinationLocationId: UUID,

    @Convert(converter = StockTransferStatusConverter::class)
    @Column(name = "status", nullable = false, length = 5)
    var status: StockTransferStatus = StockTransferStatus.DRAFT,

    @Column(name = "notes")
    var notes: String? = null,

    @Column(name = "line_count")
    var lineCount: Int? = null,

    @Column(name = "total_dispatched_cost", precision = 19, scale = 4)
    var totalDispatchedCost: BigDecimal? = null,

    @Column(name = "dispatched_at")
    var dispatchedAt: OffsetDateTime? = null,

    @Column(name = "dispatched_by_name", length = 200)
    var dispatchedByName: String? = null

) : HasReferenceEntity()
