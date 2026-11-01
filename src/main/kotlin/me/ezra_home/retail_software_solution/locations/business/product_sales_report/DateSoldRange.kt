package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import java.time.OffsetDateTime

data class DateSoldRange(val fromInclusive: OffsetDateTime, val beforeExclusive: OffsetDateTime)
