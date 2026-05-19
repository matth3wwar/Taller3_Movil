package com.example.taller3.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.taller3.R
import com.example.taller3.model.UserAuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Account(
    modifier: Modifier = Modifier,
    mAuth: FirebaseAuth,
    controller: NavController,
    authViewModel: UserAuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val userState by authViewModel.user.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val uid = mAuth.currentUser?.uid

    // URI temporal para la cámara
    val cameraUri = remember {
        val file = File(context.filesDir, "profile_edit_tmp.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) selectedImageUri = cameraUri
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    // Cargar datos del usuario al iniciar
    LaunchedEffect(uid) {
        if (uid != null) {
            isLoading = true
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        authViewModel.setUserData(
                            firstName = document.getString("firstName") ?: "",
                            lastName = document.getString("lastName") ?: "",
                            idNumber = document.getString("idNumber") ?: "",
                            email = document.getString("email") ?: "",
                            photoUrl = document.getString("photoUrl")
                        )
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    Log.e("Account", "Error fetching user data", e)
                    isLoading = false
                }
        }
    }

    fun saveChanges() {
        if (uid == null) return
        isLoading = true

        val saveToFirestore = { photoUrl: String? ->
            val updates = mutableMapOf<String, Any>(
                "firstName" to userState.firstName,
                "lastName" to userState.lastName,
                "idNumber" to userState.idNumber
            )
            if (photoUrl != null) {
                updates["photoUrl"] = photoUrl
            }

            db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener {
                    isLoading = false
                    isEditing = false
                    if (photoUrl != null) authViewModel.updatePhotoUrl(photoUrl)
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        if (selectedImageUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("profile_pics/$uid.jpg")
            storageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        saveToFirestore(uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    Toast.makeText(context, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            saveToFirestore(null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Profile" else "Account Information") },
                actions = {
                    if (!isEditing && !isLoading) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Imagen de Perfil
            Box(contentAlignment = Alignment.BottomEnd) {
                val painter = when {
                    selectedImageUri != null -> rememberAsyncImagePainter(selectedImageUri)
                    !userState.photoUrl.isNullOrEmpty() -> rememberAsyncImagePainter(userState.photoUrl)
                    else -> painterResource(id = R.drawable.contacticon)
                }

                Image(
                    painter = painter,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )

                if (isEditing) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 25.dp)
                    ) {
                        FilledTonalIconButton(onClick = { cameraLauncher.launch(cameraUri) }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Camera")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        FilledTonalIconButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Menu, contentDescription = "Gallery")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            if (isEditing) {
                // Modo Edición: TextFields
                OutlinedTextField(
                    value = userState.firstName,
                    onValueChange = { authViewModel.updateFirstName(it) },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userState.lastName,
                    onValueChange = { authViewModel.updateLastName(it) },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userState.idNumber,
                    onValueChange = { authViewModel.updateIdNumber(it) },
                    label = { Text("ID Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { 
                                isEditing = false
                                selectedImageUri = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { saveChanges() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                    }
                }
            } else {
                // Modo Lectura: Información estática
                InfoField(label = "First Name", value = userState.firstName)
                InfoField(label = "Last Name", value = userState.lastName)
                InfoField(label = "ID Number", value = userState.idNumber)
                InfoField(label = "Email Address", value = userState.email)

                Spacer(modifier = Modifier.height(40.dp))
                
                if (isLoading) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun InfoField(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value.ifEmpty { "Not provided" },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp)
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
