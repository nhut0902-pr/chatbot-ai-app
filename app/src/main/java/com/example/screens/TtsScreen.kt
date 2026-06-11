package com.example.screens

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.TtsHistoryItem
import com.example.providers.TtsViewModel

// Elegant Minimalist Premium Dark Theme colors
private val DarkBgGroup = Color(0xFF0F0F12)
private val DarkCardBg = Color(0xFF1B1B22)
private val LightTextPrimary = Color(0xFFF3F3F7)
private val SecondaryTextSlate = Color(0xFF9E9EA8)

// Accent Colors for Badges and Waves
private val AccentGreen = Color(0xFF10A37F)
private val AccentPurple = Color(0xFFAB80FF)
private val AccentCyan = Color(0xFF00D2FF)
private val AccentOrange = Color(0xFFFF9F1C)
private val StateRedAlert = Color(0xFFFF5E5E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsScreen(
    viewModel: TtsViewModel,
    modifier: Modifier = Modifier
) {
    val textInput by viewModel.textInput.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedGender by viewModel.selectedGender.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val playingItemId by viewModel.playingItemId.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberLazyListState()

    // Status / Lang dropdown status state
    var dropdownLangExpanded by remember { mutableStateOf(false) }
    var dropdownGenderExpanded by remember { mutableStateOf(false) }

    val languages = listOf("Vietnamese", "English", "Japanese", "Korean")
    val genders = listOf("female", "male")

    // Force focus release or scrolling on generation finish
    LaunchedEffect(historyList.size) {
        if (historyList.isNotEmpty()) {
            scrollState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgGroup),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            color = AccentGreen.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Face, // acts as speech / wave indicator
                                    contentDescription = "Voice API Logo",
                                    tint = AccentGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Voice TTS App",
                                color = LightTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Trình chuyển đổi văn bản sang giọng nói",
                                color = SecondaryTextSlate,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.clearAllHistory()
                                Toast.makeText(context, "Đã xóa toàn bộ lịch sử", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("clear_history_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xóa tất cả",
                                tint = StateRedAlert.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBgGroup,
                    titleContentColor = LightTextPrimary,
                    actionIconContentColor = LightTextPrimary
                )
            )
        },
        containerColor = DarkBgGroup
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- TOP PARAMETER & TEXT CONTROL BOX ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)),
                color = DarkCardBg,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Two side-by-side dropdown selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Language Selector
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dropdownLangExpanded = true },
                                color = DarkBgGroup,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Ngôn ngữ", color = SecondaryTextSlate, fontSize = 9.sp)
                                        Text(
                                            text = when (selectedLanguage) {
                                                "Vietnamese" -> "Tiếng Việt"
                                                "English" -> "English"
                                                "Japanese" -> "日本語"
                                                "Korean" -> "한국어"
                                                else -> selectedLanguage
                                            },
                                            color = LightTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown Lang",
                                        tint = SecondaryTextSlate,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = dropdownLangExpanded,
                                onDismissRequest = { dropdownLangExpanded = false },
                                modifier = Modifier
                                    .background(DarkCardBg)
                                    .width(160.dp)
                            ) {
                                languages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = when (lang) {
                                                    "Vietnamese" -> "Tiếng Việt (vi)"
                                                    "English" -> "English (en)"
                                                    "Japanese" -> "日本語 (ja)"
                                                    "Korean" -> "한국어 (ko)"
                                                    else -> lang
                                                },
                                                color = if (lang == selectedLanguage) AccentGreen else LightTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.onLanguageChanged(lang)
                                            dropdownLangExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Gender Voice Selector
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dropdownGenderExpanded = true },
                                color = DarkBgGroup,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Bộ đọc", color = SecondaryTextSlate, fontSize = 9.sp)
                                        Text(
                                            text = if (selectedGender == "female") "Nữ (Female)" else "Nam (Male)",
                                            color = LightTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown Gender",
                                        tint = SecondaryTextSlate,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = dropdownGenderExpanded,
                                onDismissRequest = { dropdownGenderExpanded = false },
                                modifier = Modifier
                                    .background(DarkCardBg)
                                    .width(160.dp)
                            ) {
                                genders.forEach { gender ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (gender == "female") "Nữ (Female)" else "Nam (Male)",
                                                color = if (gender == selectedGender) AccentGreen else LightTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = if (gender == selectedGender) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.onGenderChanged(gender)
                                            dropdownGenderExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Text input field
                    TextField(
                        value = textInput,
                        onValueChange = { viewModel.onTextInputChanged(it) },
                        placeholder = {
                            Text(
                                text = "Nhập văn bản cần chuyển đổi thành giọng nói tại đây...",
                                color = SecondaryTextSlate.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 150.dp)
                            .testTag("text_to_speech_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkBgGroup,
                            unfocusedContainerColor = DarkBgGroup,
                            disabledContainerColor = DarkBgGroup,
                            focusedTextColor = LightTextPrimary,
                            unfocusedTextColor = LightTextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = AccentGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        )
                    )

                    // Generate speech Button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.generateVoice()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_generate_voice_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            disabledContainerColor = AccentGreen.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(25.dp),
                        enabled = textInput.trim().isNotEmpty() && !isLoading
                    ) {
                        if (isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Đang kết xuất giọng nói...",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Tạo Giọng Nói",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // --- LOCAL CACHED AUDIO LIST TITLE & SUB ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Lịch sử chuyển đổi (Tối đa 10)",
                    color = LightTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (historyList.isNotEmpty()) {
                    Text(
                        text = "${historyList.size}/10 tệp",
                        color = SecondaryTextSlate,
                        fontSize = 11.sp
                    )
                }
            }

            // --- SCROLLABLE HISTORICAL AUDIOS ---
            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyTtsState()
                }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = historyList, key = { it.id }) { item ->
                        BeautifulHistoryCard(
                            item = item,
                            isPlaying = playingItemId == item.id,
                            onPlayClick = {
                                if (playingItemId == item.id) {
                                    viewModel.stopAudio()
                                } else {
                                    viewModel.playAudio(item)
                                }
                            },
                            onDeleteClick = {
                                viewModel.deleteHistoryItem(item)
                                Toast.makeText(context, "Đã xóa tệp ghi âm", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // Dialog for showing standard error message to the user gracefully
            if (errorMessage != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissError() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error Logo",
                                tint = StateRedAlert,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Có lỗi xảy ra", color = LightTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text(
                            text = errorMessage ?: "",
                            color = LightTextPrimary.copy(alpha = 0.9f),
                            fontSize = 13.5.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("Đã hiểu", color = AccentGreen, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = DarkCardBg,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyTtsState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(76.dp)
                .background(HighlightGradBrush(), shape = CircleShape),
            color = Color.Transparent,
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Info, // custom welcome decoration icon
                    contentDescription = null,
                    tint = LightTextPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Hộp âm thanh trống",
                color = LightTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Nhập văn bản phía trên và nhấn nút 'Tạo Giọng Nói' để bắt đầu phát và lưu trữ âm thanh giọng đọc nhé!",
                color = SecondaryTextSlate,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun BeautifulHistoryCard(
    item: TtsHistoryItem,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val languageColor = when (item.language) {
        "Vietnamese" -> AccentGreen
        "English" -> AccentPurple
        "Japanese" -> AccentOrange
        "Korean" -> AccentCyan
        else -> AccentGreen
    }

    val languageLabel = when (item.language) {
        "Vietnamese" -> "Tiếng Việt"
        "English" -> "Tiếng Anh"
        "Japanese" -> "Tiếng Nhật"
        "Korean" -> "Tiếng Hàn"
        else -> item.language
    }

    val genderLabel = if (item.gender == "female") "Giọng Nữ" else "Giọng Nam"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = DarkCardBg,
        border = BorderStroke(1.dp, if (isPlaying) languageColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Language badge
                    Surface(
                        color = languageColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = languageLabel,
                            color = languageColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Gender badge
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = genderLabel,
                            color = LightTextPrimary.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Friendly responsive date/time stamp
                val relativeTime = DateUtils.getRelativeTimeSpanString(
                    item.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()

                Text(
                    text = relativeTime,
                    color = SecondaryTextSlate,
                    fontSize = 10.sp
                )
            }

            // Input Text Query block
            Text(
                text = item.text,
                color = LightTextPrimary,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            // Action row (Play / stop, dynamic soundwave analyzer animation, delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onPlayClick() },
                    color = if (isPlaying) StateRedAlert.copy(alpha = 0.15f) else languageColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Clear else Icons.Default.PlayArrow, // Stop vs Play
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = if (isPlaying) StateRedAlert else languageColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Beautiful animating audio wave if playing, otherwise a flat cached audio path text
                if (isPlaying) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(16) { index ->
                            AnimatingWaveBar(delayMillis = index * 100, color = languageColor)
                        }
                    }
                } else {
                    Text(
                        text = "File: ${item.voice}.mp3",
                        color = SecondaryTextSlate.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Delete Button item
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa bản ghi",
                        tint = SecondaryTextSlate.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatingWaveBar(delayMillis: Int, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")
    
    // Smoothly animate the vertical height of bars to create voice soundwaves
    val heightScale by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heightScale"
    )

    Box(
        modifier = Modifier
            .width(3.dp)
            .height((16 * heightScale).dp)
            .clip(RoundedCornerShape(1.dp))
            .background(color)
    )
}

@Composable
fun HighlightGradBrush(): Brush {
    return Brush.radialGradient(
        colors = listOf(
            AccentPurple.copy(alpha = 0.8f),
            AccentGreen.copy(alpha = 0.3f),
            Color.Transparent
        )
    )
}
