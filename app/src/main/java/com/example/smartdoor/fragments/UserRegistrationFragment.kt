import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.smartdoor.databinding.FragmentUserRegistrationBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.FirebaseApp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import com.example.smartdoor.R


class UserRegistrationFragment : Fragment() {

    private lateinit var binding: FragmentUserRegistrationBinding
    private val CAMERA_REQUEST_CODE = 1001
    private val GALLERY_REQUEST_CODE = 1002
    private val STORAGE_BUCKET = "door-smart-security" // Supabase storage bucket name

    private val supabaseUrl = "https://tlgkpnmiwlqqkxzoequy.supabase.co" // Supabase URL
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRsZ2twbm1pd2xxcWt4em9lcXV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTE3MzIyNjYsImV4cCI6MjA2NzMwODI2Nn0.SuwdRYjz6yjudrVAQpdj6JxZ8b1rr9hJUjBcJeAM-uY" // Replace with your actual Supabase Key

    private lateinit var photoUri: Uri  // Declare the URI for the captured image
    private lateinit var progressDialog: AlertDialog
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserRegistrationBinding.inflate(inflater, container, false)

        // Initialize Firebase
        FirebaseApp.initializeApp(requireContext())

        // Load the animation
        val scaleAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.button_press)

        // Capture Button Logic
        binding.captureButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.startAnimation(scaleAnimation)
            }
            false // Let click listener still work
        }
        binding.captureButton.setOnClickListener { openCamera() }

        // Gallery Button Logic
        binding.galleryButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.startAnimation(scaleAnimation)
            }
            false
        }
        binding.galleryButton.setOnClickListener { openGallery() }

        return binding.root
    }


    // Check and request permissions
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Request the permissions
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE), CAMERA_REQUEST_CODE)
        } else {
            openCamera()  // Proceed to open the camera if permission is granted
        }
    }

    // Handle the permission request result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()  // Open the camera if permission is granted
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        checkPermissions()  // Check permissions before opening the camera
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            val photoFile = createImageFile()  // Create a file to store the photo
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider", // authority from the manifest
                photoFile
            )

            // Send the URI to the intent to store the image
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    private fun createImageFile(): File {
        val timeStamp = System.currentTimeMillis().toString()
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",  /* prefix */
            ".jpg",                 /* suffix */
            storageDir             /* directory */
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                Log.d("Camera", "Photo captured and saved at: $photoUri")
                promptForFileName(photoUri)
            } else if (requestCode == GALLERY_REQUEST_CODE) {
                val imageUri: Uri? = data?.data
                if (imageUri != null) {
                    promptForFileName(imageUri)
                } else {
                    Toast.makeText(requireContext(), "Failed to get image URI from gallery", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun promptForFileName(imageUri: Uri) {
        val inputField = EditText(requireContext()).apply {
            hint = "Enter file name (without extension)"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Enter File Name")
            .setView(inputField)
            .setPositiveButton("OK") { _, _ ->
                val customFileName = inputField.text.toString().trim()
                if (TextUtils.isEmpty(customFileName)) {
                    Toast.makeText(requireContext(), "File name cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    uploadImageToSupabase(imageUri, customFileName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadImageToSupabase(imageUri: Uri, customFileName: String) {
        val inputStream = requireContext().contentResolver.openInputStream(imageUri)

        if (inputStream == null) {
            Toast.makeText(requireContext(), "Error opening image", Toast.LENGTH_SHORT).show()
            return
        }

        val originalFile = File(requireContext().cacheDir, "user_image.jpg")
        inputStream.copyTo(originalFile.outputStream())

        if (originalFile.length() == 0L) {
            Log.e("FileError", "The file is empty!")
            Toast.makeText(requireContext(), "The image is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        val newFileName = "${customFileName}_${System.currentTimeMillis()}.jpg"
        val renamedFile = File(originalFile.parent, newFileName)

        val renameSuccess = originalFile.renameTo(renamedFile)

        if (!renameSuccess) {
            Log.e("FileError", "Failed to rename the file")
            Toast.makeText(requireContext(), "Failed to rename the file", Toast.LENGTH_SHORT).show()
            return
        }

        uploadToSupabase(renamedFile, customFileName)
    }

    private fun showProgressDialog() {
        // Create the progress bar and set it in a dialog
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_progress, null)

        progressBar = view.findViewById(R.id.progressBar)
        progressBar.isIndeterminate = false // Allow filling

        builder.setView(view)
            .setCancelable(false) // Make sure it can't be dismissed by tapping outside
        progressDialog = builder.create()

        // Show the dialog
        progressDialog.show()
    }

    private fun updateProgressBar(progress: Int) {
        progressBar.progress = progress
    }

    private fun dismissProgressDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    private fun uploadToSupabase(file: File, customFileName: String) {
        // Show progress dialog
        showProgressDialog()

        val mimeType = "image/jpeg"
        val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val filePath = "users/${file.name}"
        val uploadUrl = "$supabaseUrl/storage/v1/object/$STORAGE_BUCKET/$filePath"
        Log.d("Upload", "Upload URL: $uploadUrl")

        val request = Request.Builder()
            .url(uploadUrl)
            .header("Authorization", "Bearer $supabaseKey")
            .header("Content-Type", mimeType)
            .put(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    // Hide the progress bar
                    dismissProgressDialog()
                    Log.e("UploadError", "Upload failed: ${e.message}")
                    Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                requireActivity().runOnUiThread {
                    // Hide the progress bar when the upload completes
                    dismissProgressDialog()

                    if (response.isSuccessful) {
                        Log.d("Upload", "Image uploaded successfully")
                        Toast.makeText(requireContext(), "Image uploaded", Toast.LENGTH_SHORT).show()

                        val publicUrl = "$supabaseUrl/storage/v1/object/public/$STORAGE_BUCKET/users/${file.name}"
                        Log.d("Upload", "Public URL: $publicUrl")

                        val userName = file.name.split("_")[0].capitalize()

                        val metadata = mapOf(
                            "user_name" to customFileName, // Use the name entered by the user
                            "user_img_url" to publicUrl   // The URL of the uploaded image
                        )

                        uploadMetadataToFirebase(metadata)
                    } else {
                        Log.e("UploadError", "Upload failed with code: ${response.code}, message: ${response.message}")
                        try {
                            Log.e("UploadError", "Response body: ${response.body?.string()}")
                        } catch (e: Exception) {
                            Log.e("UploadError", "Error reading response body: ${e.message}")
                        }
                        Toast.makeText(requireContext(), "Upload failed: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }



    private fun uploadMetadataToFirebase(metadata: Map<String, String>) {
        val firebaseDatabase = FirebaseDatabase.getInstance().reference

        // Reference to the "face_db" node
        val faceDbRef = firebaseDatabase.child("face_db")

        // Query to get the latest user node
        faceDbRef.orderByKey().limitToLast(1).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                // Get the latest user node key (e.g., "user1", "user2")
                val latestUserNode = dataSnapshot.children.first().key
                val newUserNumber = latestUserNode?.removePrefix("user")?.toIntOrNull()?.plus(1) ?: 1
                val newUserNodeName = "user$newUserNumber" // Generate the next user node (e.g., "user2")

                // Create a reference for the new user node
                val newUserRef = faceDbRef.child(newUserNodeName)

                // Prepare the metadata to be uploaded under the new user node
                newUserRef.setValue(metadata)
                    .addOnSuccessListener {
                        Log.d("Firebase", "Metadata uploaded successfully to $newUserNodeName")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "Failed to upload metadata: ${e.message}")
                        Toast.makeText(requireContext(), "Failed to upload metadata", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // If no existing nodes, start with user1
                val newUserRef = faceDbRef.child("user1")
                newUserRef.setValue(metadata)
                    .addOnSuccessListener {
                        Log.d("Firebase", "Metadata uploaded successfully to user1")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "Failed to upload metadata: ${e.message}")
                        Toast.makeText(requireContext(), "Failed to upload metadata", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("Firebase", "Error fetching latest user node: ${e.message}")
            Toast.makeText(requireContext(), "Failed to check the latest user node", Toast.LENGTH_SHORT).show()
        }
    }

}
