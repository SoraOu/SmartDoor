package com.example.smartdoor.data.model

data class User(
    val userName: String = "",
    val userImgUrl: String = "",
    val imageName: String = ""   // Image file name stored in Supabase (or Firebase)
)
