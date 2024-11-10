package dev.efnilite.iep.hook

import dev.efnilite.iep.IEP
import dev.efnilite.iep.leaderboard.Score.Companion.pretty
import dev.efnilite.iep.player.ElytraPlayer.Companion.asElytraPlayer
import me.clip.placeholderapi.PlaceholderAPI
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

object PapiHook : PlaceholderExpansion() {

    override fun getIdentifier() = "iep"
    override fun getAuthor() = "Efnilite"
    override fun getVersion() = IEP.instance.description.version
    override fun canRegister() = true
    override fun persist() = true

    fun replace(player: Player?, message: String) = PlaceholderAPI.setPlaceholders(player, message)

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        // iep_default_score_1
        if (params.matches(Regex("[a-z ]+_[a-z]+_\\d+"))) {
            return request(params)
        }

        val iep = player?.asElytraPlayer() ?: return null
        val generator = iep.getGenerator()

        return when (params) {
            "score" -> generator.getScore().pretty()
            "time" -> generator.getTime().toString()
            "seed" -> generator.seed.toString()
            "speed" -> generator.getSpeed(iep).pretty()
            else -> null
        }
    }

    private fun request(params: String): String? {
        val parts = params.split("_", limit = 3)
        val mode = parts[0].lowercase()
        val type = parts[1].lowercase()
        val rank = parts[2].toInt()

        val leaderboard = IEP.getMode(mode)?.leaderboard ?: return null
        val score = leaderboard.getRank(rank)

        return when (type) {
            "name" -> score.name
            "score" -> score.score.pretty()
            "time" -> score.getFormattedTime()
            "seed" -> score.seed.toString()
            else -> null
        }
    }
}