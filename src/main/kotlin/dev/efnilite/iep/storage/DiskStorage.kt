package dev.efnilite.iep.storage

import dev.efnilite.iep.IEP
import dev.efnilite.iep.generator.Settings
import dev.efnilite.iep.leaderboard.Leaderboard
import dev.efnilite.iep.leaderboard.Score
import java.io.File
import java.util.*

internal object DiskStorage {

    fun init(uuid: UUID) {
        val file = getPlayerFile(uuid)

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
    }

    fun init(leaderboard: Leaderboard) {
        val file = getLeaderboardFile(leaderboard.name)

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
    }

    fun save(uuid: UUID, settings: Settings) {
        getPlayerFile(uuid).writer().use { IEP.GSON.toJson(settings, it) }
    }

    fun load(uuid: UUID): Settings? {
        val serialized = getPlayerFile(uuid).reader().use { IEP.GSON.fromJson(it, Settings::class.java) }

        return serialized ?: null
    }

    fun save(leaderboard: Leaderboard) {
        getLeaderboardFile(leaderboard.name).writer().use { IEP.GSON.toJson(leaderboard.data, it) }
    }

    fun load(leaderboard: Leaderboard) {
        val map = getLeaderboardFile(leaderboard.name).reader().use { IEP.GSON.fromJson(it, Map::class.java) ?: return }

        for ((uuid, score) in map) {
            leaderboard.data[UUID.fromString(uuid.toString())] = IEP.GSON.fromJson(score.toString(), Score::class.java)
        }
    }

    private fun getPlayerFile(uuid: UUID) = File(IEP.instance.dataFolder, "players/$uuid.json")
    private fun getLeaderboardFile(name: String) = File(IEP.instance.dataFolder, "leaderboards/$name.json")
}