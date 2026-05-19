package com.example.taller3.screens

import android.Manifest
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.taller3.R
import com.example.taller3.model.UserAuthViewModel
import com.example.taller3.navigation.AppScreens
import com.example.taller3.utils.validateForm
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Register(
    modifier: Modifier = Modifier,
    mAuth: FirebaseAuth,
    controller: NavController,
    authViewModel: UserAuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val userState by authViewModel.user.collectAsState()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Camera and Permission Setup
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val cameraUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        File(context.filesDir, "cameraPic_${System.currentTimeMillis()}.jpg")
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) selectedImageUri = cameraUri
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(Unit) {
        if (mAuth.currentUser != null) {
            controller.navigate(AppScreens.Home.name)
        }
    }

    fun saveUserData(uid: String, photoUrl: String?) {
        val db = FirebaseFirestore.getInstance()
        val userData = hashMapOf(
            "uid" to uid,
            "firstName" to userState.firstName,
            "lastName" to userState.lastName,
            "idNumber" to userState.idNumber,
            "email" to userState.email,
            "photoUrl" to photoUrl,
            "lastLocation" to null,
            "available" to false
        )

        db.collection("users").document(uid)
            .set(userData)
            .addOnSuccessListener {
                isLoading = false
                controller.navigate(AppScreens.Home.name)
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Error saving data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    fun uploadImageAndSaveData(uid: String) {
        if (selectedImageUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("profile_pics/$uid.jpg")
            storageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        saveUserData(uid, uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    Toast.makeText(context, "Error uploading image: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            saveUserData(uid, null)
        }
    }

    fun register() {
        if (validateForm(userState.email, userState.password)) {
            isLoading = true
            mAuth.createUserWithEmailAndPassword(userState.email, userState.password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result?.user?.uid
                        if (uid != null) {
                            uploadImageAndSaveData(uid)
                        }
                    } else {
                        isLoading = false
                        Log.e("AUTH", "Error creating user: ${task.exception?.message}")
                        Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(context, "Por favor verifica los campos", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Register",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (selectedImageUri != null) {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(R.drawable.contacticon),
                contentDescription = "logo",
                modifier = Modifier.size(100.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = {
                if (!cameraPermissionState.status.isGranted) {
                    cameraPermissionState.launchPermissionRequest()
                }
                if (cameraPermissionState.status.isGranted) {
                    cameraLauncher.launch(cameraUri)
                }
            }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Use Camera")
            }
            IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Use Gallery")
            }
        }

        OutlinedTextField(
            value = userState.firstName,
            onValueChange = { authViewModel.updateFirstName(it) },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = userState.lastName,
            onValueChange = { authViewModel.updateLastName(it) },
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = userState.idNumber,
            onValueChange = { authViewModel.updateIdNumber(it) },
            label = { Text("ID Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = userState.email,
            onValueChange = { authViewModel.updateEmailClass(it) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = userState.emailError.isNotEmpty(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        if (userState.emailError.isNotEmpty()) {
            Text(text = userState.emailError, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = userState.password,
            onValueChange = { authViewModel.updatePassClass(it) },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            isError = userState.passError.isNotEmpty()
        )

        if (userState.passError.isNotEmpty()) {
            Text(text = userState.passError, color = MaterialTheme.colorScheme.error)
        }

        Text(
            text = buildAnnotatedString {
                append("Already own an account? ")
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "login",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ),
                        linkInteractionListener = {
                            controller.navigate(AppScreens.Login.name)
                        }
                    )
                ) {
                    append("Login here")
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { register() }
            ) {
                Text("Register")
            }
        }
    }
}
