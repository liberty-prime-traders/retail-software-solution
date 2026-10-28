package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import

import me.ezra_home.retail_software_solution.util.business.StringUtils

// The keys StringUtils.isEquivalent and isEquivalentOnAlphanumerics compare, so a set lookup matches what a scan would.
internal object BulkUnitNameKeys {

    fun groupKey(groupName: String?): String =
        if (groupName.isNullOrBlank()) "" else StringUtils.normalizeForComparison(groupName)

    fun alphanumericNameKey(name: String?): String? =
        name?.takeIf { it.isNotBlank() }?.let { StringUtils.alphanumericKey(it) }?.takeIf { it.isNotEmpty() }
}
