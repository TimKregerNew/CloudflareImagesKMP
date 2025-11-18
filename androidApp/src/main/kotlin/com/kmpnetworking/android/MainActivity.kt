package com.kmpnetworking.android

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kmpnetworking.cloudflare.AndroidImageData
import com.kmpnetworking.cloudflare.CloudflareImageUploadResponse
import com.kmpnetworking.cloudflare.CloudflareImageUploader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CloudflareUploadScreen()
                }
            }
        }
    }
}

@Composable
fun CloudflareUploadScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // State
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var cloudflareImages by remember { mutableStateOf<List<CloudflareImageUploadResponse>>(emptyList()) }
    var isLoadingImages by remember { mutableStateOf(false) }
    
    // File picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uploadedImageUrl = null
        errorMessage = null
    }
    
    // Cloudflare uploader (reads from environment variables)
    val uploader = remember {
        val accountId = System.getenv("CLOUDFLARE_ACCOUNT_ID") ?: ""
        val apiToken = System.getenv("CLOUDFLARE_API_TOKEN") ?: ""
        
        if (accountId.isEmpty() || apiToken.isEmpty()) {
            error("CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN environment variables must be set")
        }
        
        CloudflareImageUploader.create(
            accountId = accountId,
            apiToken = apiToken,
            enableLogging = true,
            timeoutMillis = 60000
        )
    }
    
    DisposableEffect(Unit) {
        onDispose {
            uploader.close()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Cloudflare Image Upload",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = "All upload logic is in the shared KMP library!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        
        // Select Image Button
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Image from Gallery")
        }
        
        // Selected image preview
        selectedImageUri?.let { uri ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            // Upload Button
            Button(
                onClick = {
                    scope.launch {
                        isUploading = true
                        uploadProgress = 0f
                        errorMessage = null
                        uploadedImageUrl = null
                        
                        selectedImageUri?.let { uri ->
                            // Create ImageData from Uri - platform-specific
                            val imageData = AndroidImageData.fromUri(context, uri)
                            
                            if (imageData != null) {
                                // Upload - ALL business logic in shared library!
                                val result = uploader.uploadImage(
                                    imageData = imageData,
                                    id = null,
                                    requireSignedURLs = false,
                                    metadata = mapOf(
                                        "source" to "android_app",
                                        "timestamp" to System.currentTimeMillis().toString()
                                    ),
                                    onProgress = { progress ->
                                        uploadProgress = progress
                                    }
                                )
                                
                                isUploading = false
                                
                                if (result.isSuccess) {
                                    val response = result.getOrNull()
                                    uploadedImageUrl = response?.publicUrl
                                } else if (result.isError) {
                                    errorMessage = "Upload failed"
                                }
                            } else {
                                isUploading = false
                                errorMessage = "Failed to read image"
                            }
                        }
                    }
                },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upload to Cloudflare")
            }
        }
        
        // Progress indicator
        if (isUploading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Uploading: ${(uploadProgress * 100).toInt()}%")
            }
        }
        
        // Error message
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Error: $error",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Success message
        uploadedImageUrl?.let { url ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "✅ Upload Successful!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "URL: $url",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Additional operations
        Text(
            text = "Other Operations",
            style = MaterialTheme.typography.titleMedium
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    scope.launch {
                        isLoadingImages = true
                        errorMessage = null
                        infoMessage = null
                        
                        val result = uploader.listImages(page = 1, perPage = 50)
                        
                        if (result.isSuccess) {
                            val response = result.getOrNull()
                            if (response != null) {
                                cloudflareImages = response.images
                                infoMessage = "Found ${response.totalCount} total images"
                            }
                        } else {
                            errorMessage = "Failed to list images"
                            cloudflareImages = emptyList()
                        }
                        
                        isLoadingImages = false
                    }
                },
                enabled = !isLoadingImages,
                modifier = Modifier.weight(1f)
            ) {
                Text("List Images")
            }
            
            Button(
                onClick = {
                    scope.launch {
                        val result = uploader.getUsageStats()
                        if (result.isSuccess) {
                            val stats = result.getOrNull()
                            if (stats != null) {
                                infoMessage = "Used: ${stats.count.current} / ${stats.count.allowed}"
                            }
                        } else {
                            errorMessage = "Failed to get stats"
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Get Stats")
            }
        }
        
        if (isLoadingImages) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Loading images...")
        }
        
        if (infoMessage != null) {
            Text(
                text = infoMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Display Cloudflare images
        if (cloudflareImages.isNotEmpty()) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Your Cloudflare Images",
                style = MaterialTheme.typography.titleMedium
            )
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cloudflareImages) { image ->
                    CloudflareImageRow(
                        image = image,
                        onDelete = {
                            scope.launch {
                                val result = uploader.deleteImage(image.id)
                                if (result.isSuccess) {
                                    // Remove from local list
                                    cloudflareImages = cloudflareImages.filter { it.id != image.id }
                                    infoMessage = "Image deleted successfully"
                                } else {
                                    errorMessage = "Failed to delete image"
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CloudflareImageRow(
    image: CloudflareImageUploadResponse,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (image.filename.isNotEmpty()) image.filename else "Untitled",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    
                    Text(
                        text = "ID: ${image.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Text(
                        text = "Uploaded: ${formatDate(image.uploaded)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                // Action buttons
                Row {
                    image.publicUrl?.let { url ->
                        IconButton(
                            onClick = {
                                // Open URL in browser (would need intent launcher)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OpenInNew,
                                contentDescription = "Open in browser"
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = onDelete
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete image",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Image preview
            image.publicUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = image.filename,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            // Variants
            if (image.variants.isNotEmpty()) {
                Text(
                    text = "Variants: ${image.variants.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            // Metadata
            image.metadata?.let { metadata ->
                if (metadata.isNotEmpty()) {
                    Column {
                        metadata.forEach { (key, value) ->
                            Text(
                                text = "$key: $value",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
        val date = inputFormat.parse(dateString)
        if (date != null) {
            outputFormat.format(date)
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

