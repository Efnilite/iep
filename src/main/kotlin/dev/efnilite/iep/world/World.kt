package dev.efnilite.iep.world

import dev.efnilite.iep.IEP
import dev.efnilite.vilib.util.VoidGenerator
import org.bukkit.*
import org.bukkit.World
import java.io.File

/**
 * Class for handling Parkour world generation/deletion, etc.
 */
object World {

    private const val NAME = "iep"

    lateinit var world: World

    /**
     * Creates the world.
     */
    fun create() {
        IEP.log("Creating world $NAME")

        world = WorldCreator(NAME)
            .generator(VoidGenerator.getGenerator())
            .type(WorldType.NORMAL)
            .createWorld()!!

        setup()
    }

    /**
     * Sets all world settings.
     */
    private fun setup() {
        IEP.log("Setting up rules for world $NAME")

        world.setGameRule(GameRule.DO_FIRE_TICK, false)
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false)
        world.setGameRule(GameRule.DO_TILE_DROPS, false)
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        world.setGameRule(GameRule.LOG_ADMIN_COMMANDS, false)
        world.setGameRule(GameRule.KEEP_INVENTORY, true)
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0)

        world.worldBorder.setCenter(0.0, 0.0)
        world.worldBorder.size = 10000000.0
        world.difficulty = Difficulty.PEACEFUL
        world.clearWeatherDuration = 1000000
        world.time = 10000
        world.isAutoSave = false
        world.keepSpawnInMemory = false
    }

    /**
     * Deletes the parkour world.
     */
    fun delete() {
        IEP.log("Deleting world $NAME")

        val file = File(NAME)

        if (!file.exists()) {
            return
        }
        IEP.log("Unloading world $NAME")

        Bukkit.unloadWorld(NAME, false)

        try {
            deleteRecursive(file)
        } catch (ex: Exception) {
            IEP.instance.logging.stack("Error while trying to reset iep world", ex)
        }
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            for (child in fileOrDirectory.listFiles()!!) {
                deleteRecursive(child)
            }
        }

        fileOrDirectory.delete()
    }
}