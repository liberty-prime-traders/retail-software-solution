package me.ezra_home.retail_software_solution.organizations.business.unitvalue.api

import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.SystemUnitGroup
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

    PIECE("pc", "Piece", SystemUnitGroup.COUNTABLE),
    DOZEN("dz", "Dozen", SystemUnitGroup.COUNTABLE, baseUnit = PIECE, conversionFactor = 12.0),
    CRATE("crate", "Crate", SystemUnitGroup.COUNTABLE, baseUnit = PIECE, conversionFactor = 24.0),
    TRAY("tray", "Tray", SystemUnitGroup.COUNTABLE, baseUnit = PIECE, conversionFactor = 30.0);

    companion object {
        // unit_value.code is unique org-wide (uq_unit_value_code), but every group that gets a
        // default Piece would otherwise want the same "pc" code - folding the group name in
        // isolates each group's Piece. Group names are required to contain at least one letter
        // or digit (UnitGroupService.validateNameOnSave, BulkUnitImportValidator), so the slug is
        // never empty.
        private const val PIECE_CODE_SUFFIX = "-pc"
        private const val MAX_CODE_LENGTH = 25

        fun pieceCodeForGroup(groupName: String?): String {
            val slug = (groupName ?: "")
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .take(MAX_CODE_LENGTH - PIECE_CODE_SUFFIX.length)
                .trim('-')
            return "$slug$PIECE_CODE_SUFFIX"
        }
    }
}
