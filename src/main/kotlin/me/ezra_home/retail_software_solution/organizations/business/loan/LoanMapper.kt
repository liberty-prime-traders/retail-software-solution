package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.util.business.mappers.RtsMapperConfig
import org.mapstruct.Mapper

@Mapper(config = RtsMapperConfig::class)
interface LoanMapper {

    fun toDto(loanEntity: LoanEntity): LoanDto

    fun toRepaymentDto(loanRepaymentEntity: LoanRepaymentEntity): LoanRepaymentDto

    fun toVoidDto(loanVoidEntity: LoanVoidEntity): LoanVoidDto

    fun toRepaymentVoidDto(loanRepaymentVoidEntity: LoanRepaymentVoidEntity): LoanRepaymentVoidDto

    fun toStatusFigures(loanStatusEntity: LoanStatusEntity): LoanStatusFigures
}
