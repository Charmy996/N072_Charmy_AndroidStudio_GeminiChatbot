# Gemini AI Chatbot with Persistent Memory (Android Studio + Jetpack Compose)

A modern, clean, and robust Android chatbot application built with **Kotlin + Jetpack Compose** and the **Google Gemini API**. The chatbot features **long-term persistent conversational memory** powered by **Room Database**, allowing it to remember personal details (name, college, course, goals, preferences, and more) across conversations and complete app restarts.

---

## 🌟 Key Features

* **Persistent Conversational Memory**: Remembers user facts across app restarts using local Room Database.
* **Smart Memory Extraction**: Automatically extracts facts from phrases like *"My name is..."*, *"I study Computer Engineering at NMIMS"*, *"My favourite subject is..."*, *"My goal is..."*, etc.
* **No Duplicate Memories**: Intelligently updates existing memory keys (e.g. updating a favourite subject or goal) rather than creating clutter.
* **Memory Management Screen**: Dedicated screen to inspect all remembered facts, edit them, delete individual memories, or clear memory completely.
* **Material 3 ChatGPT-Style UI**: Modern chat bubble UI with bot avatars, formatted timestamps, animated typing indicators, quick prompt suggestion chips, and copy-to-clipboard functionality.
* **Safe API Key Architecture**: API key is loaded securely from `local.properties` via `BuildConfig` (never hardcoded in source code or Git). A runtime in-app key configuration dialog is also provided.
* **Robust Error Handling**: Gracefully handles no internet, missing API key, rate limits (HTTP 429), timeouts, and API errors without crashing.

---

## 🏗️ Architecture & Memory Flow

The application follows the recommended **UI → ViewModel → Repository → Room Database / Gemini API** architecture:

```
┌─────────────────────────────────────────────────────────┐
│                    Jetpack Compose UI                   │
│         (ChatScreen, MemoryScreen, ApiKeyDialog)        │
└───────────────────────────▲─────────────────────────────┘
                            │ (StateFlow / Actions)
┌───────────────────────────▼─────────────────────────────┐
│                      ChatViewModel                      │
└───────────────────────────▲─────────────────────────────┘
                            │ (Coroutines / ResultState)
┌───────────────────────────▼─────────────────────────────┐
│                      ChatRepository                     │
└───────────────┬─────────────────────────┬───────────────┘
                │                         │
                ▼                         ▼
  ┌──────────────────────────┐  ┌───────────────────┐
  │      Room Database       │  │    Gemini API     │
  │ (MemoryDao, MessageDao)  │  │ (Retrofit/OkHttp) │
  └──────────────────────────┘  └───────────────────┘
```

### Complete End-to-End Memory Flow:

1. **User Sends Message**: e.g., *"My name is Charmy."* or *"I study Computer Engineering at NMIMS."*
2. **Memory Extraction**: `MemoryExtractor` parses the message to detect key facts:
   - Key: `Name`, Value: `Charmy`
   - Key: `Course`, Value: `Computer Engineering`
   - Key: `College`, Value: `NMIMS`
3. **Room Database Upsert**: Memories are saved or updated in the `memories` table (`MemoryEntity`) with current timestamps. If a key already exists, it is updated to avoid duplicates.
4. **Chat History Save**: User's message is persisted to the `messages` table (`MessageEntity`).
5. **Memory Context Injection**: `ChatRepository` retrieves all stored memories from Room and injects them into Gemini's `systemInstruction`:
   ```
   --- USER SAVED MEMORIES ---
   • Name: Charmy
   • College: NMIMS
   • Course: Computer Engineering
   ---------------------------
   ```
6. **Gemini API Call**: Retrofit sends the request (context + message history) to Google Gemini API (`gemini-1.5-flash`).
7. **Response Display & Persistence**: Gemini answers in a personalized manner (e.g. *"Nice to meet you, Charmy!"*), and the response is saved to Room so conversation history persists.
8. **Memory Management**: The user can open the Memory screen at any time to view, delete, or clear stored facts.

---

## 🗄️ Database Structure

### 1. `MemoryEntity` (`memories` table)
| Field | Type | Description |
|---|---|---|
| `id` | `Long` (Primary Key) | Auto-generated ID |
| `key` | `String` (Unique Index) | Fact category (e.g. `Name`, `College`, `Course`, `Goal`) |
| `value` | `String` | Fact content (e.g. `Charmy`, `NMIMS`) |
| `createdAt` | `Long` | Timestamp of creation |
| `updatedAt` | `Long` | Timestamp of last update |

### 2. `MessageEntity` (`messages` table)
| Field | Type | Description |
|---|---|---|
| `id` | `Long` (Primary Key) | Auto-generated ID |
| `conversationId` | `String` | Identifier for conversation thread |
| `sender` | `String` | `"user"` or `"bot"` |
| `message` | `String` | Message content |
| `timestamp` | `Long` | Timestamp of message |

---

## 🔑 Gemini API Key Setup

### Method 1: Via `local.properties` (Recommended)

1. Get your free Gemini API key from [Google AI Studio](https://aistudio.google.com/).
2. Open `local.properties` in the root of the project.
3. Add your key:
   ```properties
   GEMINI_API_KEY=YOUR_ACTUAL_GEMINI_API_KEY_HERE
   ```
4. Build and run the app. Gradle automatically injects the key into `BuildConfig.GEMINI_API_KEY` without committing it to version control.

### Method 2: In-App Dialog

If you run the app directly, you can tap the 🔑 **Key** icon in the top app bar or tap the error banner to paste your API key directly in the app.

---

## 🚀 How to Run the Project

1. Open **Android Studio** (Ladybug / Koala / Hedgehog or newer).
2. Open the project folder `N072_Charmy_AndroidStudio_GeminiChatbot`.
3. Set your `GEMINI_API_KEY` in `local.properties`.
4. Sync Gradle and press **Run 'app'** (or press `Shift + F10`) on an Android Emulator or physical device (Android 8.0 / API 26+).
5. Alternatively, build from command line:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 🧪 Testing Scenarios Walkthrough

### Test 1 — Basic Chat
* **Input**: `"Hello"`
* **Output**: Gemini responds with a friendly greeting.

### Test 2 — Remember Name
* **Input**: `"My name is Charmy."`
* **Bot**: `"Nice to meet you, Charmy! ..."`
* **Input**: `"What is my name?"`
* **Bot**: `"Your name is Charmy."`

### Test 3 — App Restart Persistence
* **Input**: `"I study Computer Engineering at NMIMS."`
* **Action**: Close the application completely (kill from recent apps).
* **Action**: Re-open the application.
* **Input**: `"Where do I study?"`
* **Bot**: Accurately answers that you study Computer Engineering at NMIMS using the Room database.

### Test 4 — Update Memory
* **Input**: `"My favourite subject is Operating Systems."`
* **Input**: `"My favourite subject is Computer Networks."`
* **Action**: Open the **Memory** screen (brain icon at top right).
* **Result**: The memory for `Favourite Subject` is updated to `Computer Networks` without creating duplicate keys.

### Test 5 — Clear / Delete Memory
* **Action**: Open the **Memory** screen.
* **Action**: Tap the trash icon next to a memory or tap **Clear All**.
* **Input**: `"What is my favourite subject?"`
* **Bot**: Gemini will state that it does not know your favourite subject since the memory was removed.

---

## 📂 Project Structure

```
app/src/main/java/com/fahim/mad_lab_compose/
├── ChatbotApplication.kt            # Application class (initializes DB & Repository)
├── MainActivity.kt                  # Activity host & Screen routing
│
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt           # Room Database definition
│   │   ├── MemoryDao.kt             # Room DAO for memories
│   │   ├── MemoryEntity.kt          # Room entity for facts
│   │   ├── MessageDao.kt            # Room DAO for chat history
│   │   └── MessageEntity.kt         # Room entity for messages
│   │
│   ├── memory/
│   │   └── MemoryExtractor.kt       # Smart fact & attribute extraction
│   │
│   └── repository/
│       └── ChatRepository.kt        # Repository coordinating DB & Gemini API
│
├── network/
│   ├── ApiClient.kt                 # Retrofit & OkHttp client
│   ├── GeminiApiService.kt          # Gemini generateContent API interface
│   ├── GeminiRequest.kt             # Request payload DTOs
│   └── GeminiResponse.kt            # Response payload DTOs
│
└── ui/
    ├── ApiKeyDialog.kt              # In-app API key configuration dialog
    ├── ChatScreen.kt                # Main ChatGPT-style conversation screen
    ├── ChatState.kt                 # UI State definition
    ├── ChatViewModel.kt             # ViewModel & ViewModelFactory
    ├── MemoryScreen.kt              # Saved memories management screen
    └── theme/
        ├── Color.kt                 # Gemini theme colors
        ├── Theme.kt                 # Material 3 dynamic theme
        └── Type.kt                  # Typography definitions
```

---

## 🛡️ Required Permissions

* `android.permission.INTERNET`: Required for communicating with Google's Gemini API endpoints.
* `android.permission.ACCESS_NETWORK_STATE`: Used to detect network connectivity.
