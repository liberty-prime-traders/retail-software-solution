package me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services

object SyncQueryConstants {
  object AuditColumns {
    const val REV = "rev"
    const val REVEND = "revend"
  }

  object CursorParameters {
    const val AFTER_REFERENCE_NUMBER = "afterReferenceNumber"
    const val AFTER_REVISION = "afterRevision"
  }
}
