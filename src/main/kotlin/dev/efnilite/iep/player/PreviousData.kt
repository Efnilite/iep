package dev.efnilite.iep.player

import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.mode.Mode
import dev.efnilite.iep.reward.Reward
import dev.efnilite.iep.world.World
import dev.efnilite.vilib.inventory.Menu
import dev.efnilite.vilib.inventory.item.Item
import io.papermc.lib.PaperLib
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.util.Vector
import java.util.concurrent.CompletableFuture

/**
 * Class for storing a player's previous data.
 */
data class PreviousData(private val player: Player) {

    val leaveRewards: MutableMap<Mode, MutableSet<Reward>> = mutableMapOf()

    private val foodLevel = player.foodLevel
    private val saturation = player.saturation
    private val flying = player.isFlying
    private val allowFlight = player.allowFlight

    private val gamemode = player.gameMode
    private val position = player.location
    private val inventoryContents: Array<ItemStack?> = player.inventory.contents
    private val effects: Collection<PotionEffect> = player.activePotionEffects

    /**
     * Sets player stuff.
     */
    fun setup(vector: Vector): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        PaperLib.teleportAsync(player, vector.toLocation(World.world)).thenRun {
            player.gameMode = GameMode.ADVENTURE
            player.fallDistance = 0F

            player.resetPlayerTime()
            player.activePotionEffects.forEach { player.removePotionEffect(it.type) }

            player.foodLevel = 20
            player.saturation = 20F
            player.isFlying = false
            player.allowFlight = false

            player.inventory.clear()

            player.inventory.chestplate = Item(Material.ELYTRA, "").unbreakable().build()

            val items = mutableListOf<ItemStack>()

            items += Locales.getItem(player, "hotbar.play").build()
            items += Locales.getItem(player, "hotbar.settings").build()
            items += Locales.getItem(player, "hotbar.leaderboards").build()
            items += Locales.getItem(player, "hotbar.leave").build()

            Menu.getEvenlyDistributedSlots(items.size).forEachIndexed { index, slot ->
                player.inventory.setItem(slot, items[index])
            }

            future.complete(true)
        }

        return future
    }

    /**
     * Resets the player's data.
     */
    fun reset(switchMode: Boolean, urgent: Boolean) {
        if (switchMode) {
            reset()

            return
        }
        if (urgent) {
            player.teleport(position)
            reset()

            return
        }

        PaperLib.teleportAsync(player, position).thenRun { reset() }
    }

    private fun reset() {
        player.fallDistance = 0F

        player.gameMode = gamemode
        player.inventory.contents = inventoryContents

        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
        player.addPotionEffects(effects)

        player.foodLevel = foodLevel
        player.saturation = saturation
        player.allowFlight = allowFlight
        player.isFlying = flying

        player.resetPlayerTime()

        for ((mode, rewards) in leaveRewards) {
            rewards.forEach { it.execute(player, mode) }
        }
    }
}