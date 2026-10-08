package com.vektor.app.ui.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.hypot

private const val DOUBLE_TAP_TIMEOUT_MS = 280L
private const val TOUCH_SLOP = 5f

fun Modifier.trackpadTouchHandler(
    onPointerMove: (dx: Float, dy: Float, dt: Long) -> Unit,
    onTwoFingerScroll: (dy: Float, dt: Long) -> Unit,
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

        if (isDoubleTapHold) {
            isDragLocking = true
            onDragLockStart()
        }

        lastEventUptime = downTime

        while (true) {
            val event = awaitPointerEvent()
            val activePointers = event.changes.filter { it.pressed }

            if (activePointers.isEmpty()) {
                val upTime = event.changes.firstOrNull()?.uptimeMillis ?: System.currentTimeMillis()
                if (isDragLocking) {
                    onDragLockEnd()
                    lastTapUpTime = 0L
                } else if (!hasMovedPastSlop && !isTwoFingerScrolling && (upTime - downTime) < DOUBLE_TAP_TIMEOUT_MS) {
                    lastTapUpTime = upTime
                }
                event.changes.forEach { it.consume() }
                break
            }

            val now = activePointers.first().uptimeMillis
            val dt = (now - lastEventUptime).coerceAtLeast(1L)
            lastEventUptime = now

            if (activePointers.size >= 2) {
                isTwoFingerScrolling = true
                val avgDeltaY = (activePointers[0].positionChange().y + activePointers[1].positionChange().y) / 2f
                onTwoFingerScroll(avgDeltaY, dt)
            } else if (!isTwoFingerScrolling) {
                val primaryChange = activePointers.first()
                val delta = primaryChange.positionChange()

                if (!hasMovedPastSlop) {
                    accumulatedMovement += hypot(delta.x, delta.y)
                    if (accumulatedMovement > TOUCH_SLOP) {
                        hasMovedPastSlop = true
                    }
                }

                if (hasMovedPastSlop || isDragLocking) {
                    onPointerMove(delta.x, delta.y, dt)
                }
            }

            event.changes.forEach { it.consume() }
        }
    }
}
