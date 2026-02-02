package me.ezra_home.retail_software_solution.locations.business.sync.sync_services.product

import me.ezra_home.retail_software_solution.locations.business.sync.sync_services.SyncQueryConstants

object ProductQueryBuilder {

  fun buildFetchQueryForFull(afterReferenceNumber: String?): String {
    val p = ProductQueryConstants.Aliases.PRODUCT_TABLE
    val pg = ProductQueryConstants.Aliases.PRODUCT_GROUP_TABLE

    val whereCondition = if (afterReferenceNumber != null) {
      "WHERE $p.${ProductQueryConstants.Columns.REFERENCE_NUMBER} > :${SyncQueryConstants.CursorParameters.AFTER_REFERENCE_NUMBER}"
    } else ""

    return """
      SELECT
        $p.${ProductQueryConstants.Columns.ID},
        $p.${ProductQueryConstants.Columns.NAME},
        $p.${ProductQueryConstants.Columns.DESCRIPTION},
        $p.${ProductQueryConstants.Columns.PRODUCT_GROUP_NAME},
        $p.${ProductQueryConstants.Columns.STATUS},
        $p.${ProductQueryConstants.Columns.REFERENCE_NUMBER},
        $p.${ProductQueryConstants.Columns.BASE_UNIT_ID},
        $pg.${ProductQueryConstants.Columns.CATEGORY_ID},
        NULL AS ${ProductQueryConstants.Columns.REVISION}
      FROM ${ProductQueryConstants.Tables.MAIN} $p
      LEFT JOIN ${ProductQueryConstants.Tables.GROUP} $pg ON $p.${ProductQueryConstants.Columns.PRODUCT_GROUP_ID} = $pg.${ProductQueryConstants.Columns.ID}
      $whereCondition
      ORDER BY $p.${ProductQueryConstants.Columns.REFERENCE_NUMBER} ASC, $p.${ProductQueryConstants.Columns.ID} ASC
    """.trimIndent()
  }

  fun buildFetchQueryForIncremental(): String {
    val p = ProductQueryConstants.Aliases.PRODUCT_TABLE
    val a = ProductQueryConstants.Aliases.PRODUCT_AUDIT_TABLE
    val pg = ProductQueryConstants.Aliases.PRODUCT_GROUP_TABLE

    return """
      SELECT
        $p.${ProductQueryConstants.Columns.ID},
        $p.${ProductQueryConstants.Columns.NAME},
        $p.${ProductQueryConstants.Columns.DESCRIPTION},
        $p.${ProductQueryConstants.Columns.PRODUCT_GROUP_NAME},
        $p.${ProductQueryConstants.Columns.STATUS},
        $p.${ProductQueryConstants.Columns.REFERENCE_NUMBER},
        $p.${ProductQueryConstants.Columns.BASE_UNIT_ID},
        $pg.${ProductQueryConstants.Columns.CATEGORY_ID},
        $a.${SyncQueryConstants.AuditColumns.REV} AS ${ProductQueryConstants.Columns.REVISION}
      FROM ${ProductQueryConstants.Tables.MAIN} $p
      JOIN ${ProductQueryConstants.Tables.AUDIT} $a ON $p.${ProductQueryConstants.Columns.ID} = $a.${ProductQueryConstants.Columns.ID}
      LEFT JOIN ${ProductQueryConstants.Tables.GROUP} $pg ON $p.${ProductQueryConstants.Columns.PRODUCT_GROUP_ID} = $pg.${ProductQueryConstants.Columns.ID}
      WHERE $a.${SyncQueryConstants.AuditColumns.REVEND} IS NULL
        AND $a.${SyncQueryConstants.AuditColumns.REV} > :${SyncQueryConstants.CursorParameters.AFTER_REVISION}
      ORDER BY $a.${SyncQueryConstants.AuditColumns.REV} ASC, $p.${ProductQueryConstants.Columns.ID} ASC
    """.trimIndent()
  }
}
