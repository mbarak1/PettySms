package com.example.pettysms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class MpesaTransactionAdapter(private val mpesaTransactions: List<MpesaTransaction>): RecyclerView.Adapter<MpesaTransactionAdapter.TransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.transaction_card, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = mpesaTransactions[position]
        holder.titleTextView.text = transaction.recipient?.name?.let { capitalizeEachWord(it) }
        holder.amountTextView.text = removeDecimal(transaction.amount.toString()) + "/-"
        holder.dateTextView.text = transaction.transaction_date?.let { formatDate(it) }
        if (transaction.transaction_type == "topup"){
            val context = holder.color_frame.context
            val color = ContextCompat.getColor(context, R.color.aqua_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Topup"

            holder.amountTextView.text = "-" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, R.color.red_color)
            holder.amountTextView.setTextColor(color3)

        }
        else if (transaction.transaction_type == "send_money"){
            val context = holder.color_frame.context
            val color = ContextCompat.getColor(context, R.color.orange_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Send Money"

            holder.amountTextView.text = "-" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, R.color.red_color)
            holder.amountTextView.setTextColor(color3)


        }
        else if (transaction.transaction_type == "deposit"){
            val context = holder.color_frame.context
            holder.titleTextView.text = transaction.mpesa_depositor?.let { capitalizeEachWord(it) }
            val color = ContextCompat.getColor(context, R.color.light_green_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Deposit"

            holder.amountTextView.text = "+" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, android.R.color.holo_green_light)
            holder.amountTextView.setTextColor(color3)


        }
        else if (transaction.transaction_type == "paybill"){
            val context = holder.color_frame.context
            val color = ContextCompat.getColor(context, R.color.yellow_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Paybill"

            holder.amountTextView.text = "-" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, R.color.red_color)
            holder.amountTextView.setTextColor(color3)

        }
        else if (transaction.transaction_type == "till"){
            val context = holder.color_frame.context
            val color = ContextCompat.getColor(context, R.color.purple_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Till No."

            holder.amountTextView.text = "-" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, R.color.red_color)
            holder.amountTextView.setTextColor(color3)


        }
        else if (transaction.transaction_type == "receival"){
            val context = holder.color_frame.context
            holder.titleTextView.text = transaction.sender?.name?.let { capitalizeEachWord(it) }
            val color = ContextCompat.getColor(context, R.color.pink_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Receival"

            holder.amountTextView.text = "+" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, android.R.color.holo_green_light)
            holder.amountTextView.setTextColor(color3)


        }

        else if (transaction.transaction_type == "withdraw"){
            val context = holder.color_frame.context
            holder.titleTextView.text = transaction.mpesa_depositor?.let { capitalizeEachWord(it) }
            val color = ContextCompat.getColor(context, R.color.brown_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Withdraw"

            holder.amountTextView.text = "-" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, R.color.red_color)
            holder.amountTextView.setTextColor(color3)


        }
        else if (transaction.transaction_type == "reverse"){
            val context = holder.color_frame.context
            holder.titleTextView.text = transaction.recipient?.name?.let { capitalizeEachWord(it) }
            val color = ContextCompat.getColor(context, R.color.grey_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Reverse"

            holder.amountTextView.text = "+" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, android.R.color.holo_green_light)
            holder.amountTextView.setTextColor(color3)


        }
    }

    override fun getItemCount(): Int {
        return mpesaTransactions.size
    }

    fun capitalizeEachWord(input: String): String? {
        val words = input.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val result = StringBuilder()
        for (word in words) {
            if (!word.isEmpty()) {
                val firstLetter = word.substring(0, 1).uppercase(Locale.getDefault())
                val rest = word.substring(1).lowercase(Locale.getDefault())
                result.append(firstLetter).append(rest).append(" ")
            }
        }
        return result.toString().trim { it <= ' ' }
    }

    fun removeDecimal(input: String): String {
        val regex = Regex("\\.\\d+")
        return regex.replace(input, "")
    }

    fun formatDate(inputDate: String): String {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)
        val outputFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.US)

        val date = inputFormat.parse(inputDate)
        return outputFormat.format(date)
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.transactionTitleTextView)
        val amountTextView: TextView = itemView.findViewById(R.id.transactionAmountTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.transactionDateTextView)
        val color_frame: FrameLayout = itemView.findViewById(R.id.card_color)
        val rounded_text: TextView = itemView.findViewById(R.id.roundedTextView)


    }
}