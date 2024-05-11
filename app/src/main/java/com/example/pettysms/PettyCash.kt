package com.example.pettysms

import android.graphics.Bitmap

class PettyCash(
    val id: Int?,
    var date: String?,
    var amount: Double? = 0.00,
    var user : User? = User(1, "Mbarak", UserTypes(1, "admin")),
    var hasReceipt: Boolean? = false,
    var isDeleted: Boolean? = false,
    var truck: Truck?,
    var owner: Owner?,
    var recepient: Recepient?,
    var account: Account?,
    var paymentMode: PaymentMode?,
    var receipt: Bitmap?,
    var recipientSignature: Bitmap?,
    var transaction: Transaction?

    ) {

}