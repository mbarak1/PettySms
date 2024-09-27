package com.example.pettysms

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class MpesaTransaction(
    id: Int?,
    msgDate: String?,
    transactionDate: String?,
    mpesaCode: String?,
    recipient: Recepient?,
    account: Account?,
    companyOwner: Owner? = Owner(1, "Abdulcon Enterprises Limited", "abdulcon"),
    amount: Double? = 0.00,
    transactionType: String?,
    user: User? = User(1, "Mbarak", UserTypes(1, "admin")),
    paymentMode: PaymentMode? = PaymentMode(1, "mpesa"),
    description: String? = "General Expenses",
    var mpesaBalance: Double? = 0.00,
    var transactionCost: Double? = 0.00,
    var mpesaDepositor: String? = "none",
    var paybillAcount: String? = "none",
    var sender: Sender? = Sender("Non-sender", "Non-sender"),
    var smsText: String?,
    var isDeleted: Boolean = false,
    var transactorCheck : Boolean = false
) : Transaction(
    id,
    msgDate,
    transactionDate,
    mpesaCode,
    recipient,
    account,
    companyOwner,
    amount,
    transactionType,
    user,
    paymentMode,
    description
){

    companion object{
        data class MpesaTransactionResult(
            val transactionsList: MutableList<MpesaTransaction>,
            val rejectedSmsList: MutableList<MutableList<String>>
        )
        fun convertSmstoMpesaTransactions(msg_str: ArrayList<MutableList<String>>, progressBar: ProgressBar?): MpesaTransactionResult {
            var transactions_list = mutableListOf<MpesaTransaction>()
            val handler = Handler(Looper.getMainLooper())
            val TAG = "MpesaTransaction"
            var rejectedSmsList = mutableListOf<MutableList<String>>()


            if (msg_str != null) {

                val dateFrmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                var progress = 0

                var totalWork = msg_str[0].size

                println("lines zake ni: " + msg_str[2][0])


                for (i in msg_str[0].indices) {

                    progress = i

                    var recipient: Recepient? = null
                    var transaction_type = "none"
                    var paybill_account = "none"
                    var mpesa_depositor = "none"
                    var msg_date = ""
                    var mpesa_balance = 0.00
                    var transaction_cost = 0.00
                    var amount = 0.00
                    var transaction_date = ""
                    var message_is_not_balance = true
                    var sender: Sender? = null


                    var msg_arr = msg_str[0][i].split(" ").toTypedArray()
                    var msg_txt = msg_arr.joinToString(separator = " ")


                    if (msg_txt.contains("Your account balance was:")) {
                        message_is_not_balance = false
                        var rejectedList = mutableListOf<String>(msg_str[2][i], msg_txt)
                        rejectedSmsList.add(rejectedList)
                    }

                    if (itHasMpesaCode(msg_txt) && message_is_not_balance) {

                        val dateObjct = Date(msg_str[2][i].toLong())
                        msg_date = dateFrmt.format(dateObjct)


                        val foundDateTimes = findDatesInText(msg_txt)

                        for (dateTime in foundDateTimes) {
                            transaction_date = dateFrmt.format(dateTime)
                        }

                        var mpesa_code = msg_arr[0]

                        if (msg_arr[3] == "sent") {
                            transaction_type = "send_money"
                            if (msg_txt.contains("for account")) {
                                transaction_type = "paybill"
                            }
                        } else if (msg_txt.contains("You bought")) {
                            transaction_type = "topup"
                        } else if (msg_arr[7] == "Give") {
                            transaction_type = "deposit"
                        } else if (msg_arr[3] == "paid") {
                            transaction_type = "till"
                        }
                        else if(msg_txt.contains("reversed") || msg_txt.contains("Reversal")){
                            transaction_type = "reverse"
                        }
                        else if (msg_txt.contains("received") && msg_txt.contains("from")){
                            transaction_type = "receival"
                        }
                        else if (msg_arr[5].contains("Withdraw")){
                            transaction_type = "withdraw"
                        }

                        if (transaction_type == "send_money") {

                            var recepient_substr = extractSubstringBetweenWords(msg_txt, "to", "on")
                            var recepient_arr = recepient_substr?.split(" ")?.toTypedArray()
                            var name = ""
                            name = recepient_arr?.joinToString(
                                separator = " ",
                                limit = recepient_arr?.size!! - 2,
                                truncated = ""
                            ).toString()
                            var phone_no = recepient_arr?.get(recepient_arr.size - 2)
                            recipient = Recepient(name, phone_no)

                        } else if (transaction_type == "till") {
                            var recepient_substr = extractSubstringBetweenWords(msg_txt, "to", "on")
                            var name = recepient_substr?.trim()?.dropLast(1)

                            recipient = Recepient(name, "none")


                        } else if (transaction_type == "topup") {
                            recipient = Recepient("Mbarak Ahmed", "0700234463")
                        } else if (transaction_type == "paybill") {
                            var recepient_substr =
                                extractSubstringBetweenWords(msg_txt, "to", "for")
                            var name = recepient_substr?.trim()
                            recipient = Recepient(name, "none")
                            var paybill_account_string =
                                extractSubstringBetweenWords(msg_txt, "for account", "on")
                            if (paybill_account_string?.trim() != "") {
                                paybill_account = paybill_account_string?.trim().toString()
                            }
                        } else if (transaction_type == "deposit") {
                            var depositor_string =
                                extractSubstringBetweenWords(msg_txt, "to", "New")
                            mpesa_depositor = depositor_string?.trim().toString()
                        }
                        else if (transaction_type == "receival"){
                            var sender_substr = extractSubstringBetweenWords(msg_txt, "from", "on")
                            var sender_arr = sender_substr?.split(" ")?.toTypedArray()
                            var name = ""
                            name = sender_arr?.joinToString(
                                separator = " ",
                                limit = sender_arr?.size!! - 2,
                                truncated = ""
                            ).toString()
                            var phone_no = sender_arr?.get(sender_arr.size - 2)
                            sender = Sender(name, phone_no)
                        }
                        else if (transaction_type == "withdraw"){
                            var depositor_string =
                                extractSubstringBetweenWords(msg_txt, "from", "New")
                            mpesa_depositor = depositor_string?.trim().toString()
                        }
                        else if (transaction_type == "reverse"){
                            recipient = Recepient("Mbarak Ahmed", "0700234463")

                        }


                        var balance_string = findWordAfterPhrase(msg_txt, "balance is")
                        if (transaction_type == "reverse"){
                            balance_string = findWordAfterPhrase(msg_txt, "balance is now") ?: findWordAfterPhrase(msg_txt, "account balance is")
                        }
                        var transaction_cost_string =
                            findWordAfterPhrase(msg_txt, "Transaction cost,") ?: "none"
                        var amount_string = findFirstKshPhrase(msg_txt)

                        if (transaction_type == "reverse") {
                                Log.d(TAG,amount_string ?: "Error in Log")
                        }




                        if (balance_string != null) {
                            var provisional_mpesa_balance =
                                balance_string?.replace("Ksh", "")


                            if (provisional_mpesa_balance.isNullOrEmpty() == false){
                                //println("Hebu" + provisional_mpesa_balance.toString())
                                mpesa_balance = extractDoubleFromInput(provisional_mpesa_balance) ?: 0.00

                            }
                            else{


                                var rejectedList = mutableListOf<String>(msg_str[2][i], msg_txt)
                                rejectedSmsList.add(rejectedList)

                                continue

                            }

                        } else {

                            var rejectedList = mutableListOf<String>(msg_str[2][i], msg_txt)
                            rejectedSmsList.add(rejectedList)
                            continue

                        }

                        if (transaction_cost_string != "none") {
                            transaction_cost =
                                removeNonNumericText(transaction_cost_string)?.toDouble()!!
                            //transaction_cost = transaction_cost_string?.replace("Ksh", "")?.dropLast(1)?.toDouble()!!
                        }

                        if (amount_string != null) {
                            if (amount_string.isNullOrBlank()) {

                                var rejectedList = mutableListOf<String>(msg_str[2][i], msg_txt)
                                rejectedSmsList.add(rejectedList)

                                continue

                            } else {
                                amount =
                                    amount_string?.replace("Ksh", "")?.dropLast(1)?.toDouble()!!

                            }

                        } else {

                            var rejectedList = mutableListOf<String>(msg_str[2][i], msg_txt)
                            rejectedSmsList.add(rejectedList)
                            continue

                        }


                        var mpesa_transaction = MpesaTransaction(
                            id = null,
                            msgDate = msg_date,
                            transactionDate = transaction_date,
                            mpesaCode = mpesa_code,
                            recipient = recipient,
                            account = Account(
                                id = 1,
                                name = "General Expenses",
                                type = "Expense",
                                accountNumber = null,
                                currency = "Kenyan Shillings",
                                owner = null
                            ),
                            amount = amount,
                            transactionType = transaction_type,
                            mpesaBalance = mpesa_balance,
                            transactionCost = transaction_cost,
                            mpesaDepositor = mpesa_depositor,
                            smsText = msg_txt,
                            paybillAcount = paybill_account,
                            sender = sender
                        )

                        transactions_list.add(mpesa_transaction)

                        val currentProgress = (progress * 100 / totalWork)

                        // Update the ProgressBar on the main thread
                        if(progressBar != null){
                            handler.post {
                                progressBar.progress = currentProgress
                            }
                        }
                        //println(msg_txt)


//                    binding.smsSize.text =
                        //                      msg_arr[0] + " - " + msg_date + " - " + transaction_date + " - " + mpesa_transaction.recipient?.name + " - cool - " + recipient?.name + " - " + recipient?.phone_no + " - " + transaction_type + " - " + msg_txt + " - " + msg_arr[7] + " - " + mpesa_balance + " - " + balance_string + " - " + transaction_cost + " - " + transaction_cost_string + " - " + amount_string + " - " + amount + " - " + paybill_account + " - " + mpesa_depositor + " - " + mpesa_transaction.mpesa_code + " - " + msg_str.size + " - " + msg_str[0].size + " - " + msg_str[2].size + " - " + msg_str[1].size
                    }
                    else{
                        var rejectedList = mutableListOf<String>(msg_str[2][i], msg_txt)
                        rejectedSmsList.add(rejectedList)
                    }
                }
            }

            return MpesaTransactionResult(transactions_list, rejectedSmsList)
        }


        fun findDatesInText(text: String): List<Date> {
            val dateFound = mutableListOf<Date>()

            val datePattern = "\\d{1,2}/\\d{1,2}/\\d{2,4}"
            val timePattern = "\\d{1,2}:\\d{2}\\s(?:AM|PM)"

            val pattern = Pattern.compile("$datePattern\\s(?:at)\\s$timePattern")
            val matcher = pattern.matcher(text)

            val sdf = SimpleDateFormat("d/M/yy 'at' h:mm a", Locale.ENGLISH) // Adjust date format as needed

            while (matcher.find()) {
                val dateTimeString = matcher.group()
                try {
                    val date = sdf.parse(dateTimeString)
                    if (date != null) {
                        dateFound.add(date)
                    }
                } catch (e: Exception) {
                    // Handle parsing errors if needed
                }
            }

            return dateFound
        }

        fun extractSubstringBetweenWords(text: String, startWord: String, endWord: String): String? {
            val patternString = "\\b$startWord\\b(.*?)\\b$endWord\\b"
            val pattern = Pattern.compile(patternString)
            val matcher = pattern.matcher(text)

            if (matcher.find()) {
                return matcher.group(1)
            }

            return null
        }

        fun findWordAfterPhrase(input: String, phrase: String): String? {
            val regex = Regex("$phrase\\s+([\\w,\\.]+)")
            val match = regex.find(input)
            return match?.groups?.get(1)?.value?.replace(",", "") // Remove commas
        }

        fun findFirstKshPhrase(input: String): String? {
            val regex = Regex("\\bKsh[\\d,.]*")
            val match = regex.find(input)
            return match?.value?.replace(",", "")
        }

        fun removeNonNumericText(input: String): String {
            val regex = Regex("\\d+\\.?\\d*")
            val matches = regex.findAll(input)
            val numericStrings = matches.map { it.value }
            return numericStrings.joinToString("")
        }

        fun itHasMpesaCode(input: String): Boolean {
            val regex = Regex("^\\b[A-Z0-9]{10}\\b")
            val matchResult = regex.find(input)
            return matchResult != null
        }

        fun extractDoubleFromInput(input: String): Double? {
            // Use regular expression to extract the first decimal point and up to two decimal numbers
            val regex = Regex("""(\d+\.\d{1,2})""")
            val match = regex.find(input)

            return match?.value?.toDouble()
        }

        fun getTitleTextByTransactionType(transaction: MpesaTransaction): String {
            return when (transaction.transaction_type) {
                "topup", "send_money", "paybill", "till", "withdraw", "reverse" -> ({
                    transaction.recipient?.name?.let { capitalizeEachWord(it) } ?: ""
                }).toString()
                "deposit" -> ({
                    transaction.mpesaDepositor?.let { capitalizeEachWord(it) } ?: ""
                }).toString()
                "receival" -> ({
                    transaction.sender?.name?.let { capitalizeEachWord(it) } ?: ""
                }).toString()
                else -> ""
            }
        }

        fun capitalizeEachWord(input: String): String? {
            val words = input.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val result = StringBuilder()

            for (word in words) {
                if (!word.isEmpty()) {
                    // Replace special characters with space
                    val cleanedWord = word.replace(Regex("[^A-Za-z0-9]"), " ")

                    val capitalizedWord = cleanedWord.split(" ").joinToString(" ") {
                        if (it.isNotEmpty()) {
                            val firstLetter = it.substring(0, 1).uppercase(Locale.getDefault())
                            val rest = it.substring(1).lowercase(Locale.getDefault())
                            "$firstLetter$rest"
                        } else {
                            ""
                        }
                    }

                    result.append(capitalizedWord).append(" ")
                }
            }

            return result.toString().trim()
        }

    }

}