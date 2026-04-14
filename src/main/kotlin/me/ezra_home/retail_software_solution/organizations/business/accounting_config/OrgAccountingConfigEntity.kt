package me.ezra_home.retail_software_solution.organizations.business.accounting_config

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.accounting_config.api.FiscalPeriodCycle
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.DayOfWeek

@Entity
@Table(name = TableNames.ORG_ACCOUNTING_CONFIG)
class OrgAccountingConfigEntity(

    @Column(name = "fiscal_year_end_month", nullable = false)
    var fiscalYearEndMonth: Int,

    @Column(name = "fiscal_year_end_day", nullable = false)
    var fiscalYearEndDay: Int,

    @Column(name = "fiscal_period_cycle", nullable = false, length = 5)
    var fiscalPeriodCycle: FiscalPeriodCycle,

    @Column(name = "period_week_start_day", nullable = false, length = 9)
    @Convert(converter = DayOfWeekConverter::class)
    var periodWeekStartDay: DayOfWeek,

    @Column(name = "period_prep_days", nullable = false)
    var periodPrepDays: Int = 2

) : HasCreatorEntity()
