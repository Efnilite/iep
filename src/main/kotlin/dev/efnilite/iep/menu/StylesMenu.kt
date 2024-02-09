package dev.efnilite.iep.menu

import dev.efnilite.iep.ElytraPlayer
import dev.efnilite.iep.IEP
import dev.efnilite.iep.generator.util.Settings
import dev.efnilite.vilib.inventory.Menu
import dev.efnilite.vilib.inventory.item.Item
import org.bukkit.Material

object StylesMenu {

    fun open(player: ElytraPlayer) {
        val menu = Menu(3, "Styles")
        val styles = IEP.getStyles()
        val generator = player.getGenerator()

        for ((idx, style) in styles.withIndex()) {
            menu.item(idx, Item(style.next(), "<white><bold>${style.name()}")
                .lore("<dark_gray>Type <white>${style.name().lowercase()}")
                .click({ generator.set { settings -> Settings(settings, style = style) } }))
        }

        menu.item(21, Item(styles.random().next(), "<white><bold>Random")
            .click({ generator.set { settings -> Settings(settings, style = styles.random()) } }))
            .item(23, Item(Material.ARROW, "<white><bold>Go back").click({ SettingsMenu.open(player) }))
            .open(player.player)
    }
}