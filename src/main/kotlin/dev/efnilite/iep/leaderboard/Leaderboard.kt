package dev.efnilite.iep.leaderboard

import dev.efnilite.iep.IEP
import dev.efnilite.iep.storage.Storage
import dev.efnilite.vilib.util.Task
import java.util.*

/**
 * Represents a mode's leaderboard
 * @param name The name of the leaderboard
 * @param minScore The minimum score required to be on the leaderboard
 */
data class Leaderboard(val name: String) {

    var minScore: Double = 0.0

    constructor(name: String, minScore: Double) : this(name) {
        require(minScore >= 0) { "Minimum score must be greater than or equal to 0" }
        this.minScore = minScore
    }

    val data = mutableMapOf<UUID, Score>()

    init {
        load()
    }

    /**
     * Asynchronously reads the leaderboard.
     */
    private fun load() {
        Task.create(IEP.instance)
            .async()
            .execute {
                Storage.init(this)
                Storage.load(this)
            }
            .run()
    }

    /**
     * Asynchronously saves the leaderboard.
     */
    fun save() {
        if (IEP.stopping) {
            Storage.save(this)
            return
        }

        Task.create(IEP.instance)
            .async()
            .execute { Storage.save(this) }
            .run()
    }

    /**
     * Updates the leaderboard.
     * If this player has a score with a higher score and lower time, it will not be updated.
     * @param uuid The player's UUID
     * @param score The player's score
     */
    fun update(uuid: UUID, score: Score) {
        val existing = data.getOrDefault(uuid, EMPTY_SCORE)

        if (existing.score > score.score) {
            return
        }
        if (existing.score == score.score && existing.time < score.time) {
            return
        }

        data[uuid] = score
    }

    /**
     * Resets the score of the specified player.
     *
     * @param uuid The player's UUID
     */
    fun reset(uuid: UUID) {
        data.remove(uuid)
    }

    /**
     * Returns the score instance of the specified player.
     * @param uuid The player's UUID
     * @return The score instance of this player, else an empty score
     */
    fun getScore(uuid: UUID): Score {
        return data.getOrDefault(uuid, EMPTY_SCORE)
    }

    /**
     * Returns the score instance at the specified rank.
     * This rank is guaranteed to be above the minimum score for this leaderboard.
     * @param rank The rank
     * @return The score instance at this rank, else an empty score
     */
    fun getRank(rank: Int): Score {
        val scores = data.values
            .filter { it.score >= minScore }
            .sortedByDescending { it.score }

        if (rank < 1 || rank > scores.size) return EMPTY_SCORE

        return scores[rank - 1]
    }

    /**
     * Returns all scores on the leaderboard.
     * @return All scores on the leaderboard that are above the minimum score.
     */
    fun getAllScores() = data.filter { it.value.score >= minScore }

    companion object {
        private val EMPTY_SCORE = Score("?", 0.0, 0, 0)
    }
}