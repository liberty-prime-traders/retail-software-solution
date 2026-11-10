package me.ezra_home.retail_software_solution.locations.business.reports.payments

import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentSearchParameters
import me.ezra_home.retail_software_solution.locations.business.reports.payments.filters.PaymentVoidStatusFilterStrategy
import me.ezra_home.retail_software_solution.util.model.ColumnNames
import me.ezra_home.retail_software_solution.util.queries.AnyListFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.DateRangeFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.ExpressionRangeFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
import me.ezra_home.retail_software_solution.util.queries.SqlQuery

object PaymentSearchQueryBuilder {

  private const val PAYMENT = "p"
  private const val VOID = "v"
  private const val PARENT = "parent"

  private const val PAYMENT_ID_COLUMN = "$PAYMENT.${ColumnNames.Common.ID}"
  private const val PAYMENT_CREATED_ON_COLUMN = "$PAYMENT.${ColumnNames.Common.CREATED_ON}"
  private const val PAYMENT_DATE_COLUMN = "$PAYMENT.${ColumnNames.Payment.PAYMENT_DATE}"
  private const val PAYMENT_METHOD_ID_COLUMN = "$PAYMENT.${ColumnNames.Payment.PAYMENT_METHOD_ID}"
  private const val PAYMENT_AMOUNT_COLUMN = "$PAYMENT.${ColumnNames.Payment.AMOUNT}"
  private const val VOID_ID_COLUMN = "$VOID.${ColumnNames.Common.ID}"
  private const val VOID_REASON_COLUMN = "$VOID.${ColumnNames.PaymentVoid.REASON}"
  private const val PARENT_ID_COLUMN = "$PARENT.${ColumnNames.Common.ID}"
  private const val PARENT_REFERENCE_NUMBER_COLUMN = "$PARENT.${ColumnNames.Common.REFERENCE_NUMBER}"

  private const val RECORDED_FROM = "recordedFrom"
  private const val RECORDED_BEFORE = "recordedBefore"
  private const val PAYMENT_DATE_FROM = "paymentDateFrom"
  private const val PAYMENT_DATE_BEFORE = "paymentDateBefore"
  private const val CONTACT_IDS = "contactIds"
  private const val PAYMENT_METHOD_IDS = "paymentMethodIds"
  private const val MIN_AMOUNT = "minAmount"
  private const val MAX_AMOUNT = "maxAmount"

  private fun buildPredicate(
    paymentSource: PaymentSource,
    paymentSearchParameters: PaymentSearchParameters
  ): QueryBuilderContext {
    val context = QueryBuilderContext()
    context.whereClauses.add("1=1")

    DateRangeFilterStrategy(
      PAYMENT_CREATED_ON_COLUMN,
      RECORDED_FROM,
      paymentSearchParameters.recordedFrom,
      RECORDED_BEFORE,
      paymentSearchParameters.recordedBefore
    ).apply(context)
    DateRangeFilterStrategy(
      PAYMENT_DATE_COLUMN,
      PAYMENT_DATE_FROM,
      paymentSearchParameters.paymentDateFrom,
      PAYMENT_DATE_BEFORE,
      paymentSearchParameters.paymentDateBefore
    ).apply(context)
    AnyListFilterStrategy(
      "$PARENT.${paymentSource.parentContactColumn}",
      CONTACT_IDS,
      paymentSearchParameters.contactIds.toTypedArray()
    ).apply(context)
    AnyListFilterStrategy(
      PAYMENT_METHOD_ID_COLUMN,
      PAYMENT_METHOD_IDS,
      paymentSearchParameters.paymentMethodIds.toTypedArray()
    ).apply(context)
    PaymentVoidStatusFilterStrategy(paymentSearchParameters.statuses, VOID_ID_COLUMN).apply(context)
    ExpressionRangeFilterStrategy(
      PAYMENT_AMOUNT_COLUMN,
      MIN_AMOUNT,
      paymentSearchParameters.minAmount,
      MAX_AMOUNT,
      paymentSearchParameters.maxAmount
    ).apply(context)
    AnyListFilterStrategy(
      PARENT_REFERENCE_NUMBER_COLUMN,
      referenceNumbersParameterName(paymentSource),
      paymentSearchParameters.referenceNumbersOf(paymentSource.type).toTypedArray()
    ).apply(context)
    return context
  }

  // Each branch applies the keyset cursor and its own LIMIT, so Postgres reads at most one page per source.
  fun buildListQuery(
    paymentSources: List<PaymentSource>,
    paymentSearchParameters: PaymentSearchParameters,
    keysetSearchCursor: KeysetSearchCursor?
  ): SqlQuery {
    val queryParameters = mutableMapOf<String, Any>()
    val branches = paymentSources.map { paymentSource ->
      val predicate = buildPredicate(paymentSource, paymentSearchParameters)
      queryParameters.putAll(predicate.params)
      buildListBranch(paymentSource, predicate.whereClauses + cursorClause(keysetSearchCursor))
    }
    if (keysetSearchCursor != null) {
      queryParameters[QueryParameterNames.CURSOR_CREATED_ON] = keysetSearchCursor.createdOn
      queryParameters[QueryParameterNames.CURSOR_ID] = keysetSearchCursor.id
    }

    val sql = """
      SELECT id, reference_number, created_on, payment_date, contact_id, payment_method_id, amount, void_reason, payment_type
      FROM (
        ${branches.joinToString("\n UNION ALL \n")}
      ) AS payments
      ORDER BY created_on DESC, id DESC
      LIMIT :${QueryParameterNames.PAGE_SIZE}
    """.trimIndent()

    return SqlQuery(sql, queryParameters, PaymentSearchQueryMetadata("payment_search"))
  }

  private fun buildListBranch(paymentSource: PaymentSource, whereClauses: List<String>) = """
    (
      SELECT
        ${PAYMENT_ID_COLUMN} AS id,
        ${PARENT_REFERENCE_NUMBER_COLUMN} AS reference_number,
        ${PAYMENT_CREATED_ON_COLUMN} AS created_on,
        ${PAYMENT_DATE_COLUMN} AS payment_date,
        $PARENT.${paymentSource.parentContactColumn} AS contact_id,
        ${PAYMENT_METHOD_ID_COLUMN} AS payment_method_id,
        ${PAYMENT_AMOUNT_COLUMN} AS amount,
        ${VOID_REASON_COLUMN} AS void_reason,
        '${paymentSource.type.name}' AS payment_type
      ${fromAndJoins(paymentSource)}
      WHERE ${whereClauses.joinToString(" AND ")}
      ORDER BY ${PAYMENT_CREATED_ON_COLUMN} DESC, ${PAYMENT_ID_COLUMN} DESC
      LIMIT :${QueryParameterNames.PAGE_SIZE}
    )
  """

  private fun cursorClause(keysetSearchCursor: KeysetSearchCursor?) = listOfNotNull(
    keysetSearchCursor?.let {
      "(${PAYMENT_CREATED_ON_COLUMN}, ${PAYMENT_ID_COLUMN}) < (:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"
    }
  )

  fun buildSummaryQuery(paymentSource: PaymentSource, paymentSearchParameters: PaymentSearchParameters): SqlQuery {
    val predicate = buildPredicate(paymentSource, paymentSearchParameters)
    val sql = """
      SELECT
        ${PAYMENT_METHOD_ID_COLUMN} AS payment_method_id,
        COALESCE(SUM(${PAYMENT_AMOUNT_COLUMN}) FILTER (WHERE ${VOID_ID_COLUMN} IS NULL), 0) AS active_total,
        COALESCE(SUM(${PAYMENT_AMOUNT_COLUMN}) FILTER (WHERE ${VOID_ID_COLUMN} IS NOT NULL), 0) AS voided_total,
        COUNT(*) FILTER (WHERE ${VOID_ID_COLUMN} IS NULL) AS active_count,
        COUNT(*) FILTER (WHERE ${VOID_ID_COLUMN} IS NOT NULL) AS voided_count
      ${fromAndJoins(paymentSource)}
      WHERE ${predicate.whereClauses.joinToString(" AND ")}
      GROUP BY ${PAYMENT_METHOD_ID_COLUMN}
    """.trimIndent()

    val queryName = "payment_search_summary_${paymentSource.name.lowercase()}"
    return SqlQuery(sql, predicate.params, PaymentSearchQueryMetadata(queryName))
  }

  private fun fromAndJoins(paymentSource: PaymentSource) = """
    FROM ${paymentSource.paymentTable} $PAYMENT
    INNER JOIN ${paymentSource.parentTable} $PARENT
      ON ${PARENT_ID_COLUMN} = $PAYMENT.${paymentSource.parentLinkColumn}
    LEFT JOIN ${paymentSource.voidTable} $VOID
      ON $VOID.${paymentSource.voidPaymentColumn} = ${PAYMENT_ID_COLUMN}
  """

  // Each branch binds its own reference list, so the name carries the type.
  private fun referenceNumbersParameterName(paymentSource: PaymentSource) =
    "${paymentSource.type.name.lowercase()}ReferenceNumbers"
}
