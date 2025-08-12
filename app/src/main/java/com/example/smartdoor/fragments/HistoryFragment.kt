package com.example.smartdoor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartdoor.adapter.HistoryAdapter
import com.example.smartdoor.databinding.FragmentHistoryBinding
import com.example.smartdoor.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private lateinit var viewModel: UserViewModel
    private lateinit var binding: FragmentHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private var selectedDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[UserViewModel::class.java]

        // Set up RecyclerView
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // ✅ Fetch history entries in REALTIME
        viewModel.fetchHistoryEntriesRealtime()

        // Observe changes in real-time and update UI
        viewModel.historyEntries.observe(viewLifecycleOwner) { historyEntries ->
            if (selectedDate.isEmpty()) {
                // Show all if no date filter
                binding.noEntriesText.visibility = if (historyEntries.isEmpty()) View.VISIBLE else View.GONE
                binding.historyRecyclerView.visibility = if (historyEntries.isEmpty()) View.GONE else View.VISIBLE
                adapter = HistoryAdapter(historyEntries)
                binding.historyRecyclerView.adapter = adapter
            } else {
                // Apply filter if a date is selected
                filterHistoryByDate(selectedDate)
            }
        }

        // Set up CalendarView to detect date change
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)
            val dateFormat = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
            selectedDate = dateFormat.format(selectedCalendar.time)
            filterHistoryByDate(selectedDate)
        }

        // Handle Show All button click to display all entries
        binding.showAllButton.setOnClickListener {
            selectedDate = ""
            showAllEntries()
        }

        binding.btnKnown.setOnClickListener {
            filterHistoryByClassify("known")
        }

        binding.btnUnknown.setOnClickListener {
            filterHistoryByClassify("unknown")
        }
    }

    // Filter history entries by selected date
    private fun filterHistoryByDate(date: String) {
        val filteredEntries = viewModel.historyEntries.value?.filter { it.entry_date == date } ?: emptyList()
        if (filteredEntries.isEmpty()) {
            binding.noEntriesText.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
        } else {
            binding.noEntriesText.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE
            adapter = HistoryAdapter(filteredEntries)
            binding.historyRecyclerView.adapter = adapter
        }


    }

    // Show all history entries
    private fun showAllEntries() {
        val historyEntries = viewModel.historyEntries.value ?: emptyList()
        binding.noEntriesText.visibility = if (historyEntries.isEmpty()) View.VISIBLE else View.GONE
        binding.historyRecyclerView.visibility = if (historyEntries.isEmpty()) View.GONE else View.VISIBLE
        adapter = HistoryAdapter(historyEntries)
        binding.historyRecyclerView.adapter = adapter
    }

    private fun filterHistoryByClassify(type: String) {
        viewModel.historyEntries.value?.forEach {
            android.util.Log.d("HistoryDebug", "Entry classify value: '${it.classify}'")
        }
        val filteredEntries = viewModel.historyEntries.value
            ?.filter { it.classify?.trim()?.equals(type, ignoreCase = true) == true }
            ?: emptyList()

        if (filteredEntries.isEmpty()) {
            binding.noEntriesText.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
        } else {
            binding.noEntriesText.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE
            adapter = HistoryAdapter(filteredEntries)
            binding.historyRecyclerView.adapter = adapter
        }
    }


}
