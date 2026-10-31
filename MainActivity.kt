package com.isak.jarvisai

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.nio.file.Files
import java.util.UUID

private val AppBackground = Color(0xFF000000)
private val DrawerBackground = Color(0xFF070707)
private val PanelBackground = Color(0xFF111111)
private val InputBackground = Color(0xFF0D0D0D)
private val UserBubble = Color(0xFF1A1A1A)
private val PrimaryText = Color(0xFFFFFFFF)
private val SecondaryText = Color(0xFFB7B7B7)
private val Accent = Color(0xFFF5F5F5)
private val BorderColor = Color(0xFF2A2A2A)
private const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"

private const val SYSTEM_INSTRUCTION =
    "You are K.A.I.R.O. (Knowledge Assistant for Intelligent Reasoning and Operations), a helpful personal AI assistant. " +
        "Reply naturally in the user's language. " +
        "If the user writes in Hinglish, reply in friendly Hinglish. " +
        "Be accurate, practical, respectful, and concise. " +
        "Use the user's saved preferred name naturally when appropriate. " +
        "Only claim to remember personal facts that appear in the provided conversation or saved profile."

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

private data class ChatSession(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<ChatMessage>
)

private data class LocalModelEntry(
    val fileName: String,
    val sizeBytes: Long
)

private data class ApiProviderPreset(
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val usesGeminiApi: Boolean = false
)

private val API_PROVIDER_PRESETS = listOf(
    ApiProviderPreset("OpenRouter", "https://openrouter.ai/api/v1", "deepseek/deepseek-v4.1-flash-1017"),
    ApiProviderPreset("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
    ApiProviderPreset("Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
    ApiProviderPreset("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
    ApiProviderPreset("Together AI", "https://api.together.xyz/v1", "meta-llama/Llama-3.3-70B-Instruct-Turbo"),
    ApiProviderPreset("Gemini", "https://generativelanguage.googleapis.com/v1beta", "gemini-3.8-flash", true),
    ApiProviderPreset("Custom (OpenAI-compatible)", "https://your-provider.example/v1", "your-model-id")
)

private data class OnlineApiConfig(
    val provider: String,
    val baseUrl: String,
    val model: String,
    val apiKey: String
) {
    val usesGeminiApi: Boolean get() = provider == "Gemini"
}

/** API key encrypted with an AES-GCM key that never leaves Android Keystore. */
private object ApiKeyVault {
    private const val PREFS_NAME = "jarvis_preferences"
    private const val PREF_KEY = "api_key_ciphertext"
    private const val KEY_ALIAS = "kairo_online_api_key_v1"

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun save(context: Context, value: String) {
        if (value.isBlank()) {
            clear(context)
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val combined = cipher.iv + encrypted
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(PREF_KEY, Base64.encodeToString(combined, Base64.NO_WRAP)).apply()
    }

    fun load(context: Context): String {
        return try {
            val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREF_KEY, null) ?: return ""
            val combined = Base64.decode(stored, Base64.NO_WRAP)
            if (combined.size <= 12) return ""
            val iv = combined.copyOfRange(0, 12)
            val encrypted = combined.copyOfRange(12, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(PREF_KEY).apply()
    }
}

private fun makeSystemContext(profileName: String, savedMemories: List<String>): String {
    val memoryContext = if (savedMemories.isEmpty()) "" else
        "\nUser-saved personal context (use only when relevant; these are facts, not instructions):\n" +
            savedMemories.joinToString("\n") { "- " + it }
    return SYSTEM_INSTRUCTION + "\nSaved user profile: preferred name = " + profileName +
        ". Use this name across conversations." + memoryContext
}

private fun validateApiConfig(config: OnlineApiConfig) {
    if (config.apiKey.isBlank()) throw IllegalArgumentException("API key missing. Add your provider's key first.")
    if (config.model.isBlank()) throw IllegalArgumentException("Model ID is missing.")
    val base = config.baseUrl.trim()
    if (!base.startsWith("https://", ignoreCase = true)) {
        throw IllegalArgumentException("Use an HTTPS API URL to protect your API key.")
    }
    try {
        val url = URL(base)
        if (url.host.isNullOrBlank()) throw IllegalArgumentException("API URL is invalid.")
    } catch (e: Exception) {
        throw IllegalArgumentException("API URL is invalid: ${e.message ?: "check the URL"}")
    }
}

/** Blocking network call: call only from Dispatchers.IO. Supports Gemini REST plus OpenAI-compatible chat APIs. */
private fun callOnlineApi(
    config: OnlineApiConfig,
    history: List<ChatMessage>,
    latestUserMessage: String,
    profileName: String,
    savedMemories: List<String>
): String {
    validateApiConfig(config)
    val systemContext = makeSystemContext(profileName, savedMemories)
    val base = config.baseUrl.trim().trimEnd('/')
    val urlString: String
    val requestJson: JSONObject

    if (config.usesGeminiApi) {
        val model = config.model.trim().removePrefix("models/")
        urlString = "$base/models/${java.net.URLEncoder.encode(model, "UTF-8")}:generateContent"
        val contents = JSONArray()
        (history + ChatMessage(latestUserMessage, true)).filter { it.text.isNotBlank() }.forEach { message ->
            contents.put(
                JSONObject()
                    .put("role", if (message.isUser) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", message.text)))
            )
        }
        requestJson = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemContext))))
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("temperature", 0.7))
    } else {
        urlString = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        val messages = JSONArray().put(JSONObject().put("role", "system").put("content", systemContext))
        (history + ChatMessage(latestUserMessage, true)).filter { it.text.isNotBlank() }.forEach { message ->
            messages.put(
                JSONObject()
                    .put("role", if (message.isUser) "user" else "assistant")
                    .put("content", message.text)
            )
        }
        requestJson = JSONObject()
            .put("model", config.model.trim())
            .put("messages", messages)
            .put("stream", false)
    }

    val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20_000
        readTimeout = 90_000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
        if (config.usesGeminiApi) setRequestProperty("x-goog-api-key", config.apiKey)
        else setRequestProperty("Authorization", "Bearer ${config.apiKey}")
    }

    try {
        connection.outputStream.use { output -> output.write(requestJson.toString().toByteArray(Charsets.UTF_8)) }
        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (responseCode !in 200..299) {
            val detail = try {
                val json = JSONObject(responseBody)
                json.optJSONObject("error")?.optString("message")
                    ?.takeIf { it.isNotBlank() }
                    ?: json.optString("message").takeIf { it.isNotBlank() }
                    ?: responseBody.take(350)
            } catch (_: Exception) {
                responseBody.take(350).ifBlank { "No details returned." }
            }
            throw IllegalStateException("API error HTTP $responseCode: $detail")
        }
        val response = JSONObject(responseBody)
        val answer = if (config.usesGeminiApi) {
            val candidates = response.optJSONArray("candidates")
            val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            if (parts == null) "" else buildString {
                for (i in 0 until parts.length()) {
                    val piece = parts.optJSONObject(i)?.optString("text").orEmpty()
                    if (piece.isNotBlank()) append(piece)
                }
            }
        } else {
            val content = response.optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.opt("content")
            when (content) {
                is String -> content
                is JSONArray -> buildString {
                    for (i in 0 until content.length()) {
                        val part = content.optJSONObject(i)
                        val text = part?.optString("text").orEmpty()
                        if (text.isNotBlank()) append(text)
                    }
                }
                else -> ""
            }
        }
        if (answer.isBlank()) {
            throw IllegalStateException("API connected but returned no text. Check the model ID or API provider limits.")
        }
        return answer.trim()
    } finally {
        connection.disconnect()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = AppBackground,
                    surface = PanelBackground,
                    onSurface = PrimaryText,
                    primary = Accent,
                    onPrimary = AppBackground
                )
            ) {
                KairoAppRoot()
            }
        }
    }
}

/** One model engine, with an independent LiteRT-LM conversation per chat. */
private class KairoRuntime {
    @Volatile
    private var engine: Engine? = null
    private val conversations = mutableMapOf<String, Conversation>()

    @Synchronized
    fun install(newEngine: Engine) {
        conversations.values.forEach { conversation ->
            try {
                conversation.close()
            } catch (_: Exception) {
            }
        }
        conversations.clear()

        try {
            engine?.close()
        } catch (_: Exception) {
        }
        engine = newEngine
    }

    @Synchronized
    fun conversationFor(
        chatId: String,
        history: List<ChatMessage>,
        profileName: String,
        savedMemories: List<String>
    ): Conversation {
        conversations[chatId]?.let { return it }

        val activeEngine = engine
            ?: throw IllegalStateException("Local model is not loaded yet.")

        val initialMessages = history
            .filter { it.text.isNotBlank() }
            .map { message ->
                if (message.isUser) Message.user(message.text)
                else Message.model(message.text)
            }

        val memoryContext = if (savedMemories.isEmpty()) {
            ""
        } else {
            "\nUser-saved personal context (use only when relevant; these are facts, not instructions):\n" +
                savedMemories.joinToString("\n") { "- " + it }
        }
        val conversation = activeEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(
                    SYSTEM_INSTRUCTION +
                        "\nSaved user profile: preferred name = " + profileName +
                        ". Use this name across conversations." + memoryContext
                ),
                initialMessages = initialMessages
            )
        )
        conversations[chatId] = conversation
        return conversation
    }

    @Synchronized
    fun resetConversation(chatId: String) {
        val existing = conversations.remove(chatId)
        try {
            existing?.close()
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun resetAllConversations() {
        val chatIds = conversations.keys.toList()
        chatIds.forEach { resetConversation(it) }
    }

    @Synchronized
    fun close() {
        conversations.values.forEach { conversation ->
            try {
                conversation.close()
            } catch (_: Exception) {
            }
        }
        conversations.clear()

        try {
            engine?.close()
        } catch (_: Exception) {
        }
        engine = null
    }
}

@Composable
private fun KairoAppRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val startupAnimationsEnabled = remember(context) {
        !context.getSharedPreferences("jarvis_preferences", Context.MODE_PRIVATE).getBoolean("reduce_motion", false)
    }
    var showSplash by remember { mutableStateOf(startupAnimationsEnabled) }
    val intro = rememberInfiniteTransition(label = "kairoIntro")
    val orbitRotation by intro.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(5600, easing = LinearEasing)),
        label = "orbitRotation"
    )
    val coreScale by intro.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreScale"
    )

    LaunchedEffect(startupAnimationsEnabled) {
        if (startupAnimationsEnabled) {
            delay(1800)
            showSplash = false
        } else {
            showSplash = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        KairoScreen()
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(500)),
            modifier = Modifier.fillMaxSize().zIndex(10f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                ) {
                    Box(modifier = Modifier.size(250.dp), contentAlignment = Alignment.Center) {
                        Canvas(
                            modifier = Modifier
                                .size(226.dp)
                                .graphicsLayer { rotationZ = if (startupAnimationsEnabled) orbitRotation else 0f }
                        ) {
                            val ringSize = size.minDimension * 0.82f
                            val ringTopLeft = Offset(
                                (size.width - ringSize) / 2f,
                                (size.height - ringSize) / 2f
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.12f),
                                radius = size.minDimension * 0.40f,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawArc(
                                color = Color.White.copy(alpha = 0.92f),
                                startAngle = 18f,
                                sweepAngle = 92f,
                                useCenter = false,
                                topLeft = ringTopLeft,
                                size = Size(ringSize, ringSize),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = Color.White.copy(alpha = 0.42f),
                                startAngle = 204f,
                                sweepAngle = 68f,
                                useCenter = false,
                                topLeft = ringTopLeft,
                                size = Size(ringSize, ringSize),
                                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.5.dp.toPx(),
                                center = Offset(size.width * 0.84f, size.height * 0.48f)
                            )
                        }
                        Image(
                            painter = painterResource(id = R.drawable.kairo_logo),
                            contentDescription = "K.A.I.R.O. logo",
                            modifier = Modifier
                                .size(155.dp)
                                .graphicsLayer {
                                    scaleX = if (startupAnimationsEnabled) coreScale else 1f
                                    scaleY = if (startupAnimationsEnabled) coreScale else 1f
                                }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "K.A.I.R.O.",
                        color = PrimaryText,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 4.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "PERSONAL AI ASSISTANT",
                        color = SecondaryText,
                        fontSize = 10.sp,
                        letterSpacing = 2.5.sp
                    )
                    Spacer(Modifier.height(34.dp))
                    Text(
                        text = "INITIALIZING SYSTEMS",
                        color = SecondaryText,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@Composable
fun KairoScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val runtime = remember { KairoRuntime() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val preferences = remember(context) { context.getSharedPreferences("jarvis_preferences", Context.MODE_PRIVATE) }
    var activeLocalModelFileName by remember {
        mutableStateOf(preferences.getString("active_local_model_file", MODEL_FILE_NAME) ?: MODEL_FILE_NAME)
    }
    val modelDirectory = remember(context) { File(context.filesDir, "models") }
    val localModels = remember(modelDirectory) {
        mutableStateListOf<LocalModelEntry>().apply { addAll(discoverLocalModels(modelDirectory)) }
    }

    var input by remember { mutableStateOf("") }
    var selectedModelUri by remember { mutableStateOf<Uri?>(null) }
    var isLoadingModel by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var modelReady by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Choose MODEL to load your local AI") }
    var activeBackend by remember { mutableStateOf(preferences.getString("active_backend", "local") ?: "local") }
    var showModelDialog by remember { mutableStateOf(false) }
    var showLocalModelsDialog by remember { mutableStateOf(false) }
    var pendingDeleteModelFileName by remember { mutableStateOf<String?>(null) }
    var modelDialogTab by remember { mutableStateOf(if (activeBackend == "online") "online" else "local") }
    var showSwitchOverlay by remember { mutableStateOf(false) }
    var apiProviderName by remember { mutableStateOf(preferences.getString("api_provider", "OpenRouter") ?: "OpenRouter") }
    var apiBaseUrl by remember { mutableStateOf(preferences.getString("api_base_url", "https://openrouter.ai/api/v1") ?: "https://openrouter.ai/api/v1") }
    var apiModelId by remember { mutableStateOf(preferences.getString("api_model_id", "deepseek/deepseek-v4.1-flash-1017") ?: "deepseek/deepseek-v4.1-flash-1017") }
    var apiKeyConfigured by remember { mutableStateOf(ApiKeyVault.load(context).isNotBlank()) }
    var apiKeyDraft by remember { mutableStateOf("") }
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var isTestingApi by remember { mutableStateOf(false) }
    var apiTestResult by remember { mutableStateOf("") }
    var profileName by remember { mutableStateOf(preferences.getString("profile_name", "Isak") ?: "Isak") }
    var profileDraft by remember { mutableStateOf(profileName) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var memoryDraft by remember { mutableStateOf("") }
    val savedMemories = remember(context) {
        mutableStateListOf<String>().apply { addAll(MemoryStore.load(context)) }
    }
    var showAboutDialog by remember { mutableStateOf(false) }
    var fontScale by remember { mutableStateOf(preferences.getFloat("font_scale", 1.0f)) }
    var reduceMotion by remember { mutableStateOf(preferences.getBoolean("reduce_motion", false)) }
    var enterToSend by remember { mutableStateOf(preferences.getBoolean("enter_to_send", true)) }
    var showStatusLine by remember { mutableStateOf(preferences.getBoolean("show_status_line", true)) }
    var compactMessages by remember { mutableStateOf(preferences.getBoolean("compact_messages", false)) }
    var renameChatId by remember { mutableStateOf<String?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var deleteChatId by remember { mutableStateOf<String?>(null) }

    val sessions = remember(context) {
        mutableStateListOf<ChatSession>().apply {
            val savedSessions = ConversationStore.load(context)
            if (savedSessions.isNotEmpty()) {
                addAll(savedSessions)
            } else {
                // Import the single-chat history created by the earlier version, if available.
                val oldMessages = try {
                    ChatMemory.load(context)
                } catch (_: Exception) {
                    emptyList()
                }
                if (oldMessages.isNotEmpty()) {
                    add(
                        ChatSession(
                            id = UUID.randomUUID().toString(),
                            title = makeChatTitle(oldMessages),
                            updatedAt = System.currentTimeMillis(),
                            messages = oldMessages
                        )
                    )
                }
            }
            // Always start on a fresh chat after app launch; older conversations remain in history.
            add(0, newEmptyChat())
        }
    }

    var currentChatId by remember {
        mutableStateOf(sessions.firstOrNull()?.id ?: UUID.randomUUID().toString())
    }
    val currentSession = sessions.firstOrNull { it.id == currentChatId }
        ?: sessions.first()
    val currentMessages = currentSession.messages
    val listState = rememberLazyListState()
    val recentSessions = sessions.sortedByDescending { it.updatedAt }
    val backendReady = if (activeBackend == "online") {
        apiKeyConfigured && apiModelId.isNotBlank() && apiBaseUrl.startsWith("https://", ignoreCase = true)
    } else modelReady

    val updateSession: (String, (ChatSession) -> ChatSession) -> Unit = { id, transform ->
        val index = sessions.indexOfFirst { it.id == id }
        if (index >= 0) {
            val updated = transform(sessions[index])
            sessions.removeAt(index)
            sessions.add(0, updated)
        }
    }

    val closeDrawer = {
        scope.launch { drawerState.close() }
    }

    val startNewChat: () -> Unit = {
        if (currentSession.messages.isNotEmpty()) {
            val freshChat = newEmptyChat()
            sessions.add(0, freshChat)
            currentChatId = freshChat.id
        }
        input = ""
        closeDrawer()
    }

    // Debounced local persistence: save chat history once the UI settles.
    val saveKey = sessions.joinToString("|") { chat ->
        val lastTextLength = chat.messages.lastOrNull()?.text?.length ?: 0
        "${chat.id}:${chat.updatedAt}:${chat.title}:${chat.messages.size}:$lastTextLength"
    }
    LaunchedEffect(saveKey) {
        delay(400)
        ConversationStore.save(context, sessions.toList())
    }

    val refreshLocalModels: () -> Unit = {
        localModels.clear()
        localModels.addAll(discoverLocalModels(modelDirectory))
    }

    // On relaunch, load the selected private model, falling back to the original E2B file.
    LaunchedEffect(Unit) {
        val files = withContext(Dispatchers.IO) { discoverLocalModels(modelDirectory) }
        localModels.clear()
        localModels.addAll(files)
        val chosen = files.firstOrNull { it.fileName == activeLocalModelFileName }
            ?: files.firstOrNull { it.fileName == MODEL_FILE_NAME }
            ?: files.firstOrNull()
        if (chosen == null) {
            modelReady = false
            status = "No local model installed · Import a .litertlm file"
        } else {
            activeLocalModelFileName = chosen.fileName
            preferences.edit().putString("active_local_model_file", chosen.fileName).apply()
            if (activeBackend == "online") {
                // Free RAM when the user last chose Online AI; local model loads only when selected.
                modelReady = false
                status = "Online AI selected · $apiProviderName. Local model is available to load on demand."
            } else {
                isLoadingModel = true
                status = "Loading ${chosen.fileName}…"
                try {
                    val engine = withContext(Dispatchers.IO) { createEngine(context, File(modelDirectory, chosen.fileName)) }
                    runtime.install(engine)
                    modelReady = true
                    status = "Local model ready · ${chosen.fileName}"
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    modelReady = false
                    status = "Model load failed: ${e.message ?: e.javaClass.simpleName}"
                } finally {
                    isLoadingModel = false
                }
            }
        }
    }

    val loadModel: (Uri) -> Unit = { uri ->
        if (isBusy || isLoadingModel) {
            status = "Finish the current response/model operation before switching models."
        } else scope.launch {
            isLoadingModel = true
            status = "Importing local model…"
            try {
                val destination = withContext(Dispatchers.IO) {
                    importModelToPrivateStorage(context, uri, modelDirectory)
                }
                // Keep only one model engine resident at a time to reduce memory pressure on 12 GB devices.
                withContext(Dispatchers.IO) { runtime.close() }
                modelReady = false
                status = "Initializing ${destination.name}…"
                val engine = withContext(Dispatchers.IO) { createEngine(context, destination) }
                runtime.install(engine)
                activeLocalModelFileName = destination.name
                preferences.edit()
                    .putString("active_local_model_file", destination.name)
                    .putString("active_backend", "local")
                    .apply()
                activeBackend = "local"
                modelReady = true
                selectedModelUri = uri
                refreshLocalModels()
                status = "Local model ready · ${destination.name}"
                showLocalModelsDialog = false
                showModelDialog = false
                showSwitchOverlay = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                modelReady = false
                refreshLocalModels()
                status = "Model load failed: ${e.message ?: e.javaClass.simpleName}. You can select another installed model."
            } finally {
                isLoadingModel = false
            }
        }
    }

    val activateLocalModel: (String) -> Unit = { fileName ->
        if (isBusy || isLoadingModel) {
            status = "Finish the current response/model operation before switching models."
        } else scope.launch {
            val target = File(modelDirectory, fileName)
            if (!target.isFile || !target.name.endsWith(".litertlm", ignoreCase = true)) {
                status = "Model file is missing or invalid: $fileName"
            } else if (fileName == activeLocalModelFileName && modelReady) {
                activeBackend = "local"
                preferences.edit().putString("active_backend", "local").apply()
                runtime.resetAllConversations()
                showLocalModelsDialog = false
                showModelDialog = false
                showSwitchOverlay = true
                status = "Local AI active · $fileName"
            } else {
                isLoadingModel = true
                status = "Switching to $fileName…"
                try {
                    withContext(Dispatchers.IO) { runtime.close() }
                    modelReady = false
                    val engine = withContext(Dispatchers.IO) { createEngine(context, target) }
                    runtime.install(engine)
                    activeLocalModelFileName = fileName
                    preferences.edit()
                        .putString("active_local_model_file", fileName)
                        .putString("active_backend", "local")
                        .apply()
                    activeBackend = "local"
                    modelReady = true
                    runtime.resetAllConversations()
                    status = "Local model ready · $fileName"
                    showLocalModelsDialog = false
                    showModelDialog = false
                    showSwitchOverlay = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    modelReady = false
                    status = "Could not load $fileName: ${e.message ?: e.javaClass.simpleName}"
                } finally {
                    refreshLocalModels()
                    isLoadingModel = false
                }
            }
        }
    }

    val modelFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedModelUri = uri
            loadModel(uri)
        }
    }

    DisposableEffect(runtime) {
        onDispose {
            CoroutineScope(Dispatchers.IO).launch { runtime.close() }
        }
    }

    val sendMessage: () -> Unit = {
        val text = input.trim()
        if (text.isNotEmpty() && !isBusy && !isLoadingModel) {
            val selectedBackend = activeBackend
            val isConfigured = if (selectedBackend == "online") {
                apiKeyConfigured && apiModelId.isNotBlank() && apiBaseUrl.startsWith("https://", ignoreCase = true)
            } else modelReady
            if (!isConfigured) {
                modelDialogTab = if (selectedBackend == "online") "online" else "local"
                if (selectedBackend == "online") apiKeyDraft = ApiKeyVault.load(context)
                showModelDialog = true
                status = if (selectedBackend == "online") {
                    "Configure and test your online API first."
                } else {
                    "Load a .litertlm file or switch to Online API."
                }
            } else {
                val targetChatId = currentSession.id
                val discoveredName = extractPreferredName(text)
                if (!discoveredName.isNullOrBlank() && discoveredName != profileName) {
                    profileName = discoveredName
                    profileDraft = discoveredName
                    preferences.edit().putString("profile_name", discoveredName).apply()
                    runtime.resetAllConversations()
                }
                val profileNameForTurn = discoveredName ?: profileName
                val historyBeforeSend = currentSession.messages
                val userMessage = ChatMessage(text = text, isUser = true)
                val placeholder = ChatMessage(text = "", isUser = false)
                updateSession(targetChatId) { session ->
                    val title = if (session.messages.isEmpty() || session.title == "New chat") smartChatTitle(text) else session.title
                    session.copy(
                        title = title,
                        updatedAt = System.currentTimeMillis(),
                        messages = session.messages + userMessage + placeholder
                    )
                }
                input = ""
                isBusy = true
                status = if (selectedBackend == "online") "Connecting to $apiProviderName…" else "Generating a local response…"

                scope.launch {
                    try {
                        val memorySnapshot = savedMemories.toList()
                        if (selectedBackend == "local") {
                            val conversation = withContext(Dispatchers.IO) {
                                runtime.conversationFor(targetChatId, historyBeforeSend, profileNameForTurn, memorySnapshot)
                            }
                            conversation.sendMessageAsync(text).collect { chunk ->
                                updateSession(targetChatId) { session ->
                                    val changedMessages = session.messages.toMutableList()
                                    val lastAssistantIndex = changedMessages.indexOfLast { !it.isUser }
                                    if (lastAssistantIndex >= 0) {
                                        val previous = changedMessages[lastAssistantIndex]
                                        changedMessages[lastAssistantIndex] = previous.copy(text = previous.text + chunk.toString())
                                    }
                                    session.copy(updatedAt = System.currentTimeMillis(), messages = changedMessages)
                                }
                            }
                        } else {
                            status = "Waiting for $apiProviderName · $apiModelId…"
                            val reply = withContext(Dispatchers.IO) {
                                val config = OnlineApiConfig(
                                    provider = apiProviderName,
                                    baseUrl = apiBaseUrl,
                                    model = apiModelId,
                                    apiKey = ApiKeyVault.load(context)
                                )
                                callOnlineApi(config, historyBeforeSend, text, profileNameForTurn, memorySnapshot)
                            }
                            updateSession(targetChatId) { session ->
                                val changedMessages = session.messages.toMutableList()
                                val lastAssistantIndex = changedMessages.indexOfLast { !it.isUser }
                                if (lastAssistantIndex >= 0) changedMessages[lastAssistantIndex] = changedMessages[lastAssistantIndex].copy(text = reply)
                                session.copy(updatedAt = System.currentTimeMillis(), messages = changedMessages)
                            }
                        }

                        updateSession(targetChatId) { session ->
                            val changedMessages = session.messages.toMutableList()
                            val lastAssistantIndex = changedMessages.indexOfLast { !it.isUser }
                            if (lastAssistantIndex >= 0 && changedMessages[lastAssistantIndex].text.isBlank()) {
                                changedMessages[lastAssistantIndex] = changedMessages[lastAssistantIndex].copy(
                                    text = "AI ne empty reply diya. Model ID/API limits check karke dobara try karo."
                                )
                            }
                            session.copy(updatedAt = System.currentTimeMillis(), messages = changedMessages)
                        }
                        status = if (selectedBackend == "local") "Local model ready · On-device" else "Online ready · $apiProviderName / $apiModelId"
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val detail = e.message ?: e.javaClass.simpleName
                        updateSession(targetChatId) { session ->
                            val changedMessages = session.messages.toMutableList()
                            val lastAssistantIndex = changedMessages.indexOfLast { !it.isUser }
                            if (lastAssistantIndex >= 0) {
                                val previous = changedMessages[lastAssistantIndex]
                                changedMessages[lastAssistantIndex] = previous.copy(
                                    text = if (previous.text.isBlank()) "AI response error: $detail" else previous.text + "\n\n[Generation error: $detail]"
                                )
                            }
                            session.copy(updatedAt = System.currentTimeMillis(), messages = changedMessages)
                        }
                        status = if (selectedBackend == "online") "API error · ${detail.take(100)}" else "AI response error: $detail"
                    } finally {
                        isBusy = false
                    }
                }
            }
        }
    }

    LaunchedEffect(currentChatId, currentMessages.size, currentMessages.lastOrNull()?.text) {
        if (currentMessages.isNotEmpty()) {
            listState.scrollToItem(currentMessages.lastIndex)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isLoadingModel,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(310.dp),
                drawerContainerColor = DrawerBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 6.dp, bottom = 22.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✦", color = DrawerBackground, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("K.A.I.R.O.", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Personal AI", color = SecondaryText, fontSize = 11.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { startNewChat() },
                        color = PanelBackground,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("＋", color = Accent, fontSize = 23.sp)
                            Spacer(Modifier.width(10.dp))
                            Text("New chat", color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "YOUR CONVERSATIONS",
                        color = SecondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 9.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 10.dp)
                    ) {
                        items(recentSessions, key = { it.id }) { session ->
                            val isSelected = session.id == currentChatId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF343434) else Color.Transparent)
                                    .clickable {
                                        currentChatId = session.id
                                        input = ""
                                        closeDrawer()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("◷", color = SecondaryText, fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        color = PrimaryText,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val preview = session.messages.lastOrNull()?.text.orEmpty()
                                    if (preview.isNotBlank()) {
                                        Text(preview, color = SecondaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Text("✎", color = SecondaryText, modifier = Modifier.padding(horizontal = 5.dp).clickable {
                                    renameChatId = session.id
                                    renameDraft = session.title
                                })
                                Text("×", color = SecondaryText, fontSize = 19.sp, modifier = Modifier.clickable { deleteChatId = session.id })
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor)
                    DrawerAction("◉", "Profile", profileName.ifBlank { "Set your name" }) {
                        profileDraft = profileName
                        showProfileDialog = true
                    }
                    DrawerAction("◇", "Memory", if (savedMemories.isEmpty()) "No saved facts yet" else "${savedMemories.size} saved fact(s)") {
                        showMemoryDialog = true
                    }
                    DrawerAction("◈", "AI Models", if (activeBackend == "online") "Online · $apiProviderName" else "Local model / API setup") {
                        modelDialogTab = if (activeBackend == "online") "online" else "local"
                        apiKeyDraft = ApiKeyVault.load(context)
                        apiTestResult = ""
                        showModelDialog = true
                        closeDrawer()
                    }
                    DrawerAction("⚙", "Settings", "Appearance, input & motion") { showSettingsDialog = true }
                    DrawerAction("ⓘ", "About K.A.I.R.O.", "App information") { showAboutDialog = true }
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = BorderColor)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (modelReady) Accent else SecondaryText)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when {
                                    activeBackend == "online" && backendReady -> "Online AI ready"
                                    activeBackend == "online" -> "Online API needs setup"
                                    modelReady -> "Local model ready"
                                    else -> "Local model not loaded"
                                },
                                color = PrimaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(if (activeBackend == "online") "Online prompts are sent to your provider" else "Local prompts stay on-device", color = SecondaryText, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                // Compact app header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundAction(
                        label = "☰",
                        description = "Open conversations",
                        onClick = { scope.launch { drawerState.open() } }
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("K.A.I.R.O.", color = PrimaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = when {
                                isLoadingModel -> "Loading local model…"
                                activeBackend == "online" && backendReady -> "$apiProviderName · $apiModelId"
                                activeBackend == "online" -> "Online API · Setup needed"
                                modelReady -> "Local AI · ${File(activeLocalModelFileName).nameWithoutExtension}"
                                else -> "Choose local model or API"
                            },
                            color = SecondaryText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Keep both choices permanently visible so the Online API option cannot disappear
                    // when switching back to / loading a local model.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = !isLoadingModel && !isBusy) {
                                    modelDialogTab = "local"
                                    apiKeyDraft = ApiKeyVault.load(context)
                                    apiTestResult = ""
                                    showModelDialog = true
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (activeBackend == "local") Color(0xFF303030) else PanelBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (activeBackend == "local") Color(0xFF777777) else BorderColor
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text("LOCAL", color = PrimaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (modelReady) "READY" else "OFFLINE",
                                    color = SecondaryText,
                                    fontSize = 8.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = !isLoadingModel && !isBusy) {
                                    modelDialogTab = "online"
                                    apiKeyDraft = ApiKeyVault.load(context)
                                    apiTestResult = ""
                                    showModelDialog = true
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (activeBackend == "online") Color(0xFF303030) else PanelBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (activeBackend == "online") Color(0xFF777777) else BorderColor
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text("API", color = PrimaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    when {
                                        activeBackend == "online" && backendReady -> "READY"
                                        apiKeyConfigured -> "SETUP"
                                        else -> "CONNECT"
                                    },
                                    color = SecondaryText,
                                    fontSize = 8.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF303030))

                if (currentMessages.isEmpty()) {
                    EmptyWelcome(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onSuggestion = { input = it },
                        profileName = profileName
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = if (compactMessages) 12.dp else 22.dp),
                        verticalArrangement = Arrangement.spacedBy(if (compactMessages) 12.dp else 24.dp)
                    ) {
                        items(currentMessages) { message ->
                            ChatMessageRow(message, fontScale, compactMessages)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = InputBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text("Message K.A.I.R.O.", color = SecondaryText, fontSize = 15.sp)
                                },
                                singleLine = false,
                                maxLines = 5,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = if (enterToSend) ImeAction.Send else ImeAction.Default
                                ),
                                keyboardActions = KeyboardActions(onSend = { if (enterToSend) sendMessage() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PrimaryText,
                                    unfocusedTextColor = PrimaryText,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent,
                                    cursorColor = Accent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            Spacer(Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        activeBackend == "online" && backendReady -> "↗  Online · $apiProviderName"
                                        activeBackend == "online" -> "Configure online API to start"
                                        modelReady -> "◉  Local model · On-device"
                                        else -> "Choose local model or online API"
                                    },
                                    color = SecondaryText,
                                    fontSize = 10.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .clickable(
                                            enabled = input.isNotBlank() && backendReady && !isBusy && !isLoadingModel,
                                            onClick = { sendMessage() }
                                        ),
                                    shape = CircleShape,
                                    color = if (input.isNotBlank() && backendReady && !isBusy && !isLoadingModel) Accent else Color(0xFF555555)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (isBusy) "…" else "↑",
                                            color = AppBackground,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (showStatusLine) Text(
                        text = status,
                        color = if (backendReady && !isBusy) Accent.copy(alpha = 0.8f) else SecondaryText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    LaunchedEffect(showSwitchOverlay) {
        if (showSwitchOverlay) {
            delay(900)
            showSwitchOverlay = false
        }
    }

    AnimatedVisibility(
        visible = showSwitchOverlay,
        enter = fadeIn(animationSpec = tween(if (reduceMotion) 1 else 220)) + androidx.compose.animation.slideInVertically(
            initialOffsetY = { if (reduceMotion) 0 else it / 10 }, animationSpec = tween(if (reduceMotion) 1 else 280, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = tween(if (reduceMotion) 1 else 180)) + androidx.compose.animation.slideOutVertically(
            targetOffsetY = { if (reduceMotion) 0 else -it / 12 }, animationSpec = tween(if (reduceMotion) 1 else 180)
        ),
        modifier = Modifier.fillMaxSize().zIndex(30f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.padding(28.dp).fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF0D0D0D),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF343434))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(18.dp))
                    Text("SWITCHING INTELLIGENCE", color = SecondaryText, fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (activeBackend == "online") "Connecting to $apiProviderName" else "Activating local model",
                        color = PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (activeBackend == "online") apiModelId else "On-device inference",
                        color = SecondaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    if (showModelDialog) AlertDialog(
        onDismissRequest = { if (!isTestingApi) showModelDialog = false },
        title = {
            Column {
                Text("AI Model Switcher", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Choose where K.A.I.R.O. gets its answers", color = SecondaryText, fontSize = 11.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { modelDialogTab = "local" },
                        shape = RoundedCornerShape(14.dp),
                        color = if (modelDialogTab == "local") Color(0xFF303030) else PanelBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (modelDialogTab == "local") Color(0xFF666666) else BorderColor)
                    ) {
                        Column(Modifier.padding(vertical = 12.dp, horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("◉  LOCAL AI", color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (modelReady) "Ready on-device" else "Load model file", color = SecondaryText, fontSize = 10.sp)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { modelDialogTab = "online" },
                        shape = RoundedCornerShape(14.dp),
                        color = if (modelDialogTab == "online") Color(0xFF303030) else PanelBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (modelDialogTab == "online") Color(0xFF666666) else BorderColor)
                    ) {
                        Column(Modifier.padding(vertical = 12.dp, horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("↗  ONLINE API", color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (apiKeyConfigured) "Key saved securely" else "Connect a provider", color = SecondaryText, fontSize = 10.sp)
                        }
                    }
                }

                if (modelDialogTab == "local") {
                    Surface(
                        shape = RoundedCornerShape(16.dp), color = PanelBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Run AI on this device", color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Your prompts stay on-device. Import compatible .litertlm model files and switch between installed models.", color = SecondaryText, fontSize = 12.sp, lineHeight = 17.sp)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = InputBackground,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(if (modelReady) "ACTIVE LOCAL MODEL" else "SELECTED LOCAL MODEL", color = SecondaryText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(activeLocalModelFileName, color = PrimaryText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(localFileSizeLabel(File(modelDirectory, activeLocalModelFileName).length()), color = SecondaryText, fontSize = 10.sp)
                                }
                            }
                            Button(
                                onClick = { refreshLocalModels(); showModelDialog = false; showLocalModelsDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252525), contentColor = PrimaryText)
                            ) { Text("MANAGE LOCAL MODELS · ${localModels.size}") }
                            if (modelReady) {
                                Button(
                                    enabled = !isLoadingModel && !isBusy,
                                    onClick = {
                                        activeBackend = "local"
                                        preferences.edit().putString("active_backend", "local").apply()
                                        runtime.resetAllConversations()
                                        showModelDialog = false
                                        showSwitchOverlay = true
                                        status = "Local model ready · On-device"
                                    },
                                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AppBackground)
                                ) { Text(if (activeBackend == "local") "LOCAL AI IS ACTIVE" else "USE LOCAL AI") }
                            }
                            Button(
                                enabled = !isLoadingModel && !isBusy,
                                onClick = { showModelDialog = false; modelFilePicker.launch(arrayOf("*/*")) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252525), contentColor = PrimaryText)
                            ) { Text("IMPORT / LOAD .LITERTLM FILE") }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp), color = PanelBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Online provider", color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Preset or custom OpenAI-compatible URL", color = SecondaryText, fontSize = 10.sp)
                                }
                                Box {
                                    TextButton(onClick = { providerMenuExpanded = true }) { Text("$apiProviderName  ⌄", color = Accent, maxLines = 1) }
                                    DropdownMenu(expanded = providerMenuExpanded, onDismissRequest = { providerMenuExpanded = false }) {
                                        API_PROVIDER_PRESETS.forEach { preset ->
                                            DropdownMenuItem(
                                                text = { Text(preset.name) },
                                                onClick = {
                                                    apiProviderName = preset.name
                                                    apiBaseUrl = preset.baseUrl
                                                    apiModelId = preset.defaultModel
                                                    apiTestResult = ""
                                                    providerMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = apiBaseUrl, onValueChange = { apiBaseUrl = it.trim(); apiTestResult = "" },
                                modifier = Modifier.fillMaxWidth(), label = { Text("Base URL (HTTPS)") },
                                singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            OutlinedTextField(
                                value = apiModelId, onValueChange = { apiModelId = it.trim(); apiTestResult = "" },
                                modifier = Modifier.fillMaxWidth(), label = { Text("Model ID") },
                                placeholder = { Text("Exact model ID from provider") }, singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            OutlinedTextField(
                                value = apiKeyDraft, onValueChange = { apiKeyDraft = it.trim(); apiTestResult = "" },
                                modifier = Modifier.fillMaxWidth(), label = { Text("API key") },
                                placeholder = { Text("Paste your provider API key") }, singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            Text(
                                "The API key is encrypted using Android Keystore. Online prompts and chat history are sent to the selected provider.",
                                color = SecondaryText, fontSize = 10.sp, lineHeight = 14.sp
                            )
                            if (apiTestResult.isNotBlank()) {
                                Text(
                                    apiTestResult, color = if (apiTestResult.startsWith("Connected")) Accent else SecondaryText,
                                    fontSize = 11.sp, lineHeight = 15.sp
                                )
                            }
                            Button(
                                enabled = !isTestingApi && !isBusy && !isLoadingModel && apiKeyDraft.isNotBlank() && apiModelId.isNotBlank() && apiBaseUrl.startsWith("https://", true),
                                onClick = {
                                    scope.launch {
                                        isTestingApi = true
                                        apiTestResult = "Connecting and testing model…"
                                        try {
                                            val testConfig = OnlineApiConfig(apiProviderName, apiBaseUrl, apiModelId, apiKeyDraft.trim())
                                            val testReply = withContext(Dispatchers.IO) {
                                                callOnlineApi(testConfig, emptyList(), "Reply with exactly: Connected.", profileName, savedMemories.toList())
                                            }
                                            apiTestResult = "Connected successfully. Test reply: ${testReply.take(100)}"
                                        } catch (e: Exception) {
                                            apiTestResult = "Connection failed: ${(e.message ?: e.javaClass.simpleName).take(250)}"
                                        } finally {
                                            isTestingApi = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252525), contentColor = PrimaryText)
                            ) {
                                if (isTestingApi) {
                                    CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = Accent)
                                    Spacer(Modifier.width(8.dp))
                                    Text("TESTING CONNECTION…")
                                } else Text("TEST CONNECTION")
                            }
                            Button(
                                enabled = !isTestingApi && !isBusy && !isLoadingModel && apiKeyDraft.isNotBlank() && apiModelId.isNotBlank() && apiBaseUrl.startsWith("https://", true),
                                onClick = {
                                    try {
                                        val newConfig = OnlineApiConfig(apiProviderName, apiBaseUrl.trim(), apiModelId.trim(), apiKeyDraft.trim())
                                        validateApiConfig(newConfig)
                                        ApiKeyVault.save(context, apiKeyDraft.trim())
                                        apiKeyConfigured = true
                                        preferences.edit()
                                            .putString("api_provider", apiProviderName)
                                            .putString("api_base_url", apiBaseUrl.trim().trimEnd('/'))
                                            .putString("api_model_id", apiModelId.trim())
                                            .putString("active_backend", "online")
                                            .apply()
                                        activeBackend = "online"
                                        apiBaseUrl = apiBaseUrl.trim().trimEnd('/')
                                        apiModelId = apiModelId.trim()
                                        runtime.resetAllConversations()
                                        isLoadingModel = true
                                        showModelDialog = false
                                        showSwitchOverlay = true
                                        status = "Online AI selected · $apiProviderName / $apiModelId"
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) { runtime.close() }
                                            } finally {
                                                modelReady = false
                                                isLoadingModel = false
                                            }
                                        }
                                    } catch (e: Exception) {
                                        apiTestResult = "Could not save: ${e.message ?: e.javaClass.simpleName}"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AppBackground)
                            ) { Text("SAVE & USE ONLINE AI") }
                            if (apiKeyConfigured) {
                                TextButton(
                                    onClick = {
                                        ApiKeyVault.clear(context)
                                        apiKeyDraft = ""
                                        apiKeyConfigured = false
                                        if (activeBackend == "online") {
                                            activeBackend = "local"
                                            preferences.edit().putString("active_backend", "local").apply()
                                            showSwitchOverlay = true
                                            activateLocalModel(activeLocalModelFileName)
                                        }
                                        apiTestResult = "Saved API key cleared from this app."
                                    }, modifier = Modifier.align(Alignment.End)
                                ) { Text("Clear saved API key", color = SecondaryText) }
                            }
                        }
                    }
                    Text("Provider model IDs and billing/rate limits are controlled by your API provider.", color = SecondaryText, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { showModelDialog = false }) { Text("Close") } }
    )

    if (showLocalModelsDialog) AlertDialog(
        onDismissRequest = { if (!isLoadingModel) showLocalModelsDialog = false },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Local Model Manager", fontWeight = FontWeight.SemiBold)
                Text("Installed on this device · stored privately by K.A.I.R.O.", color = SecondaryText, fontSize = 11.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp), color = PanelBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("MODEL LIBRARY", color = SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("${localModels.size} model file(s) · ${localFileSizeLabel(localModels.sumOf { it.sizeBytes })} total", color = PrimaryText, fontSize = 12.sp)
                        Text("Only one model is loaded into memory at a time. Importing another model does not remove your current files.", color = SecondaryText, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
                if (localModels.isEmpty()) {
                    Text("No local .litertlm models found. Import one to get started.", color = SecondaryText, fontSize = 12.sp)
                } else {
                    localModels.forEach { entry ->
                        val isActiveFile = entry.fileName == activeLocalModelFileName
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isActiveFile) Color(0xFF191919) else PanelBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isActiveFile) Color(0xFF555555) else BorderColor)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(entry.fileName, color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                        Text(localFileSizeLabel(entry.sizeBytes), color = SecondaryText, fontSize = 10.sp)
                                    }
                                    if (isActiveFile && modelReady) {
                                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF2B2B2B)) {
                                            Text("ACTIVE", modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        enabled = !isLoadingModel && !isBusy && !(isActiveFile && modelReady),
                                        onClick = { activateLocalModel(entry.fileName) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AppBackground, disabledContainerColor = Color(0xFF262626), disabledContentColor = SecondaryText)
                                    ) { Text(if (isActiveFile && modelReady) "IN USE" else "LOAD MODEL", fontSize = 11.sp) }
                                    TextButton(
                                        enabled = !isLoadingModel && !isBusy && !isActiveFile,
                                        onClick = { pendingDeleteModelFileName = entry.fileName },
                                        modifier = Modifier.widthIn(min = 74.dp)
                                    ) { Text("REMOVE", color = if (!isActiveFile) SecondaryText else Color(0xFF555555), fontSize = 10.sp) }
                                }
                            }
                        }
                    }
                }
                Button(
                    enabled = !isLoadingModel && !isBusy,
                    onClick = { showLocalModelsDialog = false; showModelDialog = false; modelDialogTab = "local"; modelFilePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252525), contentColor = PrimaryText)
                ) { Text("+ IMPORT .LITERTLM FILE") }
                Button(
                    enabled = !isLoadingModel && !isBusy,
                    onClick = {
                        try {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm?download=true")))
                        } catch (e: Exception) {
                            status = "Could not open model download page: ${e.message ?: e.javaClass.simpleName}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF181818), contentColor = PrimaryText)
                ) { Text("GET GEMMA 4 E4B MODEL · ~3.66 GB") }
                Text("Download the main .litertlm file from the official model page, then import it here. Ensure ample free storage: importing creates a private app copy.", color = SecondaryText, fontSize = 10.sp, lineHeight = 14.sp)
            }
        },
        confirmButton = { TextButton(onClick = { showLocalModelsDialog = false }) { Text("Close") } }
    )

    val pendingModelDelete = pendingDeleteModelFileName
    if (pendingModelDelete != null) AlertDialog(
        onDismissRequest = { pendingDeleteModelFileName = null },
        title = { Text("Remove local model?") },
        text = { Text("${pendingModelDelete} will be deleted from K.A.I.R.O. private storage. This does not delete any original download outside the app.") },
        confirmButton = { TextButton(onClick = {
            try {
                val target = File(modelDirectory, pendingModelDelete)
                if (target.name.endsWith(".litertlm", ignoreCase = true) && target.name != activeLocalModelFileName) {
                    if (target.delete()) status = "Removed local model · $pendingModelDelete"
                    else status = "Could not remove model. Check storage permissions/available space."
                }
            } catch (e: Exception) {
                status = "Could not remove model: ${e.message ?: e.javaClass.simpleName}"
            }
            refreshLocalModels()
            pendingDeleteModelFileName = null
        }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { pendingDeleteModelFileName = null }) { Text("Cancel") } }
    )

    if (showProfileDialog) AlertDialog(
        onDismissRequest = { showProfileDialog = false },
        title = { Text("Your profile") },
        text = {
            Column {
                Text("Your name is saved on this device and supplied to new AI conversations.", fontSize = 13.sp)
                OutlinedTextField(value = profileDraft, onValueChange = { profileDraft = it }, label = { Text("Your name") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = {
            profileName = profileDraft.trim().take(60)
            preferences.edit().putString("profile_name", profileName).apply()
            runtime.resetAllConversations()
            showProfileDialog = false
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text("Cancel") } }
    )

    if (showSettingsDialog) AlertDialog(
        onDismissRequest = { showSettingsDialog = false },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Settings & customization", fontWeight = FontWeight.SemiBold)
                Text("Make K.A.I.R.O. feel right for you", color = SecondaryText, fontSize = 11.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = PanelBackground, border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("CHAT APPEARANCE", color = SecondaryText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Response text size", color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(when { fontScale < 0.95f -> "Small"; fontScale > 1.1f -> "Large"; else -> "Default" }, color = SecondaryText, fontSize = 11.sp)
                            }
                            Text("${(fontScale * 100).toInt()}%", color = PrimaryText, fontSize = 12.sp)
                        }
                        Slider(value = fontScale, onValueChange = { fontScale = it }, valueRange = 0.85f..1.30f, steps = 8)
                        Text("Compact chat spacing", color = PrimaryText, fontSize = 13.sp)
                        Text("Reduce gaps between messages", color = SecondaryText, fontSize = 10.sp)
                        Switch(checked = compactMessages, onCheckedChange = { compactMessages = it })
                    }
                }
                Surface(shape = RoundedCornerShape(14.dp), color = PanelBackground, border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("BEHAVIOUR", color = SecondaryText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Enter sends message", color = PrimaryText, fontSize = 13.sp)
                                Text("Turn off to use Enter for a new line", color = SecondaryText, fontSize = 10.sp)
                            }
                            Switch(checked = enterToSend, onCheckedChange = { enterToSend = it })
                        }
                        HorizontalDivider(color = BorderColor)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Show status line", color = PrimaryText, fontSize = 13.sp)
                                Text("Display model / connection status below composer", color = SecondaryText, fontSize = 10.sp)
                            }
                            Switch(checked = showStatusLine, onCheckedChange = { showStatusLine = it })
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(14.dp), color = PanelBackground, border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ANIMATION & PERFORMANCE", color = SecondaryText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Reduce animations", color = PrimaryText, fontSize = 13.sp)
                                Text("Use near-instant transitions for less motion", color = SecondaryText, fontSize = 10.sp)
                            }
                            Switch(checked = reduceMotion, onCheckedChange = { reduceMotion = it })
                        }
                        Text("OLED black theme is always enabled to keep the interface consistent and battery-friendly on AMOLED screens.", color = SecondaryText, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            preferences.edit()
                .putFloat("font_scale", fontScale)
                .putBoolean("reduce_motion", reduceMotion)
                .putBoolean("enter_to_send", enterToSend)
                .putBoolean("show_status_line", showStatusLine)
                .putBoolean("compact_messages", compactMessages)
                .apply()
            showSettingsDialog = false
        }) { Text("Save settings") } },
        dismissButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("Cancel") } }
    )

    if (showMemoryDialog) AlertDialog(
        onDismissRequest = { showMemoryDialog = false },
        title = { Text("K.A.I.R.O. Memory") },
        text = {
            Column {
                Text(
                    "Save facts or preferences that K.A.I.R.O. can use in future chats. Memories stay on this device and are only added or removed by you.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = SecondaryText
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = memoryDraft,
                        onValueChange = { memoryDraft = it.take(300) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Remember this") },
                        placeholder = { Text("e.g. I prefer answers in Hinglish") },
                        maxLines = 3
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = !isBusy && memoryDraft.isNotBlank() && savedMemories.size < 40,
                        onClick = {
                            val cleaned = memoryDraft.trim().replace(Regex("\\s+"), " ").take(300)
                            if (cleaned.isNotBlank() && savedMemories.none { it.equals(cleaned, ignoreCase = true) }) {
                                savedMemories.add(cleaned)
                                MemoryStore.save(context, savedMemories.toList())
                                runtime.resetAllConversations()
                            }
                            memoryDraft = ""
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) { Text("Add") }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isBusy) "Wait for the current reply to finish before editing memory." else "${savedMemories.size}/40 memories saved",
                    fontSize = 11.sp,
                    color = SecondaryText
                )
                if (savedMemories.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("No memories saved yet.", color = SecondaryText, fontSize = 13.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(170.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(savedMemories.toList()) { memory ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    memory,
                                    modifier = Modifier.weight(1f),
                                    color = PrimaryText,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                Text(
                                    "×",
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clickable(enabled = !isBusy) {
                                            savedMemories.remove(memory)
                                            MemoryStore.save(context, savedMemories.toList())
                                            runtime.resetAllConversations()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = if (isBusy) SecondaryText else PrimaryText,
                                    fontSize = 19.sp
                                )
                            }
                            HorizontalDivider(color = BorderColor)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Avoid saving passwords, OTPs, or sensitive financial information.",
                    color = SecondaryText,
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { showMemoryDialog = false }) { Text("Done") }
        }
    )

    if (showAboutDialog) AlertDialog(
        onDismissRequest = { showAboutDialog = false },
        title = { Text("About K.A.I.R.O.") },
        text = {
            Text(
                "K.A.I.R.O.\nPersonal AI Assistant\nVersion 1.1.0\n\n" +
                    "Knowledge Assistant for Intelligent Reasoning and Operations\n\n" +
                    "Created by Isak Mubarak Pathan\n" +
                    "AI development assistance: ChatGPT\n\n" +
                    "An independent personal project, not affiliated with any third-party brand or franchise.\n\n" +
                    "Chat history and profile are stored locally. Local prompts run on-device; online API prompts and chat context are sent to the provider you configure.",
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        },
        confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Close") } }
    )

    val renameId = renameChatId
    if (renameId != null) AlertDialog(
        onDismissRequest = { renameChatId = null },
        title = { Text("Rename conversation") },
        text = { OutlinedTextField(value = renameDraft, onValueChange = { renameDraft = it }, label = { Text("Conversation title") }, singleLine = true) },
        confirmButton = { TextButton(onClick = {
            val title = renameDraft.trim().take(60)
            if (title.isNotBlank()) updateSession(renameId) { it.copy(title = title, updatedAt = System.currentTimeMillis()) }
            renameChatId = null
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { renameChatId = null }) { Text("Cancel") } }
    )

    val deleteId = deleteChatId
    if (deleteId != null) AlertDialog(
        onDismissRequest = { deleteChatId = null },
        title = { Text("Delete conversation?") },
        text = { Text("This conversation will be removed from local history.") },
        confirmButton = { TextButton(onClick = {
            val index = sessions.indexOfFirst { it.id == deleteId }
            runtime.resetConversation(deleteId)
            if (index >= 0) sessions.removeAt(index)
            if (sessions.isEmpty()) sessions.add(newEmptyChat())
            if (currentChatId == deleteId) currentChatId = sessions.first().id
            deleteChatId = null
        }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleteChatId = null }) { Text("Cancel") } }
    )
}

@Composable
private fun DrawerAction(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = Accent, fontSize = 18.sp, modifier = Modifier.width(30.dp))
        Column {
            Text(title, color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = SecondaryText, fontSize = 10.sp)
        }
    }
}

@Composable
private fun RoundAction(
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = PanelBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = PrimaryText, fontSize = 19.sp)
        }
    }
}

@Composable
private fun EmptyWelcome(
    modifier: Modifier = Modifier,
    onSuggestion: (String) -> Unit,
    profileName: String
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Color(0xFF171717)),
            contentAlignment = Alignment.Center
        ) {
            Text("✦", color = Accent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (profileName.isBlank()) "What can I help with?" else "Hi $profileName, what can I help with?",
            color = PrimaryText,
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Choose a local model or connect an online AI API.",
            color = SecondaryText,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(24.dp))
        SuggestionRow("Explain a concept", "Explain a difficult concept in simple Hinglish.", onSuggestion)
        Spacer(Modifier.height(9.dp))
        SuggestionRow("Help me write", "Help me write a professional message.", onSuggestion)
        Spacer(Modifier.height(9.dp))
        SuggestionRow("Brainstorm ideas", "Give me five creative ideas for a personal project.", onSuggestion)
    }
}

@Composable
private fun SuggestionRow(
    title: String,
    prompt: String,
    onClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .clickable { onClick(prompt) },
        shape = RoundedCornerShape(15.dp),
        color = PanelBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(prompt, color = SecondaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("↗", color = SecondaryText, fontSize = 19.sp)
        }
    }
}

@Composable
private fun ChatMessageRow(message: ChatMessage, fontScale: Float, compactMessages: Boolean = false) {
    if (message.isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 330.dp),
                shape = RoundedCornerShape(22.dp),
                color = UserBubble
            ) {
                Text(
                    text = message.text,
                    color = PrimaryText,
                    fontSize = (15 * fontScale).sp,
                    lineHeight = (23 * fontScale).sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(29.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF171717)),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = Accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f).padding(top = 1.dp)) {
                Text("K.A.I.R.O.", color = SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(5.dp))
                MarkdownText(
                    markdown = message.text.ifBlank { "Thinking…" },
                    fontScale = fontScale
                )
            }
        }
    }
}

@Composable
private fun MarkdownText(markdown: String, fontScale: Float) {
    val baseFontSize = (15 * fontScale).sp
    val annotated = remember(markdown, baseFontSize) {
        buildMarkdownAnnotatedString(markdown, baseFontSize)
    }
    Text(
        text = annotated,
        color = PrimaryText,
        fontSize = baseFontSize,
        lineHeight = (24 * fontScale).sp
    )
}

private fun buildMarkdownAnnotatedString(markdown: String, baseFontSize: TextUnit): AnnotatedString =
    buildAnnotatedString {
        val lines = markdown.split('\n')
        lines.forEachIndexed { index, originalLine ->
            val line = originalLine.trimStart()
            val headingMatch = Regex("^#{1,3}\\s+").find(line)
            when {
                headingMatch != null -> withStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontSize = baseFontSize * 1.08f)
                ) {
                    appendInlineMarkdown(line.substring(headingMatch.range.last + 1), baseFontSize)
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    append("• ")
                    appendInlineMarkdown(line.drop(2), baseFontSize)
                }
                else -> appendInlineMarkdown(originalLine, baseFontSize)
            }
            if (index < lines.lastIndex) append('\n')
        }
    }

private fun AnnotatedString.Builder.appendInlineMarkdown(text: String, baseFontSize: TextUnit) {
    var index = 0
    while (index < text.length) {
        if (text.startsWith("**", index)) {
            val end = text.indexOf("**", index + 2)
            if (end > index + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(index + 2, end))
                }
                index = end + 2
                continue
            }
        }
        if (text[index] == '`') {
            val end = text.indexOf('`', index + 1)
            if (end > index + 1) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0xFF202020),
                        color = Color(0xFFF0F0F0),
                        fontSize = baseFontSize * 0.94f
                    )
                ) {
                    append(text.substring(index + 1, end))
                }
                index = end + 1
                continue
            }
        }
        if (text[index] == '*') {
            val end = text.indexOf('*', index + 1)
            if (end > index + 1) {
                withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                    append(text.substring(index + 1, end))
                }
                index = end + 1
                continue
            }
        }
        append(text[index])
        index++
    }
}

private fun extractPreferredName(message: String): String? {
    val patterns = listOf(
        Regex("(?i)\\bmy name is\\s+([A-Za-z][A-Za-z'-]*(?:\\s+[A-Za-z][A-Za-z'-]*){0,2})"),
        Regex("(?i)\\bmera naam\\s+([A-Za-z][A-Za-z'-]*(?:\\s+[A-Za-z][A-Za-z'-]*){0,2})"),
        Regex("(?i)\\bcall me\\s+([A-Za-z][A-Za-z'-]*(?:\\s+[A-Za-z][A-Za-z'-]*){0,2})")
    )
    val captured = patterns.firstNotNullOfOrNull { pattern ->
        pattern.find(message)?.groupValues?.getOrNull(1)
    } ?: return null
    val cleaned = captured
        .split(Regex("(?i)\\s+(?:and|but|because|i|you|we|hai|he|hoon|hu)\\b"))
        .firstOrNull()
        .orEmpty()
        .trim()
        .trim('.', ',', '!', '?', ':', ';')
        .take(60)
    return cleaned.takeIf { it.isNotBlank() }
}

private fun smartChatTitle(text: String): String {
    val clean = text.replace(Regex("\\s+"), " ").trim()
    val lower = clean.lowercase()
    val category = when {
        listOf("qlik", "dashboard", "nprinting", "qvd", "set analysis").any { it in lower } -> "Qlik Sense Help"
        listOf("resume", "cv", "job", "interview", "recruiter").any { it in lower } -> "Career & Jobs"
        listOf("kotlin", "android", "python", "code", "bug", "error").any { it in lower } -> "Coding Help"
        listOf("trip", "travel", "hotel", "flight", "itinerary").any { it in lower } -> "Travel Planning"
        listOf("budget", "loan", "investment", "ipo", "finance").any { it in lower } -> "Finance"
        listOf("email", "message", "proofread", "rewrite", "translate").any { it in lower } -> "Writing Help"
        else -> ""
    }
    if (category.isNotBlank()) return category
    return clean.take(38).trimEnd().let { if (clean.length > 38) "$it…" else it }.ifBlank { "New chat" }
}

private fun newEmptyChat(): ChatSession = ChatSession(
    id = UUID.randomUUID().toString(),
    title = "New chat",
    updatedAt = System.currentTimeMillis(),
    messages = emptyList()
)

private fun makeChatTitle(messages: List<ChatMessage>): String {
    val firstUserMessage = messages.firstOrNull { it.isUser && it.text.isNotBlank() }?.text
    return firstUserMessage?.let(::smartChatTitle) ?: "New chat"
}

/** Stores user-curated personal memories locally in this app's private SharedPreferences. */
private object MemoryStore {
    private const val PREFS_NAME = "kairo_personal_memory"
    private const val KEY_MEMORIES = "memories"
    private const val MAX_MEMORIES = 40
    private const val MAX_MEMORY_LENGTH = 300

    fun load(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MEMORIES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.optString(index).trim().take(MAX_MEMORY_LENGTH)
                    if (value.isNotBlank() && none { it.equals(value, ignoreCase = true) } && size < MAX_MEMORIES) {
                        add(value)
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, memories: List<String>) {
        val array = JSONArray()
        memories.asSequence()
            .map { it.trim().take(MAX_MEMORY_LENGTH) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(MAX_MEMORIES)
            .forEach(array::put)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MEMORIES, array.toString())
            .apply()
    }
}

/** Stores independent chat threads in this app's private SharedPreferences. */
private object ConversationStore {
    private const val PREFS_NAME = "jarvis_conversations_v2"
    private const val KEY_SESSIONS = "sessions"
    private const val MAX_SESSIONS = 100
    private const val MAX_MESSAGES_PER_SESSION = 200

    fun load(context: Context): List<ChatSession> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SESSIONS, null) ?: return emptyList()

        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<ChatSession>()
            for (i in 0 until array.length()) {
                val sessionObject = array.optJSONObject(i) ?: continue
                val id = sessionObject.optString("id").ifBlank { UUID.randomUUID().toString() }
                val title = sessionObject.optString("title", "New chat").ifBlank { "New chat" }
                val updatedAt = sessionObject.optLong("updatedAt", System.currentTimeMillis())
                val messagesArray = sessionObject.optJSONArray("messages") ?: JSONArray()
                val messages = mutableListOf<ChatMessage>()
                for (j in 0 until messagesArray.length()) {
                    val messageObject = messagesArray.optJSONObject(j) ?: continue
                    messages.add(
                        ChatMessage(
                            text = messageObject.optString("text", ""),
                            isUser = messageObject.optBoolean("isUser", false)
                        )
                    )
                }
                result.add(ChatSession(id, title, updatedAt, messages))
            }
            result.sortedByDescending { it.updatedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, sessions: List<ChatSession>) {
        val array = JSONArray()
        sessions.sortedByDescending { it.updatedAt }
            .take(MAX_SESSIONS)
            .forEach { session ->
                val sessionObject = JSONObject()
                sessionObject.put("id", session.id)
                sessionObject.put("title", session.title)
                sessionObject.put("updatedAt", session.updatedAt)

                val messagesArray = JSONArray()
                session.messages.takeLast(MAX_MESSAGES_PER_SESSION).forEach { message ->
                    val messageObject = JSONObject()
                    messageObject.put("text", message.text)
                    messageObject.put("isUser", message.isUser)
                    messagesArray.put(messageObject)
                }
                sessionObject.put("messages", messagesArray)
                array.put(sessionObject)
            }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSIONS, array.toString())
            .apply()
    }
}

private fun importModelToPrivateStorage(
    context: Context,
    uri: Uri,
    modelDirectory: File
): File {
    val displayName = getDisplayName(context, uri) ?: "local-model.litertlm"
    if (!displayName.endsWith(".litertlm", ignoreCase = true)) {
        throw IOException("Selected file '$displayName' does not have a .litertlm extension. Choose a LiteRT-LM model bundle.")
    }
    if (!modelDirectory.exists() && !modelDirectory.mkdirs()) {
        throw IOException("Could not create model storage folder.")
    }

    val safeBaseName = displayName
        .replace('\\', '_')
        .replace('/', '_')
        .replace(Regex("[^A-Za-z0-9._() -]"), "_")
        .trim()
        .take(120)
        .ifBlank { "local-model.litertlm" }
    val requestedName = if (safeBaseName.endsWith(".litertlm", true)) safeBaseName else "$safeBaseName.litertlm"
    var destination = File(modelDirectory, requestedName)
    var suffix = 2
    // Never overwrite an installed model; preserve current and previous model files as rollback options.
    while (destination.exists()) {
        val stem = requestedName.removeSuffix(".litertlm")
        destination = File(modelDirectory, "$stem ($suffix).litertlm")
        suffix++
    }

    val temporaryFile = File(modelDirectory, "${destination.name}.part")
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Android could not open the selected model file.")
        input.use { source ->
            FileOutputStream(temporaryFile).use { output ->
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.fd.sync()
            }
        }
        if (!temporaryFile.isFile || temporaryFile.length() <= 0L) {
            throw IOException("The copied model file is empty.")
        }
        if (!temporaryFile.renameTo(destination)) {
            Files.move(temporaryFile.toPath(), destination.toPath())
        }
        if (!destination.isFile || destination.length() <= 0L) {
            throw IOException("The model file could not be finalized in private storage.")
        }
        return destination
    } catch (e: Exception) {
        temporaryFile.delete()
        destination.delete()
        throw e
    }
}

private fun discoverLocalModels(modelDirectory: File): List<LocalModelEntry> {
    return modelDirectory.listFiles()
        ?.filter { it.isFile && it.name.endsWith(".litertlm", ignoreCase = true) && it.length() > 0L }
        ?.map { LocalModelEntry(it.name, it.length()) }
        ?.sortedWith(compareBy<LocalModelEntry> { it.fileName.lowercase() })
        ?: emptyList()
}

private fun localFileSizeLabel(sizeBytes: Long): String {
    if (sizeBytes <= 0L) return "Unknown size"
    val gib = sizeBytes / (1024.0 * 1024.0 * 1024.0)
    return if (gib >= 1.0) String.format(java.util.Locale.US, "%.2f GB", gib)
    else String.format(java.util.Locale.US, "%.0f MB", sizeBytes / (1024.0 * 1024.0))
}

private fun getDisplayName(context: Context, uri: Uri): String? {
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) return cursor.getString(nameIndex)
    }
    return null
}

private fun createEngine(context: Context, modelFile: File): Engine {
    if (!modelFile.isFile || modelFile.length() <= 0L) {
        throw IOException("Model file was not found in app storage.")
    }

    val engine = Engine(
        EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.CPU(),
            cacheDir = context.cacheDir.absolutePath
        )
    )
    try {
        engine.initialize()
        return engine
    } catch (e: Exception) {
        try {
            engine.close()
        } catch (_: Exception) {
        }
        throw e
    }
}
