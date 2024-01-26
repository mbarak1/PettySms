package com.example.pettysms

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import xyz.schwaab.avvylib.AvatarView
import java.text.SimpleDateFormat
import java.util.Locale


class MpesaTransactionAdapter(private val context: Context, private val mpesaTransactions: MutableList<MpesaTransaction>, private val itemClickListener: OnItemClickListener
): RecyclerView.Adapter<MpesaTransactionAdapter.TransactionViewHolder>() {
    private var colorAvatar: Int = 0
    private val handler = Handler()
    private var actionMode: ActionMode? = null
    private var isInActionMode = false
    private var selectedItems = mutableListOf<Int>()
    // Filtered list that only contains transactions with isDeleted = false
    private val filteredList: List<MpesaTransaction> = mpesaTransactions.filter { !it.isDeleted }
    private val selectedTransactions = HashSet<Int>()
    private var removedTrasactions = HashSet<Int>()
    private var rotatedTransactions = HashSet<Int>()

    interface OnItemClickListener {
        fun onItemClick(transactionId: Int?)
        fun onItemLongClick(transactionId: Int?)
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.transaction_card, parent, false)
        return TransactionViewHolder(view)
    }




    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = filteredList[position]


        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(transaction.id)
            true
        }

        holder.itemView.setOnLongClickListener {
            itemClickListener.onItemLongClick(transaction.id)
            println("from adapter position" + transaction.id)
            true
        }

        println("actionmode: " + isInActionMode)
        println("contains: " + selectedTransactions.contains(transaction.id))
        println("removal: " + removedTrasactions.contains(transaction.id))
        println("hash set :" + selectedTransactions.toString())
        println("transaction_id: " + transaction.id)

        setAvatarView(holder, getTitleTextByTransactionType(transaction), getColorAvatar(context, transaction.transaction_type!!))
        val isPositiveAmount =
            transaction.transaction_type == "deposit" || transaction.transaction_type == "receival"
        val formattedAmount =
            formatAmountWithColor(transaction?.amount!!, isPositiveAmount, context)
        holder.amountTextView.text = formattedAmount
        holder.rounded_text.text = transaction.transaction_type?.let { capitalizeEachWord(it) }
        holder.titleTextView.text = getTitleTextByTransactionType(transaction)
        holder.dateTextView.text = transaction.transaction_date?.takeIf { it.isNotEmpty() }
            ?.let { formatDate(it) }
            ?: transaction.msg_date?.let { formatDate(it) } ?: "Unknown Date"

        if (selectedTransactions.contains(transaction.id)){
            val colorSurfaceVariant = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorPrimaryInverse)
            val colorSurface = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorPrimary)
            val drawableResourceId = R.drawable.ic_check_new // Replace with your actual small-sized drawable resource ID
            val smallDrawable: Drawable? = ContextCompat.getDrawable(context, drawableResourceId)
            println("rotated: " + holder.rotated)
            holder.avatarView.apply {
                isAnimating = true
                avatarBackgroundColor = colorSurfaceVariant
                highlightBorderColor = colorSurfaceVariant
                highlightBorderColorEnd = colorSurfaceVariant
                highlightedBorderThickness = 0
                borderThickness = 0
                isHighlighted = false
                distanceToBorder = 0
                setImageDrawable(smallDrawable)

                // Create a fade-in animation
                val fadeIn = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f)
                fadeIn.duration = 10 // Adjust duration as needed

                // Start the fade-in animation
                fadeIn.start()

                // Set isAnimating to false after starting the animation
                isAnimating = false

            }
            if (!rotatedTransactions.contains(transaction.id)) {
                holder.avatarView.animate().rotationY(180f).setDuration(500).withLayer().start()
                rotatedTransactions.add(transaction.id!!)



            }
            else{
                holder.avatarView.rotationY = 180f

            }
            val backgroundColor = MaterialColors.getColor(holder.itemView.context, com.google.android.material.R.attr.colorSurfaceContainer,"")
            holder.cardView.setCardBackgroundColor(backgroundColor)
        }else{
            holder.avatarView.apply {
                text = getTitleTextByTransactionType(transaction)
                highlightBorderColorEnd = getColorAvatar(context, transaction.transaction_type!!)
                isAnimating = false
                avatarBackgroundColor = android.R.color.transparent
                highlightBorderColor = MaterialColors.getColor(
                    holder.itemView.context,
                    com.google.android.material.R.attr.colorPrimary,
                    ""
                )
                highlightedBorderThickness = 10
                isHighlighted = true
                borderThickness = 10
                // Create a fade-in animation
                val fadeIn = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f)
                fadeIn.duration = 200 // Adjust duration as needed
                setImageDrawable(null)

                // Start the fade-in animation
                fadeIn.start()




                //stopAnimating(5000, holder.avatarView)
            }
            val isPositiveAmount =
                transaction.transaction_type == "deposit" || transaction.transaction_type == "receival"
            val formattedAmount =
                formatAmountWithColor(transaction?.amount!!, isPositiveAmount, context)
            holder.amountTextView.text = formattedAmount
            holder.rounded_text.text = transaction.transaction_type?.let { capitalizeEachWord(it) }
            holder.titleTextView.text = getTitleTextByTransactionType(transaction)
            holder.dateTextView.text = transaction.transaction_date?.takeIf { it.isNotEmpty() }
                ?.let { formatDate(it) }
                ?: transaction.msg_date?.let { formatDate(it) } ?: "Unknown Date"
            val backgroundColor = Color.TRANSPARENT
            holder.cardView.setCardBackgroundColor(backgroundColor)

            if (rotatedTransactions.contains(transaction.id)){
                holder.avatarView.animate().rotationY(-360f).setDuration(500).withLayer().start()
                rotatedTransactions.remove(transaction.id)
            }else{
                if (holder.avatarView.rotationY == 180f) {
                    holder.avatarView.rotationY = 0f
                }
            }
        }

    }
    fun formatAmountWithColor(amount: Double, isPositive: Boolean, context: Context): CharSequence {
        val formattedAmount = amount

        val textColor = if (isPositive) {
            ContextCompat.getColor(context, android.R.color.holo_green_light)
        } else {
            ContextCompat.getColor(context, R.color.red_color)
        }

        val spannableString = SpannableString("$formattedAmount/-")
        spannableString.setSpan(ForegroundColorSpan(textColor), 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannableString
    }
    fun setAvatarView(holder: TransactionViewHolder, text: String?, color: Int) {
        holder.avatarView.apply {
            this.text = text
            highlightBorderColorEnd = color
            isAnimating = true
        }
        stopAnimating(5000, holder.avatarView)
    }

    fun getTitleTextByTransactionType(transaction: MpesaTransaction): String {
        return when (transaction.transaction_type) {
            "topup", "send_money", "paybill", "till", "withdraw", "reverse" -> {
                transaction.recipient?.name?.let { capitalizeEachWord(it) } ?: ""
            }
            "deposit" -> {
                transaction.mpesa_depositor?.let { capitalizeEachWord(it) } ?: ""
            }
            "receival" -> {
                transaction.sender?.name?.let { capitalizeEachWord(it) } ?: ""
            }
            else -> ""
        }
    }
    fun getColorAvatar(context: Context, transactionType: String): Int {
        return when (transactionType) {
            "topup" -> R.color.aqua_color
            "send_money" -> R.color.orange_color
            "deposit" -> R.color.light_green_color
            "paybill" -> R.color.yellow_color
            "till" -> R.color.purple_color
            "receival" -> R.color.pink_color
            "withdraw" -> R.color.brown_color
            "reverse" -> R.color.grey_color
            else -> android.R.color.black // Default color for unknown type
        }.let {
            ContextCompat.getColor(context, it)
        }
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
        val color_frame: View = itemView.findViewById(R.id.card_color)
        val rounded_text: TextView = itemView.findViewById(R.id.roundedTextView)
        val avatarView: AvatarView = itemView.findViewById(R.id.avatar_view)
        val cardView: CardView = itemView.findViewById(R.id.card_transaction)
        var rotated = false
        // Get the RecyclerView reference from the itemView

    }



    fun stopAnimating(timeLimit: Long, avatarView: AvatarView){

        // Create a Runnable that will be executed after the time limit
        val timeoutRunnable = Runnable {
            // Perform actions when the time limit is reached
            // For example, show a message, close an activity, etc.
            avatarView.apply {
                isAnimating = false
            }

        }

        // Schedule the Runnable to be executed after the time limit
        handler.postDelayed(timeoutRunnable, timeLimit.toLong())
    }

    // Helper method to set a gradient background
    private fun setGradientBackground(view: View, startColor: Int, endColor: Int) {
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(startColor, endColor)
        )

        gradientDrawable.cornerRadius = 5f // Adjust the corner radius as needed
        // Set the angle (0 is left to right, 90 is top to bottom)
        gradientDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT
        gradientDrawable.orientation = GradientDrawable.Orientation.TOP_BOTTOM

        // Set the background to the GradientDrawable
        view.background = gradientDrawable
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    override fun getItemId(position: Int): Long {
        return filteredList[position].hashCode().toLong()
    }

    fun setSelectedTransactions(selected: Set<Int>) {
        selectedTransactions.clear()
        selectedTransactions.addAll(selected)
        //notifyDataSetChanged()
    }



    fun clearSelection() {
        selectedTransactions.clear()
        rotatedTransactions.clear()
        notifyDataSetChanged()
    }

    fun reinitializeAdapter() {
        //Update the dataset with new transactions
        clearSelection()
        notifyDataSetChanged()



        // Notify the adapter that the dataset has changed
    }
    fun setActionModeStatus(b: Boolean) {
        this.isInActionMode = b
        println("cool")
    }

    fun setRemovedtransactions(removedTransactions: HashSet<Int>) {
        removedTransactions.clear()
        removedTransactions.addAll(removedTransactions)

        notifyDataSetChanged()

    }


}