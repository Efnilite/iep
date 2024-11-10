package dev.efnilite.iep.mode

import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.generator.Generator
import dev.efnilite.iep.generator.section.PointType
import dev.efnilite.iep.leaderboard.Leaderboard
import dev.efnilite.iep.leaderboard.Score.Companion.pretty
import org.bukkit.entity.Player
import org.jetbrains.annotations.Contract

/**
 * Interface for all modes.
 * Every registered mode needs to inherit this class, because it needs identifying functions.
 */
interface Mode {

    /**
     * @return The internal name used for this mode.
     */
    val name: String

    /**
     * @return The [Leaderboard] that belongs to this mode
     */
    val leaderboard: Leaderboard

    /**
     * The [PointType].
     */
    val pointType
        get() = PointType.CIRCLE

    /**
     * Returns a new [Generator] for this mode.
     */
    @Contract(pure = true)
    fun getGenerator(): Generator

    /**
     * @param player The player.
     * @return The item used in menus to show this mode. If this item is null, the mode won't be displayed.
     */
    fun getItem(player: Player) = Locales.getItem(player, "modes.$name")

    /**
     * @param score The score to format.
     * @return The formatted score.
     */
    fun formatDisplayScore(score: Double): String = score.pretty()

}