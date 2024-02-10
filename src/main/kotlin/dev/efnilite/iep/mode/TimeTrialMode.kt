package dev.efnilite.iep.mode

import dev.efnilite.iep.generator.Generator
import dev.efnilite.iep.generator.TimeTrialGenerator
import dev.efnilite.iep.generator.util.PointType
import dev.efnilite.iep.leaderboard.Leaderboard
import dev.efnilite.vilib.inventory.item.Item
import org.bukkit.Material
import org.bukkit.entity.Player

object TimeTrialMode : Mode {

    override val name = "time trial"

    override val leaderboard = Leaderboard(name)

    override fun create(player: Player) = Generator.create(player, leaderboard, PointType.CIRCLE) { TimeTrialGenerator() }

    override fun getItem(locale: String): Item = Item(Material.CLOCK, "<#ddd200><bold>Time Trial")
        .lore("<gray>Reach a score of ${TimeTrialGenerator.SCORE}", "<gray>as soon as possible.")
}