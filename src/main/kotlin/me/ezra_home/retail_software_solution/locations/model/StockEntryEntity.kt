package me.ezra_home.retail_software_solution.locations.model

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
@Table(name = TableNames.STOCK_ENTRY)
@HasReference(tableName = TableName.STOCK_ENTRY)
internal class StockEntryEntity(

  @Column(name = "purchase_delivery_line_id")
  var purchaseDeliveryLineId: UUID? = null,

  @Column(name = "location_product_id", nullable = false)
  var locationProductId: UUID,

  @Column(name = "source_type_id", nullable = false)
  var sourceTypeId: UUID,

  @Column(name = "source_id")
  var sourceId: UUID? = null,

  @Column(name = "batch_size", nullable = false, precision = 15, scale = 3, updatable = false)
  var batchSize: BigDecimal,

  @Column(name = "quantity_remaining", nullable = false, precision = 15, scale = 3)
  var quantityRemaining: BigDecimal,

  @Column(name = "priority")
  var priority: Int? = null,

  @Column(name = "unit_cost", precision = 15, scale = 2)
  var unitCost: BigDecimal? = null

) : HasReferenceEntity()
