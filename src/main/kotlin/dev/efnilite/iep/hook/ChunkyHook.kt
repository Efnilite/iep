package dev.efnilite.iep.hook

import dev.efnilite.iep.IEP
import dev.efnilite.iep.world.World
import org.bukkit.Bukkit
import org.bukkit.Location
import org.popcraft.chunky.api.ChunkyAPI

object ChunkyHook {

    lateinit var chunky: ChunkyAPI

    fun init() {
        chunky = Bukkit.getServicesManager().load<ChunkyAPI>(ChunkyAPI::class.java)!!
    }

    fun load(location: Location) {
        chunky.startTask(World.world.name, "rectangle", location.x, location.z, 5 * 32.0, 10 * 32.0, "region")

        chunky.onGenerationProgress { IEP.log("Chunky progress: ${it.progress}, rate: ${it.rate}") }
        chunky.onGenerationComplete { IEP.log("Chunky complete") }

        IEP.log("Starting Chunky at ${location.x}, ${location.z}")
    }

}