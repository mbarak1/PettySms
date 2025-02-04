package com.example.pettysms

import android.content.Context
import android.util.Log
import com.example.pettysms.Transactor.Companion.toJson

class PettyCash(
    val id: Int?,
    var pettyCashNumber: String?,
    var date: String?,
    var amount: Double? = 0.00,
    var user : User? = User(1, "Mbarak", UserTypes(1, "admin")),
    var isDeleted: Boolean? = false,
    var trucks: MutableList<Truck>? = mutableListOf(),
    var owner: Owner?,
    var transactor: Transactor?,
    var account: Account?,
    var paymentMode: String?,
    var description: String?,
    var supportingDocument: SupportingDocument?,
    var signature: String?,
    var mpesaTransaction: MpesaTransaction?
    ) {


    companion object {
        fun insertPettyCashToDb(pettyCash: PettyCash, context: Context) {
            var dbHelper = DbHelper(context)

            if (pettyCash.supportingDocument != null) {
                dbHelper.insertPettyCash(pettyCash)
                dbHelper.insertSupportingDocument(pettyCash.supportingDocument!!)
            } else {
                dbHelper.insertPettyCash(pettyCash)
            }


        }

        fun convertMpesaTransactionToPettyCash(
            mpesaTransaction: MpesaTransaction,
            context: Context
        ): PettyCash {
            var dbHelper = DbHelper(context)

            val name =
                MpesaTransaction.getTitleTextByTransactionTypeWithoutFormatting(mpesaTransaction)

            val selectedTransactor = dbHelper.getSingleTransactorByName(name.trim())


            Log.d("PettyCashClass", "First Transactor: " + selectedTransactor?.toJson())




            return PettyCash(
                id = null,
                pettyCashNumber = null,
                date = mpesaTransaction.transaction_date,
                amount = mpesaTransaction.amount,
                isDeleted = false,
                trucks = null,
                transactor = selectedTransactor,
                account = null,
                description = mpesaTransaction.description.toString(),
                mpesaTransaction = mpesaTransaction,
                owner = null,
                paymentMode = "M-Pesa",
                signature = null,
                supportingDocument = null
            )
        }


        fun getTransactionCostPettyCashObject(
            mpesaTransaction: MpesaTransaction,
            context: Context
        ): PettyCash {
            var dbHelper = DbHelper(context)

            val name =
                MpesaTransaction.getTitleTextByTransactionTypeWithoutFormatting(mpesaTransaction)
            val selectedTransactor = dbHelper.getSingleTransactorByName(name.trim())



            Log.d("PettyCashClass", "Final Transactor: " + selectedTransactor?.toJson())




            return PettyCash(
                id = null,
                pettyCashNumber = null,
                date = mpesaTransaction.transaction_date,
                amount = mpesaTransaction.transactionCost,
                isDeleted = false,
                trucks = null,
                transactor = selectedTransactor,
                account = null,
                description = "Mpesa Transaction Cost on M-Pesa Transaction: " + mpesaTransaction.mpesa_code,
                mpesaTransaction = mpesaTransaction,
                owner = null,
                paymentMode = "M-Pesa",
                signature = null,
                supportingDocument = null
            )
        }

        fun updatePettyCashInDb(
            pettyCash: PettyCash,
            context: Context,
            updateSupportingDocumentFlag: Boolean
        ) {
            var dbHelper = DbHelper(context)

            if (pettyCash.supportingDocument != null) {
                dbHelper.updatePettyCash(pettyCash)
                if (updateSupportingDocumentFlag) {
                    dbHelper.updateSupportingDocument(pettyCash.supportingDocument!!)
                }else{
                    dbHelper.insertSupportingDocument(pettyCash.supportingDocument!!)
                }
            } else {
                println("updating Petty Cash")
                dbHelper.updatePettyCash(pettyCash)
            }
        }


    }

}