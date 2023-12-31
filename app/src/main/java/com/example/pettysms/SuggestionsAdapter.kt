package com.example.pettysms

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class SuggestionsAdapter(
    private val suggestions: List<MpesaTransaction>,
    private val query: String,
    private val onSuggestionClick: (MpesaTransaction) -> Unit
) : RecyclerView.Adapter<SuggestionsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_transaction)
        val titleTextView: TextView = itemView.findViewById(R.id.transactionTitleTextView)
        val amountTextView: TextView = itemView.findViewById(R.id.transactionAmountTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.transactionDateTextView)
        val color_frame: FrameLayout = itemView.findViewById(R.id.card_color)
        val rounded_text: TextView = itemView.findViewById(R.id.roundedTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.transaction_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.titleTextView.text = suggestion.recipient?.name?.let { capitalizeEachWord(it) }
        holder.amountTextView.text = removeDecimal(suggestion.amount.toString()) + "/-"
        holder.dateTextView.text = suggestion.transaction_date?.let { formatDate(it) }
        // Highlight the search query in the titleTextView
        holder.titleTextView.text = highlightSearchQuery(
            suggestion.recipient?.name?.let { capitalizeEachWord(it) }.orEmpty(),
            query
        )
        if (suggestion.transaction_type == "topup"){
            val context = holder.color_frame.context
            val color = ContextCompat.getColor(context, R.color.aqua_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Topup"

            holder.amountTextView.text = "-" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, R.color.red_color)
            holder.amountTextView.setTextColor(color3)

        }
        else if (suggestion.transaction_type == "send_money"){
            val context = holder.color_frame.context
            val color = ContextCompat.getColor(context, R.color.orange_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Send Money"

            holder.amountTextView.text = "-" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, R.color.red_color)
            holder.amountTextView.setTextColor(color3)


        }
        else if (suggestion.transaction_type == "deposit"){
            val context = holder.color_frame.context
            holder.titleTextView.text = suggestion.mpesa_depositor?.let { capitalizeEachWord(it) }
            // Highlight the search query in the titleTextView
            holder.titleTextView.text = highlightSearchQuery(
                suggestion.mpesa_depositor?.let { capitalizeEachWord(it) }.orEmpty(),
                query
            )
            val color = ContextCompat.getColor(context, R.color.light_green_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Deposit"

            holder.amountTextView.text = "+" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, android.R.color.holo_green_light)
            holder.amountTextView.setTextColor(color3)


        }
        else if (suggestion.transaction_type == "paybill"){
            val context = holder.color_frame.context
            val color = ContextCompat.getColor(context, R.color.yellow_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Paybill"

            holder.amountTextView.text = "-" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, R.color.red_color)
            holder.amountTextView.setTextColor(color3)

        }
        else if (suggestion.transaction_type == "till"){
            val context = holder.color_frame.context
            val color = ContextCompat.getColor(context, R.color.purple_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Till No."

            holder.amountTextView.text = "-" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, R.color.red_color)
            holder.amountTextView.setTextColor(color3)


        }
        else if (suggestion.transaction_type == "receival"){
            val context = holder.color_frame.context
            holder.titleTextView.text = suggestion.sender?.name?.let { capitalizeEachWord(it) }
            holder.titleTextView.text = highlightSearchQuery(
                suggestion.sender?.name?.let { capitalizeEachWord(it) }.orEmpty(),
                query
            )
            val color = ContextCompat.getColor(context, R.color.pink_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Receival"

            holder.amountTextView.text = "+" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, android.R.color.holo_green_light)
            holder.amountTextView.setTextColor(color3)


        }

        else if (suggestion.transaction_type == "withdraw"){
            val context = holder.color_frame.context
            holder.titleTextView.text = suggestion.mpesa_depositor?.let { capitalizeEachWord(it) }
            holder.titleTextView.text = highlightSearchQuery(
                suggestion.mpesa_depositor?.let { capitalizeEachWord(it) }.orEmpty(),
                query
            )
            val color = ContextCompat.getColor(context, R.color.brown_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Withdraw"

            holder.amountTextView.text = "-" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, R.color.red_color)
            holder.amountTextView.setTextColor(color3)


        }
        else if (suggestion.transaction_type == "reverse"){
            val context = holder.color_frame.context
            holder.titleTextView.text = suggestion.recipient?.name?.let { capitalizeEachWord(it) }
            holder.titleTextView.text = highlightSearchQuery(
                suggestion.recipient?.name?.let { capitalizeEachWord(it) }.orEmpty(),
                query
            )
            val color = ContextCompat.getColor(context, R.color.grey_color)
            holder.color_frame.setBackgroundColor(color)
            holder.rounded_text.text = "Reverse"

            holder.amountTextView.text = "+" + holder.amountTextView.text

            val context3 = holder.color_frame.context
            val color3 = ContextCompat.getColor(context3, android.R.color.holo_green_light)
            holder.amountTextView.setTextColor(color3)


        }

        // Highlight the search query in the amountTextView
        holder.amountTextView.text = highlightSearchQuery(
            removeDecimal(suggestion.amount.toString()) + "/-",
            query
        )

        // Highlight the search query in the dateTextView
        holder.dateTextView.text = highlightSearchQuery(
            suggestion.transaction_date?.let { formatDate(it) }.orEmpty(),
            query
        )

        // Highlight the search query in the Transaction Type
        holder.rounded_text.text = highlightSearchQuery(
            suggestion.transaction_type?.let { capitalizeEachWord(it) }.orEmpty(),
            query
        )


        holder.cardView.setOnClickListener {
            onSuggestionClick(suggestion)
        }
    }

    override fun getItemCount(): Int {
        return suggestions.size
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

    private fun highlightSearchQuery(text: String, query: String): SpannableString {
        val spannableString = SpannableString(text)
        val queryLowerCase = query.toLowerCase(Locale.getDefault())
        val textLowerCase = text.toLowerCase(Locale.getDefault())

        val startPos = textLowerCase.indexOf(queryLowerCase)
        if (startPos != -1) {
            val endPos = startPos + query.length
            spannableString.setSpan(
                BackgroundColorSpan(Color.YELLOW),
                startPos,
                endPos,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannableString
    }
}

