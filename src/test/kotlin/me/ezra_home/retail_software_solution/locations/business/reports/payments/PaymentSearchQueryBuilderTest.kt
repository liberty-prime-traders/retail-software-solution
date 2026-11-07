package me.ezra_home.retail_software_solution.locations.business.reports.payments

import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentSearchParameters
import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentVoidStatusFilter
import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentType
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.SqlQuery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PaymentSearchQueryBuilderTest {

  private val now = OffsetDateTime.of(2026, 3, 10, 9, 0, 0, 0, ZoneOffset.UTC)
  private val bothSources = PaymentSource.entries

  private fun listQuery(
    sources: List<PaymentSource>,
    parameters: PaymentSearchParameters,
    keysetSearchCursor: KeysetSearchCursor? = null
  ): SqlQuery {
    val query = PaymentSearchQueryBuilder.buildListQuery(sources, parameters, keysetSearchCursor)
    return query.copy(sql = query.sql.replace(Regex("\\s+"), " "))
  }

  @Test
  fun `the list is one union of per-source branches, each with its own limit`() {
    val sql = listQuery(bothSources, PaymentSearchParameters()).sql

    assertTrue(sql.contains("FROM sale_payment p INNER JOIN sale parent ON parent.id = p.sale_id"))
    assertTrue(sql.contains("LEFT JOIN sale_payment_void v ON v.sale_payment_id = p.id"))
    assertTrue(sql.contains("parent.contact_id AS contact_id, p.payment_method_id"))
    assertTrue(sql.contains("'SALE' AS payment_type"))
    assertTrue(sql.contains(" UNION ALL "))
    assertTrue(sql.contains("FROM supplier_payment p INNER JOIN purchase parent ON parent.id = p.purchase_id"))
    assertTrue(sql.contains("LEFT JOIN supplier_payment_void v ON v.supplier_payment_id = p.id"))
    assertTrue(sql.contains("parent.supplier_id AS contact_id, p.payment_method_id"))
    assertTrue(sql.contains("'PURCHASE' AS payment_type"))
    assertEquals(3, Regex("LIMIT :pageSize").findAll(sql).count())
    assertTrue(sql.endsWith(") AS payments ORDER BY created_on DESC, id DESC LIMIT :pageSize"))
  }

  @Test
  fun `a single source has no union`() {
    val sql = listQuery(listOf(PaymentSource.PURCHASE), PaymentSearchParameters()).sql

    assertFalse(sql.contains("UNION ALL"))
    assertFalse(sql.contains("sale_payment"))
  }

  @Test
  fun `the contact filter targets each source's parent contact column`() {
    val sql = listQuery(bothSources, PaymentSearchParameters(contactIds = listOf(UUID.randomUUID()))).sql

    assertTrue(sql.contains("parent.contact_id = ANY(:contactIds)"))
    assertTrue(sql.contains("parent.supplier_id = ANY(:contactIds)"))
  }

  @Test
  fun `each branch binds only its own type's reference numbers`() {
    val parameters = PaymentSearchParameters(
      referenceNumbersByType = mapOf(
        PaymentType.SALE to listOf("SL000001"),
        PaymentType.PURCHASE to listOf("PU000001")
      )
    )

    val query = listQuery(bothSources, parameters)

    assertEquals(listOf("SL000001"), (query.params.getValue("saleReferenceNumbers") as Array<*>).toList())
    assertEquals(listOf("PU000001"), (query.params.getValue("purchaseReferenceNumbers") as Array<*>).toList())
  }

  @Test
  fun `a single status adds a void clause and both statuses add none`() {
    val purchase = listOf(PaymentSource.PURCHASE)
    val activeOnly = PaymentSearchParameters(statuses = setOf(PaymentVoidStatusFilter.ACTIVE))
    val voidedOnly = PaymentSearchParameters(statuses = setOf(PaymentVoidStatusFilter.VOIDED))
    val both = PaymentSearchParameters(statuses = PaymentVoidStatusFilter.entries.toSet())

    assertTrue(listQuery(purchase, activeOnly).sql.contains("v.id IS NULL"))
    assertTrue(listQuery(purchase, voidedOnly).sql.contains("v.id IS NOT NULL"))
    assertFalse(listQuery(purchase, both).sql.contains("v.id IS"))
  }

  @Test
  fun `every branch applies the keyset cursor before its limit`() {
    val query = listQuery(bothSources, PaymentSearchParameters(), KeysetSearchCursor(now, UUID.randomUUID()))

    assertEquals(2, Regex("\\(p\\.created_on, p\\.id\\) < \\(:").findAll(query.sql).count())
    assertTrue(query.params.keys.containsAll(listOf("cursorCreatedOn", "cursorId")))
  }
}
