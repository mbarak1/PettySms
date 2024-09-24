package com.example.pettysms

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.brandongogetap.stickyheaders.exposed.StickyHeaderHandler
import com.google.android.material.card.MaterialCardView
import com.l4digital.fastscroll.FastScroller
import xyz.schwaab.avvylib.AvatarView


class TransactorsAdapter(val transactors: List<Any>, val dbHelper: DbHelper, val supportFragmentManager: androidx.fragment.app.FragmentManager, val listener: AddOrEditTransactorDialog.OnAddTransactorListener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), FastScroller.SectionIndexer {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (transactors[position] is Char) {
            VIEW_TYPE_HEADER
        } else {
            VIEW_TYPE_ITEM
        }
    }

    inner class TransactorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.transactorName)
        val avatarView: AvatarView = view.findViewById(R.id.avatarView)
        val transactorCard: MaterialCardView = itemView.findViewById(R.id.transactorCard)

        fun bind(transactor: Transactor) {
            var formattedTransactorName = formatName(transactor.name)
            nameTextView.text = formattedTransactorName

            avatarView.apply {
                this.text = formattedTransactorName
                if (transactor.avatarColor != null) {
                    highlightBorderColorEnd = transactor.avatarColor?.let { getColorInt(it) }!!
                }
                isAnimating = false
            }

            transactorCard.setOnClickListener {
                transactor.incrementTransactorInteraction(transactor, dbHelper = dbHelper, supportFragmentManager, listener)
            }

            if(!transactor.transactorProfilePicturePath.isNullOrEmpty()){
                setImageViewFromBase64(avatarView, transactor.transactorProfilePicturePath!!)
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

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerTextView: TextView = view.findViewById(R.id.sectionTitle)

        fun bind(header: Char) {
            headerTextView.text = header.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_section_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transactor, parent, false)
            TransactorViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.bind(transactors[position] as Char)
        } else if (holder is TransactorViewHolder) {
            holder.bind(transactors[position] as Transactor)
        }
    }

    override fun getItemCount() = transactors.size

    override fun getSectionText(position: Int): CharSequence {
        return if (transactors[position] is Transactor) {
            (transactors[position] as Transactor).name?.firstOrNull()?.toString() ?: ""
        } else {
            transactors[position].toString()
        }
    }


}