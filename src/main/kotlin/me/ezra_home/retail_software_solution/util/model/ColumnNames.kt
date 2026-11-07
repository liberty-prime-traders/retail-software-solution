package me.ezra_home.retail_software_solution.util.model

// Column names that SQL outside the owning entity depends on. The entity maps its column from the same constant, so a
// rename breaks the build instead of the query.
object ColumnNames {

    object Common {
        const val ID = "id"
        const val REFERENCE_NUMBER = "reference_number"
        const val CREATED_ON = "created_on"
    }

    // Shared by every payment table the Payments report reads, so each table maps the same constants.
    object Payment {
        const val PAYMENT_METHOD_ID = "payment_method_id"
        const val AMOUNT = "amount"
        const val PAYMENT_DATE = "payment_date"
    }

    object PaymentVoid {
        const val REASON = "reason"
    }

    object Sale {
        const val CONTACT_ID = "contact_id"
    }

    object SalePayment {
        const val SALE_ID = "sale_id"
    }

    object SalePaymentVoid {
        const val SALE_PAYMENT_ID = "sale_payment_id"
    }

    object Purchase {
        const val SUPPLIER_ID = "supplier_id"
    }

    object SupplierPayment {
        const val PURCHASE_ID = "purchase_id"
    }

    object SupplierPaymentVoid {
        const val SUPPLIER_PAYMENT_ID = "supplier_payment_id"
    }
}
