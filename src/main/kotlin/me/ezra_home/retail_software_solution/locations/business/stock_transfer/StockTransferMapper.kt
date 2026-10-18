package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedLineDto


fun StockTransferDispatchEntity.toDomainDto() = StockTransferDispatchDomainDto(
    id = id!!,
    referenceNumber = requiredReference(),
    stockTransferOrderRef = stockTransferOrderRef,
    status = status,
    dispatchedById = dispatchedById,
    dispatchedAt = dispatchedAt,
    notes = notes,
    createdById = requiredCreatedById(),
    createdOn = requiredCreatedOn()
)

fun StockTransferDraftLineEntity.toDomainDto() = StockTransferDraftLineDomainDto(
    id = id!!,
    referenceNumber = requiredReference(),
    stockTransferDispatchId = stockTransferDispatchId,
    locationProductId = locationProductId,
    userProvidedQuantity = userProvidedQuantity,
    unitId = unitId,
    storageUnitsPerUnit = storageUnitsPerUnit,
    createdById = requiredCreatedById(),
    createdOn = requiredCreatedOn()
)

fun StockTransferDispatchLineEntity.toDomainDto() = StockTransferDispatchLineDomainDto(
    id = id!!,
    referenceNumber = requiredReference(),
    stockTransferDispatchId = stockTransferDispatchId,
    locationProductId = locationProductId,
    wholeLineUserProvidedQuantity = wholeLineUserProvidedQuantity,
    storageQuantityDispatched = storageQuantityDispatched,
    unitId = unitId,
    unitCost = unitCost,
    costDispatched = costDispatched,
    storageUnitsPerUnit = storageUnitsPerUnit,
    createdById = requiredCreatedById(),
    createdOn = requiredCreatedOn()
)

fun StockTransferReceiptEntity.toDomainDto() = StockTransferReceiptDomainDto(
    id = id!!,
    referenceNumber = requiredReference(),
    stockTransferOrderRef = stockTransferOrderRef,
    receivedById = receivedById,
    receivedAt = receivedAt,
    status = status,
    notes = notes,
    createdById = requiredCreatedById(),
    createdOn = requiredCreatedOn()
)

fun StockTransferReceiptLineEntity.toDomainDto() = StockTransferReceiptLineDomainDto(
    id = id!!,
    referenceNumber = requiredReference(),
    stockTransferReceiptId = stockTransferReceiptId,
    stockTransferDispatchLineRef = stockTransferDispatchLineRef,
    locationProductId = locationProductId,
    storageQuantityReceived = storageQuantityReceived,
    createdById = requiredCreatedById(),
    createdOn = requiredCreatedOn()
)

fun StockTransferDispatchLineEntity.toDispatchedLineDto() = StockTransferDispatchedLineDto(
    dispatchLineReferenceNumber = requiredReference(),
    orgProductId = orgProductId,
    wholeLineUserProvidedQuantity = wholeLineUserProvidedQuantity,
    storageQuantityDispatched = storageQuantityDispatched,
    costDispatched = costDispatched,
    unitId = unitId,
    storageUnitsPerUnit = storageUnitsPerUnit
)
