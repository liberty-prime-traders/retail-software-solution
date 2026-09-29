package me.ezra_home.retail_software_solution.locations.business.purchase.api

import java.io.Serializable
import java.util.Optional

data class PurchaseNotesUpdateDto(val notes: Optional<String>?) : Serializable
