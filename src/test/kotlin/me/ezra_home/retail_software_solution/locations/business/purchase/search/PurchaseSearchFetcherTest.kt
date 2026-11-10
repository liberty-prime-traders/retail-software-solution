package me.ezra_home.retail_software_solution.locations.business.purchase.search

import me.ezra_home.retail_software_solution.locations.business.purchase.api.search.PurchaseSearchParameters
import me.ezra_home.retail_software_solution.util.queries.SqlQuery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock

class PurchaseSearchFetcherTest {

  private val purchaseSearchListExecutor = mock(PurchaseSearchListExecutor::class.java)
  private val purchaseSearchFetcher = PurchaseSearchFetcher(
    purchaseSearchListExecutor, mock(PurchaseSearchSummaryExecutor::class.java)
  )
  private val executedQueries = mutableListOf<SqlQuery>()
  private val executedPageSizes = mutableListOf<Int>()

  @Suppress("UNCHECKED_CAST")
  private fun <T> anyNonNull(): T {
    ArgumentMatchers.any<T>()
    return null as T
  }

  private fun givenEmptyResults() {
    doAnswer { invocation ->
      executedQueries.add(invocation.getArgument(0))
      executedPageSizes.add(invocation.getArgument(1))
      emptyList<Any>()
    }.`when`(purchaseSearchListExecutor).execute(anyNonNull(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyBoolean())
  }

  @Test
  fun `a search is newest first unless asked otherwise, and reads one row past the page to detect more`() {
    givenEmptyResults()

    purchaseSearchFetcher.search(PurchaseSearchParameters(), null, 10)

    assertEquals(listOf(11), executedPageSizes)
    assertTrue(executedQueries.single().sql.contains("created_on DESC"), executedQueries.single().sql)
  }

  @Test
  fun `a search asked for oldest first orders ascending`() {
    givenEmptyResults()

    purchaseSearchFetcher.search(PurchaseSearchParameters(), null, 10, oldestFirst = true)

    assertTrue(executedQueries.single().sql.contains("created_on ASC"), executedQueries.single().sql)
  }
}
