package me.ezra_home.retail_software_solution.locations.business.stock_transfer


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
    quantity = quantity,
    unitId = unitId,
    conversionRatio = conversionRatio(),
    baseUnitId = baseUnitId,
    createdById = requiredCreatedById(),
    createdOn = requiredCreatedOn()
)

fun StockTransferDispatchLineEntity.toDomainDto() = StockTransferDispatchLineDomainDto(
    id = id!!,
    referenceNumber = requiredReference(),
    stockTransferDispatchId = stockTransferDispatchId,
    locationProductId = locationProductId,
    quantityDispatched = quantityDispatched,
    unitId = unitId,
    unitCost = unitCost,
    conversionRatio = conversionRatio(),
    baseUnitId = baseUnitId,
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
    quantityReceived = quantityReceived,
    createdById = requiredCreatedById(),
    createdOn = requiredCreatedOn()
)
