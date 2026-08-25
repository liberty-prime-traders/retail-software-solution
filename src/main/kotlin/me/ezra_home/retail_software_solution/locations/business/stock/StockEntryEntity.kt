package me.ezra_home.retail_software_solution.locations.business.stock

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.StockItemSourceConverter
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
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
class StockEntryEntity(

  @Column(name = "location_product_id", nullable = false)
  var locationProductId: UUID,

  @Column(name = "source_type", length = 5)
  @Convert(converter = StockItemSourceConverter::class)
  var sourceType: StockItemSource? = null,

  @Column(name = "external_reference_number", length = 30)
  var externalReferenceNumber: String? = null,

  @Column(name = "batch_size", nullable = false, precision = 15, scale = 3, updatable = false)
  var batchSize: BigDecimal,

  @Column(name = "quantity_remaining", nullable = false, precision = 15, scale = 3)
  var quantityRemaining: BigDecimal,

  @Column(name = "priority")
  var priority: Int,

  @Column(name = "unit_cost", precision = 15, scale = 2)
  var unitCost: BigDecimal? = null

) : HasReferenceEntity()
