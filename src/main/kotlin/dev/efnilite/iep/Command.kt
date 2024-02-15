package dev.efnilite.iep

import dev.efnilite.iep.config.Config
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.generator.util.Settings
import dev.efnilite.iep.menu.LeaderboardMenu
import dev.efnilite.iep.menu.PlayMenu
import dev.efnilite.iep.menu.SettingsMenu
import dev.efnilite.iep.player.ElytraPlayer.Companion.asElytraPlayer
import dev.efnilite.vilib.command.ViCommand
import dev.efnilite.vilib.mm.adventure.text.Component
import dev.efnilite.vilib.mm.adventure.text.minimessage.MiniMessage
import dev.efnilite.vilib.util.Cooldowns
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Command : ViCommand() {

    override fun execute(player: CommandSender, args: Array<out String>): Boolean {
        if (player !is Player) return false

        if (args.isEmpty()) {


            return true
        }

        when (args[0]) {
            "play" -> {
                if (Config.CONFIG.getBoolean("permissions") && !player.hasPermission("iep.play")) {
                    return true
                }

                PlayMenu.open(player)

                return true
            }
            "leaderboards" -> {
                if (Config.CONFIG.getBoolean("permissions") && !player.hasPermission("iep.leaderboard")) {
                    return true
                }

                LeaderboardMenu.open(player)

                return true
            }
            "settings" -> {
                val ep = player.asElytraPlayer() ?: return true

                if (ep.hasPermission("iep.setting")) {
                    return true
                }

                SettingsMenu.open(ep)
            }
            "leave" -> {
                val ep = player.asElytraPlayer() ?: return true

                if (player.hasPermission("iep.leave")) {
                    return true
                }

                ep.leave()
            }
            "reset" -> player.isInvulnerable = false
        }

        if (args.size > 1) {
            when (args[0]) {
                "seed" -> {
                    if (!Cooldowns.canPerform(player, "ep set seed", 2500)) {
                        return true
                    }

                    val iep = player.asElytraPlayer() ?: return true
                    val seed = args[1]

                    try {
                        iep.getGenerator().set { settings -> Settings(settings, seed = seed.toInt()) }
                        iep.getGenerator().reset(s = seed.toInt())
                        iep.send(Locales.getString(player, "settings.seed.set").replace("%a", seed))
                    } catch (ex: NumberFormatException) {
                        iep.send(Locales.getString(player, "settings.seed.invalid").replace("%a", seed))
                    }
                }
            }
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}
