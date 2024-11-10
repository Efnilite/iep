package dev.efnilite.iep.menu

import dev.efnilite.iep.IEP
import dev.efnilite.iep.config.Config
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.leaderboard.Leaderboard
import dev.efnilite.iep.mode.Mode
import dev.efnilite.vilib.inventory.Menu
import dev.efnilite.vilib.inventory.PagedMenu
import dev.efnilite.vilib.inventory.item.Item
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta

object LeaderboardMenu {

    fun open(player: Player) {
        val menu = Menu(3, Locales.getString(player, "leaderboards.title"))
            .distributeRowsEvenly()
            .item(23, Locales.getItem(player, "go back").click({ player.closeInventory() }))

        for (mode in IEP.getModes()) {
            if (Config.CONFIG.getBoolean("permissions") && !player.hasPermission("iep.leaderboard.${mode.name}")) {
                continue
            }

            menu.item(menu.items.size + 9, mode.getItem(player)
                .click({ SingleLeaderboardMenu.open(player, mode, mode.leaderboard.sort) })
            )
        }

        menu.open(player.player)
    }

}

private object SingleLeaderboardMenu {

    fun open(player: Player, mode: Mode, sort: Leaderboard.Sort) {
        val leaderboard = mode.leaderboard
        val menu = PagedMenu(3, Locales.getString(player, "modes.${mode.name}.title"))
            .displayRows(0, 1)

        for ((idx, entry) in sort.sort(leaderboard.getAllScores()).withIndex()) {
            val (uuid, score) = entry

            val item = Locales.getItem(player, "leaderboards.head", (idx + 1).toString(), score.name,
                mode.formatDisplayScore(score.score), score.getFormattedTime(), score.seed.toString())

            // prevent crashes from fetching all the skulls at once
            if (idx <= 36) {
                val meta = item.build().itemMeta
                (meta as SkullMeta).owningPlayer = Bukkit.getOfflinePlayer(uuid)
                item.meta(meta)
            }

            menu.addToDisplay(listOf(item))

            if (uuid == player.uniqueId) {
                menu.item(20, item.clone())
            }
        }

        val current = Locales.getStringList(player, "leaderboards.sort.values")

        val next = when (sort) {
            Leaderboard.Sort.SCORE -> Leaderboard.Sort.TIME
            Leaderboard.Sort.TIME -> Leaderboard.Sort.SCORE
        }

        menu
            .prevPage(19, Item(Material.RED_DYE, "<#DE1F1F><bold>«").click({ menu.page(-1) }))
            .nextPage(26, Item(Material.GREEN_DYE, "<#0DCB07><bold>»").click({ menu.page(1) }))
            .item(22, Locales.getItem(player, "leaderboards.sort", current[sort.ordinal])
                .click({ open(player, mode, next) }))
            .item(24, Locales.getItem(player, "go back").click({ LeaderboardMenu.open(player) }))
            .distributeRowEvenly(2)
            .open(player.player)
    }
}