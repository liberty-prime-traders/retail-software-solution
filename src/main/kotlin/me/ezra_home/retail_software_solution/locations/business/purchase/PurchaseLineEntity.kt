package me.ezra_home.retail_software_solution.locations.business.purchase

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.PURCHASE_LINE)
@HasReference(tableName = TableName.PURCHASE_LINE)
class PurchaseLineEntity(

  @Column(name = "purchase_id", nullable = false)
  var purchaseId: UUID,

  @Column(name = "location_product_id", nullable = false, updatable = false)
  override var locationProductId: UUID,

  @Column(name = "user_provided_quantity_ordered", nullable = false, precision = 19, scale = 4)
  var userProvidedQuantityOrdered: BigDecimal,

  @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
  var unitCost: BigDecimal,

  @Column(name = "unit_id", nullable = false)
  var unitId: UUID,

  @Column(name = "storage_units_per_user_provided_unit", nullable = false)
  var storageUnitsPerUserProvidedUnit: Long,

  @Column(name = "storage_quantity_delivered", nullable = false, precision = 28, scale = 4)
  var storageQuantityDelivered: BigDecimal = BigDecimal.ZERO,

  @Column(name = "storage_quantity_canceled", nullable = false, precision = 28, scale = 4)
  var storageQuantityCanceled: BigDecimal = BigDecimal.ZERO

) : HasLocationProduct, HasReferenceEntity() {

  fun storageQuantityOrdered(): ExactQuantity = ExactQuantity.ofStorageFromUserProvided(userProvidedQuantityOrdered, storageUnitsPerUserProvidedUnit)

  fun storageQuantityExpected(): ExactQuantity = storageQuantityOrdered() - ExactQuantity.ofStorage(storageQuantityCanceled)

  fun storageQuantityRemaining(): ExactQuantity = storageQuantityExpected() - ExactQuantity.ofStorage(storageQuantityDelivered)

  fun getTotalCost(): BigDecimal = Decimals.MoneyScale.divide(
    unitCost.multiply(storageQuantityExpected().toStorageQuantity()),
    BigDecimal.valueOf(storageUnitsPerUserProvidedUnit)
  )

}
