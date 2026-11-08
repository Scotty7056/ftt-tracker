package com.example.freizeittickettracker.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.freizeittickettracker.domain.StoryCardGenerator
import com.example.freizeittickettracker.domain.StorySummaryData
import com.example.freizeittickettracker.ui.GlacierBlue

@Composable
fun StoryShareDialog(
    data: StorySummaryData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedIndex by remember { mutableStateOf(0) }

    // Generiert die 3 Story-Varianten im Speicher
    val styles = StoryCardGenerator.StoryStyle.values()
    val bitmaps = remember(data) {
        styles.map { style ->
            StoryCardGenerator.generateStoryBitmap(context, data, style)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Instagram Story Vorlage",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Schließen", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Haupt-Vorschau des ausgewählten Bildes (9:16 Aspect Ratio)
                val currentBitmap = bitmaps[selectedIndex]
                Box(
                    modifier = Modifier
                        .height(360.dp)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                ) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Story Vorschau",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Style Selector Thumbnails
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(styles) { index, style ->
                        val isSelected = index == selectedIndex
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) GlacierBlue.copy(alpha = 0.25f) else Color(0xFF334155),
                            modifier = Modifier
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) GlacierBlue else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedIndex = index }
                        ) {
                            Text(
                                text = style.title,
                                color = if (isSelected) GlacierBlue else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Teilen-Button
                Button(
                    onClick = {
                        shareStoryImage(context, currentBitmap, styles[selectedIndex].name)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlacierBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("In Story teilen / Bild senden", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun shareStoryImage(context: Context, bitmap: Bitmap, styleName: String) {
    val uri = StoryCardGenerator.saveBitmapToCache(context, bitmap, "ftt_wrapped_$styleName")

    // Direkter Intent für Instagram Stories falls vorhanden, sonst nativer Chooser
    val storiesIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
        setDataAndType(uri, "image/png")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    val pm = context.packageManager
    if (storiesIntent.resolveActivity(pm) != null) {
        context.startActivity(storiesIntent)
    } else {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(sendIntent, "FTT Wrapped Story teilen"))
    }
}