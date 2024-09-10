package dev.efnilite.iep

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dev.efnilite.iep.config.Config
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.hook.ChunkyHook
import dev.efnilite.iep.hook.PapiHook
import dev.efnilite.iep.mode.*
import dev.efnilite.iep.style.IncrementalStyle
import dev.efnilite.iep.style.RandomStyle
import dev.efnilite.iep.style.Style
import dev.efnilite.iep.world.Divider
import dev.efnilite.iep.world.World
import dev.efnilite.vilib.ViPlugin
import dev.efnilite.vilib.bstats.bukkit.Metrics
import dev.efnilite.vilib.inventory.Menu
import dev.efnilite.vilib.schematic.Schematics
import dev.efnilite.vilib.util.Logging
import dev.efnilite.vilib.util.Task
import dev.efnilite.vilib.util.UpdateChecker
import io.papermc.lib.PaperLib
import org.bukkit.Material
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level

class IEP : ViPlugin() {

    val logging = Logging(this)

    override fun enable() {
        instance = this
        stopping = false

        registerListener(Events)
        registerCommand("iep", Command)

        saveFile("schematics/spawn-island")

        World.create()
        Locales.init()
        Menu.init(this)

        Schematics.addFromFiles(this,
            *Files.list(Path.of(dataFolder.toString(), "schematics"))
                .map { it.toFile() }.toList().toTypedArray()
        )

        registerStyle("styles.random") { name, data -> RandomStyle(name, data) }
        registerStyle("styles.incremental") { name, data -> IncrementalStyle(name, data) }

        registerMode(DefaultMode, false)
        registerMode(SpeedDemonMode)
        registerMode(MinSpeedMode)
        registerMode(TimeTrialMode)
        registerMode(CloseMode)
        registerMode(ObstacleMode)

        if (server.pluginManager.isPluginEnabled("PlaceholderAPI")) {
            log("Registered PlaceholderAPI Hook")
            papiHook = PapiHook
            PapiHook.register()
        }
        if (server.pluginManager.isPluginEnabled("Chunky")) {
            chunkyHook = ChunkyHook
//            ChunkyHook.init() todo
        }
        if (server.pluginManager.isPluginEnabled("Vault")) {
            log("Registered Vault Hook")
        }
        if (Config.CONFIG.getBoolean("proxy.enabled")) {
            log("Registered BungeeCord Hook")
            server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
        }
        
        PaperLib.suggestPaper(this, Level.WARNING)

        Metrics(this, 21243)

        Task.create(this)
            .async()
            .repeat(5 * 60 * 20)
            .execute {
                log("Saving all leaderboards")
                modes.forEach { it.leaderboard.save() }
            }
            .run()

        UpdateChecker.check(this, 115322)
    }

    fun saveFile(path: String) {
        val file = File(dataFolder.toString(), path)

        if (!file.exists()) {
            saveResource(path, false)
        }
    }

    private fun registerStyle(path: String, fn: (name: String, data: List<Material>) -> Style) {
        Config.CONFIG.getPaths(path).forEach { name ->
            registerStyle(
                fn.invoke(name, Config.CONFIG.getStringList("$path.$name")
                    .map {
                        try {
                            return@map Material.getMaterial(it.uppercase())!!
                        } catch (ex: NullPointerException) {
                            logging.error("Invalid material in style $path.$name: $it")
                            return@map Material.STONE
                        }
                    })
            )
        }
    }

    override fun disable() {
        stopping = true

        try {
            for (generator in HashSet(Divider.generators)) {
                generator.player.leave(urgent = true)
            }

            getModes().forEach { it.leaderboard.save() }

            World.delete()
        } catch (_: Exception) {
            // for no class found errors if nobody has joined yet
        }
    }

    companion object {
        var stopping = false
            private set
        var papiHook: PapiHook? = null
        var chunkyHook: ChunkyHook? = null

        val GSON: Gson = GsonBuilder().disableHtmlEscaping().create()

        lateinit var instance: IEP
            private set

        fun log(message: String) {
            if (Config.CONFIG.getBoolean("debug")) {
                instance.logging.info("[Debug] $message")
            }
        }

        private val modes: MutableList<Mode> = mutableListOf()

        fun registerMode(mode: Mode, checkExists: Boolean = true) {
            if (!Config.CONFIG.getBoolean("mode-settings.${mode.name.replace(" ", "-")}.enabled") && checkExists) {
                return
            }

            log("Registered mode ${mode.name}")

            modes += mode
        }

        fun getMode(name: String): Mode? = modes.firstOrNull { it.name == name }

        fun getModes() = modes.toList()

        private val styles: MutableList<Style> = mutableListOf()

        fun registerStyle(style: Style) {
            log("Registered style ${style.name()}")

            styles += style
        }

        fun getStyle(name: String) = styles.firstOrNull { it.name() == name } ?: styles.first()

        fun getStyles() = styles.toList()
    }
}