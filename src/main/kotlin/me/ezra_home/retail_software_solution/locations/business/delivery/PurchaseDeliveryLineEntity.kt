package me.ezra_home.retail_software_solution.locations.business.delivery

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
@Table(name = TableNames.PURCHASE_DELIVERY_LINE)
@HasReference(tableName = TableName.PURCHASE_DELIVERY_LINE)
class PurchaseDeliveryLineEntity(

  @Column(name = "purchase_delivery_id", nullable = false)
  var purchaseDeliveryId: UUID,

  @Column(name = "purchase_line_id", nullable = false)
  var purchaseLineId: UUID,

  @Column(name = "quantity_delivered", nullable = false, precision = 19, scale = 4)
  var quantityDelivered: BigDecimal,

  @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
  var unitCost: BigDecimal,

  @Column(name = "unit_id", nullable = false)
  var unitId: UUID

) : HasReferenceEntity()
