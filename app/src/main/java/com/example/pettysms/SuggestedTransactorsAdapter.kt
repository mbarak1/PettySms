package com.example.pettysms

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import xyz.schwaab.avvylib.AvatarView

class SuggestedTransactorsAdapter(val transactors: List<Transactor>, val dbHelper: DbHelper, val supportFragmentManager: androidx.fragment.app.FragmentManager, val listener: AddOrEditTransactorDialog.OnAddTransactorListener) : RecyclerView.Adapter<SuggestedTransactorsAdapter.SuggestedTransactorViewHolder>() {

    inner class SuggestedTransactorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val transactorName: TextView = itemView.findViewById(R.id.transactorName)
        val transactorAvatarView: AvatarView = itemView.findViewById(R.id.avatarView)
        val transactorCard: MaterialCardView = itemView.findViewById(R.id.cardSuggestedTransactor)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestedTransactorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transactor_suggestion, parent, false)
        return SuggestedTransactorViewHolder(view)
    }

    override fun getItemCount() = transactors.size

    override fun onBindViewHolder(holder: SuggestedTransactorViewHolder, position: Int) {
        val currentTransactor = transactors[position]

        var formattedTransactorName = formatName(currentTransactor.name)
        holder.transactorName.text = formattedTransactorName

        holder.transactorAvatarView.apply {
            this.text = formattedTransactorName
            if (currentTransactor.avatarColor != null) {
                highlightBorderColorEnd = currentTransactor.avatarColor?.let { getColorInt(it) }!!
            }
            this.isAnimating = false
        }

        holder.transactorCard.setOnClickListener{
            currentTransactor.incrementTransactorInteraction(currentTransactor, dbHelper, supportFragmentManager =  supportFragmentManager, listener )
        }

        if(currentTransactor.transactorProfilePicturePath != null){
            setImageViewFromBase64(holder.transactorAvatarView , currentTransactor.transactorProfilePicturePath!!)
        }else{
            holder.transactorAvatarView.setImageResource(0) // Clears the image
        }



    }

    fun setImageViewFromBase64(imageView: ImageView, base64String: String) {
        // Decode the Base64 string into a byte array
        val decodedString = Base64.decode(base64String, Base64.DEFAULT)

        // Convert the byte array into a Bitmap
        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        // Set the Bitmap to the ImageView
        imageView.setImageBitmap(decodedByte)
    }

    fun getColorInt(colorString: String): Int {
        return Color.parseColor(colorString)
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