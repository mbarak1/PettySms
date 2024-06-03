package com.example.pettysms

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

class TruckModelImageAdapter(private val truckModels: List<String>, private val truckImages: List<Int>, private val viewPagerChangeListener: ViewPagerChangeListener) :
    RecyclerView.Adapter<TruckModelImageAdapter.TruckViewHolder>() {

    var selectedItemPosition: Int = RecyclerView.NO_POSITION // Initially, no item is selected
        private set


    inner class TruckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val truckImageView: ImageView = itemView.findViewById(R.id.truckImageView)
        val truckModelTextView: TextView = itemView.findViewById(R.id.truckModelTextView)
        val truckInnerCard: MaterialCardView = itemView.findViewById(R.id.inside_card_model_truck)
        val truckModelImage: ImageView = itemView.findViewById(R.id.truckModelImage)
        val truckCheckCircularContainer: FrameLayout = itemView.findViewById(R.id.circularContainer)
        val itemViewRoot: View = itemView // Reference to the root view of the item layout
        init {
            // Set OnClickListener to handle item clicks
            itemViewRoot.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Call the onItemClick method in the adapter to handle item clicks
                    onItemClick(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TruckViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.truck_model_card_layout, parent, false)
        return TruckViewHolder(view)
    }

    override fun onBindViewHolder(holder: TruckViewHolder, position: Int) {
        val truckModel = truckModels[position]
        val truckImageResId = truckImages[position]

        holder.truckModelTextView.text = truckModel
        holder.truckImageView.setImageResource(truckImageResId)

        val colorSurfaceVariant = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorPrimaryInverse)
        val colorPrimary = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurfaceContainerHighest)

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TR_BL, // Set the orientation of the gradient
            intArrayOf(
                colorPrimary, // Start color of the gradient
                colorSurfaceVariant    // End color of the gradient
            )
        )

        if(truckModel == "Mercedes-Benz Axor"){
            holder.truckModelImage.setImageResource(R.drawable.axor_logo)
        }

        gradientDrawable.cornerRadius = holder.truckInnerCard.resources.getDimension(R.dimen.corner_radius)

        holder.truckInnerCard.background = gradientDrawable

        println("selected position: " + selectedItemPosition)

        // Apply fading animation when changing visibility
        if (position == selectedItemPosition) {
            holder.truckCheckCircularContainer.apply {
                visibility = View.VISIBLE
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
            }
        } else {
            holder.truckCheckCircularContainer.apply {
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out))
                visibility = View.GONE
            }
        }

    }

    override fun getItemCount(): Int {
        return truckModels.size
    }

    // Method to handle item clicks
    private fun onItemClick(position: Int) {
        // Update the selected item position
        selectedItemPosition = position
        notifyDataSetChanged()

        // Notify the ViewPagerChangeListener about the page change
        viewPagerChangeListener.onPageChanged(position)
    }

    fun setSelectedItemPosition(position: Int) {
        selectedItemPosition = position
        notifyDataSetChanged()
    }

    fun getSelectedTruckModel(): String{
        println("in get selected : " + selectedItemPosition)
        return truckModels[selectedItemPosition]
    }

    fun getTruckModelAtPosition(position: Int): String {
        return truckModels[position]
    }
}