package dev.efnilite.iep.mode

import dev.efnilite.iep.leaderboard.Leaderboard
import dev.efnilite.vilib.inventory.item.Item
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
     * Method that gets called when a mode is clicked in the menu or joined using /parkour join.
     *
     * @param player The player.
     */
    fun create(player: Player)

    /**
     * @param locale The locale of the menu, used to adjust the name.
     * @return The item used in menus to show this mode. If this item is null, the mode won't be displayed.
     */
    @Contract(pure = true)
    fun getItem(locale: String): Item

}