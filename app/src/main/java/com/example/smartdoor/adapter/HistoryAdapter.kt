package com.example.smartdoor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smartdoor.databinding.ItemHistoryBinding
import com.example.smartdoor.data.model.HistoryEntry
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val historyList: List<HistoryEntry>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // Toggle visibility of the content (expand/collapse) on title click
            binding.entryTitle.setOnClickListener {
                val isVisible = binding.contentLayout.visibility == View.VISIBLE
                binding.contentLayout.visibility = if (isVisible) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val entry = historyList[position]

        // Format date to "July 29, 2025"
        val inputDateFormat = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
        val outputDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val formattedDate = try {
            outputDateFormat.format(inputDateFormat.parse(entry.entry_date)!!)
        } catch (e: Exception) {
            entry.entry_date // fallback to original
        }

        // Set the title text as "Date - Time"
        holder.binding.entryTitle.text = "$formattedDate - ${entry.entry_time}"

        // Set expanded content details
        //holder.binding.entryDate.text = "Date: $formattedDate"
        //holder.binding.entryTime.text = "Time: ${entry.entry_time}"
        holder.binding.classification.text = "User is ${entry.classify}"

        // Load the image using Glide
        Glide.with(holder.binding.imageView.context)
            .load(entry.entry_img_url)
            .into(holder.binding.imageView)

        // Initially hide the expanded content
        holder.binding.contentLayout.visibility = View.GONE
    }

    override fun getItemCount(): Int = historyList.size
}
