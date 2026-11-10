package me.ezra_home.retail_software_solution.locations.business.purchase

object PurchaseOrdering {

  // Postgres orders uuid as unsigned bytes while UUID.compareTo is signed; the text form sorts like Postgres.
  val OLDEST_FIRST: Comparator<PurchaseEntity> =
    compareBy<PurchaseEntity> { it.requiredCreatedOn() }.thenBy { it.id.toString() }
}
