# 🎯 Project: Real-Time AR Annotation App

---

## 🏗️ Architecture

```
Phone A (Expert)                        Phone B (Field User)
─────────────────                       ─────────────────────
Camera Stream ──── WebRTC Video ──────► View Expert Feed
View Field Feed ◄─ WebRTC Video ─────── Camera Stream
Draw Annotations ── WebRTC Data ──────► Show Annotations

                        │
                    Firebase
                (Signaling ONLY)
              offer / answer / ICE
```

Both devices stream video to each other. Firebase is used only for signaling — video and annotation data flow directly peer-to-peer via WebRTC.

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
│   ├── WebRTCManager.kt          ← PeerConnection, data channel, ICE, camera, video tracks
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
│       ├── CallScreen.kt         ← Active session (video + overlay + toolbar)
│       └── CallViewModel.kt      ← Orchestrates WebRTC + signaling + video state
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
Both:       initPeerConnection → startCamera → addTrack (track in SDP)
Expert:     createRoom → createOffer → sendOffer → observeAnswer
               → setRemoteDescription → observeRemoteICE
Field User: observeOffer → createAnswer → sendAnswer
               → observeRemoteICE
Both:       onIceCandidate → sendIceCandidate to Firebase
Both:       onAddTrack → _remoteVideoTrack emitted → UI renders remote video
```

**Video layout states**
```
remoteVideoTrack != null  →  remote video full-screen + local camera PiP (120×160dp, bottom-right)
remoteVideoTrack == null  →  own camera full-screen, mirrored (waiting for peer)
ICE DISCONNECTED/FAILED   →  _remoteVideoTrack cleared → "Call ended" overlay shown
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
- [x] Both devices call `startCamera()` in `CallViewModel.init()` before offer/answer — track must be in SDP at negotiation time
- [x] `Camera2Enumerator` picks front camera; `Camera2Capturer` + `SurfaceTextureHelper` + `VideoSource` + `VideoTrack` created synchronously; `peerConnection.addTrack()` called before `createOffer()`/`createAnswer()`
- [x] `onAddTrack` in `PeerConnection.Observer` emits incoming `VideoTrack` to `_remoteVideoTrack` StateFlow
- [x] `SurfaceViewRenderer` as `AndroidView` in `CallScreen` — initialized with shared `EglBase`, `SCALE_ASPECT_FILL`, hardware scaler enabled
- [x] Remote video full-screen; local camera mirrored PiP (120×160dp) overlaid bottom-right above toolbar
- [x] Own camera shown full-screen while waiting for peer to connect
- [x] ICE `DISCONNECTED`/`FAILED` clears `_remoteVideoTrack` — prevents frozen last frame; "Call ended" card overlay shown
- [x] Data channel smoke test — `sendMessage` / `onMessage` verified
- Goal: live two-way video working across different networks via STUN ✅

**Week 2 — Implementation Notes**
- `EglBase` lives in `WebRTCManager`, exposed via `CallViewModel.eglBase` — shared across all `SurfaceViewRenderer` instances; never create a second instance
- Use `onAddTrack` (Unified Plan), not the deprecated `onAddStream`
- `AndroidView` `factory` lambda captures the track reference; `onRelease` uses that same reference for `removeSink` + `renderer.release()` — ensures clean teardown on Compose navigation
- `setScalingType(SCALE_ASPECT_FILL)` required — without it, `SurfaceViewRenderer` shrinks its underlying `SurfaceView` to the video's native resolution, leaving a tiny box in the center
- `startCamera()` must be called before `createOffer()` / `createAnswer()` so the video transceiver is present in the SDP; both roles call it inside `init()`
- `_remoteVideoTrack.value = null` on ICE disconnect is the correct freeze fix — Compose removes the `AndroidView`, `onRelease` fires, renderer is released

**Week 2 — CallScreen UI**
- Top bar: `statusBarsPadding()` fixes overlap with system status bar and allows touch on all buttons
- Left column: "● Connected" chip + "Room XXXXXX" chip stacked with 6dp spacing
- Right: wide red pill "End Call" button (`CallEnd` icon + label, `RoundedCornerShape(24.dp)`, 24dp horizontal padding)
- Bottom toolbar: pencil / arrow / circle tool selector + color dot + undo + clear all

**Week 3 — Annotation Engine**
- `AnnotationOverlayView` with Canvas over the video layer
- Freehand draw → serialize to JSON → send over data channel → render on other device
- Add Arrow, Circle tools (Rectangle optional)
- Undo / Clear All (data channel messages, not just local state)

**Week 4 — Polish & Portfolio**
- Color picker + stroke width
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
• Implemented bidirectional WebRTC video (offer/answer/ICE via
  Firebase) with Camera2 capture, SurfaceViewRenderer, and a
  Compose AndroidView lifecycle that handles track teardown cleanly.
• Built a custom Canvas rendering layer with normalized
  coordinate mapping to support freehand draw, arrows, and
  circles across any screen size.
• Stack: Kotlin, Jetpack Compose, WebRTC, Canvas, Firebase,
  MVVM, Coroutines, Flow
```
