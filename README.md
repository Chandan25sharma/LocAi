<p align="center">
  <img src="Logo-aa.png" alt="LocAi logo" width="120" />
</p>

<h1 align="center">LocAi</h1>

<p align="center">
  <b>Private. Local. Offline. Yours.</b><br/>
  An open-source AI assistant that runs <b>entirely on your Android phone</b>, with no cloud, no account and no API key.
</p>

<p align="center">
  <a href="LICENSE"><img alt="License: Apache 2.0" src="https://img.shields.io/badge/license-Apache%202.0-blue.svg"></a>
  <img alt="Platform: Android 8.0+" src="https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84.svg">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF.svg">
  <img alt="Offline" src="https://img.shields.io/badge/inference-100%25%20on--device-6C5DD3.svg">
</p>

---

## Screenshots

<table>
  <tr>
    <td align="center"><img src="docs/screenshots/setup.png" width="220" alt="Model setup"/><br/><sub>One-time model setup</sub></td>
    <td align="center"><img src="docs/screenshots/home.png" width="220" alt="Home with topics"/><br/><sub>Pick a topic &amp; starter question</sub></td>
    <td align="center"><img src="docs/screenshots/chat.png" width="220" alt="Chat with markdown"/><br/><sub>Streaming replies with Markdown</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/drawer.png" width="220" alt="Chat history drawer"/><br/><sub>Search, pin &amp; rename chats</sub></td>
    <td align="center"><img src="docs/screenshots/vision.png" width="220" alt="Ask about a photo"/><br/><sub>Ask about a photo (optional model)</sub></td>
    <td align="center"><img src="docs/screenshots/settings.png" width="220" alt="Settings"/><br/><sub>Models &amp; generation settings</sub></td>
  </tr>
</table>

---

## Why LocAi?

Most AI assistants send every message to a remote server. LocAi doesn't. The language model runs **on your phone's own hardware**, and your conversations are stored **only on your device**.

- **Private:** your prompts and chats never leave the phone.
- **Offline:** once the model is downloaded, chat works with no internet (on a flight, in the mountains, underground).
- **No account, no API key:** install, download a model once, start chatting.
- **Open source:** every line is here to inspect, fork and improve.

---

## Features

### Chat
- **Streaming replies** with a typing indicator, and a **Stop** button to end a reply early (the partial answer is kept)
- **Markdown rendering**: headings, lists, **bold**, *italic*, `inline code` and code blocks with a one-tap **Copy**
- Message actions: **Copy**, **Share**, **Read aloud** and **Regenerate**
- **Retry** when a reply fails to generate
- **Voice input** through the phone's speech recognizer (prefers offline recognition)
- **Read aloud** through Android's built-in text-to-speech (works offline)

### Topics
Each chat starts with a topic that shapes the assistant's persona and starter questions:

| Topic | Good for |
|---|---|
| General | Everyday questions, writing, ideas |
| Health | General wellness information (not medical advice) |
| Finance | Budgeting, saving and money basics (not financial advice) |
| Legal | How legal concepts and processes generally work (not legal advice) |
| Tech | Coding help, debugging, explanations |

### Chat history
- All conversations are saved locally in a Room (SQLite) database
- **Search** by chat title or message text
- **Pin**, **rename** and **delete** chats
- Chats grouped by Pinned / Today / Yesterday / Last 7 days / Older
- **Export** a chat: share it as text, or save it as a Markdown (`.md`) file
- **On-device memory**: relevant past exchanges from the same topic are retrieved locally and added to the prompt

### Images (optional)
- **Image understanding**: attach a photo and ask about it (requires the optional vision model)
- **Image generation**: text-to-image with Stable Diffusion v1.5 (requires manually installing the model files)

### Personalisation
- Short onboarding picks a model that suits how you'll use LocAi
- Adjustable **temperature** and **top-K**
- Follows the system light or dark theme

---

## On-device models

The app ships **without** model weights. You download a model once, inside the app, and after that no network is needed.

| Model | Size (approx.) | Purpose | License |
|---|---|---|---|
| Qwen2.5-0.5B-Instruct (q8), "Fast & Light" | ~520 MB | Default chat model, runs on almost any phone | Apache-2.0 |
| Qwen2.5-1.5B-Instruct (q8), "Best Quality" | ~1.5 GB | Sharper answers, needs a stronger phone | Apache-2.0 |
| Gemma 4 E2B (multimodal) | ~2.6 GB | Optional: ask questions about photos | See model card |
| Stable Diffusion v1.5 | Varies | Optional: generate images (manual install) | CreativeML Open RAIL-M |

Models come from the [`litert-community`](https://huggingface.co/litert-community) conversions on Hugging Face and run with [Google MediaPipe LLM Inference](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference).

---

## How it works

```text
┌─────────────────────────── Android device ───────────────────────────┐
│                                                                      │
│   Jetpack Compose UI  ──►  ViewModel  ──►  ChatRepository            │
│                                               │                      │
│                       ┌───────────────────────┼──────────────────┐   │
│                       ▼                       ▼                  ▼   │
│               HistoryRetriever         PromptBuilder      Room database
│            (past chats as memory)  (persona + context)   (chats, messages)
│                       └───────────┬───────────┘                      │
│                                   ▼                                  │
│                     MediaPipe LLM Inference (on-device)              │
│                     Qwen2.5 / Gemma model file in app storage        │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
                 ✕  No prompts or chats are sent to any server
```

---

## Privacy and network use

LocAi follows a **local-first** design:

- Prompts, replies, chat history and attached photos are stored in the app's **private internal storage**.
- Inference runs **on-device**. No prompt is sent to any AI service.
- The `INTERNET` permission is used **only to download model files** from Hugging Face when you ask it to.
- There is no analytics, no tracking and no account.
- Voice input uses your phone's own speech recognizer. Some recognizers may use the network even when offline mode is requested; this depends on your device.

| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Downloading model files |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` | Keeping a large model download running in the background |

---

## Requirements

- Android **8.0 (API 26)** or newer
- **64-bit ARM (arm64-v8a)** device
- About **1–4 GB free storage**, depending on the models you choose
- **4 GB+ RAM** recommended; 6 GB+ for the larger and vision models

---

## Install

Download the latest APK from the [**Releases**](https://github.com/Chandan25sharma/LocAi/releases) page, install it, then follow the in-app setup to download a model.

---

## Build from source

**Prerequisites:** Android Studio (recent stable), JDK 11+, Android SDK 36.

```bash
git clone https://github.com/Chandan25sharma/LocAi.git
cd LocAi

# Debug build (arm64-v8a)
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug

# Install on a connected device
./gradlew installDebug

# Run unit tests
./gradlew testDebugUnitTest
```

The APK is written to `app/build/outputs/apk/debug/`.

### Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| On-device AI | MediaPipe Tasks GenAI (LLM Inference), MediaPipe Image Generator |
| Storage | Room (SQLite), DataStore Preferences |
| Async | Kotlin Coroutines and Flow |
| Downloads | OkHttp with a foreground service |

### Project structure

```text
app/src/main/java/com/locai/app/
├── data/
│   ├── db/          Room entities, DAOs, migrations
│   ├── llm/         Model managers, downloader, prompt builder
│   ├── prefs/       DataStore settings
│   ├── repository/  ChatRepository: storage + memory + model
│   └── retrieval/   On-device history retrieval
├── domain/          Topics (categories) and user personas
├── service/         Foreground model-download service
└── ui/
    ├── components/  Logo, Markdown renderer, text-to-speech
    ├── home/        Main chat screen and drawer
    ├── imagegen/    Text-to-image screen
    ├── settings/    Settings screen
    ├── setup/       Onboarding and model download
    └── theme/       Colors, typography
```

---

## Roadmap

**Done**
- [x] On-device chat with streaming replies
- [x] Model download and management
- [x] Multiple conversations, search, pin, rename
- [x] Topic personas with starter questions
- [x] Local memory from past chats
- [x] Markdown rendering, copy, share, regenerate, stop
- [x] Voice input and text-to-speech
- [x] Chat export (text and Markdown file)
- [x] Image understanding and image generation (optional models)

**Next**
- [ ] Import your own `.task` / `.litertlm` models
- [ ] Chat with local documents (PDF, text)
- [ ] On-device embeddings for better memory search
- [ ] GPU acceleration and faster startup
- [ ] Encrypted chat backup and restore
- [ ] More languages

---

## Contributing

Contributions are welcome:

1. Fork the repo and create a branch: `git checkout -b feature/my-change`
2. Make your change and run `./gradlew testDebugUnitTest`
3. Open a pull request describing what changed and why

Bug reports, device test results (phone model, RAM, which model you used) and UI ideas are all valuable. Please open an [issue](https://github.com/Chandan25sharma/LocAi/issues).

---

## Disclaimer

- LocAi is an **independent open-source project**. It is **not affiliated with or endorsed by** Google, Alibaba Cloud, OpenAI, Anthropic, Meta, Microsoft, Stability AI or any other AI provider. Product and model names belong to their respective owners.
- Small on-device models **can be wrong**, biased or out of date. Always verify important information.
- Health, Finance and Legal answers are **general information only**, not professional medical, financial or legal advice. In an emergency, contact your local emergency services.
- The software is provided **"as is", without warranty of any kind** (see [LICENSE](LICENSE)).

---

## License

```text
Copyright 2026 Chandan Sharma

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this project except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

Third-party libraries and AI models keep their own licenses. See [NOTICE](NOTICE).

---

<p align="center">
  <b>AI on your phone. Offline when you need it. Private by design.</b><br/>
  If LocAi is useful to you, please give it a ⭐ on GitHub.
</p>
