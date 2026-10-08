package com.vektor.app.ui.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.hypot

private const val DOUBLE_TAP_TIMEOUT_MS = 260L
private const val TAP_TIMEOUT_MS = 200L
private const val TOUCH_SLOP = 6f

fun Modifier.trackpadTouchHandler(
    onPointerMove: (dx: Float, dy: Float, dt: Long) -> Unit,
    onTwoFingerScroll: (dy: Float, dt: Long) -> Unit,
    onSingleTap: () -> Unit,
    onTwoFingerTap: () -> Unit,
    onDragLockStart: () -> Unit,
    onDragLockEnd: () -> Unit
): Modifier = pointerInput(Unit) {
    var lastTapUpTime = 0L
    var lastEventUptime = 0L

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downTime = down.uptimeMillis
        val isDoubleTapHold = (downTime - lastTapUpTime) <= DOUBLE_TAP_TIMEOUT_MS

        var isDragLocking = false
        var hasMovedPastSlop = false
        var accumulatedMovement = 0f
        var isTwoFingerScrolling = false
        var maxPointers = 1

        if (isDoubleTapHold) {
            isDragLocking = true
            onDragLockStart()
        }

        lastEventUptime = downTime

        while (true) {
            val event = awaitPointerEvent()
            val activePointers = event.changes.filter { it.pressed }

            if (activePointers.size > maxPointers) {
                maxPointers = activePointers.size
            }

            if (activePointers.isEmpty()) {
                val upTime = event.changes.firstOrNull()?.uptimeMillis ?: System.currentTimeMillis()
                val duration = upTime - downTime

                if (isDragLocking) {
                    onDragLockEnd()
                    lastTapUpTime = 0L
                } else if (!hasMovedPastSlop && duration <= TAP_TIMEOUT_MS) {
                    if (maxPointers == 1) {
                        // Ketuk 1 Jari = Klik Kiri Instan
                        onSingleTap()
                        lastTapUpTime = upTime
                    } else if (maxPointers >= 2) {
                        // Ketuk 2 Jari = Klik Kanan Instan
                        onTwoFingerTap()
                        lastTapUpTime = 0L
                    }
                }
                event.changes.forEach { it.consume() }
                break
            }

            val now = activePointers.first().uptimeMillis
            val dt = (now - lastEventUptime).coerceAtLeast(1L)
            lastEventUptime = now

            if (activePointers.size >= 2) {
                // Mode Scroll 2 Jari
                isTwoFingerScrolling = true
                val avgDeltaY = (activePointers[0].positionChange().y + activePointers[1].positionChange().y) / 2f
                onTwoFingerScroll(avgDeltaY, dt)
            } else if (!isTwoFingerScrolling) {
                // Mode Gerak Kursor 1 Jari
                val primaryChange = activePointers.first()
                val delta = primaryChange.positionChange()

                accumulatedMovement += hypot(delta.x, delta.y)
                if (accumulatedMovement > TOUCH_SLOP) {
                    hasMovedPastSlop = true
                }

                if (hasMovedPastSlop || isDragLocking) {
                    onPointerMove(delta.x, delta.y, dt)
                }
            }

            event.changes.forEach { it.consume() }
        }
    }
}
