package com.example.pettysms

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson

class OwnersAdapter(private val context: OwnersActivity, private var owners: List<Owner>) : RecyclerView.Adapter<OwnersAdapter.OwnerViewHolder>() {

    inner class OwnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val ownerNameTextView: TextView = itemView.findViewById(R.id.ownerNameTextView)
        val ownerLogoImageView: ImageView = itemView.findViewById(R.id.ownerLogo)
        val editButton: ImageButton = itemView.findViewById(R.id.editActionButton)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OwnerViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.owner_card, parent, false)
        return OwnerViewHolder(view)
    }

    override fun getItemCount(): Int {
        return owners.size
    }

    override fun onBindViewHolder(holder: OwnerViewHolder, position: Int) {
        holder.ownerNameTextView.text = owners[position].name
        val base64String = owners[position].logoPath

        // If there's an image string, decode and display it
        if (!base64String.isNullOrEmpty()) {
            val bitmap = base64ToBitmap(base64String)
            holder.ownerLogoImageView.setImageBitmap(bitmap)
        }

        holder.editButton.setOnClickListener {
            val gson = Gson()
            val ownerJson = gson.toJson(owners[position])
            (context).showAddOrEditOwnerDialog("Edit", ownerJson)
        }
    }

    fun updateOwners(newOwners: List<Owner>) {
        owners = newOwners
        notifyDataSetChanged()
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedString = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    }

}
