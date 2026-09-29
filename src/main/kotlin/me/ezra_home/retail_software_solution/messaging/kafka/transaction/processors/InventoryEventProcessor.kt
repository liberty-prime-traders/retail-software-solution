package me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent

interface InventoryEventProcessor<EVENT: TransactionEvent> : TransactionEventProcessor<EVENT>
