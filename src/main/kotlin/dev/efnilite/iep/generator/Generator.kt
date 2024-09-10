package dev.efnilite.iep.generator

import dev.efnilite.iep.IEP
import dev.efnilite.iep.IEP.Companion.chunkyHook
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.generator.Settings.Companion.asStyle
import dev.efnilite.iep.generator.section.ClientBlockChanger
import dev.efnilite.iep.generator.section.PointType
import dev.efnilite.iep.generator.section.Section
import dev.efnilite.iep.leaderboard.Leaderboard
import dev.efnilite.iep.leaderboard.Score
import dev.efnilite.iep.leaderboard.Score.Companion.pretty
import dev.efnilite.iep.mode.Mode
import dev.efnilite.iep.player.ElytraPlayer
import dev.efnilite.iep.reward.Rewards
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max
import kotlin.random.Random

private class RewardHandler(val mode: Mode) {

    /**
     * Achieved score rewards this run.
     */
    private val scoreRewards = mutableListOf<Int>()

    /**
     * Achieved interval rewards this run.
     */
    private val intervalRewards = mutableListOf<Int>()

    /**
     * Achieved one-time rewards this run.
     */
    private val oneTimeRewards = mutableListOf<Int>()

    fun checkScores(score: Int, generator: Generator) {
        ((score - 4)..score)
            .filter { it > 0 }
            .forEach { check(it, generator.player.player, generator) }
    }

    private fun check(score: Int, player: Player, generator: Generator) {
        for ((key, rewards) in Rewards.scoreRewards) {
            if (score < key || key in scoreRewards) continue

            scoreRewards += key

            rewards.forEach { it.execute(player, mode) }
        }

        for ((key, rewards) in Rewards.oneTimeRewards) {
            if (score < key || score in oneTimeRewards || key in generator.settings.rewards) continue

            oneTimeRewards += score
            generator.set { settings ->
                val newRewards = settings.rewards
                newRewards += key
                return@set Settings(settings, rewards = newRewards)
            }

            rewards.forEach { it.execute(player, mode) }
        }

        for ((key, rewards) in Rewards.intervalRewards) {
            if (score % key != 0 || score in intervalRewards) continue

            intervalRewards += score

            rewards.forEach { it.execute(player, mode) }
        }
    }

    fun clear() {
        scoreRewards.clear()
        oneTimeRewards.clear()
    }
}

private class Island(vector: Vector, schematic: Schematic) {

    private val blocks: List<Block> = schematic.paste(vector.toLocation(World.world))
    val playerSpawn: Vector
    val blockSpawn: Vector

    init {
        require(blocks.isNotEmpty())

        blocks.first { it.type == Material.DIAMOND_BLOCK }.let {
            playerSpawn = it.location.toVector().add(Vector(0.5, 0.0, 0.5))

            it.type = Material.AIR
        }
        blocks.first { it.type == Material.EMERALD_BLOCK }.let {
            blockSpawn = it.location.toVector().add(Vector(5, 0, 0))

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


open class Generator {

    private val blockChanger = ClientBlockChanger()

    protected val sections = mutableMapOf<Int, Section>()

    private lateinit var rewardHandler: RewardHandler
    private lateinit var island: Island
    private lateinit var task: BukkitTask
    private lateinit var leaderboard: Leaderboard
    private var start: Instant? = null
    private var pointType: PointType = PointType.CIRCLE
    private var random = Random(0)
    private var movementScore = 0.0
    private var resetTo: Vector? = null

    lateinit var player: ElytraPlayer
        private set
    lateinit var settings: Settings
        private set
    var seed = 0
        protected set

    /**
     * Adds a player to the generator.
     * @param player The player to add.
     */
    fun add(player: ElytraPlayer) {
        IEP.log("Adding player to generator ${player.name}")

        this.player = player

        settings = player.load()
    }

    /**
     * Removes a player from the generator.
     * @param player The player to remove.
     */
    fun remove(player: ElytraPlayer) {
        IEP.log("Removing player from generator ${player.name}")

        player.save(settings)

        task.cancel()

        reset(ResetReason.RESET, false)

        island.clear()

        Divider.remove(this)
    }

    /**
     * Initializes all the stuff.
     * @param mode The mode to use.
     * @param start The vector to spawn the island at.
     * @param point The point type to use.
     */
    open fun start(mode: Mode, start: Vector, point: PointType) {
        IEP.log("Starting generator at $start")

        rewardHandler = RewardHandler(mode)
        leaderboard = mode.leaderboard
        island = Island(start, Schematics.getSchematic(IEP.instance, "spawn-island"))
        pointType = point

        player.player.setPlayerTime(settings.time.toLong(), false)

        reset(ResetReason.RESET)

        task = Task.create(IEP.instance)
            .delay(5)
            .repeat(1)
            .execute(::tick)
            .run()

        chunkyHook?.init()
    }

    open fun getScore() = max(0.0, movementScore)

    fun getTime(): Instant = Instant.now().minusMillis(start?.toEpochMilli() ?: Instant.now().toEpochMilli())

    fun getHighScore() = leaderboard.getScore(player.uuid)

    /**
     * Ticks the generator.
     */
    protected open fun tick() {
        val (idx, section) = sections.maxBy { it.key }

        val pos = player.position
        val score = getScore()

        if (shouldScore()) {
            this.movementScore += player.player.velocity.x
        }

        if (Rewards.enabled) {
            rewardHandler.checkScores(score.toInt(), this)
        }

        blockChanger.check(player.player, settings.style.asStyle())

        updateBoard(score, getTime())
        updateInfo()

        if (start == null && score > 0) {
            start = Instant.now()
        }

        if (sections.size > 2) {
            val (minIdx, minSection) = sections.minBy { it.key }

            clear(minIdx, minSection)
        }

        val resetReason = shouldReset(player, pos)
        if (resetReason != null) {
            reset(resetReason)
            return
        }

        if (section.isNearKnot(pos, 0) && section.end.y < 25) {
            val offset = Vector(0, 200, 0)
            val cloned = section.clone(offset)

            sections[idx + 1] = cloned

            IEP.log("Generating cloned section for low y at ${idx + 1}")

            cloned.generate(settings, pointType).thenApply { blockChanger.queue(it) }

            resetTo = offset

            return
        }

        if (resetTo != null) {
            val previous = sections[idx - 1]!!

            if (!previous.isNearKnot(pos, 2)) {
                return
            }

            resetPlayerHeight(resetTo!!)

            resetTo = null

            return
        }

        if (section.isNearKnot(pos, 2)) {
            generate(CompletableFuture.completedFuture(null))

            return
        }
    }

    protected fun shouldScore(): Boolean {
        return player.player.isGliding && player.position.x - island.blockSpawn.x > 0
    }

    private fun updateBoard(score: Double, time: Instant) {
        player.updateBoard(score, Score.timeFormatter.format(time), seed)
    }

    private fun updateInfo() {
        if (!settings.info) return

        val speed = getSpeed(player)

        player.sendActionBar("<gray>${convertSpeed(speed)}")
    }

    private fun convertSpeed(speed: Double): String {
        return if (settings.metric) {
            "${(speed * 3.6).pretty()} km/h"
        } else {
            "${(speed * 2.236936).pretty()} mph"
        }
    }

    private fun shouldReset(player: ElytraPlayer, pos: Vector): ResetReason? {
        val (idx, section) = sections
            .filter { pos.x < it.value.end.x }
            .minByOrNull { it.key } ?: return null

        val progress = pos.x - section.beginning.x

        if (progress < 0) {
            if (idx == 0 && pos.y < island.blockSpawn.y - settings.radius) {
                IEP.log("Player ${player.name} is below spawn")

                return ResetReason.BOUNDS
            }

            return null
        }

        val isPastSpawn = progress > 0
        val isNotGliding = isPastSpawn && !player.player.isGliding
        val isOutOfBounds = isPastSpawn && !section.isNearPoint(pos, progress.toInt(), settings.radius.toDouble())

        if (isNotGliding) {
            IEP.log("Player ${player.name} is not gliding")
            return ResetReason.FLYING
        } else if (isOutOfBounds) {
            IEP.log("Player ${player.name} is out of bounds")
            return ResetReason.BOUNDS
        }

        return null
    }

    /**
     * Generates the next section.
     */
    protected open fun generate(waitForDisplay: CompletableFuture<Void>) {
        if (sections.isEmpty()) {
            val section = Section(island.blockSpawn.clone().add(Vector(0, pointType.heightOffset, 0)), random)

            sections[0] = section

            IEP.log("Generating section at 0")

            section.generate(settings, pointType).thenApply {
                waitForDisplay.thenRun {
                    Task.create(IEP.instance)
                        .delay(15)
                        .execute { blockChanger.queue(it) }
                        .run()
                }
            }

            return
        }

        val latest = sections.maxBy { it.key }
        val idx = latest.key
        val previous = latest.value
        val end = previous.end

        val section = Section(end, random)

        sections[idx + 1] = section

        IEP.log("Generating section at ${idx + 1}")

        section.generate(settings, pointType).thenApply { blockChanger.queue(it) }
    }

    protected open fun resetPlayerHeight(toStart: Vector) {
        val velocity = player.player.velocity

        IEP.log("Resetting player height, velocity = $velocity")

        val to = player.player.location.add(toStart)

        val (minIdx, minSection) = sections.minBy { it.key }

        clear(minIdx, minSection)

        player.teleport(to).thenRun {
            Task.create(IEP.instance)
                .delay(2)
                .execute {
                    IEP.log("Restoring pre-teleport velocity, velocity = $velocity")
                    player.player.velocity = velocity
                }
                .run()
        }
    }

    protected open fun clear(idx: Int, section: Section) {
        IEP.log("Clearing section $idx")

        section.clear(player.player)

        sections.remove(idx)
    }

    /**
     * Resets the players and knots.
     */
    open fun reset(
        resetReason: ResetReason,
        regenerate: Boolean = true,
        s: Int = settings.seed,
        overrideSeedSettings: Boolean = false
    ) {
        IEP.log("Resetting generator, regenerate = $regenerate, seed = $s")

        if (getScore() > 0.0) {
            val score = Score(
                name = player.name,
                score = getScore(),
                time = getTime().toEpochMilli(),
                seed = seed
            )

            leaderboard.update(player.uuid, score)

            if (settings.fall) {
                Locales.getStringList(player, "reset.lines")
                    .map { line -> updateLine(player, line, score, resetReason) }
                    .forEach { line -> player.send(line) }
            }
        }

        movementScore = 0.0
        start = null
        resetTo = null
        if (settings.seed == -1 && !overrideSeedSettings) {
            seed = ThreadLocalRandom.current().nextInt(SEED_BOUND)
            random = Random(seed)
        } else {
            seed = s
            random = Random(s)
        }

        sections.toMap().forEach { clear(it.key, it.value) }
        sections.clear()

        blockChanger.clear()

        rewardHandler.clear()

        if (!regenerate) {
            return
        }

        val spawn = island.playerSpawn.toLocation(World.world)
        spawn.yaw = -90f

        player.player.velocity = Vector(0, 0, 0)
        player.player.fallDistance = 0f

        generate(CompletableFuture.allOf(player.teleport(spawn)))
    }

    private fun updateLine(player: ElytraPlayer, line: String, score: Score, resetReason: ResetReason): String {
        return line.replace("%score%", score.score.pretty())
            .replace("%high-score%", getHighScore().score.pretty())
            .replace("%time%", score.getFormattedTime())
            .replace("%seed%", score.seed.toString())
            .replace("%reason%", Locales.getString(player, "reset.reasons.${resetReason.name.lowercase()}"))
    }

    /**
     * Returns the current speed of the player in m/s.
     * @param player The player to get the speed for.
     * @return The current speed.
     */
    fun getSpeed(player: ElytraPlayer) = player.player.velocity.clone().setY(0).length() * 20

    /**
     * Allows for easy setting of the current [Settings] instance.
     */
    fun set(mapper: (Settings) -> Settings) {
        IEP.log("Updating settings from $settings to ${mapper.invoke(settings)}")

        settings = mapper.invoke(settings)

        player.save(settings)
    }

    companion object {

        const val SEED_BOUND = 1_000_000

    }
}