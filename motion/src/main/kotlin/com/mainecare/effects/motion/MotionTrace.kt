package com.mainecare.effects.motion

/**
 * Optional frame telemetry for the restoration gates. Disabled (no allocation, one
 * volatile read) unless a debug build installs a sink. Each line is
 * `kind,value...,monotonicNanos`.
 */
object MotionTrace {
    @Volatile
    var sink: ((String) -> Unit)? = null

    fun emit(kind: String, vararg values: Float) {
        val target = sink ?: return
        val builder = StringBuilder(kind)
        for (value in values) {
            builder.append(',').append(value)
        }
        builder.append(',').append(System.nanoTime())
        target(builder.toString())
    }
}
