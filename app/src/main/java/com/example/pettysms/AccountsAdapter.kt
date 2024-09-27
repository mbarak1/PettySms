package com.example.pettysms


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AccountsAdapter(private var accounts: MutableList<Account>) : RecyclerView.Adapter<AccountsAdapter.AccountViewHolder>() {
    inner class AccountViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {

        val accountNameTextView: TextView = itemView.findViewById(R.id.accountNameTextView)
        val accountType: TextView = itemView.findViewById(R.id.accountType)
        val currencyTextView: TextView = itemView.findViewById(R.id.shillingsCurrencyTextView)
        val currencyImageView: ImageView = itemView.findViewById(R.id.dollarCurrencyLogo)
        val ownerTextView: TextView = itemView.findViewById(R.id.accountOwnerName)


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AccountsAdapter.AccountViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.account_card, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountsAdapter.AccountViewHolder, position: Int) {
        val currentAccount = accounts[position]

        holder.accountNameTextView.text = currentAccount.name
        holder.accountType.text = currentAccount.type
        holder.ownerTextView.text = currentAccount.owner?.name

        if (currentAccount.currency == "Kenyan Shilling"){
            holder.currencyTextView.visibility = View.VISIBLE
            holder.currencyImageView.visibility = View.GONE
        }else{
            holder.currencyImageView.visibility = View.VISIBLE
            holder.currencyTextView.visibility = View.GONE

        }

    }

    fun updateAccounts(newAccounts: List<Account>) {
        accounts = newAccounts.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = accounts.size

}