# 🎯 Project: Real-Time AR Annotation App

---

## 🏗️ Architecture

```
Phone B (Field User)                    Phone A (Expert)
─────────────────────                   ─────────────────
Camera Stream ──── WebRTC Video ──────► View Live Feed
Show Annotations ◄─ WebRTC Data Channel ─ Draw Annotations

                        │
                    Firebase
                (Signaling ONLY)
              offer / answer / ICE
```

---

## 🛠️ Tech Stack

| Layer | Choice | Reason |
|---|---|---|
| Language | Kotlin | Modern, no limitations for this use case |
| UI | Jetpack Compose | Clean, modern |
| Rendering | Canvas over TextureView | 2D overlays, simpler than OpenGL |
| Real-time video | Stream WebRTC (Google's WebRTC compiled) | Actively maintained, same API |
| Signaling | Firebase Realtime DB | Free, fast, minimal setup |
| STUN | Google's free STUN server | Works across different networks |
| Architecture | MVVM + Coroutines + Flow | Clean separation |
| Serialization | Gson/Moshi | Annotation objects over data channel |

---

## 🧰 Core Utilities

| File | Purpose |
|---|---|
| `core/Config.kt` | Environment switch (`DEV`/`PROD`). Set `environment = Environment.PROD` before release |
| `core/AppLogger.kt` | Wrapper around `android.util.Log`. All 5 levels (`v/d/i/w/e`) + optional `Throwable`. Logs are silenced automatically in `PROD` via `Config.isLogEnabled` |

**Convention:** every class has `private const val TAG = "ClassName"` and uses `AppLogger.x(TAG, "message")`.

---

## 📁 Project Structure

```
app/
├── webrtc/
│   ├── WebRTCManager.kt          ← PeerConnection, data channel, ICE
│   ├── SignalingRepository.kt    ← Firebase offer/answer/ICE flows
│   └── IceCandidateModel.kt      ← Firebase-serializable ICE data class
├── annotation/
│   ├── AnnotationOverlayView.kt  ← Canvas drawing (Week 3)
│   ├── AnnotationViewModel.kt
│   └── models/
│       ├── Annotation.kt         ← sealed class: FreeDraw, Arrow, Circle
│       └── DataMessage.kt        ← sealed class: Added, Undone, ClearAll
├── ui/
│   ├── home/
│   │   ├── HomeScreen.kt         ← Create/Join room UI
│   │   └── HomeViewModel.kt
│   └── call/
│       ├── CallScreen.kt         ← Active session (video + overlay)
│       └── CallViewModel.kt      ← Orchestrates WebRTC + signaling
├── screens/
│   ├── SplashScreen.kt
│   ├── MainScreen.kt             ← Bottom nav (Home, History, Profile)
│   ├── HomeScreen.kt
│   ├── HistoryScreen.kt
│   └── ProfileScreen.kt
└── data/
    └── FirebaseRepository.kt     ← Room existence, 6-digit code generator
```

---

## 📐 Key Design Decisions

**Annotations as objects, not images**
```kotlin
sealed class Annotation {
    data class FreeDraw(
        val id: String,
        val points: List<PointF>,
        val color: Int,
        val width: Float
    ) : Annotation()
    data class Arrow(val id: String, val start: PointF, val end: PointF) : Annotation()
    data class Circle(val id: String, val center: PointF, val radius: Float) : Annotation()
}
```

**Normalized coordinates always**
```kotlin
// Send this ✅
x = 0.53f, y = 0.27f

// Never this ❌
x = 845, y = 332
```

**STUN server from day 1**
```kotlin
val iceServers = listOf(
    PeerConnection.IceServer
        .builder("stun:stun.l.google.com:19302")
        .createIceServer()
)
```

**Firebase DB structure**
```
rooms/
└── {roomId}/
    ├── offer/              { sdp: "..." }
    ├── answer/             { sdp: "..." }
    ├── offerCandidates/    { id: { candidate, sdpMid, sdpMLineIndex } }
    └── answerCandidates/   { id: { candidate, sdpMid, sdpMLineIndex } }
```

**Signaling flow**
```
Expert:     createRoom → createOffer → sendOffer → observeAnswer
               → setRemoteDescription → observeRemoteICE
Field User: observeOffer → createAnswer → sendAnswer → startCamera
               → observeRemoteICE
Both:       onIceCandidate → sendIceCandidate to Firebase
```

---

## 📅 4-Week Build Plan

**Week 1 — Foundation ✅**
- [x] Firebase project setup + `google-services.json`
- [x] Stream WebRTC dependency added
- [x] MVVM skeleton (ViewModel, Repository, sealed states)
- [x] `SignalingRepository` — offer/answer/ICE via Firebase Flows
- [x] `WebRTCManager` — PeerConnection, data channel, STUN, ICE
- [x] `CallViewModel` — full signaling orchestration
- [x] Firebase Realtime DB rules configured
- Goal: two phones connected, no video yet ✅

**Week 2 — Video Call ✅**
- [x] Runtime camera permission request in `CallScreen` / `CallViewModel`
- [x] Camera capture on Phone B — `startCamera()` in `WebRTCManager` using `Camera2Enumerator`, `VideoSource`, `VideoTrack`
- [x] Wire `onTrack` in `PeerConnection.Observer` to expose incoming remote `VideoTrack`
- [x] Remote video rendering on Phone A — `SurfaceViewRenderer` as `AndroidView` in `CallScreen`, initialized with shared `EglBase`
- [x] Data channel smoke test — send `"ping"`, verify receipt on other device
- Goal: live video working across different networks via STUN ✅

**Week 2 — Implementation Notes**
- `EglBase` is shared from `WebRTCManager.eglBase` — never create a second instance or rendering glitches occur
- Use `onTrack` (Unified Plan), not the deprecated `onAddStream`
- `SurfaceViewRenderer` must be initialized before the remote track arrives; release in `onDispose`
- Camera + PeerConnection callbacks run on different threads — use `WebRTCManager`'s existing executor pattern

**Week 3 — Annotation Engine**
- `AnnotationOverlayView` with Canvas
- Freehand draw → serialize to JSON → send over data channel → render on other device
- Add Arrow, Circle, Rectangle tools
- Undo / Clear All

**Week 4 — Polish & Portfolio**
- Color picker + stroke width
- 6-digit room code system
- Error handling + reconnection logic
- README with architecture diagram
- 60-second demo video (both phones side by side)

---

## ✨ Bonus Features (if time allows)
- **Laser pointer** — tap → red dot → disappears after 1 second
- **Freeze frame** — expert freezes video, annotates, resumes
- **Voice call** — WebRTC already supports audio, easy add-on
- **Snapshot** — annotate a still frame and save it

---

## 📝 Final Resume Entry

```
Real-Time AR Annotation App                         GitHub ↗
• Built a peer-to-peer AR annotation system for Android using
  WebRTC, enabling remote experts to draw overlays on a live
  camera feed synced across devices in real-time.
• Implemented WebRTC signaling (offer/answer/ICE) via Firebase
  and routed annotation data through a dedicated WebRTC data
  channel, keeping video and annotation pipelines independent.
• Built a custom Canvas rendering layer with normalized
  coordinate mapping to support freehand draw, arrows, and
  circles across any screen size.
• Stack: Kotlin, Jetpack Compose, WebRTC, Canvas, Firebase,
  MVVM, Coroutines, Flow
```
