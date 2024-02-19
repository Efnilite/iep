package dev.efnilite.iep.generator

import dev.efnilite.iep.config.Config
import kotlin.math.min

class TimeTrialGenerator : Generator() {

    override fun getScore() = min(SCORE, super.getScore())

    override fun tick() {
        super.tick()

        val score = getScore()
        val player = players[0]

        if (score >= SCORE) {
            reset(ResetReason.RESET)
            return
        }

        player.sendActionBar("${getProgressBar(score)} <reset><dark_gray>| <gray>${"%.1f".format(score)}/${"%.1f".format(SCORE)}")
    }

    private fun getProgressBar(t: Double): String {
        return (0 until ACTIONBAR_LENGTH)
            .map { if (it * INCREMENTS < t) return@map "<green><bold>|" else return@map "<reset><dark_gray>|" }
            .joinToString("") { it }
    }

    override fun reset(resetReason: ResetReason, regenerate: Boolean, s: Int, overrideSeedSettings: Boolean) {
        // todo dont register scores < 2500

        super.reset(resetReason, regenerate, SEED, true)
    }

    companion object {
        val SEED = Config.CONFIG.getInt("mode-settings.time-trial.seed") { it >= 0 }
        val SCORE = Config.CONFIG.getDouble("mode-settings.time-trial.score") { it > 0 }
        val ACTIONBAR_LENGTH = Config.CONFIG.getInt("mode-settings.time-trial.actionbar-length") { it > 0 }
        val INCREMENTS = SCORE / ACTIONBAR_LENGTH.toDouble()
    }
}