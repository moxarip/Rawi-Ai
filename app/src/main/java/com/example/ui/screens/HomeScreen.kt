package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GenerationState
import com.example.ui.StoryViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioPrimaryLight
import com.example.ui.theme.StudioSecondary
import com.example.ui.theme.StudioSecondaryLight
import com.example.ui.theme.StudioTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: StoryViewModel,
    onNavigateToEditor: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Text prompt, 1: YouTube link

    var promptText by remember {
        mutableStateOf("مستكشف آثار يعثر على بوابة سحرية قديمة مخفية تحت رمال الصحراء الكبرى تأخذه إلى مدينة تطفو بين السحاب.")
    }
    var youtubeUrlText by remember {
        mutableStateOf("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    }
    var youtubeNotesText by remember {
        mutableStateOf("أريد قصة مستوحاة من الإيقاع السريع والتصوير الملحمي لهذا المقطع مع التركيز على الغموض.")
    }

    val styles = listOf("سينمائي كلاسيكي", "أنمي خيالي", "وثائقي تاريخي", "خيال علمي سايبربانك", "إثارة وغموض")
    var selectedStyle by remember { mutableStateOf(styles[0]) }

    val aspectRatios = listOf("16:9", "9:16", "1:1", "4:3")
    var selectedRatio by remember { mutableStateOf(aspectRatios[0]) }

    val voices = listOf("Kore" to "كور (حماسي)", "Puck" to "باك (شاب)", "Fenrir" to "فينرير (عميق)", "Aoede" to "أيودي (هادئ)")
    var selectedVoice by remember { mutableStateOf(voices[0].first) }

    var useSearchGrounding by remember { mutableStateOf(true) }
    var sceneCount by remember { mutableFloatStateOf(3f) }

    val genState by viewModel.generationState.collectAsState()
    val quickTitles by viewModel.quickTitles.collectAsState()
    val activeStory by viewModel.activeStory.collectAsState()
    val updateInfo by viewModel.appUpdateManager.updateInfo.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val providerManager = viewModel.providerManager
    val selectedImageProvider by providerManager.selectedImageProvider.collectAsState()
    val isGoogleOneLinked by providerManager.isGoogleOneLinked.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Studio Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF2E1065), Color(0xFF1E1B4B))
                        )
                    )
                    .border(1.dp, StudioPrimary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ستوديو راوي الذكي",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioSecondary.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AI 3.5 Pro",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioSecondaryLight
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اكتب فكرتك أو ضع رابط يوتيوب لنقوم بتحليله وإنتاج قصة سينمائية متكاملة بمشاهدها وصورها وموسيقاها.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "توليد الصور: ${selectedImageProvider.displayName.take(15)}",
                                    fontSize = 10.sp,
                                    color = StudioSecondaryLight
                                )
                            }
                            if (isGoogleOneLinked) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF14532D))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Google One متصل",
                                        fontSize = 10.sp,
                                        color = Color(0xFF86EFAC),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(StudioPrimary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = StudioSecondaryLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // In-App Update Available Banner
        if (updateInfo.hasUpdate) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261247)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(StudioPrimary, StudioSecondary))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.checkForUpdates() }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioSecondaryLight, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "تحديث داخلي جديد متاح (${updateInfo.latestVersion})",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "اضغط لتنزيل وتثبيت الإصدار الجديد مباشرة من داخل التطبيق",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.downloadAndInstallUpdate { success, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioSecondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تثبيت v2", color = Color.Black, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Story Banner with immediate reset option
        if (activeStory != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1735)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, StudioSecondary.copy(alpha = 0.5f)))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "المشروع الحالي: ${activeStory?.title?.take(24)}...",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "كل مشهد 10 ثوانٍ • جاهز للتصدير والعرض",
                                color = StudioSecondaryLight,
                                fontSize = 11.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.startNewStory() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("مسح وبدء جديد", color = Color(0xFFF87171), fontSize = 10.5.sp)
                            }

                            Button(
                                onClick = onNavigateToEditor,
                                colors = ButtonDefaults.buttonColors(containerColor = StudioSecondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("المحرر", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Mode Selector Tab Row
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, DarkBorder)))
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = StudioPrimary,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("كتابة وصف للقصة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        selectedContentColor = StudioPrimaryLight,
                        unselectedContentColor = Color(0xFF94A3B8)
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SmartDisplay, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تحليل رابط يوتيوب", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        selectedContentColor = StudioSecondaryLight,
                        unselectedContentColor = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Main Input Fields according to active tab
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (selectedTab == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "وصف القصة والسيناريو:",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )

                            // Quick title generator button using gemini-3.1-flash-lite
                            IconButton(
                                onClick = {
                                    viewModel.fetchQuickTitles(promptText.take(40))
                                },
                                modifier = Modifier.testTag("quick_title_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "اقتراح أفكار",
                                    tint = StudioSecondaryLight
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("story_idea_input")
                                .testTag("story_prompt_input"),
                            placeholder = { Text("اكتب تفاصيل القصة والشخصيات والأحداث المشوقة...", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StudioPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // If quick titles generated, show chips
                        AnimatedVisibility(visible = quickTitles.isNotEmpty()) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = "عناوين مقترحة (Flash-Lite):",
                                    fontSize = 12.sp,
                                    color = StudioSecondaryLight,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    quickTitles.forEach { title ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF271F42))
                                                .clickable {
                                                    promptText = "$title: $promptText"
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(text = title, fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // YouTube Link Remaker Tab
                        Text(
                            text = "رابط فيديو يوتيوب للتحليل والمحاكاة:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = youtubeUrlText,
                            onValueChange = { youtubeUrlText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("youtube_url_input"),
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null, tint = StudioSecondary)
                            },
                            placeholder = { Text("https://www.youtube.com/watch?v=...", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StudioSecondary,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "ملاحظات إضافية على النسخة الجديدة:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = youtubeNotesText,
                            onValueChange = { youtubeNotesText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .testTag("youtube_notes_input"),
                            placeholder = { Text("مثال: اجعل النسخة خيالية ذات نهاية غامضة...", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StudioSecondary,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Creative Controls (Style, Aspect Ratio, Voice, Search Grounding)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Style selection
                    Text(
                        text = "النمط البصري (Visual Style):",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        styles.forEach { style ->
                            FilterChip(
                                selected = selectedStyle == style,
                                onClick = { selectedStyle = style },
                                label = { Text(style, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Aspect Ratio selection
                    Text(
                        text = "نسبة العرض والارتفاع (Aspect Ratio):",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        aspectRatios.forEach { ratio ->
                            val label = when (ratio) {
                                "16:9" -> "16:9 سينما"
                                "9:16" -> "9:16 ريلز"
                                "1:1" -> "1:1 مربع"
                                else -> "4:3 كلاسيك"
                            }
                            FilterChip(
                                selected = selectedRatio == ratio,
                                onClick = { selectedRatio = ratio },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioSecondary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Voice selection
                    Text(
                        text = "الصوت الراوي (Gemini TTS):",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        voices.forEach { (voiceId, voiceLabel) ->
                            FilterChip(
                                selected = selectedVoice == voiceId,
                                onClick = { selectedVoice = voiceId },
                                label = { Text(voiceLabel, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioTertiary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scene count slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "عدد المشاهد السينمائية: ${sceneCount.toInt()} مشاهد",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Slider(
                        value = sceneCount,
                        onValueChange = { sceneCount = it },
                        valueRange = 2f..5f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioPrimary,
                            activeTrackColor = StudioPrimaryLight,
                            inactiveTrackColor = DarkBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Google Search Grounding toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF221A3B))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = StudioSecondaryLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "البحث المباشر (Google Search Grounding)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "تغذية القصة بمعلومات حية وحقائق دقيقة",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Switch(
                            checked = useSearchGrounding,
                            onCheckedChange = { useSearchGrounding = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StudioSecondary,
                                checkedTrackColor = Color(0xFF4A3410)
                            )
                        )
                    }
                }
            }
        }

        // Status messages and Progress Bar
        item {
            when (val state = genState) {
                is GenerationState.Loading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF261D42)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = StudioSecondary,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = state.message,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (state.progressFraction > 0f) {
                                Spacer(modifier = Modifier.height(10.dp))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { state.progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = StudioSecondary,
                                    trackColor = Color(0xFF3B2F5C)
                                )
                            }
                        }
                    }
                }
                is GenerationState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF451A1A)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = state.errorMessage,
                                color = Color(0xFFFCA5A5),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { viewModel.dismissState() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))
                            ) {
                                Text("إغلاق", fontSize = 12.sp)
                            }
                        }
                    }
                }
                is GenerationState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF143825)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.message,
                                color = Color(0xFF86EFAC),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = onNavigateToEditor,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("فتح المحرر", fontSize = 12.sp)
                            }
                        }
                    }
                }
                is GenerationState.Idle -> {}
            }
        }

        // Primary Action Generate Button
        item {
            val isLoading = genState is GenerationState.Loading
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        viewModel.createStoryFromPrompt(
                            prompt = promptText,
                            style = selectedStyle,
                            aspectRatio = selectedRatio,
                            sceneCount = sceneCount.toInt(),
                            useSearch = useSearchGrounding
                        )
                    } else {
                        viewModel.createStoryFromYouTube(
                            youtubeUrl = youtubeUrlText,
                            notes = youtubeNotesText,
                            style = selectedStyle,
                            aspectRatio = selectedRatio
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("create_button")
                    .testTag("create_story_button")
                    .testTag("generate_story_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioPrimary,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = StudioSecondaryLight,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (selectedTab == 0) "إنشاء" else "تحليل وإنشاء",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
