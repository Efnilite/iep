package dev.efnilite.iep.generator

import dev.efnilite.iep.Config
import dev.efnilite.iep.IEP
import dev.efnilite.iep.leaderboard.Score
import dev.efnilite.iep.player.ElytraPlayer
import dev.efnilite.iep.player.ElytraPlayer.Companion.asElytraPlayer
import dev.efnilite.iep.world.Divider
import dev.efnilite.iep.world.World
import dev.efnilite.vilib.schematic.Schematic
import dev.efnilite.vilib.schematic.Schematics
import dev.efnilite.vilib.util.Task
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private class Island(vector: Vector, schematic: Schematic) {

    val blocks: List<Block> = schematic.paste(vector.toLocation(World.world))
    val playerSpawn: Vector
    val blockSpawn: Vector

    init {
        assert(blocks.isNotEmpty())

        blocks.first { it.type == Material.DIAMOND_BLOCK }.let {
            playerSpawn = it.location.toVector()

            it.type = Material.AIR
        }
        blocks.first { it.type == Material.EMERALD_BLOCK }.let {
            blockSpawn = it.location.toVector()

            it.type = Material.AIR
        }
    }

    /**
     * Clears the island.
     */
    fun clear() {
        blocks.forEach { it.type = Material.AIR }
    }
}

class Generator {

    val players = mutableListOf<ElytraPlayer>()
    private val rings = mutableMapOf<Int, Ring>()

    var style = IEP.getStyles()[0]
    private var start: Instant = Instant.now()
    private lateinit var task: BukkitTask

    private lateinit var island: Island
    private val heading = Vector(1, 0, 0)

    private val director = RingDirector()

    private val leaderboard = IEP.getLeaderboard("default")

    /**
     * Adds a player to the generator.
     * @param player The player to add.
     */
    fun add(player: ElytraPlayer) {
        players.add(player)
    }

    /**
     * Removes a player from the generator.
     * @param player The player to remove.
     */
    fun remove(player: ElytraPlayer) {
        players.remove(player)

        if (players.isEmpty()) {
            task.cancel()

            reset()

            island.clear()
        }
    }

    /**
     * Initializes all the stuff.
     * @param vector The vector to spawn the island at.
     */
    fun start(vector: Vector) {
        island = Island(vector, Schematics.getSchematic(IEP.instance, "spawn-island"))

        players.forEach { it.teleport(island.playerSpawn) }

        rings[0] = Ring(heading, island.blockSpawn, 0)
        generate()

        task = Task.create(IEP.instance)
            .repeat(1)
            .execute(::tick)
            .run()
    }

    /**
     * Updates all players' scoreboards.
     */
    private fun updateBoard(score: Int) {
        val timeMs = Instant.now().minusMillis(start.toEpochMilli())
        val time = DateTimeFormatter.ofPattern(Config.CONFIG.getString("time-format"))
            .withZone(ZoneOffset.UTC)
            .format(timeMs)

        players.forEach { it.updateBoard(score, time) }
    }

    /**
     * Ticks the generator.
     */
    private fun tick() {
        val latest = rings.maxBy { it.key }
        val idx = latest.key
        val ring = latest.value

        val pos = players[0].position

        updateBoard(idx - 1)

        if (ring.isNear(pos)) {
            for (player in players) {
                leaderboard.update(player.uuid, Score(
                    name = player.name,
                    score = idx - 1,
                    time = Instant.now().minusMillis(start.toEpochMilli()).toEpochMilli(),
                    difficulty = 0.0))
            }

            generate()

            if (idx - 1 == 0) {
                start = Instant.now()
            }

            return
        }

        if (ring.isOutOfBounds(pos)) {
            reset()
            return
        }
    }

    /**
     * Generates the next ring.
     */
    private fun generate() {
        val latest = rings.maxBy { it.key }
        val idx = latest.key
        val ring = latest.value

        val next = ring.center.clone().add(director.nextOffset())

        val nextRing = Ring(heading, next, director.nextRadius())
        rings[idx + 1] = nextRing

        nextRing.blocks.forEach { it.toLocation(World.world).block.type = style.next() }
    }

    /**
     * Resets the players and rings.
     */
    private fun reset() {
        players.forEach { it.teleport(island.playerSpawn) }

        rings.forEach { (_, ring) -> ring.blocks.forEach { it.toLocation(World.world).block.type = Material.AIR } }
        rings.clear()

        rings[0] = Ring(heading, island.blockSpawn, 0)
        generate()
    }

    companion object {

        /**
         * Creates a new generator.
         * @param player The player to create the generator for.
         */
        fun create(player: Player) {
            val elytraPlayer = ElytraPlayer(player)

            elytraPlayer.join()

            val generator = Generator()

            Divider.add(generator)

            generator.add(elytraPlayer)

            generator.start(Divider.toLocation(generator))
        }

        /**
         * Removes a player from the generator.
         * @param player The player to remove.
         */
        fun remove(player: Player) {
            val elytraPlayer = player.asElytraPlayer() ?: return

            val generator = elytraPlayer.getGenerator()

            generator.remove(elytraPlayer)

            elytraPlayer.leave()
        }
    }
}