package com.example.pettysms

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.pettysms.MpesaTransaction.Companion.capitalizeEachWord
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import xyz.schwaab.avvylib.AvatarView
import java.text.SimpleDateFormat
import java.util.*

class MpesaAutoCompleteTextInputAdapter(
    context: Context,
    private var suggestions: List<MpesaTransaction>
) : ArrayAdapter<MpesaTransaction>(context, R.layout.transaction_card, suggestions), Filterable {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val view: View

        if (convertView == null) {
            view = inflater.inflate(R.layout.transaction_card, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val suggestion = getItem(position) ?: return view

        // Set the title
        holder.titleTextView.text = highlightSearchQuery(
            suggestion.recipient?.name?.let { capitalizeEachWord(it) }.orEmpty(),
            suggestions.toString() // Adjust this to get the query from somewhere
        )

        holder.transactionCard.isClickable = false

        // Set amount with formatting
        holder.amountTextView.text = removeDecimal(suggestion.amount.toString()) + "/-"

        // Set the date
        holder.dateTextView.text = suggestion.transaction_date?.let { formatDate(it) }
            ?: suggestion.msg_date?.let { formatDate(it) } ?: "Unknown Date"

        // Customize the transaction UI
        customizeTransactionUI(holder, suggestion)

        return view
    }

    private fun customizeTransactionUI(holder: ViewHolder, suggestion: MpesaTransaction) {
        val context = holder.color_frame.context
        val colorPrimary = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, "") // Replace with your primary color
        val color = when (suggestion.transaction_type) {
            "topup" -> ContextCompat.getColor(context, R.color.aqua_color).also { holder.rounded_text.text = "Topup" }
            "send_money" -> ContextCompat.getColor(context, R.color.orange_color).also { holder.rounded_text.text = "Send Money" }
            "deposit" -> ContextCompat.getColor(context, R.color.light_green_color).also { holder.rounded_text.text = "Deposit" }
            "paybill" -> ContextCompat.getColor(context, R.color.yellow_color).also { holder.rounded_text.text = "Paybill" }
            "till" -> ContextCompat.getColor(context, R.color.purple_color).also { holder.rounded_text.text = "Till No." }
            "receival" -> ContextCompat.getColor(context, R.color.pink_color).also { holder.rounded_text.text = "Receival" }
            "withdraw" -> ContextCompat.getColor(context, R.color.brown_color).also { holder.rounded_text.text = "Withdraw" }
            "reverse" -> ContextCompat.getColor(context, R.color.grey_color).also { holder.rounded_text.text = "Reverse" }
            else -> Color.GRAY // Default color
        }

        holder.avatarView.apply {
            text = suggestion.recipient?.name?.let { capitalizeEachWord(it) }
            animationOrchestrator = CrazyOrchestrator.create()
            isAnimating = false
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            maxWidth = 60
            maxHeight = 60
            highlightBorderColorEnd = color
            isAnimating = false

        }

        holder.color_frame.setBackgroundColor(color)
        setGradientBackground(holder.color_frame, colorPrimary, color)

        // Set amount text
        when (suggestion.transaction_type) {
            "deposit", "receival" -> holder.amountTextView.text = "+" + holder.amountTextView.text
            else -> holder.amountTextView.text = "-" + holder.amountTextView.text
        }

        // Set amount text color
        holder.amountTextView.setTextColor(when (suggestion.transaction_type) {
            "deposit", "receival" -> ContextCompat.getColor(context, android.R.color.holo_green_light)
            else -> ContextCompat.getColor(context, R.color.red_color)
        })
        when (suggestion.transaction_type) {
            "deposit", "withdraw" ->  {
                holder.titleTextView.text = suggestion.mpesaDepositor?.let { capitalizeEachWord(it) }
                holder.avatarView.apply{
                    text = suggestion.mpesaDepositor?.let { capitalizeEachWord(it) }
                }
            }
            "receival" ->  {
                holder.titleTextView.text = suggestion.sender?.name?.let { capitalizeEachWord(it) }
                holder.avatarView.apply{
                    text = suggestion.sender?.name?.let { capitalizeEachWord(it) }
                }
            }
            "reverse" -> holder.avatarView.apply {
                holder.titleTextView.text = suggestion.recipient?.name?.let { capitalizeEachWord(it) }
                holder.avatarView.apply{
                    text = suggestion.recipient?.name?.let { capitalizeEachWord(it) }
                }
            }

        }



    }

    private fun highlightSearchQuery(text: String, query: String): SpannableString {
        val spannableString = SpannableString(text)
        val queryLowerCase = query.lowercase(Locale.getDefault())
        val textLowerCase = text.lowercase(Locale.getDefault())

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

    private fun setGradientBackground(view: View, colorPrimary: Int, color: Int) {
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(colorPrimary, color)
        )
        view.background = gradientDrawable
    }

    override fun getCount(): Int {
        return suggestions.size
    }

    override fun getItem(position: Int): MpesaTransaction? {
        return suggestions[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint.toString()
                val filteredSuggestions = if (query.isEmpty()) {
                    suggestions
                } else {
                    suggestions.filter {
                        it.recipient?.name?.contains(query, ignoreCase = true) == true
                        it.mpesaDepositor?.contains(query, ignoreCase = true) == true
                        it.sender?.name?.contains(query, ignoreCase = true) == true
                        it.transaction_type?.contains(query, ignoreCase = true) == true
                        it.transaction_date?.contains(query, ignoreCase = true) == true
                        it.mpesa_code?.contains(query, ignoreCase = true) == true
                        it.amount.toString().contains(query, ignoreCase = true) == true

                    }
                }

                return FilterResults().apply {
                    values = filteredSuggestions
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                suggestions = results?.values as List<MpesaTransaction>
                notifyDataSetChanged()
            }
        }
    }

    private fun removeDecimal(input: String): String {
        val regex = Regex("\\.\\d+")
        return regex.replace(input, "")
    }

    private fun formatDate(inputDate: String): String {
        val inputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)
        val outputFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.US)
        val date = inputFormat.parse(inputDate)
        return outputFormat.format(date)
    }

    private class ViewHolder(view: View) {
        val titleTextView: TextView = view.findViewById(R.id.transactionTitleTextView)
        val amountTextView: TextView = view.findViewById(R.id.transactionAmountTextView)
        val dateTextView: TextView = view.findViewById(R.id.transactionDateTextView)
        val color_frame: View = view.findViewById(R.id.card_color)
        val rounded_text: TextView = view.findViewById(R.id.roundedTextView)
        // Assuming AvatarView is a custom view; replace it with the appropriate type if needed
        val avatarView: AvatarView = view.findViewById(R.id.avatar_view)
        val transactionCard: MaterialCardView = view.findViewById(R.id.card_transaction)
    }
}
