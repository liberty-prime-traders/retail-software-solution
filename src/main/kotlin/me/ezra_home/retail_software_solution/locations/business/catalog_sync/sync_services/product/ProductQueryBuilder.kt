package me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.product

import me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.SyncQueryConstants

object ProductQueryBuilder {

  private const val P = ProductQueryConstants.Aliases.PRODUCT_TABLE
  private const val A = ProductQueryConstants.Aliases.PRODUCT_AUDIT_TABLE
  private const val PG = ProductQueryConstants.Aliases.PRODUCT_GROUP_TABLE

  fun buildFetchQueryForFull(afterReferenceNumber: String?): String {
    val whereCondition = if (afterReferenceNumber != null) {
      "WHERE $P.${ProductQueryConstants.Columns.REFERENCE_NUMBER} > :${SyncQueryConstants.CursorParameters.AFTER_REFERENCE_NUMBER}"
    } else ""

    return """
      ${buildSelectClause("NULL")}
      ${buildBaseFromClause()}
      $whereCondition
      ORDER BY $P.${ProductQueryConstants.Columns.REFERENCE_NUMBER} ASC, $P.${ProductQueryConstants.Columns.ID} ASC
    """.trimIndent()
  }

  fun buildFetchQueryForIncremental(): String {
    return """
      ${buildSelectClause("$A.${SyncQueryConstants.AuditColumns.REV}")}
      ${buildAuditFromClause()}
      WHERE $A.${SyncQueryConstants.AuditColumns.REVEND} IS NULL
        AND $A.${SyncQueryConstants.AuditColumns.REV} > :${SyncQueryConstants.CursorParameters.AFTER_REVISION}
      ORDER BY $A.${SyncQueryConstants.AuditColumns.REV} ASC, $P.${ProductQueryConstants.Columns.ID} ASC
    """.trimIndent()
  }

  fun buildFetchQueryByOrgProductId(): String {
    return """
      ${buildSelectClause("$A.${SyncQueryConstants.AuditColumns.REV}")}
      ${buildAuditFromClause()}
      WHERE $A.${SyncQueryConstants.AuditColumns.REVEND} IS NULL
        AND $P.${ProductQueryConstants.Columns.ID} = :${ProductQueryConstants.Parameters.ORG_PRODUCT_ID}
    """.trimIndent()
  }

  private fun buildSelectClause(revisionExpression: String) = """
    SELECT
      $P.${ProductQueryConstants.Columns.ID},
      $P.${ProductQueryConstants.Columns.NAME},
      $P.${ProductQueryConstants.Columns.DESCRIPTION},
      $P.${ProductQueryConstants.Columns.PRODUCT_GROUP_NAME},
      $P.${ProductQueryConstants.Columns.STATUS},
      $P.${ProductQueryConstants.Columns.REFERENCE_NUMBER},
      $P.${ProductQueryConstants.Columns.BASE_UNIT_ID},
      $PG.${ProductQueryConstants.Columns.CATEGORY_ID},
      $revisionExpression AS ${ProductQueryConstants.Columns.REVISION}
  """.trimIndent()

  private fun buildBaseFromClause() = """
    FROM ${ProductQueryConstants.Tables.MAIN} $P
    LEFT JOIN ${ProductQueryConstants.Tables.GROUP} $PG ON $P.${ProductQueryConstants.Columns.PRODUCT_GROUP_ID} = $PG.${ProductQueryConstants.Columns.ID}
  """.trimIndent()

  private fun buildAuditFromClause() = """
    FROM ${ProductQueryConstants.Tables.MAIN} $P
    JOIN ${ProductQueryConstants.Tables.AUDIT} $A ON $P.${ProductQueryConstants.Columns.ID} = $A.${ProductQueryConstants.Columns.ID}
    LEFT JOIN ${ProductQueryConstants.Tables.GROUP} $PG ON $P.${ProductQueryConstants.Columns.PRODUCT_GROUP_ID} = $PG.${ProductQueryConstants.Columns.ID}
  """.trimIndent()
}
