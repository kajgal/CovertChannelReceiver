package com.example.covertChannelReceiver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(private val contacts : ArrayList<Contact>) :
        RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    private var listOfContacts = contacts

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactAdapter.ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.contact_recyclerview_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ContactAdapter.ViewHolder, position: Int) {
        holder.contactName.text = listOfContacts[position].Name
        holder.contactNumber.text = listOfContacts[position].Number
    }

    override fun getItemCount(): Int {
        return listOfContacts.size
    }

    fun addContact(index : Int, contact : Contact) {
        listOfContacts.add(index, contact)
        notifyItemInserted(index)
    }

    fun editContact(index : Int, contact : Contact) {
        listOfContacts[index] = contact
        notifyItemChanged(index)
    }

    fun removeContact(index : Int) {
        listOfContacts.removeAt(index)
        notifyItemRemoved(index)
    }

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        var contactName : TextView = itemView.findViewById(R.id.contactName)
        var contactNumber : TextView = itemView.findViewById(R.id.contactNumber)
    }
}