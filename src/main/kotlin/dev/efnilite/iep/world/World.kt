package dev.efnilite.iep.world

import dev.efnilite.iep.IEP
import net.kyori.adventure.util.TriState
import org.bukkit.*
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import org.codehaus.plexus.util.FileUtils
import java.io.File
import java.io.IOException

private class EmptyChunkGenerator : ChunkGenerator() {

    override fun shouldGenerateCaves() = false
    override fun shouldGenerateDecorations() = false
    override fun shouldGenerateMobs() = false
    override fun shouldGenerateStructures() = false
    override fun shouldGenerateSurface() = false
    override fun shouldGenerateNoise() = false

}

private class EmptyBiomeGenerator : BiomeProvider() {

    override fun getBiome(p0: WorldInfo, p1: Int, p2: Int, p3: Int): Biome = Biome.PLAINS
    override fun getBiomes(p0: WorldInfo): MutableList<Biome> = mutableListOf(Biome.PLAINS)

}

/**
 * Class for handling Parkour world generation/deletion, etc.
 */
object World {

    const val NAME = "iep"

    lateinit var world: World

    /**
     * Creates the world.
     */
    fun create() {
        world = WorldCreator(NAME)
            .generator(EmptyChunkGenerator())
            .generateStructures(false)
            .biomeProvider(EmptyBiomeGenerator())
            .type(WorldType.NORMAL)
            .keepSpawnLoaded(TriState.FALSE)
            .createWorld()!!

        setup()
    }

    /**
     * Sets all world settings.
     */
    private fun setup() {
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
        world.isAutoSave = false
    }

    /**
     * Deletes the parkour world.
     */
    fun delete() {
        val file = File(NAME)

        if (!file.exists()) {
            return
        }

        Bukkit.unloadWorld(NAME, false)

        try {
            FileUtils.deleteDirectory(file)
        } catch (ex: IOException) {
            IEP.instance.logging.stack("Error while trying to reset iep world", ex)
        }
    }
}