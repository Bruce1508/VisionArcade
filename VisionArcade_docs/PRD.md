# VisionArcade PRD

Status: Draft for MVP  
Last reviewed: 2026-10-03

## 1. Product statement

VisionArcade is a local desktop application that uses a webcam and real-time computer vision to convert physical objects and, later, body movement into game controls.

The product should feel like a small camera-powered arcade, not an ML visualization demo.

## 2. Primary user experience

The player launches the app, grants camera permission, sees a live camera preview, starts a game mode, and interacts physically with the camera.

### MVP game: Object Hunt

Example round:

- Prompt: `FIND A BOTTLE`
- Timer starts.
- The detector processes live webcam frames.
- A matching detection above the configured confidence threshold must remain valid for a short stability window.
- The round succeeds.
- Score is awarded.
- A new supported object is selected.

## 3. MVP functional requirements

### Application
- Launch as a local Java desktop app.
- Show a responsive JavaFX window.
- Open and release the webcam cleanly.
- Show the live camera feed.
- Fail gracefully when the camera cannot be opened.

### Detection
- Run an object-detection model locally.
- Return at minimum:
  - class label
  - confidence
  - bounding box
  - capture timestamp/frame identity
- Show detections on the camera preview.
- Keep inference off the JavaFX Application Thread.

### Object Hunt
- Select only object classes the chosen model can reasonably detect.
- Show target object, timer, score, and round result.
- Require a configurable confidence threshold.
- Require a short stability duration to reduce one-frame false positives.
- Support restart/new game.

## 4. Quality requirements

- UI must remain responsive while camera and inference are active.
- Stale camera frames should be dropped rather than queued indefinitely.
- Camera/inference resources must be released on shutdown.
- Game logic should be testable without a real webcam.
- The detector should be replaceable without rewriting game logic.
- Metrics must be measured, not invented.

## 5. MVP success criteria

The MVP is complete when a user can:

1. launch the app,
2. see live webcam video,
3. see correct object-detection overlays,
4. start Object Hunt,
5. complete several rounds using real physical objects,
6. quit without leaked camera/native resources.

## 6. Future game modes

Not part of the MVP:

- **Vision Pong:** detected object x/y position controls a paddle.
- **Human Dodge:** person position controls a character avoiding obstacles.
- **Pose Challenge:** requested poses/actions are validated by a pose model.
- **Reaction Games:** react to visual prompts with an object or gesture.
- **Local multiplayer variants:** only after a strong single-player core exists.

## 7. Explicit non-goals

Do not implement during MVP:

- custom user accounts
- server/backend
- Spring Boot
- cloud deployment
- online leaderboard
- database persistence
- microservices
- plugin framework
- dependency injection framework
- custom neural-network training
- TensorFlow
- LLM integration
- generalized game scripting system

## 8. Product principle

Every new feature must answer:

> Does this make the camera-to-game interaction more playable, reliable, measurable, or easier to maintain?

If not, defer it.
