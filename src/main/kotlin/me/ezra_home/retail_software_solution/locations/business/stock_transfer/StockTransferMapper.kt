package me.ezra_home.retail_software_solution.locations.business.stock_transfer


fun StockTransferDispatchEntity.toDomainDto() = StockTransferDispatchDomainDto(
    id = id!!,
    referenceNumber = requiredReference(),
    stockTransferOrderRef = stockTransferOrderRef,
    status = status,
    dispatchedById = dispatchedById,
    dispatchedAt = dispatchedAt,
    notes = notes,
    createdById = createdById!!,
    createdOn = createdOn!!
)

fun StockTransferDraftLineEntity.toDomainDto() = StockTransferDraftLineDomainDto(
    id = id!!,
    referenceNumber = requiredReference(),
    stockTransferDispatchId = stockTransferDispatchId,
    locationProductId = locationProductId,
    quantity = quantity,
    unitId = unitId,
    conversionFactor = conversionFactor,
    baseUnitId = baseUnitId,
    createdById = createdById!!,
    createdOn = createdOn!!
)

fun StockTransferDispatchLineEntity.toDomainDto() = StockTransferDispatchLineDomainDto(
    id = id!!,
    referenceNumber = requiredReference(),
    stockTransferDispatchId = stockTransferDispatchId,
    locationProductId = locationProductId,
    quantityDispatched = quantityDispatched,
    unitId = unitId,
    unitCost = unitCost,
    conversionFactor = conversionFactor,
    baseUnitId = baseUnitId,
    createdById = createdById!!,
    createdOn = createdOn!!
)

fun StockTransferReceiptEntity.toDomainDto() = StockTransferReceiptDomainDto(
    id = id!!,
    referenceNumber = requiredReference(),
    stockTransferOrderRef = stockTransferOrderRef,
    receivedById = receivedById,
    receivedAt = receivedAt,
    status = status,
    notes = notes,
    createdById = createdById!!,
    createdOn = createdOn!!
)

fun StockTransferReceiptLineEntity.toDomainDto() = StockTransferReceiptLineDomainDto(
    id = id!!,
    referenceNumber = requiredReference(),
    stockTransferReceiptId = stockTransferReceiptId,
    stockTransferDispatchLineRef = stockTransferDispatchLineRef,
    locationProductId = locationProductId,
    quantityReceived = quantityReceived,
    createdById = createdById!!,
    createdOn = createdOn!!
)
