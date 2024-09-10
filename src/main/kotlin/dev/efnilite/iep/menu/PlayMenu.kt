package dev.efnilite.iep.menu

import dev.efnilite.iep.IEP
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.player.ElytraPlayer
import dev.efnilite.iep.player.ElytraPlayer.Companion.asElytraPlayer
import dev.efnilite.vilib.inventory.Menu
import dev.efnilite.vilib.util.Cooldowns
import dev.efnilite.vilib.util.Task
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object PlayMenu {

    fun open(player: Player) {
        val menu = Menu(3, Locales.getString(player, "play.title"))
            .item(23, Locales.getItem(player, "go back").click({ player.closeInventory() }))
            .distributeRowsEvenly()

        for (mode in IEP.getModes()) {
            menu.item(9 + menu.items.size, mode.getItem(player)
                .click({
                    if (!Cooldowns.canPerform(player, "ep join", 1000)) {
                        return@click
                    }

                    val join = {
                        val ep = player.asElytraPlayer()

                        if (ep == null) {
                            ElytraPlayer(player).join(mode)
                        } else {
                            ep.leave(true)

                            ep.join(mode)
                        }
                    }

                    if (Bukkit.getPluginManager().isPluginEnabled("IP")) {
                        IEP.log("IP detected, leaving IP before joining mode")

                        player.performCommand("ip:ip leave")

                        Task.create(IEP.instance)
                            .delay(1)
                            .execute(join)
                            .run()
                    } else {
                        join()
                    }
                }))
        }

        menu.open(player)
    }

}