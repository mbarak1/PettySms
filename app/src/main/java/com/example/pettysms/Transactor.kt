package com.example.pettysms

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlin.random.Random

class Transactor(
    var id: Int?,
    var name: String?,
    var phoneNumber: String?,
    var idCard: Int?,
    var address: String? = "N/A",
    var transactorType: String?,
    var transactorProfilePicturePath: String? = null,
    var interactions: Int? = 0,
    var isDeleted: Boolean? = false,
    var isImported: Boolean? = false,
    var avatarColor: String? = null,
) {
    fun incrementTransactorInteraction(currentTransactor: Transactor, dbHelper: DbHelper, supportFragmentManager: androidx.fragment.app.FragmentManager, listener: AddOrEditTransactorDialog.OnAddTransactorListener) {
        // Show a Toast message
        currentTransactor.interactions = currentTransactor.interactions!! + 1
        dbHelper.incrementTransactorInteractions(currentTransactor.id ?: return)
        println("item clicked")
        val gson = Gson()
        val transactorJson = gson.toJson(currentTransactor)
        showAddorEditTransactorDialog("Edit", transactorJson, supportFragmentManager, listener)

    }

    fun showAddorEditTransactorDialog(action: String, transactorJson: String = "", supportFragmentManager: androidx.fragment.app.FragmentManager, listener: AddOrEditTransactorDialog.OnAddTransactorListener) {
        val dialog = AddOrEditTransactorDialog()

        val args = Bundle()
        args.putString("Action", action)
        if (transactorJson.isNotEmpty()) {
            args.putString("TransactorJson", transactorJson)
        }
        dialog.arguments = args
        dialog.setOnAddTransactorListener(listener)
        dialog.show(supportFragmentManager, "AddOrEditTransactorDialog")
    }

    companion object{
        fun checkTransactorType(mpesa_transaction: MpesaTransaction) : String {
            if (mpesa_transaction.transaction_type == "paybill" || mpesa_transaction.transaction_type == "till" || mpesa_transaction.transaction_type == "deposit")
            {
                return "Corporate"
            }else{
                return "Individual"
            }
        }

        fun getTransactorFromTransaction(transaction: MpesaTransaction) : Transactor? {
            val transactorType = checkTransactorType(transaction)

            var senderJson = transaction.sender?.toJson()
            var mpesaDepositorJson = transaction.mpesaDepositor?.toJson()
            var recepientJson = transaction.recipient?.toJson()

            var avatarColor = getRandomAvatarColor()


            if (transaction.mpesaDepositor != "none") {
                val transactor = Transactor(id = null, name = transaction.mpesaDepositor?.trim(), phoneNumber = null, idCard = null, transactorType = transactorType, avatarColor = avatarColor, isImported = true)
                return transactor
            }
            else if (senderJson != "{}") {
                val transactor = Transactor(id = null, name = transaction.sender?.name?.trim(), phoneNumber = transaction.sender?.phone_no, idCard = null, transactorType = transactorType, avatarColor = avatarColor, isImported = true)
                return transactor
            } else if (recepientJson != "{}"){
                val transactor = Transactor(id = null, name = transaction.recipient?.name?.trim(), phoneNumber = transaction.recipient?.phone_no, idCard = null, transactorType = transactorType, avatarColor = avatarColor, isImported = true)
                return transactor
            }else{
                return null
            }

        }

        fun getTransactorsFromTransactions(transactions: List<MpesaTransaction>) : List<Transactor>{
            val transactors = mutableListOf<Transactor>()
            for (transaction in transactions){
                val transactor = getTransactorFromTransaction(transaction)
                if (transactor != null) {
                    transactors.add(transactor)
                }
            }
            return transactors
        }

        fun Any.toJson(): String {
            val gson: Gson = GsonBuilder().setPrettyPrinting().create()
            return gson.toJson(this)
        }

        fun getRandomAvatarColor(): String {
            val random = Random.Default
            val r = random.nextInt(256)
            val g = random.nextInt(256)
            val b = random.nextInt(256)
            return String.format("#%02X%02X%02X", r, g, b)
        }

        fun formatName(name: String?): String? {
            if (name.isNullOrBlank()) return name

            // Split the name into words and take up to 3 words
            val words = name.split(" ").take(3)

            // Capitalize each word
            val formattedWords = words.map { word ->
                word.toLowerCase().capitalize()
            }

            // Join the words back into a single string
            return formattedWords.joinToString(" ")
        }

    }


}
