package me.ezra_home.retail_software_solution.locations.business.purchase.search

import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineEntity
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class PurchaseSearchQueryBuilderTest {

  private val orderedTotalRoundingPattern = Regex("""storage_units_per_user_provided_unit,\s*(\d+)\s*\)\)""")

  @BeforeEach
  fun setUp() {
    SessionContextProvider.setSession(SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC")))
  }

  @AfterEach
  fun clearSession() {
    SessionContextProvider.clear()
  }

  @Test
  fun `the amount filter rounds each line at the money scale`() {
    val predicate = QueryBuilderContext(params = mutableMapOf(PurchaseSearchParameterNames.MIN_AMOUNT to BigDecimal.ONE))

    val sql = PurchaseSearchQueryBuilder.buildListQuery(predicate, null).sql

    assertRoundsAtMoneyScale(sql)
  }

  @Test
  fun `both summaries round each line at the money scale`() {
    val predicate = QueryBuilderContext()

    assertRoundsAtMoneyScale(PurchaseSearchQueryBuilder.buildPaymentStatusSummaryQuery(predicate).sql)
    assertRoundsAtMoneyScale(PurchaseSearchQueryBuilder.buildSupplierSummaryQuery(predicate).sql)
  }

  @Test
  fun `an unfiltered list does not sum the ordered total`() {
    val sql = PurchaseSearchQueryBuilder.buildListQuery(QueryBuilderContext(), null).sql

    assertTrue(!sql.contains("ordered_total"), sql)
  }

  @Test
  fun `the SQL line total equals the entity total to the cent for fractional per-unit ratios`() {
    val unitCosts = listOf("10.0000", "1.0000", "0.0050", "3.3333", "7.7777", "123456.7891")
    val storageUnitsPerUserProvidedUnits = listOf(3L, 7L, 8L, 12L, 18L, 24L)
    val canceledStorageQuantities = listOf("0", "1", "2.5")

    unitCosts.forEach { unitCost ->
      storageUnitsPerUserProvidedUnits.forEach { storageUnitsPerUserProvidedUnit ->
        canceledStorageQuantities.forEach { canceledStorageQuantity ->
          val purchaseLineEntity = PurchaseLineEntity(
            purchaseId = UUID.randomUUID(),
            locationProductId = UUID.randomUUID(),
            userProvidedQuantityOrdered = BigDecimal("3"),
            unitCost = BigDecimal(unitCost),
            unitId = UUID.randomUUID(),
            storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
            storageQuantityCanceled = BigDecimal(canceledStorageQuantity)
          )
          val sqlLineTotal = Decimals.MoneyScale.round(
            Decimals.Scale10.divide(
              BigDecimal(unitCost).multiply(BigDecimal(3 * storageUnitsPerUserProvidedUnit).subtract(BigDecimal(canceledStorageQuantity))),
              BigDecimal(storageUnitsPerUserProvidedUnit)
            )
          )

          assertEquals(
            0,
            sqlLineTotal.compareTo(purchaseLineEntity.getTotalCost()),
            "unitCost=$unitCost units=$storageUnitsPerUserProvidedUnit canceled=$canceledStorageQuantity"
          )
        }
      }
    }
  }

  private fun assertRoundsAtMoneyScale(sql: String) {
    val roundingScale = orderedTotalRoundingPattern.find(sql)?.groupValues?.get(1)?.toInt()

    assertTrue(roundingScale == Decimals.MoneyScale.scale, "ordered_total rounds at $roundingScale, expected ${Decimals.MoneyScale.scale}: $sql")
  }
}
