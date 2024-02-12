package dev.efnilite.iep.generator

import dev.efnilite.iep.Config
import dev.efnilite.iep.generator.util.PointType
import dev.efnilite.iep.leaderboard.Leaderboard
import org.bukkit.util.Vector
import kotlin.math.min

class TimeTrialGenerator : Generator() {

    override val score: Double
        get() = min(SCORE, super.score)

    override fun tick() {
        super.tick()

        val player = players[0]

        if (score >= SCORE) {
            reset(s = SEED)
            return
        }

        player.sendActionBar("${getProgressBar(score, SCORE, ACTIONBAR_LENGTH)} <reset><dark_gray>| " +
                "<gray>${"%.1f".format(score)}/$SCORE")
    }

    override fun start(ld: Leaderboard, start: Vector, point: PointType) {
        super.start(ld, start, point)

        seed = SEED
    }

    companion object {
        val SEED = Config.CONFIG.getInt("mode-settings.time-trial.seed") { it >= 0 }
        val SCORE = Config.CONFIG.getDouble("mode-settings.time-trial.score") { it > 0 }
        val ACTIONBAR_LENGTH = Config.CONFIG.getInt("mode-settings.time-trial.actionbar-length") { it > 0 }
    }
}