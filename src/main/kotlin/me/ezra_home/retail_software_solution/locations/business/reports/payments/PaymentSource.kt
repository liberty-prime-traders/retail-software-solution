package me.ezra_home.retail_software_solution.locations.business.reports.payments

import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentType
import me.ezra_home.retail_software_solution.util.model.ColumnNames
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect

enum class PaymentSource(
  val type: PaymentType,
  val balanceEffect: BalanceEffect,
  val paymentTable: String,
  val voidTable: String,
  val voidPaymentColumn: String,
  val parentTable: String,
  val parentLinkColumn: String,
  val parentContactColumn: String
) {
  SALE(
    type = PaymentType.SALE,
    balanceEffect = BalanceEffect.ADDS,
    paymentTable = TableNames.SALE_PAYMENT,
    voidTable = TableNames.SALE_PAYMENT_VOID,
    voidPaymentColumn = ColumnNames.SalePaymentVoid.SALE_PAYMENT_ID,
    parentTable = TableNames.SALE,
    parentLinkColumn = ColumnNames.SalePayment.SALE_ID,
    parentContactColumn = ColumnNames.Sale.CONTACT_ID
  ),
  PURCHASE(
    type = PaymentType.PURCHASE,
    balanceEffect = BalanceEffect.SUBTRACTS,
    paymentTable = TableNames.SUPPLIER_PAYMENT,
    voidTable = TableNames.SUPPLIER_PAYMENT_VOID,
    voidPaymentColumn = ColumnNames.SupplierPaymentVoid.SUPPLIER_PAYMENT_ID,
    parentTable = TableNames.PURCHASE,
    parentLinkColumn = ColumnNames.SupplierPayment.PURCHASE_ID,
    parentContactColumn = ColumnNames.Purchase.SUPPLIER_ID
  );

  companion object {

    fun forType(type: PaymentType): PaymentSource = entries.single { it.type == type }
  }
}
