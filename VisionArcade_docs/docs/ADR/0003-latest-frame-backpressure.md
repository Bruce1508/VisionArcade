# ADR 0003 — Latest-Frame Backpressure

Status: Accepted  
Date: 2026-10-03

## Context

A webcam can produce frames faster than inference can process them. Processing every frame through a FIFO queue causes latency to grow continuously.

## Decision

Use latest-value/single-slot handoff between camera capture and inference.

When a newer frame supersedes an unprocessed frame, the stale frame is dropped and its native resources are released safely.

For the MVP:
- camera runs on a background worker;
- inference runs on a background worker;
- game update/render stays on the JavaFX Application Thread.

## Consequences

Positive:
- bounded memory;
- low interactive latency;
- system reacts to the present rather than old queued frames.

Tradeoff:
- not every captured frame is analyzed;
- frame/resource ownership needs explicit care.
