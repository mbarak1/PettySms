package com.example.pettysms

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
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
    private val filteredList: MutableList<MpesaTransaction> = mpesaTransactions.filter { !it.isDeleted }.toMutableList()
    private val selectedTransactions = HashSet<Int>()
    private var removedTransactions = HashSet<Int>()
    private var rotatedTransactions = HashSet<Int>()
    private val adapterName = this::class.simpleName



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
        }

        holder.itemView.setOnLongClickListener {
            itemClickListener.onItemLongClick(transaction.id)
            true
        }

        Log.d(adapterName, "selectedItems: " + selectedTransactions)
        Log.d(adapterName, "RotatedItems:  " + rotatedTransactions)


        /*Log.d(adapterName, "actionmode: " + isInActionMode)
        Log.d(adapterName,"contains: " + selectedTransactions.contains(transaction.id))
        Log.d(adapterName, "hash set :" + selectedTransactions.toString())
        Log.d(adapterName, "transaction_id: " + transaction.id)*/

        val colorAvatar = getColorAvatar(context, transaction.transaction_type!!)
        val titleAvatar = getTitleTextByTransactionType(transaction)

        setAvatarView(holder, titleAvatar, colorAvatar)
        val isPositiveAmount =
            transaction.transaction_type == "deposit" || transaction.transaction_type == "receival" || transaction.transaction_type == "reverse"
        val formattedAmount =
            formatAmountWithColor(transaction?.amount!!, isPositiveAmount, context)
        holder.amountTextView.text = formattedAmount
        holder.rounded_text.text = transaction.transaction_type?.let { capitalizeEachWord(it) }
        holder.titleTextView.text = titleAvatar
        holder.dateTextView.text = transaction.transaction_date?.takeIf { it.isNotEmpty() }
            ?.let { formatDate(it) }
            ?: transaction.msg_date?.let { formatDate(it) } ?: "Unknown Date"

        if (selectedTransactions.contains(transaction.id)){
            val colorSurfaceVariant = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorPrimaryInverse)
            val colorSurface = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorPrimary)
            val drawableResourceId = R.drawable.ic_check_new // Replace with your actual small-sized drawable resource ID
            val smallDrawable: Drawable? = ContextCompat.getDrawable(context, drawableResourceId)

            if (!rotatedTransactions.contains(transaction.id)) {
                rotatedTransactions.add(transaction.id!!)
                val rotationAnimator = ObjectAnimator.ofFloat(holder.avatarView, "rotationY", 0f, 180f)
                rotationAnimator.duration = 500
                rotationAnimator.interpolator = AccelerateDecelerateInterpolator()

                rotationAnimator.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
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
                            setImageDrawable(null)
                        }
                        holder.avatarView.rotationY = 0f
                        holder.itemView.setOnClickListener{}

                        // Animation started
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        // Animation ended
                        holder.itemView.setOnClickListener {
                            itemClickListener.onItemClick(transaction.id)
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        // Animation cancelled
                    }

                    override fun onAnimationRepeat(animation: Animator) {
                        // Animation repeated
                    }
                })

                rotationAnimator.start()

// Start another animation to change AvatarView properties midway
                val handler = Handler()
                handler.postDelayed({
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

                        // Set isAnimating to false after starting the animation
                        isAnimating = false

                    }
                }, 250) // Adjust the delay to match the midway point of the rotation animation

            }
            else{
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

                    // Set isAnimating to false after starting the animation
                    isAnimating = false

                }
                holder.avatarView.rotationY = 180f

            }
            val backgroundColor = MaterialColors.getColor(holder.itemView.context, com.google.android.material.R.attr.colorSurfaceContainer,"")
            holder.cardView.setCardBackgroundColor(backgroundColor)
        }else{
            //println("imageview: " + holder.avatarView.avatarBackgroundColor)
            val isPositiveAmount =
                transaction.transaction_type == "deposit" || transaction.transaction_type == "receival" || transaction.transaction_type == "reverse"
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

            //Log.d(adapterName, "hello inside")

            if (rotatedTransactions.contains(transaction.id)){
                rotatedTransactions.remove(transaction.id)
                val colorSurfaceVariant = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorPrimaryInverse)
                val colorSurface = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorPrimary)
                val drawableResourceId = R.drawable.ic_check_new // Replace with your actual small-sized drawable resource ID
                val smallDrawable: Drawable? = ContextCompat.getDrawable(context, drawableResourceId)
                val rotationAnimator = ObjectAnimator.ofFloat(holder.avatarView, "rotationY", 180f, 0f)
                rotationAnimator.duration = 500
                rotationAnimator.interpolator = AccelerateDecelerateInterpolator()

                rotationAnimator.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
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

                            // Set isAnimating to false after starting the animation
                            isAnimating = false

                        }
                        holder.avatarView.rotationY = 180f
                        holder.itemView.setOnClickListener{}
                        // Animation started
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        // Animation ended
                        holder.itemView.setOnClickListener {
                            itemClickListener.onItemClick(transaction.id)
                        }

                    }

                    override fun onAnimationCancel(animation: Animator) {
                        // Animation cancelled
                        holder.itemView.isClickable = false
                    }

                    override fun onAnimationRepeat(animation: Animator) {
                        // Animation repeated
                    }
                })

                rotationAnimator.start()

// Start another animation to change AvatarView properties midway
                val handler = Handler()
                handler.postDelayed({
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
                        setImageDrawable(null)
                    }

                }, 250) // Adjust the delay to match the midway point of the rotation animation

            }else{
                //println("jj")
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
                    setImageDrawable(null)

                }
                if (holder.avatarView.rotationY == 180f) {
                    holder.avatarView.rotationY = 0f
                }

            }
        }

        Log.d(adapterName, "Transactions removed on bind: " + removedTransactions.toString())

        /*if (removedTransactions.contains(transaction.id)){
            val index = filteredList.indexOfFirst { it.id == transaction.id }
            Log.d(adapterName, "Transaction removed in loop: " + removedTransactions.toString())
            holder.itemView.animate()?.alpha(0f)?.setDuration(250)?.withEndAction {
                filteredList.removeAt(index)
                removedTransactions.remove(transaction.id)
                notifyItemRemoved(index)
            }
        }*/
        

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
            isAnimating = false
        }
        //stopAnimating(5000, holder.avatarView)
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

    fun setSelectedTransactions(selected: Set<Int>, selectAllFlag: Boolean) {
        selectedTransactions.clear()
        selectedTransactions.addAll(selected)
        if (selectAllFlag){
            if (selected.size == mpesaTransactions.size){
                rotatedTransactions.clear()
                rotatedTransactions.addAll(selected)
            }else if (selected.size == 0){
                rotatedTransactions.clear()
            }
        }
        notifyDataSetChanged()
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
        Log.d(adapterName,
            "cool")
    }

    fun removetransactions(transactionIds: HashSet<Int>, selectAllFlag: Boolean){
        if (selectAllFlag){
            filteredList.clear()
            selectedItems.clear()
            rotatedTransactions.clear()
            notifyDataSetChanged()
        }else{
            removedTransactions.clear()
            removedTransactions.addAll(transactionIds)
            selectedItems.clear()
            rotatedTransactions.clear()
            //notifyDataSetChanged()
            //removedTransactions.clear()
            Log.d(adapterName, "Transactions removed: " + removedTransactions.toString())
        }

        notifyDataSetChanged()
    }

    fun setRemovedTransactions(
        removedTransactionList: HashSet<Int>,
        checked: Boolean
    ) {
        if (checked) {
            filteredList.clear()
            selectedTransactions.clear()
            rotatedTransactions.clear()
            removeItems(removedTransactionList)
            removedTransactions.addAll(removedTransactionList)
            //notifyDataSetChanged()
        }else{
            rotatedTransactions.clear()
            removedTransactions.clear()
            selectedTransactions.clear()
            removeItems(removedTransactionList)
            Log.d(adapterName, "Removed Transactions size: "  + removedTransactionList.size.toString())
            removedTransactions.addAll(removedTransactionList)
            //removedTransactions.addAll(removedTransactionList)

            Log.d(adapterName, "Removed Transactions size in function: "  + filteredList.size.toString())
        }

    }

    fun removeItems(transactionIds: HashSet<Int>) {
        // Create a list to store the indices of items to be removed
        val indicesToRemove = mutableListOf<Int>()

        // Iterate over the transaction IDs in the HashSet
        transactionIds.forEach { transactionId ->
            // Find the index of the item with the corresponding ID
            val index = filteredList.indexOfFirst { it.id == transactionId }
            // If the item is found, add its index to the list
            if (index != -1) {
                indicesToRemove.add(index)
            }
        }

        // Sort the list of indices in descending order to maintain consistency
        indicesToRemove.sortDescending()

        // Remove the items from the dataSet and notify the adapter with fade-out animation
        indicesToRemove.forEach { index ->
            filteredList.removeAt(index)
            notifyItemRemoved(index)
        }
    }



}