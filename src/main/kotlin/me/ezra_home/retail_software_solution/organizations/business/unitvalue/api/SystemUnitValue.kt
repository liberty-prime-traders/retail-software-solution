package me.ezra_home.retail_software_solution.organizations.business.unitvalue.api

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

    MILLILITRE("ml", "Millilitre", SystemUnitGroup.VOLUME),
    LITRE("l", "Litre", SystemUnitGroup.VOLUME, baseUnit = MILLILITRE, unitsOfBasePerUnit = 1000L),
    HALF_LITRE("hl", "Half Litre", SystemUnitGroup.VOLUME, baseUnit = MILLILITRE, unitsOfBasePerUnit = 500L),

    PIECE("pc", "Piece", SystemUnitGroup.COUNTABLE),
    DOZEN("dz", "Dozen", SystemUnitGroup.COUNTABLE, baseUnit = PIECE, unitsOfBasePerUnit = 12L),
    HALF_DOZEN("hdz", "Half Dozen", SystemUnitGroup.COUNTABLE, baseUnit = PIECE, unitsOfBasePerUnit = 6L),

    MISC_PIECE("misc-pc", "Piece", SystemUnitGroup.MISC);

    companion object {
        private const val BASE_CODE_SUFFIX = "-base"
        const val MAX_CODE_LENGTH = 25

        fun baseUnitCodeForGroup(groupName: String?): String {
            val slug = (groupName ?: "")
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .take(MAX_CODE_LENGTH - BASE_CODE_SUFFIX.length)
                .trim('-')
            return "$slug$BASE_CODE_SUFFIX"
        }
    }
}
