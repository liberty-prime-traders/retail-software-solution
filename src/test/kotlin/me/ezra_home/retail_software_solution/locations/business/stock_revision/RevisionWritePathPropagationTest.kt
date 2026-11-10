package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineReplacer
import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryRevisionApplier
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockReplacementLayerRecorder
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockRevisionCommitter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Propagation

class RevisionWritePathPropagationTest {

    private fun propagationOf(serviceClass: Class<*>, methodName: String): Propagation =
        serviceClass.declaredMethods.single { it.name == methodName }
            .getAnnotation(TransactionalOnLocationSchema::class.java)
            .propagation

    @Test
    fun `every write of a revision joins the revisioner's transaction and cannot run alone`() {
        val mandatoryWrites = mapOf(
            StockRevisionCommitter::class.java to listOf("commitReset", "commitReplay"),
            StockReplacementLayerRecorder::class.java to listOf("record"),
            PurchaseDeliveryLineReplacer::class.java to listOf("replace"),
            PurchaseDeliveryRevisionApplier::class.java to listOf("apply"),
        )

        mandatoryWrites.forEach { (serviceClass, methodNames) ->
            methodNames.forEach { methodName ->
                assertEquals(
                    Propagation.MANDATORY, propagationOf(serviceClass, methodName), "$serviceClass.$methodName"
                )
            }
        }
    }
}
