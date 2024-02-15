package dev.efnilite.iep.menu

import dev.efnilite.iep.IEP
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.generator.util.Settings
import dev.efnilite.iep.player.ElytraPlayer
import dev.efnilite.iep.style.RandomStyle
import dev.efnilite.vilib.inventory.Menu

object StylesMenu {

    fun open(player: ElytraPlayer) {
        val menu = Menu(3, Locales.getString(player, "styles.title"))
            .distributeRowEvenly(2)

        val styles = IEP.getStyles()
        val generator = player.getGenerator()

        for ((idx, style) in styles.withIndex()) {
            if (!player.hasPermission("iep.setting.style.${style.name()}")) {
                continue
            }

            val item = Locales.getItem(player, "styles.style",
                style.name(), if (style is RandomStyle) "random" else "incremental")
                .material(style.next())

            menu.item(idx, item
                    .click({
                        generator.set { settings -> Settings(settings, style = style) }

                        // todo for speed demon
                        if (generator.getScore() == 0.0) {
                            generator.reset()
                        }

                        player.player.inventory.close()
                    }))
        }

        menu.item(21, Locales.getItem(player, "styles.random").material(styles.random().next())
                .click({
                    generator.set { settings -> Settings(settings, style = styles.random()) }
                    player.player.inventory.close()
                }))
            .item(23, Locales.getItem(player, "go back").click({ SettingsMenu.open(player) }))
            .open(player.player)
    }
}