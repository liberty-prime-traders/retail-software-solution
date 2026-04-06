package me.ezra_home.retail_software_solution.util.model

interface HasReferenceNumber {
    val referenceNumber: String?

    fun getNullSafeReferenceNumber(): String = referenceNumber ?: throw IllegalStateException("Reference number is not set")
}
