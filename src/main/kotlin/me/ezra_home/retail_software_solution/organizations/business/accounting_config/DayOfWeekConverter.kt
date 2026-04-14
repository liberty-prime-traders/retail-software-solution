package me.ezra_home.retail_software_solution.organizations.business.accounting_config

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.DayOfWeek

@Converter
class DayOfWeekConverter : AttributeConverter<DayOfWeek, String> {
    override fun convertToDatabaseColumn(attribute: DayOfWeek?): String? = attribute?.name
    override fun convertToEntityAttribute(dbData: String?): DayOfWeek? = dbData?.let { DayOfWeek.valueOf(it) }
}
