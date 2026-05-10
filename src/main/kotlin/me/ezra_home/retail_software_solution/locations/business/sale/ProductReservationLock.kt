package me.ezra_home.retail_software_solution.locations.business.sale

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductReservationLock(
    private val reservationRepository: SaleLineStockReservationRepository,
) {
    // Sorted acquisition prevents the classic two-threads-different-order deadlock.
    fun acquire(locationProductIds: Collection<UUID>) {
        locationProductIds.toSortedSet().forEach { reservationRepository.acquireProductReservationLock(it) }
    }
}
