package dev.efnilite.iep.mode

import dev.efnilite.iep.config.Config
import dev.efnilite.iep.generator.TimeTrialGenerator
import dev.efnilite.iep.leaderboard.Leaderboard

object TimeTrialMode : Mode {

    override val name = "time trial"

    override val leaderboard = Leaderboard(
        name,
        Config.CONFIG.getDouble("mode-settings.time-trial.score"),
        Leaderboard.Sort.TIME
    )

    override fun getGenerator() = TimeTrialGenerator()
}