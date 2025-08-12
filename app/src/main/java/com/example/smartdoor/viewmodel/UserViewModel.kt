package com.example.smartdoor.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smartdoor.data.model.HistoryEntry
import com.example.smartdoor.data.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Locale


class UserViewModel : ViewModel() {

    private val firebaseUrl = "https://door--smart-security-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val database = FirebaseDatabase.getInstance(firebaseUrl)
    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users
    private val _historyEntries = MutableLiveData<List<HistoryEntry>>()
    val historyEntries: LiveData<List<HistoryEntry>> = _historyEntries


    // Fetch users from Firebase
    fun fetchUsers() {
        database.goOnline()

        // Fetch all users under 'face_db'
        database.getReference("face_db").get()
            .addOnSuccessListener { snapshot ->
                val userList = mutableListOf<User>()
                for (child in snapshot.children) {
                    val name = child.child("user_name").value?.toString()
                    val url = child.child("user_img_url").value?.toString()

                    // Add the user if both fields exist
                    if (!name.isNullOrEmpty() && !url.isNullOrEmpty()) {
                        userList.add(User(userName = name, userImgUrl = url))
                    }
                }
                _users.postValue(userList)
            }
            .addOnFailureListener { error ->
                Log.e("FirebaseData", "Fetch failed", error)
            }
    }

    // Fetch history entries from Firebase
    fun fetchHistoryEntries() {
        val historyRef = database.getReference("history") // Reference to the 'history' node
        historyRef.get().addOnSuccessListener { snapshot ->
            val historyList = mutableListOf<HistoryEntry>()
            for (child in snapshot.children) {
                val entry_date = child.child("entry_date").value?.toString()
                val entry_img_url = child.child("entry_img_url").value?.toString()
                val entry_time = child.child("entry_time").value?.toString()
                val classify = child.child("classify").value?.toString()

                if (!entry_date.isNullOrEmpty() && !entry_img_url.isNullOrEmpty() && !entry_time.isNullOrEmpty()) {
                    historyList.add(HistoryEntry(entry_date, entry_img_url, entry_time, classify))
                }
            }

            // Sort from latest to oldest
            val inputDateFormat = SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault())
            val sortedList = historyList.sortedByDescending { entry ->
                try {
                    inputDateFormat.parse("${entry.entry_date} ${entry.entry_time}")
                } catch (e: Exception) {
                    null
                }
            }

            _historyEntries.postValue(sortedList)
        }.addOnFailureListener { exception ->
            Log.e("Firebase", "Error fetching history entries: ${exception.message}")
        }
    }


    // Delete user from Firebase
    fun deleteUserFromFirebase(user: User) {
        // Target the Firebase node where user metadata is stored
        database.getReference("face_db").orderByChild("user_name").equalTo(user.userName).get()
            .addOnSuccessListener { snapshot ->
                snapshot.children.forEach {
                    it.ref.removeValue() // Deletes the metadata from Firebase
                }
                Log.d("Firebase", "User metadata deleted from Firebase")
            }
            .addOnFailureListener { error ->
                Log.e("Firebase", "Error deleting user metadata", error)
            }
    }

    // Delete user image from Supabase
    fun deleteUserFromSupabase(user: User) {

        val supabaseUrl = "https://tlgkpnmiwlqqkxzoequy.supabase.co"
        val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRsZ2twbm1pd2xxcWt4em9lcXV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTE3MzIyNjYsImV4cCI6MjA2NzMwODI2Nn0.SuwdRYjz6yjudrVAQpdj6JxZ8b1rr9hJUjBcJeAM-uY"
        val bucketId = "door-smart-security"
        val filePath = "users/${user.imageName}"

        val url = "$supabaseUrl/storage/v1/object/delete/$bucketId"
        val jsonBody = """["$filePath"]""" // JSON array of paths

        val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .delete(requestBody)
            .addHeader("Authorization", supabaseKey)
            .addHeader("Content-Type", "application/json")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d("Supabase", "Image deleted successfully from bucket")
                } else {
                    Log.e("Supabase", "Failed to delete image: ${response.message}, ${response.body?.string()}")
                }
            } catch (e: Exception) {
                Log.e("Supabase", "Error: ${e.message}")
            }
        }
    }


    // Remove user from the local list (LiveData)
    fun removeUser(user: User) {
        _users.value = _users.value?.filter { it.userName != user.userName }
    }

    fun fetchUsersRealtime() {
        database.getReference("face_db").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = mutableListOf<User>()
                for (child in snapshot.children) {
                    val name = child.child("user_name").value?.toString()
                    val url = child.child("user_img_url").value?.toString()
                    if (!name.isNullOrEmpty() && !url.isNullOrEmpty()) {
                        userList.add(User(userName = name, userImgUrl = url))
                    }
                }
                _users.postValue(userList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching users: ${error.message}")
            }
        })
    }

    fun fetchHistoryEntriesRealtime() {
        val historyRef = database.getReference("history")
        historyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val historyList = mutableListOf<HistoryEntry>()
                for (child in snapshot.children) {
                    val entry_date = child.child("entry_date").value?.toString()
                    val entry_img_url = child.child("entry_img_url").value?.toString()
                    val entry_time = child.child("entry_time").value?.toString()
                    val classify = child.child("classify").value?.toString()

                    if (!entry_date.isNullOrEmpty() && !entry_img_url.isNullOrEmpty() && !entry_time.isNullOrEmpty()) {
                        historyList.add(HistoryEntry(entry_date, entry_img_url, entry_time, classify))
                    }
                }

                // ✅ Sort latest-first by date then time
                val sortedList = historyList.sortedWith(compareByDescending<HistoryEntry> {
                    // Convert date "MM/dd/yy" to Date for sorting
                    SimpleDateFormat("MM/dd/yy HH:mm:ss", Locale.getDefault())
                        .parse("${it.entry_date} ${it.entry_time}")
                })

                _historyEntries.postValue(sortedList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching history entries: ${error.message}")
            }
        })
    }


}

