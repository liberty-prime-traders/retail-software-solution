package me.ezra_home.retail_software_solution.util.enums

import java.util.UUID

enum class ServiceAccount(val displayName: String, val uniqueId: UUID) {
    CATALOG_SYNC("Catalog Sync", UUID.fromString("11111111-1111-1111-1111-111111111111")),
    RECORD_INITIALIZER("Record Initializer", UUID.fromString("22222222-2222-2222-2222-222222222222")),
    SECURITY_MONITOR("Security Monitor", UUID.fromString("33333333-3333-3333-3333-333333333333")),
    INVENTORY_PROCESSOR("Inventory Processor", UUID.fromString("44444444-4444-4444-4444-444444444444")),
    ACCOUNTING_PROCESSOR("Accounting Processor", UUID.fromString("55555555-5555-5555-5555-555555555555")),
}
