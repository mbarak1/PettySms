package com.example.pettysms

class MpesaTransaction(
    id: Int?,
    msg_date: String?,
    transaction_date: String?,
    mpesa_code: String?,
    recipient: Recepient?,
    account: Account?,
    company_owner: Owner? = Owner(1, "abdulcon"),
    amount: Double? = 0.00,
    transaction_type: String?,
    user: User? = User(1, "Mbarak", UserTypes(1, "admin")),
    payment_mode: PaymentMode? = PaymentMode(1, "mpesa"),
    description: String? = "General Expenses",
    var mpesa_balance: Double? = 0.00,
    var transaction_cost: Double? = 0.00,
    var mpesa_depositor: String? = "none",
    var paybill_acount: String? = "none",
    var sender: Sender? = Sender("Non-sender", "Non-sender"),
    var sms_text: String?
) : Transaction(
    id,
    msg_date,
    transaction_date,
    mpesa_code,
    recipient,
    account,
    company_owner,
    amount,
    transaction_type,
    user,
    payment_mode,
    description
) {

}