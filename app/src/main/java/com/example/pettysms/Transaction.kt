package com.example.pettysms

open class Transaction(val id: Int?, var msg_date: String?, var transaction_date: String?, var mpesa_code: String?, var recipient: Recepient?, var account: Account?, var company_owner: Owner? = null, var amount: Double?, var transaction_type: String?, var user: User?, var payment_mode: PaymentMode?, var description: String?) {
}