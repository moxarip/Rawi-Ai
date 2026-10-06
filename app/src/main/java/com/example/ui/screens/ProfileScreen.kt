package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Download
import android.widget.Toast
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.api.ImageProvider
import com.example.data.api.MusicProvider
import com.example.data.api.SpeechProvider
import com.example.data.api.TextProvider
import com.example.data.api.VideoProvider
import com.example.ui.StoryViewModel
import com.example.ui.auth.signOut
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StudioAccent
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioPrimaryLight
import com.example.ui.theme.StudioSecondary
import com.example.ui.theme.StudioSecondaryLight
import com.example.ui.theme.StudioTertiary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: StoryViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    val user = Firebase.auth.currentUser

    val providerManager = viewModel.providerManager
    val customKey by providerManager.customGeminiKey.collectAsState()
    val isGoogleOneLinked by providerManager.isGoogleOneLinked.collectAsState()

    val selectedImageProvider by providerManager.selectedImageProvider.collectAsState()
    val selectedTextProvider by providerManager.selectedTextProvider.collectAsState()
    val selectedSpeechProvider by providerManager.selectedSpeechProvider.collectAsState()
    val selectedMusicProvider by providerManager.selectedMusicProvider.collectAsState()
    val selectedVideoProvider by providerManager.selectedVideoProvider.collectAsState()

    var keyInput by remember(customKey) { mutableStateOf(customKey) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestingKey by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // User Info Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, DarkBorder))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Brush.radialGradient(listOf(StudioPrimary, Color(0xFF2E1065))), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user?.displayName ?: "مستخدم راوي AI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            if (isGoogleOneLinked) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Google One Linked",
                                    tint = StudioSecondaryLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = user?.email ?: "حساب مسجل عبر Google",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // GOOGLE ONE & GEMINI SUBSCRIPTION INTEGRATION CARD
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1338)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        listOf(StudioSecondary, StudioPrimary)
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = StudioSecondaryLight
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "اشتراك Google One و Gemini Advanced",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isGoogleOneLinked) Color(0xFF14532D) else Color(0xFF374151))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isGoogleOneLinked) "Google One متصل" else "غير مقترن",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGoogleOneLinked) Color(0xFF86EFAC) else Color(0xFFD1D5DB)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "إذا كنت تمتلك اشتراك Google One مع Gemini Advanced، يمكنك الاستفادة من أعلى حصة وسرعة لتوليد النصوص والصور والفيديوهات عبر Gemini 3.5 و Gemini Pro و Veo باستخدام مفتاح API الخاص بحسابك من Google AI Studio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 1: Open Google AI Studio button
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = StudioSecondaryLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "فتح Google AI Studio لنسخ المفتاح مجاناً",
                            color = StudioSecondaryLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Key Input Field
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it
                            providerManager.setCustomGeminiKey(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_gemini_key_input"),
                        placeholder = { Text("ألصق مفتاح Gemini API هنا (AIza...)", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = StudioPrimaryLight)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioSecondary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Key Test Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isTestingKey = true
                                testResultText = null
                                viewModel.testCustomApiKey(keyInput) { success, msg ->
                                    isTestingKey = false
                                    testResultText = msg
                                }
                            },
                            enabled = !isTestingKey && keyInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_key_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isTestingKey) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري الفحص...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فحص واختبار الاتصال", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (keyInput.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    keyInput = ""
                                    providerManager.setCustomGeminiKey("")
                                    providerManager.setGoogleOneLinked(false)
                                    testResultText = "تمت إزالة المفتاح، سيعمل التطبيق عبر الاستضافات المفتوحة البديلة."
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("مسح", color = Color(0xFFFCA5A5), fontSize = 12.sp)
                            }
                        }
                    }

                    if (testResultText != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (testResultText!!.contains("بنجاح")) Color(0xFF14532D) else Color(0xFF451A1A))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = testResultText!!,
                                color = if (testResultText!!.contains("بنجاح")) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // MULTI-PROVIDER AI HUB SETTINGS
        item {
            Text(
                text = "استضافات ومحركات الذكاء الاصطناعي (AI Providers)",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Image Providers Card
        item {
            ProviderSelectorCard(
                title = "محرك توليد الصور (Image Generation)",
                icon = Icons.Default.Image,
                options = ImageProvider.values().map { it.displayName to it },
                selected = selectedImageProvider.displayName,
                onSelect = { providerManager.setImageProvider(it) }
            )
        }

        // Text & Script Providers Card
        item {
            ProviderSelectorCard(
                title = "محرك تأليف القصص والسيناريو (Text & Script)",
                icon = Icons.Default.AutoAwesome,
                options = TextProvider.values().map { it.displayName to it },
                selected = selectedTextProvider.displayName,
                onSelect = { providerManager.setTextProvider(it) }
            )
        }

        // Speech Narration Providers Card
        item {
            ProviderSelectorCard(
                title = "محرك التعليق الصوتي الدرامي (Voice & Speech)",
                icon = Icons.Default.RecordVoiceOver,
                options = SpeechProvider.values().map { it.displayName to it },
                selected = selectedSpeechProvider.displayName,
                onSelect = { providerManager.setSpeechProvider(it) }
            )
        }

        // Music Soundtrack Providers Card
        item {
            ProviderSelectorCard(
                title = "محرك الموسيقى التصويرية (Soundtrack & Music)",
                icon = Icons.Default.MusicNote,
                options = MusicProvider.values().map { it.displayName to it },
                selected = selectedMusicProvider.displayName,
                onSelect = { providerManager.setMusicProvider(it) }
            )
        }

        // Video Motion Providers Card
        item {
            ProviderSelectorCard(
                title = "محرك الفيديو والمونتاج (Video Engine)",
                icon = Icons.Default.Movie,
                options = VideoProvider.values().map { it.displayName to it },
                selected = selectedVideoProvider.displayName,
                onSelect = { providerManager.setVideoProvider(it) }
            )
        }

        // System Info & Cloud Database
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, DarkBorder))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "معلومات النظام والاتصال السحابي",
                        fontWeight = FontWeight.Bold,
                        color = StudioSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingRow(icon = Icons.Default.Storage, title = "قاعدة البيانات السحابية", value = "Cloud Firestore Enterprise")
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingRow(icon = Icons.Default.Security, title = "طريقة المصادقة", value = "Google Sign-In Credential Manager")
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingRow(icon = Icons.Default.Movie, title = "حزمة الاستضافات المتاحة", value = "Google Gemini + Pollinations + Android Native")
                }
            }
        }

        // In-App Update Management Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1735)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(DarkBorder, StudioSecondary.copy(alpha = 0.5f)))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = StudioSecondaryLight, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "التحديث الداخلي (In-App Updater)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioPrimary.copy(alpha = 0.3f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "النسخة: v1",
                                color = StudioSecondaryLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "يقوم التطبيق بالتحقق التلقائي من تحديثات GitHub (v1 -> v2) وتنزيل حزمة APK وتثبيتها مباشرة من داخل التطبيق دون الحاجة للمتصفح.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.checkForUpdates { hasUpd, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فحص التحديثات", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.triggerSimulatedUpdate("v2")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = StudioSecondaryLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تجربة تحديث v2", color = StudioSecondaryLight, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Sign Out Button
        item {
            Button(
                onClick = {
                    signOut(context, credentialManager, onSignOut, scope)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7F1D1D),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("تسجيل الخروج", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ProviderSelectorCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    options: List<Pair<String, T>>,
    selected: String,
    onSelect: (T) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, DarkBorder))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEach { (displayName, item) ->
                    val isSelected = displayName == selected
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(item) },
                        label = { Text(displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceVariant,
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = StudioPrimary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(value, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
    }
}
