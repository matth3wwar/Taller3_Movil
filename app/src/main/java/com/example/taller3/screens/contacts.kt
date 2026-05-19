package com.example.taller3.screens

import android.Manifest
import android.content.ContentResolver
import android.provider.ContactsContract
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taller3.model.Contact
import com.google.accompanist.permissions.*
import com.example.taller3.R

val contactsPerm = Manifest.permission.READ_CONTACTS

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Contacts() {

    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val permissionStatus = rememberPermissionState(contactsPerm)

    SideEffect {
        if (!permissionStatus.status.isGranted) {
            permissionStatus.launchPermissionRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        if (permissionStatus.status.isGranted) {

            val contacts = loadContacts(contentResolver)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(contacts) { contact ->

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Image(
                                        painter = painterResource(R.drawable.contacticon),
                                        contentDescription = "Contact Icon",
                                        modifier = Modifier
                                            .size(50.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {

                                        // NOMBRE
                                        Text(
                                            text = contact.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // NUMERO CEL
                                        Text(
                                            text = contact.phone,
                                            fontSize = 14.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } else {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No access to contacts",
                    fontSize = 20.sp,
                    color = Color.Red
                )
            }
        }
    }
}

// ---------------- CARGAR CONTACTOS ----------------
fun loadContacts(contentResolver: ContentResolver): List<Contact> {

    val contacts = mutableListOf<Contact>()
    val seenNumbers = mutableSetOf<String>() // Cada numero solo sale una vez

    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone._ID,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )

    val cursor = contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )

    cursor?.use {

        val idColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
        val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

        while (it.moveToNext()) {

            val id = it.getString(idColumn)
            val name = it.getString(nameColumn)
            val number = it.getString(numberColumn)

            val cleanNumber = number.replace("\\s|-".toRegex(), "")

            if (!seenNumbers.contains(cleanNumber)) {
                seenNumbers.add(cleanNumber)
                contacts.add(Contact(id, name, number))
            }
        }
    }

    return contacts
}