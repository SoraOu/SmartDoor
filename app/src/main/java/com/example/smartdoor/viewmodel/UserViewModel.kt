package com.example.smartdoor.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smartdoor.data.model.User
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request


class UserViewModel : ViewModel() {

    private val firebaseUrl = "https://door--smart-security-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val database = FirebaseDatabase.getInstance(firebaseUrl)
    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users


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

        val url = "$supabaseUrl/storage/v1/object/$bucketId"
        val jsonBody = """["$filePath"]""" // JSON array of paths

        val bucket = supabase.storage.from(bucketId) // Replace "your_bucket_name" with your actual bucket name
        try {
            val result = bucket.delete(filePath)

            Log.e("Supabase", "${result}")
        }
        catch (e: Exception){
            Log.e("Supabase", "Error: ${e.message}")
        }
        finally{
            Log.e("Supabase", "done")
        }
    }


    // Remove user from the local list (LiveData)
    fun removeUser(user: User) {
        _users.value = _users.value?.filter { it.userName != user.userName }
    }
}

