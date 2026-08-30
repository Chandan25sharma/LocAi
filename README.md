# LocAi

### Private. Local. Offline. Yours.

**LocAi** is an open-source, privacy-first AI assistant designed to run **directly on your Android device**.

No cloud.
No mandatory account.
No API key.
No internet required for inference.
Your conversations stay on your device.

> **AI that runs where you do — locally.**

---

## ✨ Why LocAi?

Most modern AI assistants depend on cloud servers. Your messages are sent somewhere else, processed remotely, and returned to your device.

LocAi takes a different approach.

The goal is simple:

**Bring AI directly to your phone.**

LocAi is being built around **local inference**, allowing an AI model to run directly on Android hardware instead of relying on a remote AI API.

### 🔐 Privacy First

Your conversations should belong to you.

LocAi is designed to work without sending your conversations to a cloud AI provider.

* 🔒 Local conversations
* 📱 On-device inference
* 🌐 Offline capable
* 🚫 No mandatory cloud account
* 🔑 No API key required for local inference
* 📴 Designed to work without an internet connection

---

## 🚀 Features

* 🤖 **Local AI Chat**
* 📱 **Android-first**
* 📴 **Offline inference**
* 🔐 **Privacy-focused**
* 💬 **Local conversation history**
* ⚡ **No cloud dependency for inference**
* 🧠 **Support for local/quantized AI models**
* 🎨 **Clean and simple chat interface**
* 🔧 **Open-source and customizable**

> Features are actively evolving as LocAi develops.

---

## 🧠 How It Works

LocAi is designed around an on-device AI architecture:

```text
┌──────────────────────────────┐
│          Android App         │
│                              │
│  ┌────────────────────────┐  │
│  │       Chat UI           │  │
│  └───────────┬────────────┘  │
│              │               │
│  ┌───────────▼────────────┐  │
│  │   Local AI Runtime      │  │
│  └───────────┬────────────┘  │
│              │               │
│  ┌───────────▼────────────┐  │
│  │     Local AI Model      │  │
│  └───────────┬────────────┘  │
│              │               │
│  ┌───────────▼────────────┐  │
│  │ Local Conversation DB   │  │
│  └────────────────────────┘  │
│                              │
└──────────────────────────────┘

             │
             X
       No cloud required
```

The model runs locally, while the application handles the user interface, model interaction, and conversation history.

---

## 🌐 Offline by Design

LocAi aims to remain useful even when there is no internet connection.

Once the required application and AI model are available on the device, the core chat experience can operate locally.

That means you can use your AI assistant:

* ✈️ On a flight
* 🏔️ In remote areas
* 🚇 Without network coverage
* 🔒 Where privacy is important
* 📵 When you simply don't want to use the cloud

Internet access may still be useful for optional features such as downloading models, updates, or other explicitly online functionality.

---
screen_welcome3.png

## 🧩 Local Models

LocAi is intended to support efficient AI models that can run on mobile hardware.

Depending on the device, users may be able to use different model sizes and quantization levels.

For example:

```text
Small model
   ↓
Lower memory usage
   ↓
Faster mobile inference

Larger model
   ↓
Higher memory requirements
   ↓
Potentially better responses
```

The exact models supported by LocAi will depend on the runtime and hardware capabilities.

---

## 📱 Android

LocAi is being developed with Android devices as the primary target.

The long-term goal is to make local AI practical on everyday smartphones rather than requiring a powerful server or dedicated GPU.

### Target experience

```text
Install LocAi
      ↓
Download/select a model
      ↓
Start chatting
      ↓
AI runs locally
      ↓
No cloud required
```

---

## 🔒 Privacy

LocAi follows a **local-first** philosophy.

The core principle is:

> **Your conversations should not need to leave your device to use an AI assistant.**

LocAi does not require sending your prompts to a remote AI service for local inference.

However, users should always review the permissions, dependencies, model sources, and optional network features of the specific version they install.

---

## ⚠️ Important

LocAi is an independent open-source project.

It is **not affiliated with or endorsed by OpenAI, Google, Anthropic, Meta, Microsoft, or any other AI provider**.

Local AI models may produce incorrect, biased, unsafe, or misleading information. Always verify important information independently.

---

## 🛠️ Development

Clone the repository:

```bash
git clone https://github.com/Chandan25sharma/LocAi.git

cd LocAi
```

Then follow the project-specific Android build instructions.

---

## 🗺️ Roadmap

### Phase 1 — Foundation

* [x] Project initialization
* [ ] Android chat interface
* [ ] Local model integration
* [ ] Basic inference
* [ ] Conversation history

### Phase 2 — Offline AI

* [ ] Fully offline chat
* [ ] Model management
* [ ] Model download/import
* [ ] Quantized model support
* [ ] Memory optimization
* [ ] Streaming responses

### Phase 3 — Better Assistant

* [ ] System prompts
* [ ] Multiple conversations
* [ ] Conversation search
* [ ] Context management
* [ ] Custom model parameters
* [ ] Voice input
* [ ] Text-to-speech

### Phase 4 — Advanced Local AI

* [ ] Vision models
* [ ] Local document processing
* [ ] Local RAG
* [ ] On-device embeddings
* [ ] Tool/function calling
* [ ] Agent capabilities
* [ ] Background tasks

### Phase 5 — Mobile Optimization

* [ ] Android GPU acceleration
* [ ] Hardware-specific optimization
* [ ] Memory-efficient inference
* [ ] Battery optimization
* [ ] Faster startup
* [ ] Improved thermal management

---

## 🎯 Project Philosophy

LocAi is built around a few simple ideas:

### Privacy

AI shouldn't require giving your conversations to someone else's server.

### Ownership

Users should be able to choose which model they run.

### Accessibility

Local AI should become practical on ordinary consumer devices.

### Offline

AI should remain useful even when the internet doesn't exist.

### Open Source

The technology should be inspectable, modifiable, and community-driven.

---

## 🤝 Contributing

Contributions are welcome.

You can contribute by:

* Reporting bugs
* Suggesting features
* Improving Android performance
* Adding model support
* Improving the UI
* Optimizing inference
* Improving documentation
* Testing on different Android devices

Fork the repository, create a branch, make your changes, and submit a pull request.

---

##  License

See the [`LICENSE`](LICENSE) file for the license applicable to this project.

---

##  Support the Project

If you find LocAi interesting, consider giving the repository a ⭐ on GitHub.

Your support helps the project grow and encourages further development of private, local AI.

---

# 🚀 LocAi

**AI on your phone.
Offline when you need it.
Private by design.**

[GitHub](https://github.com/Chandan25sharma/LocAi)
