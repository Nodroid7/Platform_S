package com.example.platform.privacy.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.database.getStringOrNull
import coil.compose.AsyncImage
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@Sample(
    name = "Permissionless",
    description = "This sample demonstrate how you can avoid requesting permission for certain actions by leveraging System APIs",
    documentation = "https://developer.android.com/training/permissions/evaluating"
)
@Composable
fun Permissionless() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TitleCard()
        }
        item {
            TakePhotoCard()
        }
        item {
            CaptureVideoCard()
        }
        item {
            PickContactCard()
        }
        item {
            PickMediaCard()
        }
        item {
            OpenDocumentsCard()
        }
    }
}

@Composable
@OptIn(ExperimentalTextApi::class)
private fun TitleCard() {
    val uriHandler = LocalUriHandler.current
    val title = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onSurface,
            )
        ) {
            append("The following actions don't require any permission (")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                val startIndex = this.length
                append("more actions")
                addUrlAnnotation(
                    UrlAnnotation("https://developer.android.com/training/permissions/evaluating"),
                    start = startIndex,
                    end = this.length
                )
            }
            append(").")
        }
    }
    ClickableText(text = title, style = MaterialTheme.typography.bodyLarge) {
        val url = title.getUrlAnnotations(it, it).firstOrNull()?.item?.url ?: return@ClickableText
        uriHandler.openUri(url)
    }
}

/**
 * Create a file in the cacheDir and returns an shareable URI from the FileProvider as defined in the
 * AndroidManifest.xml.
 *
 * In a real world app you would move this into another layer like a ViewModel or a Repository.
 *
 * Important: there are different ways to create the URI based on your needs, see the table in
 * https://developer.android.com/training/data-storage
 */
private suspend fun Context.createTemporaryFile(name: String, type: String): Uri =
    withContext(Dispatchers.IO) {
        val file = File.createTempFile(name, type, cacheDir).apply {
            createNewFile()
        }
        FileProvider.getUriForFile(applicationContext, "$packageName.provider", file)
    }

/**
 * Utility class to hold the state of the camera request with the created URI.
 */
sealed class CameraRequest {

    abstract val uri: Uri

    object None : CameraRequest() {
        override val uri: Uri = Uri.EMPTY
    }

    data class Pending(override val uri: Uri) : CameraRequest()
    data class Completed(override val uri: Uri) : CameraRequest()
}

/**
 * Register the provided camera related contract and display the UI to request and show the returned
 * value from the camera.
 */
@Composable
private fun CameraRequestCard(
    actionTitle: String,
    fileName: String,
    fileType: String,
    contract: ActivityResultContract<Uri, Boolean>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Hold the camera request use to display the selected media.
    var request by remember {
        mutableStateOf<CameraRequest>(CameraRequest.None)
    }

    // Create the launcher for the given contract and handle result
    val launcher = rememberLauncherForActivityResult(contract) { isSuccessful ->
        // Create a new image request with the previously create URI to force Coil to display the
        // new URI content, otherwise it would cache the "empty" URI.
        request = if (isSuccessful) {
            CameraRequest.Completed(request.uri)
        } else {
            CameraRequest.None
        }
        Toast.makeText(context, "Captured? $isSuccessful", Toast.LENGTH_SHORT).show()
    }

    Card(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    // On click create a new file (outside of the main thread!) and launch the
                    // camera request with the new file URI
                    request = CameraRequest.Pending(
                        context.createTemporaryFile(fileName, fileType)
                    )
                    launcher.launch(request.uri)
                }
            }
        ) {
            Text(text = actionTitle)
        }
        if (request is CameraRequest.Completed) {
            // Using [Coil](https://github.com/coil-kt/coil) to load images from URIs
            AsyncImage(
                model = request.uri,
                contentDescription = "Image captured by you",
                modifier = Modifier
                    .clickable {
                        // Request system to display URI
                        val intent = Intent(Intent.ACTION_VIEW, request.uri)
                        // Important: flag needed to allow other apps read internal app's files.
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(intent)
                    }
            )
        }
    }
}

@Composable
private fun TakePhotoCard() {
    CameraRequestCard(
        actionTitle = "Take a photo",
        fileName = "sample-photo",
        fileType = ".jpeg",
        contract = ActivityResultContracts.TakePicture()
    )
}

@Composable
private fun CaptureVideoCard() {
    CameraRequestCard(
        actionTitle = "Capture video",
        fileName = "sample-video",
        fileType = ".mp4",
        contract = ActivityResultContracts.CaptureVideo()
    )
}

@Composable
private fun PickContactCard() {
    val context = LocalContext.current

    // Hold the contact information selected by the user
    var contactInfo by rememberSaveable {
        mutableStateOf("")
    }

    // Note: PickContact does not provide all contact information, if you need certain information,
    // like phone number or email, you should create your own contract and launch the intent with
    // ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri == null) {
            Toast.makeText(context, "No contact selected", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        context.contentResolver.query(uri, null, null, null).use { cursor ->
            // If the cursor returned is valid, get the phone number and (or) name
            contactInfo = if (cursor != null && cursor.moveToFirst()) {
                (0 until cursor.columnCount).joinToString("\n") {
                    "${cursor.getColumnName(it)}: ${cursor.getStringOrNull(it)}"
                }
            } else {
                "Error while retrieving contact information"
            }
        }
    }
    Card(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                launcher.launch(null)
            }
        ) {
            Text(text = "Pick contact")
        }

        if (contactInfo.isNotBlank()) {
            Text(text = contactInfo, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun PickMediaCard() {
    // Holds the selected URI from the media selected by the user
    var selectedUri by rememberSaveable {
        mutableStateOf<Uri>(Uri.EMPTY)
    }

    // Create the launcher for the given contract and handle result
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Show the image if the user selected any.
            selectedUri = uri ?: return@rememberLauncherForActivityResult
        }

    Card(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                // Launch the Photo Picker request. Check PickVisualMedia types for other options
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        ) {
            Text(text = "Pick Image")
        }
        if (selectedUri != Uri.EMPTY) {
            // Using [Coil](https://github.com/coil-kt/coil) to load images from URIs
            AsyncImage(
                model = selectedUri,
                contentDescription = "Image selected by you",
            )
        }
    }
}

@Composable
private fun OpenDocumentsCard() {
    val context = LocalContext.current

    // Holds the information of the selected file by the user
    var documentInfo by rememberSaveable {
        mutableStateOf("")
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                Toast.makeText(context, "No document selected", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }

            // Retrieve the metadata info from the file
            context.contentResolver.query(uri, null, null, null, null).use { cursor ->
                documentInfo = if (cursor != null && cursor.moveToFirst()) {
                    (0 until cursor.columnCount).joinToString("\n") {
                        "${cursor.getColumnName(it)}: ${cursor.getStringOrNull(it)}"
                    }
                } else {
                    "Error while retrieving file information"
                }
            }
        }
    Card(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                // Request the user to pick a file
                // Note: You can provide multiple mime types to filter results.
                launcher.launch(arrayOf("*/*"))
            }
        ) {
            Text(text = "Open document")
        }

        if (documentInfo.isNotBlank()) {
            Text(text = documentInfo, modifier = Modifier.padding(16.dp))
        }
    }
}