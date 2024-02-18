package dev.efnilite.iep.generator

import dev.efnilite.iep.mode.Mode

class SpeedDemonGenerator(mode: Mode) : Generator(mode) {

    private var maxSpeedSoFar = 0.0

    override fun getScore() = maxSpeedSoFar

    override fun tick() {
        super.tick()

        if (shouldScore()) {
            val speed = getSpeed(players[0])

            maxSpeedSoFar = maxOf(maxSpeedSoFar, speed)
        }
    }

    override fun reset(resetReason: ResetReason, regenerate: Boolean, s: Int, overrideSeedSettings: Boolean) {
        super.reset(resetReason, regenerate, s, overrideSeedSettings)

        maxSpeedSoFar = 0.0
    }
}