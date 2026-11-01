package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.time.LocalDate

// Whole organization-zone days, so the day buckets combine into any range. before is the day after the last day.
data class ProductSalesReportWindow(val from: LocalDate, val before: LocalDate) {

  val days: List<LocalDate> = from.datesUntil(before).toList()

  fun toDateRange(): DateSoldRange = DateSoldRange(
    DateTimes.Offset.atStartOfDayInOrganizationZone(from),
    DateTimes.Offset.atStartOfDayInOrganizationZone(before)
  )

  companion object {

    fun ofInclusiveDates(from: LocalDate?, to: LocalDate?): ProductSalesReportWindow {
      if (from == null || to == null) {
        throw RtsGenericException("from and to are required")
      }
      if (from.isAfter(to)) {
        throw RtsGenericException("from must not be after to")
      }
      if (!to.isBefore(from.plusYears(1))) {
        throw RtsGenericException("from and to must be less than one year apart")
      }
      return ProductSalesReportWindow(from, to.plusDays(1))
    }
  }
}
