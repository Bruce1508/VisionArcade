# ADR 0002 — Vision and Inference Boundaries

Status: Accepted with implementation details pending  
Date: 2026-10-03

## Context

The app needs webcam capture and local object detection while keeping Java as the runtime.

## Decision

- OpenCV handles camera/image operations.
- Target OpenCV 4.14.x for MVP rather than the newly released OpenCV 5.0 major line.
- ONNX Runtime Java is the preferred inference runtime.
- A small ONNX object detector is hidden behind `ObjectDetector`.
- YOLO26n is the first model candidate, but game code must not depend on YOLO-specific outputs.
- DJL, TensorFlow, and a Python inference service are excluded from the MVP.

The exact OpenCV native packaging artifact and ONNX Runtime version are selected only after they pass Apple Silicon technical spikes.

## Consequences

Positive:
- direct, understandable inference path;
- model/runtime can change without rewriting game logic;
- less dependency surface.

Tradeoff:
- preprocessing/postprocessing code must be implemented and tested carefully.
