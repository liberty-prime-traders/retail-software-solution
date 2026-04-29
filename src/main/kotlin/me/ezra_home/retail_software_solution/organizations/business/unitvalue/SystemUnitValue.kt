package me.ezra_home.retail_software_solution.organizations.business.unitvalue

import me.ezra_home.retail_software_solution.organizations.business.unitgroup.SystemUnitGroup
import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class SystemUnitValue(
    override val code: String,
    val unitName: String,
    val group: SystemUnitGroup,
    val baseUnit: SystemUnitValue? = null,
    val conversionFactor: Double? = null
) : HasCode {

    GRAM("g", "Gram", SystemUnitGroup.WEIGHT),
    KILOGRAM("kg", "Kilogram", SystemUnitGroup.WEIGHT, baseUnit = GRAM, conversionFactor = 1000.0),
    TONNE("t", "Tonne", SystemUnitGroup.WEIGHT, baseUnit = KILOGRAM, conversionFactor = 1000.0),

    MILLILITRE("ml", "Millilitre", SystemUnitGroup.VOLUME),
    LITRE("l", "Litre", SystemUnitGroup.VOLUME, baseUnit = MILLILITRE, conversionFactor = 1000.0),
    HALF_LITRE("hl", "Half Litre", SystemUnitGroup.VOLUME, baseUnit = LITRE, conversionFactor = 0.5),

    SINGLE("pc", "Single", SystemUnitGroup.COUNTABLE),
    DOZEN("dz", "Dozen", SystemUnitGroup.COUNTABLE, baseUnit = SINGLE, conversionFactor = 12.0),
    CRATE("crate", "Crate", SystemUnitGroup.COUNTABLE, baseUnit = SINGLE, conversionFactor = 24.0),
    TRAY("tray", "Tray", SystemUnitGroup.COUNTABLE, baseUnit = SINGLE, conversionFactor = 30.0)
}
