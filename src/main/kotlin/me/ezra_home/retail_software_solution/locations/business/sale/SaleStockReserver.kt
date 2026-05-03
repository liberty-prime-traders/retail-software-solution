package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class SaleStockReserver(
    private val reservationRepository: SaleLineStockReservationRepository
) {

    private val map: MutableMap<String, MutableMap<UUID, BigDecimal>> = ConcurrentHashMap()

    fun getReserved(locationProductId: UUID): BigDecimal = getOrLoadFromDb(locationProductId)

    fun add(locationProductId: UUID, quantity: BigDecimal) {
        val schema = SessionContextProvider.getLocationSchema()
        map.getOrPut(schema) { ConcurrentHashMap() }
            .merge(locationProductId, quantity, BigDecimal::add)
    }

    fun reduce(locationProductId: UUID, quantity: BigDecimal) {
        val schema = SessionContextProvider.getLocationSchema()
        val schemaMap = map[schema] ?: return
        val current = schemaMap[locationProductId] ?: return
        val updated = current.subtract(quantity)
        if (updated <= BigDecimal.ZERO) {
            schemaMap.remove(locationProductId)
        } else {
            schemaMap[locationProductId] = updated
        }
        if (schemaMap.isEmpty()) map.remove(schema)
    }

    fun getOrLoadFromDb(locationProductId: UUID): BigDecimal {
        val schema = SessionContextProvider.getLocationSchema()
        val schemaMap = map.getOrPut(schema) { ConcurrentHashMap() }
        return schemaMap.computeIfAbsent(locationProductId) {
            reservationRepository.sumQuantityReservedByLocationProductId(locationProductId)
        }
    }

    fun getOrLoadFromDbBulk(locationProductIds: Collection<UUID>): Map<UUID, BigDecimal> {
        val schema = SessionContextProvider.getLocationSchema()
        val schemaMap = map.getOrPut(schema) { ConcurrentHashMap() }
        val missing = locationProductIds.filter { !schemaMap.containsKey(it) }
        if (missing.isNotEmpty()) {
            reservationRepository.sumQuantityReservedByLocationProductIdIn(missing)
                .forEach { row -> schemaMap.putIfAbsent(row[0] as UUID, row[1] as BigDecimal) }
            missing.forEach { id -> schemaMap.putIfAbsent(id, BigDecimal.ZERO) }
        }
        return locationProductIds.associateWith { schemaMap[it] ?: BigDecimal.ZERO }
    }

    fun reserve(saleId: UUID, lines: List<SaleLineEntity>, baseQtyByProductId: Map<UUID, BigDecimal>) {
        val reservations = lines.map { line ->
            SaleLineStockReservationEntity(
                saleId = saleId,
                saleLineId = line.id!!,
                locationProductId = line.locationProductId,
                quantityReserved = baseQtyByProductId[line.locationProductId]!!
            )
        }
        reservationRepository.saveAll(reservations)
        lines.forEach { line -> add(line.locationProductId, baseQtyByProductId[line.locationProductId]!!) }
    }

    fun clearBySale(saleId: UUID) {
        val reservations = reservationRepository.findBySaleId(saleId)
        reservationRepository.deleteBySaleId(saleId)
        reservations.forEach { r -> reduce(r.locationProductId, r.quantityReserved) }
    }

    fun clearByLines(lineIds: List<UUID>, locationProductId: UUID) {
        val total = reservationRepository.sumQuantityReservedBySaleLineIdIn(lineIds)
        reservationRepository.deleteBySaleLineIdIn(lineIds)
        reduce(locationProductId, total)
    }

    fun syncUpdatedReservations(
        updatedLinesWithNewBaseQty: List<Pair<SaleLineEntity, BigDecimal>>,
        newLines: List<SaleLineEntity>,
        newBaseQtyByProductId: Map<UUID, BigDecimal>,
        saleId: UUID
    ) {
        if (updatedLinesWithNewBaseQty.isNotEmpty()) {
            val lineIds = updatedLinesWithNewBaseQty.map { it.first.id!! }
            val oldReservations = reservationRepository.findBySaleId(saleId)
                .filter { it.saleLineId in lineIds }
                .associateBy { it.saleLineId }
            reservationRepository.deleteBySaleLineIdIn(lineIds)
            reservationRepository.saveAll(updatedLinesWithNewBaseQty.map { (line, newBaseQty) ->
                SaleLineStockReservationEntity(saleId, line.id!!, line.locationProductId, newBaseQty)
            })
            updatedLinesWithNewBaseQty.forEach { (line, newBaseQty) ->
                val oldBaseQty = oldReservations[line.id!!]?.quantityReserved ?: BigDecimal.ZERO
                val delta = newBaseQty.subtract(oldBaseQty)
                if (delta > BigDecimal.ZERO) add(line.locationProductId, delta)
                else if (delta < BigDecimal.ZERO) reduce(line.locationProductId, delta.negate())
            }
        }
        if (newLines.isNotEmpty()) reserve(saleId, newLines, newBaseQtyByProductId)
    }
}
