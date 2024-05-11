package com.example.pettysms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

class TruckAdapter(private val trucks: List<Truck>) : RecyclerView.Adapter<TruckAdapter.TruckViewHolder>() {

    inner class TruckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Define views in the truck card layout
        val truckNoTextView: TextView = itemView.findViewById(R.id.truckNo)
        val truckMakeImage: ImageView = itemView.findViewById(R.id.modelImage)
        val truckCard: MaterialCardView = itemView.findViewById(R.id.truckMainCard)
        val truckImage: ImageView = itemView.findViewById(R.id.truckImage)
        // Add other TextViews for truck details
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TruckViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.truck_card, parent, false)
        return TruckViewHolder(view)
    }

    override fun onBindViewHolder(holder: TruckViewHolder, position: Int) {
        val currentTruck = trucks[position]
        holder.truckNoTextView.text = currentTruck.truckNo?.let { addSpaceAfterThreeLetters(it) }
        var truckLogoImage = R.drawable.actros_logo
        if (currentTruck.make == "Axor"){
            truckLogoImage = R.drawable.axor_logo
            val layoutParams = holder.truckMakeImage.layoutParams
            layoutParams.height = 55
            holder.truckMakeImage.layoutParams = layoutParams
        }


        if (currentTruck.activeStatus == true) {
            // Assuming you have a color resource named "colorSurfaceContainerLow"
            val lowSurfaceContainerColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurfaceContainerHigh)
            holder.truckCard.setCardBackgroundColor(lowSurfaceContainerColor)
            //holder.truckNoTextView.setTextColor(R.color.grey_color)
        }else{
            holder.truckNoTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.grey_color))
            holder.truckMakeImage.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.grey_color))
            holder.truckImage.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.grey_color))
            val lowSurfaceContainerColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurfaceContainerLow)
            holder.truckCard.setCardBackgroundColor(lowSurfaceContainerColor)

        }
        holder.truckMakeImage.setImageResource(truckLogoImage)


        // Bind other truck details to TextViews
    }

    fun addSpaceAfterThreeLetters(input: String): String {
        // Check if the input string has at least 3 characters
        if (input.length >= 3) {
            // Get the first three characters
            val firstThreeLetters = input.substring(0, 3)

            // Get the remaining characters
            val remainingText = input.substring(3)

            // Concatenate the first three letters with a space and the remaining text
            return "$firstThreeLetters $remainingText"
        } else {
            // If the input string has less than 3 characters, return it as is
            return input
        }
    }


    override fun getItemCount(): Int {
        return trucks.size
    }
}