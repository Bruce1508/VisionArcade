# ADR 0001 — Java and UI Baseline

Status: Accepted  
Date: 2026-10-03

## Context

VisionArcade is a new Java desktop application starting in late 2026. It needs a modern LTS JDK, desktop UI, Gradle support, and compatibility with native CV/ML libraries.

## Decision

- Java 25 LTS
- Gradle 9.x wrapper compatible with Java 25
- JavaFX 25 stable line
- JUnit 6

Use JavaFX directly without Spring or another application container.

## Consequences

Positive:
- current LTS Java baseline;
- modern language/runtime support;
- simple desktop architecture.

Tradeoff:
- native OpenCV/ONNX compatibility must be verified during Milestone 0/2.
