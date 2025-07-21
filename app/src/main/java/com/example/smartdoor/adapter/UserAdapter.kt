package com.example.smartdoor.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smartdoor.data.model.User
import com.example.smartdoor.databinding.ItemUserBinding

class UserAdapter(
    private val userList: List<User>,
    private val onDeleteClick: (User) -> Unit // Add this callback to handle delete
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.binding.userName.text = user.userName

        // Load image from URL using Glide
        Glide.with(holder.binding.userImage.context)
            .load(user.userImgUrl)
            .into(holder.binding.userImage)

        // Handle delete button click
        holder.binding.deleteButton.setOnClickListener {
            onDeleteClick(user) // Call the delete action passed from the fragment
        }
    }

    override fun getItemCount(): Int = userList.size
}
