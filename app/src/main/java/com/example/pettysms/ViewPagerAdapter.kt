package com.example.pettysms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

class ViewPagerAdapter(@LayoutRes private val cardLayouts: List<Int>) :
    RecyclerView.Adapter<ViewPagerAdapter.CardViewHolder>() {

    private var imageView: ImageView? = null


    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val imageView: ImageView = itemView.findViewById(R.id.menuIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(cardLayouts[viewType], parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        // You can implement binding data to each card here if needed
        imageView = holder.imageView
    }

    override fun getItemCount(): Int = cardLayouts.size

    override fun getItemViewType(position: Int): Int = position

    fun getImageView(): ImageView? {
        return imageView
    }
}