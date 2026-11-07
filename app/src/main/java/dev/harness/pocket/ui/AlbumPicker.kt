package dev.harness.pocket.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class GalleryPhoto(
    val id: Long,
    val uri: Uri,
    val name: String
)

@Composable
fun AlbumPicker(
    onDismiss: () -> Unit,
    onPick: (List<Uri>) -> Unit,
    onSystemPicker: () -> Unit
) {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(permission)
    }

    var photos by remember { mutableStateOf<List<GalleryPhoto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(granted) {
        if (!granted) {
            photos = emptyList()
            loaded = false
            return@LaunchedEffect
        }
        loading = true
        photos = withContext(Dispatchers.IO) { queryPhotos(context) }
        loading = false
        loaded = true
    }

    var selected by remember { mutableStateOf<List<Long>>(emptyList()) }
    val selectedPhotos = selected.mapNotNull { id -> photos.firstOrNull { it.id == id } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F8FA)) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "关闭相册",
                            tint = Ink,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        "最近照片",
                        color = Ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = Muted, fontSize = 14.sp)
                    }
                }

                when {
                    !granted -> PermissionState(
                        onRequest = { permissionLauncher.launch(permission) },
                        onSystemPicker = onSystemPicker
                    )

                    loading && !loaded -> Box(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Blue, strokeWidth = 2.dp)
                    }

                    photos.isEmpty() -> EmptyState(onSystemPicker = onSystemPicker)

                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(photos, key = { it.id }) { photo ->
                            val index = selected.indexOf(photo.id)
                            PhotoCell(
                                photo = photo,
                                selectedIndex = index,
                                onClick = {
                                    selected = when {
                                        index >= 0 -> selected - photo.id
                                        selected.size >= 9 -> selected
                                        else -> selected + photo.id
                                    }
                                }
                            )
                        }
                    }
                }

                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (selected.isEmpty()) "选择照片" else "已选 ${selected.size}/9",
                                color = Ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (selected.isEmpty()) "点击图片可多选" else "按选择顺序发送",
                                color = Muted,
                                fontSize = 10.5.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = if (selected.isEmpty()) Color(0xFFE7EAF0) else Blue,
                            modifier = Modifier.clickable(enabled = selected.isNotEmpty()) {
                                onPick(selectedPhotos.map { it.uri })
                            }
                        ) {
                            Text(
                                "发送",
                                color = if (selected.isEmpty()) Muted else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCell(
    photo: GalleryPhoto,
    selectedIndex: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, photo.uri) {
        value = withContext(Dispatchers.IO) { loadThumbnail(context, photo.uri) }
    }
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFE6E8EC))
            .clickable(onClick = onClick)
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = photo.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        if (selectedIndex >= 0) {
            Box(Modifier.fillMaxSize().background(Blue.copy(alpha = 0.16f)))
            Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, Blue, RoundedCornerShape(4.dp))
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Blue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${selectedIndex + 1}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PermissionState(
    onRequest: () -> Unit,
    onSystemPicker: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.PhotoLibrary,
            contentDescription = null,
            tint = Color(0xFF9AA3B2),
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text("需要访问相册", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "允许后可以按微信相册的九宫格样式挑选照片，最多 9 张。",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(20.dp))
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = Blue,
            modifier = Modifier.clickable(onClick = onRequest)
        ) {
            Text(
                "允许访问相册",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 11.dp)
            )
        }
        TextButton(onClick = onSystemPicker) {
            Text("改用系统相册", color = Muted)
        }
    }
}

@Composable
private fun EmptyState(onSystemPicker: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("相册里还没有照片", color = InkSoft, fontSize = 14.sp)
        TextButton(onClick = onSystemPicker) {
            Text("改用系统相册", color = Blue)
        }
    }
}

private fun queryPhotos(context: Context): List<GalleryPhoto> {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME
    )
    val result = mutableListOf<GalleryPhoto>()
    try {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext() && result.size < 600) {
                val id = cursor.getLong(idColumn)
                result += GalleryPhoto(
                    id = id,
                    uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    ),
                    name = cursor.getString(nameColumn) ?: "IMG_$id.jpg"
                )
            }
        }
    } catch (_: SecurityException) {
        return emptyList()
    }
    return result
}

private fun loadThumbnail(context: Context, uri: Uri): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 480 || bounds.outHeight / sample > 480) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    } catch (_: Exception) {
        null
    }
}
