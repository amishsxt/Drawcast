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
| Real-time video | Google's official WebRTC | Better internals knowledge |
| Signaling | Firebase Realtime DB | Free, fast, minimal setup |
| STUN | Google's free STUN server | Works across different networks |
| Architecture | MVVM + Coroutines + Flow | Clean separation |
| Serialization | Gson/Moshi | Annotation objects over data channel |

---

## 📁 Project Structure

```
app/
├── webrtc/
│   ├── WebRTCManager.kt
│   └── SignalingRepository.kt
├── annotation/
│   ├── AnnotationOverlayView.kt    ← Canvas drawing
│   ├── AnnotationViewModel.kt
│   └── models/
│       ├── Annotation.kt           ← sealed class
│       └── DataMessage.kt          ← sealed class
├── ui/
│   ├── home/                       ← Create/Join room
│   └── call/                       ← Active session screen
└── data/
    └── FirebaseRepository.kt
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
    data class Arrow(val start: PointF, val end: PointF) : Annotation()
    data class Circle(val center: PointF, val radius: Float) : Annotation()
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

---

## 📅 4-Week Build Plan

**Week 1 — Foundation**
- Firebase project setup
- WebRTC dependency (Google's official)
- MVVM skeleton
- Signaling: create room → offer → answer → ICE
- Goal: two phones connected, no video yet

**Week 2 — Video Call**
- Camera capture on Phone B
- Remote video rendering on Phone A via `SurfaceViewRenderer`
- Data channel setup — send a simple "Hello" and verify latency
- Goal: live video working across different networks (STUN)

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

---

## ✅ Start Here — Right Now

1. Create a new Android project (Kotlin + Compose)
2. Set up Firebase at `console.firebase.google.com`
3. Add Google's WebRTC dependency
4. Build the signaling flow first — everything else depends on it
