# 🤖 Gemini Memory Chatbot (Android Kotlin + Jetpack Compose)

> A modern, conversational Android AI Chatbot built with **Kotlin**, **Jetpack Compose (Material 3)**, **Google Gemini 1.5 API**, and **Room Database** for persistent, long-term conversational memory across app restarts.

---

## 🌟 Key Features

### 1. 💬 Gemini AI Chatbot
- Powered by Google's latest **Gemini 1.5 Flash** model.
- Network communication built using **Retrofit 2** + **OkHttp 3** with structured coroutines.
- Context-aware responses injected with your personal facts via dynamic system instructions.

### 2. 🧠 Persistent Long-Term Memory (Room Database)
- **Automatic Fact Extraction**: Listens for personal statements (e.g., *"My name is Charmy"*, *"I study Computer Engineering at NMIMS"*) and saves them as key-value entities in an SQLite/Room database.
- **Cross-Session Memory**: Even after clearing the app from memory or restarting your phone, asking *"What is my name?"* or *"Where do I study?"* immediately recalls your saved facts.
- **Memory Dashboard**: Dedicated UI screen to inspect, manually add, edit, or delete stored memories.

### 3. 🎙️ Voice-to-Text & Text-to-Speech
- **Speech Input**: Microphone button beside the chat input converts speech into editable text in real-time.
- **Permission Handling**: Runtime `RECORD_AUDIO` permission request integrated seamlessly using Jetpack Compose `rememberLauncherForActivityResult`.
- **Audio Fallback**: Native `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` system modal ensures 100% compatibility across all physical devices and Android emulators.
- **Voice Playback (TTS)**: Built-in Android `TextToSpeech` engine with a Speaker icon (🔊/⏹) on each bot message to read responses aloud.

### 4. 📤 Share & Export Conversation
- Tap the **Share** icon in the Top App Bar to export the current conversation transcript.
- Uses Android's native `Intent.ACTION_SEND` chooser to share formatted chat logs with WhatsApp, Email, Notes, Telegram, or any installed app.

### 5. 🗑️ Clear Chat History
- Tap the **Trash** icon to clear current conversation messages from the screen and SQLite database.
- **Safe Deletion**: Clears chat messages without erasing your saved personal memories from the Room database.

### 6. 📝 AI Conversation Summaries
- Automatically analyzes the entire conversation history and generates concise, structured bullet-point summaries.
- Summaries can be named and saved into Room database for future review.

---

## 🚀 1. Obtain a Gemini API Key

1. **Access Google AI Studio**: Visit [Google AI Studio (https://aistudio.google.com/)](https://aistudio.google.com/) and sign in with your Google account.
2. **Generate the API Key**: Click **"Get API key"** ➔ **"Create API key in new project"**.
3. **Store the Key Locally**:
   - Open `local.properties` in the root folder of this project (this file is git-ignored and never committed).
   - Add your API key:
     ```properties
     GEMINI_API_KEY=your_actual_api_key_here
     ```
   - Gradle automatically reads this property at build time and exposes it safely via `BuildConfig.GEMINI_API_KEY`.
   - Alternatively, you can configure or change your API key directly inside the running app by tapping the **Key (🔑)** icon in the top toolbar.

---

## 🏗️ 2. Project Architecture & Codebase Overview

The project follows the standard **Unidirectional Data Flow (UDF)** and **MVVM Repository Pattern**:

```
app/src/main/java/com/fahim/mad_lab_compose/
├── MainActivity.kt                  # Main entry point with permission launchers & screen routing
├── ChatbotApplication.kt            # Application subclass initializing Room DB singleton & Repository
├── data/
│   ├── api/
│   │   ├── ApiClient.kt             # OkHttp & Retrofit client builder with timeout interceptors
│   │   ├── GeminiApiService.kt      # Retrofit interface for Google Gemini API endpoint
│   │   └── GeminiModels.kt          # Request & Response data models for Gemini payload
│   ├── database/
│   │   ├── AppDatabase.kt           # Room Database definition with schema migrations
│   │   ├── MemoryDao.kt             # DAO for persistent user memories
│   │   ├── MessageDao.kt            # DAO for chat history messages
│   │   ├── SummaryDao.kt            # DAO for conversation summaries
│   │   └── Entities.kt              # Room entities (MemoryEntity, MessageEntity, SummaryEntity)
│   └── repository/
│       ├── ChatRepository.kt        # Orchestrates network calls, database transactions & memory extraction
│       └── MemoryExtractor.kt       # Regex & NLP pattern rule-engine for personal fact detection
├── ui/
│   ├── ChatScreen.kt                # Jetpack Compose Chat UI, bubbles, input bar & dialogs
│   ├── MemoryScreen.kt              # Interactive Memory Management Dashboard
│   ├── ApiKeyDialog.kt              # In-app API key configuration dialog
│   ├── ChatViewModel.kt             # ViewModel managing StateFlow<ChatState> & business logic
│   ├── ChatState.kt                 # Immutable UI state dataclass
│   └── theme/                       # Material 3 typography, color schemes & dynamic theme
└── voice/
    └── VoiceHelper.kt               # SpeechRecognizer (STT) and TextToSpeech (TTS) manager
```

---

## 🔒 3. API Key Security (Best Practices)

### Never Hardcode
- The Gemini API key is **never** hardcoded as a string literal in any Kotlin file, XML resource, or committed Gradle file.
- The repository was checked with automated scanners to guarantee zero secrets in commit diffs and git logs.

### Git Ignore & Template
- `local.properties` is strictly declared in [`.gitignore`](.gitignore).
- A [`local.properties.example`](local.properties.example) template file is committed so teammates and reviewers know how to configure their local environment.

### CI/CD Environment Fallback
- `app/build.gradle.kts` supports continuous integration pipelines by falling back to environment variables when `local.properties` is absent:
  ```kotlin
  val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY")
      ?: System.getenv("GEMINI_API_KEY")
      ?: ""
  ```

### Client-Side Encryption at Rest
- In production, sensitive keys configured at runtime are encrypted using **AES-256-GCM** backed by the **Android Keystore** (`KeyGenParameterSpec`) and stored in `EncryptedSharedPreferences`.
- The key is decrypted only in memory when initializing the network client and is never logged to Logcat or toasted.

### Obfuscation via R8
- Release builds enable R8 shrinking and obfuscation (`isMinifyEnabled = true`) using [`proguard-rules.pro`](app/proguard-rules.pro) to prevent reverse engineering of string constants and class definitions.

### Production Recommendations (Beyond Client-Side Security)
> [!NOTE]
> Client-side encryption raises the security bar against basic decompilation, but client apps cannot be 100% trusted with root access. A production-grade architecture should:
> 1. **Backend Proxy**: Place Gemini API calls behind a secure backend server (e.g. Node.js/Go/Python on Cloud Functions/Cloud Run) that handles authentication and rate limiting.
> 2. **Firebase App Check**: Attest device integrity with Play Integrity API so only genuine instances of the app can communicate with the backend.
> 3. **Restricted API Keys**: Restrict Google Cloud API keys by Android package name and SHA-1 signing certificate fingerprint.

---

## 🎨 4. Jetpack Compose UI Architecture

- **`LazyColumn` with Stable Keys**: Renders chat messages with smooth performance and zero jank. Auto-scrolls to the newest message whenever a response is received.
- **State Hoisting**: All UI state is bundled in an immutable `ChatState` dataclass, exposed as `StateFlow<ChatState>` from `ChatViewModel`, and observed reactively in composables.
- **Loading & Error States**: Displays animated multi-dot typing indicators while Gemini generates responses, and animated dismissible error banners for network or quota issues.
- **Material 3 Theming**: Full support for dynamic colors, typography, rounded card bubbles, badges, and dark mode.
- **Compose Previews (`@Preview`)**: Includes interactive testing previews in `ChatScreen.kt` and `MemoryScreen.kt` for rapid UI inspection inside Android Studio.

---

## 🧪 5. Testing & Running the App

### Running in Android Studio
1. Open the project in **Android Studio (Ladybug / Koala / Meerkat)**.
2. Ensure **JDK 21** or **JDK 17** is configured under *Settings ➔ Build, Execution, Deployment ➔ Build Tools ➔ Gradle ➔ Gradle JDK*.
3. Add your `GEMINI_API_KEY` to `local.properties`.
4. Select your device or emulator (e.g. `Medium Phone API 35`) in the toolbar.
5. Click the green **Run (▶)** button (or press `Shift + F10`).

### Running Automated Unit Tests
Run the automated test suite from the terminal or Android Studio:
```bash
./gradlew test
```

### Building Debug APK
```bash
./gradlew assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📋 6. Submission Checklist

- [x] **Roll Number Branching**: Git feature branches created and pushed (`feature/voice-chat`, `feature/conversation-summary`, `feature/chat-sharing`).
- [x] **No Secrets in Git History**: All API keys excluded from version control; `local.properties` git-ignored.
- [x] **`local.properties.example` Included**: Template file committed for easy onboarding.
- [x] **Room Persistent Memory**: User facts survive full app restarts and task removals.
- [x] **Voice Recognition & TTS**: Seamless audio speech-to-text input and spoken audio output.
- [x] **Conversation Export**: Share conversation transcript via standard Android Share Sheet.
- [x] **Clean Architecture**: Repository pattern, Coroutines, MVVM, Room DB, Retrofit & Jetpack Compose Material 3.
