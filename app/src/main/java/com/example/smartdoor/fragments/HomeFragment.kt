package com.example.smartdoor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.smartdoor.adapter.UserAdapter
import com.example.smartdoor.databinding.FragmentHomeBinding
import com.example.smartdoor.viewmodel.UserViewModel
import com.example.smartdoor.data.model.User

class HomeFragment : Fragment() {

    private lateinit var viewModel: UserViewModel
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[UserViewModel::class.java]

        // Dynamically adjust the number of columns based on screen width
        val screenWidth = resources.displayMetrics.widthPixels
        val numberOfColumns = if (screenWidth > 800) 2 else 2 // 3 columns for larger screens, 2 for smaller ones
        binding.userRecyclerView.layoutManager = GridLayoutManager(requireContext(), numberOfColumns)

        // Fetch users from Firebase in REALTIME
        viewModel.fetchUsersRealtime()

        // Observe users list and update RecyclerView
        viewModel.users.observe(viewLifecycleOwner) { users ->
            adapter = UserAdapter(users) { user -> deleteUser(user) }
            binding.userRecyclerView.adapter = adapter
        }
    }

    private fun deleteUser(user: User) {
        // Delete from Firebase
        viewModel.deleteUserFromFirebase(user)

        // Delete from Supabase
        viewModel.deleteUserFromSupabase(user)

        // Remove user from local list
        viewModel.removeUser(user)
    }
}

