package com.example.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.config.AppConfig
import com.example.models.AiModel
import com.example.models.Message
import com.example.models.MessageRole
import com.example.models.ProviderType
import com.example.providers.ChatViewModel

// ChatGPT Sleek Dark Palette
private val ChatGPTDarkBg = Color(0xFF171717)
private val ChatGPTItemUser = Color(0xFF2F2F2F)
private val ChatGPTItemBot = Color(0xFF1E1E1E)
private val ChatGPTPanelBg = Color(0xFF212121)
private val ChatGPTPurpleAccent = Color(0xFFAB80FF)
private val ChatGPTGreenAccent = Color(0xFF10A37F)
private val StateRed = Color(0xFFFF5252)
private val SlateTextSecondary = Color(0xFFB0B0B0)

// Modern Code Highlight Palette
private val CodeHeaderBg = Color(0xFF2D2D2D)
private val CodeBodyBg = Color(0xFF0F0F0F)
private val CodeKeywordColor = Color(0xFFF92672)
private val CodeStringColor = Color(0xFFA6E22E)
private val CodeNumberColor = Color(0xFFAE81FF)
private val CodeCommentColor = Color(0xFF75715E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messagesState.collectAsStateWithLifecycle()
    val selectedModelId by viewModel.selectedModel.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val apiError by viewModel.apiError.collectAsStateWithLifecycle()
    val showApiKeyError by viewModel.showApiKeyError.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Chat, 1 = Settings
    var inputPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Automatically scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ChatGPTDarkBg),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = ChatGPTGreenAccent.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (activeTab == 0) Icons.Default.Face else Icons.Default.Settings,
                                    contentDescription = "Logo",
                                    tint = ChatGPTGreenAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (activeTab == 0) "Cerebras & NVIDIA" else "Cấu hình tài khoản",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (activeTab == 0) "Multi-provider Chat Hub" else "Kiểm tra quyền truy cập API",
                                fontSize = 11.sp,
                                color = SlateTextSecondary
                            )
                        }
                    }
                },
                actions = {
                    if (activeTab == 0) {
                        // Reset Chat
                        IconButton(
                            onClick = { 
                                viewModel.clearChatHistory() 
                                Toast.makeText(context, "Hội thoại đã được làm sạch", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Dọn sạch lịch sử",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ChatGPTDarkBg,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ChatGPTPanelBg,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Chat") },
                    label = { Text("Trò chuyện") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ChatGPTGreenAccent,
                        selectedTextColor = ChatGPTGreenAccent,
                        indicatorColor = ChatGPTGreenAccent.copy(alpha = 0.15f),
                        unselectedIconColor = SlateTextSecondary,
                        unselectedTextColor = SlateTextSecondary
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Cấu đặt") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ChatGPTGreenAccent,
                        selectedTextColor = ChatGPTGreenAccent,
                        indicatorColor = ChatGPTGreenAccent.copy(alpha = 0.15f),
                        unselectedIconColor = SlateTextSecondary,
                        unselectedTextColor = SlateTextSecondary
                    )
                )
            }
        },
        containerColor = ChatGPTDarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeTab == 0) {
                // --- CHAT TAB VIEW ---
                Column(modifier = Modifier.fillMaxSize()) {
                    // Model Selection dropdown widget at the top of conversation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ChatGPTDarkBg)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ModelCustomDropdownSelector(
                            selectedModelId = selectedModelId,
                            onModelSelected = { viewModel.selectModel(it) }
                        )
                    }

                    // Messages List or Welcome Grid
                    if (messages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyWelcomeState(
                                selectedModelId = selectedModelId,
                                onQuerySelected = { inputPrompt = it }
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(messages) { message ->
                                BeautifulMessageCard(
                                    message = message,
                                    onCopyClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Cerebras AI reply", message.content)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Đã sao chép nội dung!", Toast.LENGTH_SHORT).show()
                                    },
                                    onRegenerateClick = {
                                        viewModel.regenerateMessage()
                                        Toast.makeText(context, "Đang tạo lại câu trả lời...", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            if (isLoading) {
                                item {
                                    TypewriterThinkingBubble()
                                }
                            }
                        }
                    }

                    // Input Prompt Controller
                    Surface(
                        color = ChatGPTDarkBg,
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(ChatGPTPanelBg, RoundedCornerShape(26.dp))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = inputPrompt,
                                onValueChange = { inputPrompt = it },
                                placeholder = {
                                    Text(
                                        text = "Nhập tin nhắn đến Cerebras / NVIDIA...",
                                        color = SlateTextSecondary,
                                        fontSize = 14.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("message_input")
                                    .padding(vertical = 4.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    errorContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = ChatGPTPurpleAccent
                                ),
                                maxLines = 6,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Send
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (inputPrompt.trim().isNotEmpty() && !isLoading) {
                                            viewModel.sendMessage(inputPrompt)
                                            inputPrompt = ""
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        }
                                    }
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            val validSubmit = inputPrompt.trim().isNotEmpty() && !isLoading
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (validSubmit) ChatGPTGreenAccent else Color.White.copy(alpha = 0.12f))
                                    .clickable(
                                        enabled = validSubmit,
                                        onClick = {
                                            viewModel.sendMessage(inputPrompt)
                                            inputPrompt = ""
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        }
                                    )
                                    .testTag("submit_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Gửi",
                                    tint = if (validSubmit) Color.White else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // --- SETTINGS TAB VIEW ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ChatGPTDarkBg)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        Text(
                            text = "Trạng thái Nhà Cung Cấp (Provider Status)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // Cerebras block
                    item {
                        val isCerebrasOk = AppConfig.isCerebrasAvailable()
                        ProviderStatusCard(
                            providerName = "Cerebras API Provider",
                            isAvailable = isCerebrasOk,
                            prefixHint = "csk-...",
                            envKey = "CEREBRAS_API_KEY",
                            details = "Cerebras CS-3 là bộ tăng tốc suy luận AI có thông lượng cực cao, cung cấp tốc độ phản hồi tính bằng mili-giây cho các LLM mã nguồn mở."
                        )
                    }

                    // NVIDIA NIM block
                    item {
                        val isNvidiaOk = AppConfig.isNvidiaAvailable()
                        ProviderStatusCard(
                            providerName = "NVIDIA NIM SDK Provider",
                            isAvailable = isNvidiaOk,
                            prefixHint = "nvapi-...",
                            envKey = "NVIDIA_API_KEY",
                            details = "NVIDIA NIM (NVIDIA Inference Microservice) cung cấp các mô hình ngôn ngữ lớn tiên tiến chạy trên cơ sở hạ tầng đám mây tối ưu hóa của NVIDIA."
                        )
                    }

                    // Custom model descriptions list
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Thông tin về các dòng mô hình",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ChatGPTPanelBg),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ModelDetailRow(name = "gpt-oss-120b", description = "Cerebras core high-speed instruction fine-tune LLM.")
                                ModelDetailRow(name = "zai-glm-4.7", description = "Đại diện hiệu năng cao đa tác vụ xử lý tiếng Việt giỏi.")
                                ModelDetailRow(name = "GLM-5", description = "Dòng flagship xuất sắc của NVIDIA NIM với 50B parameters kịch trần.")
                                ModelDetailRow(name = "Kimi K2.6 & Qwen3.6", description = "Đột phá lý luận logic, toán học và coding từ NVIDIA API hub.")
                            }
                        }
                    }
                }
            }

            // Standard runtime network error warnings
            if (apiError != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissError() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Lỗi",
                                tint = StateRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Lỗi kết nối API", color = Color.White)
                        }
                    },
                    text = {
                        Text(
                            text = apiError ?: "",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("Đóng", color = ChatGPTPurpleAccent)
                        }
                    },
                    containerColor = ChatGPTItemUser,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // CRITICAL CONFIGURATION BLOCKED MODAL FOR BOTH APIs
            if (showApiKeyError) {
                AlertDialog(
                    onDismissRequest = { /* System-mandated: do not auto-close unless set */ },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Warning",
                                tint = StateRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Yêu Cầu Cài Đặt API Key", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        val activeModel = AiModel.findById(selectedModelId)
                        val expectedKey = if (activeModel.provider == ProviderType.CEREBRAS) "CEREBRAS_API_KEY" else "NVIDIA_API_KEY"
                        val expectedPrefix = if (activeModel.provider == ProviderType.CEREBRAS) "csk-" else "nvapi-"

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Mô hình '${activeModel.name}' yêu cầu nhà cung cấp ${activeModel.provider}.",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Biến môi trường mã khóa '$expectedKey' chưa hoàn hảo hoặc bị thiếu (phải có dạng tiền tố '$expectedPrefix').",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                            Surface(
                                color = ChatGPTDarkBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "HƯỚNG DẪN CONFIG:\n1. Mở panel Secrets ở góc dưới AI Studio UI.\n2. Thiết lập key: $expectedKey\n3. Tránh hardcode trực tiếp vào code.",
                                        fontSize = 11.sp,
                                        color = SlateTextSecondary,
                                        lineHeight = 15.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { 
                                viewModel.checkApiKeyStatus()
                                viewModel.dismissApiKeyError()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChatGPTGreenAccent)
                        ) {
                            Text("Bỏ qua & Xem ứng dụng", color = Color.White)
                        }
                    },
                    containerColor = ChatGPTItemUser,
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }
    }
}

@Composable
fun ProviderStatusCard(
    providerName: String,
    isAvailable: Boolean,
    prefixHint: String,
    envKey: String,
    details: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ChatGPTPanelBg),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (isAvailable) ChatGPTGreenAccent.copy(alpha = 0.4f) else StateRed.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = providerName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                // Label badge status
                Surface(
                    color = if (isAvailable) ChatGPTGreenAccent.copy(alpha = 0.15f) else StateRed.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isAvailable) ChatGPTGreenAccent else StateRed)
                        )
                        Text(
                            text = if (isAvailable) "API Available" else "API Missing",
                            color = if (isAvailable) ChatGPTGreenAccent else StateRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = details,
                color = SlateTextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Biến ENV: ",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$envKey ($prefixHint)",
                    color = ChatGPTPurpleAccent,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ModelDetailRow(name: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ChatGPTGreenAccent,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(14.dp)
        )
        Column {
            Text(text = name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = description, color = SlateTextSecondary, fontSize = 11.5.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
fun ModelCustomDropdownSelector(
    selectedModelId: String,
    onModelSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentModel = remember(selectedModelId) { AiModel.findById(selectedModelId) }

    Box {
        Surface(
            color = ChatGPTItemUser,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Provider Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (currentModel.provider == ProviderType.CEREBRAS) ChatGPTGreenAccent else ChatGPTPurpleAccent)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = currentModel.provider.name,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = currentModel.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown model",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(ChatGPTPanelBg)
                .width(280.dp)
        ) {
            // Group 1: Cerebras
            DropdownMenuItem(
                text = {
                    Text(
                        text = "CEREBRAS HIGH SPEED INFERENCE",
                        color = ChatGPTGreenAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                onClick = {},
                enabled = false
            )

            AiModel.ALL_MODELS.filter { it.provider == ProviderType.CEREBRAS }.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (model.id == selectedModelId) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = ChatGPTGreenAccent, modifier = Modifier.size(14.dp))
                            } else {
                                Spacer(modifier = Modifier.width(14.dp))
                            }
                            Text(text = model.name, color = Color.White, fontSize = 13.5.sp)
                        }
                    },
                    onClick = {
                        onModelSelected(model.id)
                        expanded = false
                    }
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

            // Group 2: NVIDIA NIM
            DropdownMenuItem(
                text = {
                    Text(
                        text = "NVIDIA NIM ACCELERATED CLOUD",
                        color = ChatGPTPurpleAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                onClick = {},
                enabled = false
            )

            AiModel.ALL_MODELS.filter { it.provider == ProviderType.NVIDIA }.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (model.id == selectedModelId) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = ChatGPTPurpleAccent, modifier = Modifier.size(14.dp))
                            } else {
                                Spacer(modifier = Modifier.width(14.dp))
                            }
                            Text(text = model.name, color = Color.White, fontSize = 13.5.sp)
                        }
                    },
                    onClick = {
                        onModelSelected(model.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyWelcomeState(
    selectedModelId: String,
    onQuerySelected: (String) -> Unit
) {
    val model = remember(selectedModelId) { AiModel.findById(selectedModelId) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = if (model.provider == ProviderType.CEREBRAS) ChatGPTGreenAccent.copy(alpha = 0.15f) else ChatGPTPurpleAccent.copy(alpha = 0.15f),
            shape = CircleShape,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (model.provider == ProviderType.CEREBRAS) ChatGPTGreenAccent else ChatGPTPurpleAccent,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Tôi có thể giúp ích gì cho bạn?",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = Color.White
            )
            Text(
                text = "Model: ${model.name} (${model.provider})",
                color = SlateTextSecondary,
                fontSize = 13.sp
            )
        }

        val suggestions = listOf(
            "Hướng dẫn viết thuật toán Quick Sort bằng Python",
            "Viết nội dung email gửi khách hàng khảo sát chất lượng",
            "Sức mạnh của trí tuệ nhân tạo đối với lập trình là gì?"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            suggestions.forEach { suggestion ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ChatGPTItemUser),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuerySelected(suggestion) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = suggestion,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Chọn",
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BeautifulMessageCard(
    message: Message,
    onCopyClick: () -> Unit,
    onRegenerateClick: () -> Unit
) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                color = if (AiModel.findById(message.model).provider == ProviderType.CEREBRAS) ChatGPTGreenAccent else ChatGPTPurpleAccent,
                shape = CircleShape,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "AI",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 285.dp)
        ) {
            Surface(
                color = if (isUser) ChatGPTItemUser else ChatGPTItemBot,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                border = if (!isUser) BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f)) else null
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                    if (!isUser) {
                        val parsedModel = remember(message.model) { AiModel.findById(message.model) }
                        Text(
                            text = "${parsedModel.name} • ${parsedModel.provider}",
                            color = if (parsedModel.provider == ProviderType.CEREBRAS) ChatGPTGreenAccent else ChatGPTPurpleAccent,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // Handles markdown text block and code syntax highlight neatly
                    MarkdownTextVisualizer(text = message.content)
                }
            }

            // Quick actions under message
            if (!isUser && message.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCopyClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share, // acts as Copy icon
                            contentDescription = "Sao chép",
                            tint = SlateTextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    IconButton(
                        onClick = onRegenerateClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Tạo lại",
                            tint = SlateTextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

// Simple Parser and High-Performance visualizer of code blocks inside messaging
sealed class BlockPart {
    data class Text(val annotatedString: AnnotatedString) : BlockPart()
    data class Code(val code: String, val language: String) : BlockPart()
}

@Composable
fun MarkdownTextVisualizer(text: String) {
    val blockParts = remember(text) {
        val parts = mutableListOf<BlockPart>()
        val matches = text.split("```")
        for (i in matches.indices) {
            val chunk = matches[i]
            if (i % 2 == 1) {
                // Code block chunk. Format is typically "language\ncode" or just "\ncode"
                val lines = chunk.split("\n", limit = 2)
                val lang = if (lines.isNotEmpty()) lines[0].trim() else "code"
                val actualCode = if (lines.size > 1) lines[1] else ""
                parts.add(BlockPart.Code(actualCode.trimEnd(), lang))
            } else {
                if (chunk.isNotEmpty()) {
                    // Regular text chunk. Parse basic bold notations (**text**)
                    val textWithBold = buildAnnotatedString {
                        val subchunks = chunk.split("**")
                        for (j in subchunks.indices) {
                            val subchunk = subchunks[j]
                            if (j % 2 == 1) {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color.White)) {
                                    append(subchunk)
                                }
                            } else {
                                append(subchunk)
                            }
                        }
                    }
                    parts.add(BlockPart.Text(textWithBold))
                }
            }
        }
        parts
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blockParts.forEach { part ->
            when (part) {
                is BlockPart.Text -> {
                    Text(
                        text = part.annotatedString,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp
                    )
                }
                is BlockPart.Code -> {
                    BlockCodeStructure(code = part.code, language = part.language)
                }
            }
        }
    }
}

@Composable
fun BlockCodeStructure(code: String, language: String) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CodeBodyBg)
    ) {
        // Code Header Block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CodeHeaderBg)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.uppercase(),
                color = SlateTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Row(
                modifier = Modifier.clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Code Block", code)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Đã chép mã nguồn!", Toast.LENGTH_SHORT).show()
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share, // acts as copy icon prefix
                    contentDescription = null,
                    tint = SlateTextSecondary,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "Sao chép mã",
                    color = SlateTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Highlighted Code Panel
        Box(modifier = Modifier.padding(12.dp)) {
            val highlightedText = remember(code) {
                buildAnnotatedString {
                    val tokens = code.split(Regex("(?<=\\b)|(?=\\b)|(?<=\\W)|(?=\\W)"))
                    val keywords = setOf("val", "var", "fun", "class", "import", "package", "return", "if", "else", "for", "while", "void", "public", "private", "protected", "override", "null", "true", "false", "String", "Int", "Boolean", "Double")

                    var inString = false
                    for (token in tokens) {
                        when {
                            token == "\"" || token == "'" -> {
                                withStyle(SpanStyle(color = CodeStringColor)) { append(token) }
                                inString = !inString
                            }
                            inString -> {
                                withStyle(SpanStyle(color = CodeStringColor)) { append(token) }
                            }
                            keywords.contains(token) -> {
                                withStyle(SpanStyle(color = CodeKeywordColor, fontWeight = FontWeight.Bold)) { append(token) }
                            }
                            token.toIntOrNull() != null -> {
                                withStyle(SpanStyle(color = CodeNumberColor)) { append(token) }
                            }
                            token.startsWith("//") -> {
                                withStyle(SpanStyle(color = CodeCommentColor)) { append(token) }
                            }
                            else -> {
                                withStyle(SpanStyle(color = Color(0xFFDCDCDC))) { append(token) }
                            }
                        }
                    }
                }
            }

            Text(
                text = highlightedText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun TypewriterThinkingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingAnim")
    
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 900; 0.3f at 0; 1f at 200; 0.3f at 400 },
            repeatMode = RepeatMode.Restart
        ),
        label = "D1"
    )

    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 900; 0.3f at 150; 1f at 350; 0.3f at 550 },
            repeatMode = RepeatMode.Restart
        ),
        label = "D2"
    )

    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 900; 0.3f at 300; 1f at 500; 0.3f at 700 },
            repeatMode = RepeatMode.Restart
        ),
        label = "D3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = ChatGPTGreenAccent,
            shape = CircleShape,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "AI thinking",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Surface(
            color = ChatGPTItemBot,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 100.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = dot1Scale)))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = dot2Scale)))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = dot3Scale)))
            }
        }
    }
}
