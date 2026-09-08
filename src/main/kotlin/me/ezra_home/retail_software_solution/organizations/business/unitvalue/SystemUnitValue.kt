package me.ezra_home.retail_software_solution.organizations.business.unitvalue

import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.SystemUnitGroup
import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class SystemUnitValue(
    override val code: String,
    val unitName: String,
    val group: SystemUnitGroup,
    val baseUnit: SystemUnitValue? = null,
    val unitsOfBasePerUnit: Long? = null
) : HasCode {

    GRAM("g", "Gram", SystemUnitGroup.WEIGHT),
    KILOGRAM("kg", "Kilogram", SystemUnitGroup.WEIGHT, baseUnit = GRAM, unitsOfBasePerUnit = 1000L),
    TONNE("t", "Tonne", SystemUnitGroup.WEIGHT, baseUnit = KILOGRAM, unitsOfBasePerUnit = 1000L),

    MILLILITRE("ml", "Millilitre", SystemUnitGroup.VOLUME),
    LITRE("l", "Litre", SystemUnitGroup.VOLUME, baseUnit = MILLILITRE, unitsOfBasePerUnit = 1000L),
    HALF_LITRE("hl", "Half Litre", SystemUnitGroup.VOLUME, baseUnit = MILLILITRE, unitsOfBasePerUnit = 500L),

    PIECE("pc", "Piece", SystemUnitGroup.COUNTABLE),
    DOZEN("dz", "Dozen", SystemUnitGroup.COUNTABLE, baseUnit = PIECE, unitsOfBasePerUnit = 12L),
    CRATE("crate", "Crate", SystemUnitGroup.COUNTABLE, baseUnit = PIECE, unitsOfBasePerUnit = 24L),
    TRAY("tray", "Tray", SystemUnitGroup.COUNTABLE, baseUnit = PIECE, unitsOfBasePerUnit = 30L)
}
