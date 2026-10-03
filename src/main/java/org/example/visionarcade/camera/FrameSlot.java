package org.example.visionarcade.camera;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Single-slot latest-frame handoff (ADR 0003). Never queues: publishing a
 * newer frame drops and releases whatever was waiting unread.
 */
public final class FrameSlot {

    private final AtomicReference<FrameSnapshot> latest = new AtomicReference<>();

    public void publish(FrameSnapshot snapshot) {
        FrameSnapshot superseded = latest.getAndSet(snapshot);
        if (superseded != null) {
            superseded.close();
        }
    }

    /** Takes exclusive ownership of the latest snapshot, or null if nothing new is waiting. */
    public FrameSnapshot take() {
        return latest.getAndSet(null);
    }

    public void clear() {
        FrameSnapshot leftover = latest.getAndSet(null);
        if (leftover != null) {
            leftover.close();
        }
    }
}
