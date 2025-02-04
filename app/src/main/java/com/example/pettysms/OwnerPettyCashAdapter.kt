package com.example.pettysms

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.ByteArrayInputStream

class OwnerPettyCashAdapter (
    private val owners: List<Owner>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<OwnerPettyCashAdapter.OwnerViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    fun setSelectedPosition(position: Int) {
        notifyItemChanged(selectedPosition) // Reset previous selection
        selectedPosition = position
        notifyItemChanged(selectedPosition) // Notify new selection
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OwnerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_owner_petty_cash, parent, false)
        return OwnerViewHolder(view)
    }

    override fun onBindViewHolder(holder: OwnerViewHolder, position: Int) {
        holder.bind(owners[position], position == selectedPosition)
        holder.itemView.setOnClickListener { onClick(position) }
    }

    override fun getItemCount(): Int = owners.size

    inner class OwnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ownerName: TextView = itemView.findViewById(R.id.owner_name_text_view)
        private val ownerLogo: ImageView = itemView.findViewById(R.id.carousel_image_view)
        private val checkMark: ImageView = itemView.findViewById(R.id.check_mark)

        fun bind(owner: Owner, isSelected: Boolean) {
            ownerName.text = owner.name

            if (!owner.logoPath.isNullOrBlank()) {
                // If logoPath is not null or blank, decode and load the image
                val decodedString = Base64.decode(owner.logoPath, Base64.DEFAULT)
                val inputStream = ByteArrayInputStream(decodedString)
                val bitmap = BitmapFactory.decodeStream(inputStream)

                Glide.with(itemView.context)
                    .load(bitmap)
                    .placeholder(R.drawable.p_logo_cropped) // Optional: Show placeholder while loading
                    .into(ownerLogo)
            } else {
                // If logoPath is null or blank, load a placeholder or default image
                Glide.with(itemView.context)
                    .load(R.drawable.p_logo_cropped) // Replace with your default image resource
                    .into(ownerLogo)
            }

            checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }
}
