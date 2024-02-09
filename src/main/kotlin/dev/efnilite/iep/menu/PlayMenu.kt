package dev.efnilite.iep.menu

import dev.efnilite.iep.IEP
import dev.efnilite.vilib.inventory.Menu
import org.bukkit.entity.Player

object PlayMenu {

    fun open(player: Player) {
        val menu = Menu(3, "Play")
            .distributeRowsEvenly()

        for (mode in IEP.getModes()) {
            menu.item(9 + menu.items.size, mode.getItem("")
                .click({ mode.create(player) }))
        }

        menu.open(player)
    }

}