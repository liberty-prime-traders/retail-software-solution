package me.ezra_home.retail_software_solution.locations.business.delivery

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseDeliveryStatusConverter
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.PURCHASE_DELIVERY)
@HasReference(tableName = TableName.PURCHASE_DELIVERY)
class PurchaseDeliveryEntity(

  @Column(name = "purchase_id", nullable = false)
  var purchaseId: UUID,

  @Convert(converter = PurchaseDeliveryStatusConverter::class)
  @Column(name = "status", nullable = false, length = 5)
  var status: PurchaseDeliveryStatus = PurchaseDeliveryStatus.PROCESSING,

  @Column(name = "delivered_at")
  var deliveredAt: OffsetDateTime? = null,

  @Column(name = "notes")
  var notes: String? = null

) : HasReferenceEntity()
