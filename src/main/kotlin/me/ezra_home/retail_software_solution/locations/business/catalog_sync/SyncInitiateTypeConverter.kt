package me.ezra_home.retail_software_solution.locations.business.catalog_sync

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
internal class SyncInitiateTypeConverter : EnumConverter<SyncInitiateType>(SyncInitiateType::class.java)
