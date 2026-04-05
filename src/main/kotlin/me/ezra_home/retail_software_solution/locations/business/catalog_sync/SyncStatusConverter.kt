package me.ezra_home.retail_software_solution.locations.business.catalog_sync

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.locations.business.catalog_sync.api.SyncStatus
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class SyncStatusConverter : EnumConverter<SyncStatus>(SyncStatus::class.java)
