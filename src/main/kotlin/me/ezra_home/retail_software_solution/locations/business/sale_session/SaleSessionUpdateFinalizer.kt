package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionResponseDto
import org.springframework.stereotype.Component

@Component
class SaleSessionUpdateFinalizer(
    private val saleSessionStore: SaleSessionStore,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
) {

    fun finalize(updated: SaleSession): SaleSessionResponseDto {
        val touched = updated.markTouched(SessionContextProvider.getUserId())
        val withTotals = saleSessionTotalsCalculator.recompute(touched)
        saleSessionValidator.validate(withTotals)
        saleSessionStore.save(withTotals)
        return saleSessionAssembler.buildResponse(withTotals)
    }
}
