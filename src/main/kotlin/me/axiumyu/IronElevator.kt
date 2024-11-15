package me.axiumyu

import me.axiumyu.IronElevator.Companion.ALLOW_BLOCKS
import me.axiumyu.IronElevator.Companion.CD_LIST
import me.axiumyu.IronElevator.Companion.ELEVATOR_BLOCK
import me.yic.xconomy.api.XConomyAPI
import net.kyori.adventure.text.Component.text
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import kotlin.math.abs


class IronElevator : JavaPlugin(), Listener {
    companion object {
        @JvmField
        val ELEVATOR_BLOCK = Material.IRON_BLOCK

        @JvmField
        val TELEPORT_SOUND = Sound.ENTITY_IRON_GOLEM_ATTACK

        @JvmField
        val MAX_TP_HEIGHT = NamespacedKey("axiumyu", "max_tp_height")

        const val DEFAULT_MAX_HEIGHT = 25

        const val MIN_TELEPORT_HEIGHT = 3

        @JvmField
        val CD = NamespacedKey("axiumyu", "cd")

        const val DEFAULT_CD = 60

        @JvmField
        val ALLOW_BLOCKS: Set<Material> = setOf (
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,
            Material.WATER,
            Material.WHITE_CARPET,
            Material.LIGHT_GRAY_CARPET,
            Material.GRAY_CARPET,
            Material.BLACK_CARPET,
            Material.BLUE_CARPET,
            Material.BROWN_CARPET,
            Material.CYAN_CARPET,
            Material.GREEN_CARPET,
            Material.LIGHT_BLUE_CARPET,
            Material.LIME_CARPET,
            Material.MAGENTA_CARPET,
            Material.ORANGE_CARPET,
            Material.PINK_CARPET,
            Material.PURPLE_CARPET,
            Material.RED_CARPET,
            Material.OAK_TRAPDOOR,
            Material.SPRUCE_TRAPDOOR,
            Material.BIRCH_TRAPDOOR,
            Material.JUNGLE_TRAPDOOR,
            Material.ACACIA_TRAPDOOR,
            Material.DARK_OAK_TRAPDOOR,
            Material.CRIMSON_TRAPDOOR,
            Material.WARPED_TRAPDOOR,
            Material.OAK_FENCE_GATE,
            Material.SPRUCE_FENCE_GATE,
            Material.BIRCH_FENCE_GATE,
            Material.JUNGLE_FENCE_GATE,
            Material.ACACIA_FENCE_GATE,
            Material.DARK_OAK_FENCE_GATE,
            Material.CRIMSON_FENCE_GATE,
            Material.WARPED_FENCE_GATE,
            Material.OAK_DOOR,
            Material.SPRUCE_DOOR,
            Material.BIRCH_DOOR,
            Material.JUNGLE_DOOR,
            Material.ACACIA_DOOR,
            Material.DARK_OAK_DOOR,
            Material.CRIMSON_DOOR,
            Material.WARPED_DOOR,
            Material.MOSS_CARPET,

            )

        @JvmField
        val CD_LIST = mutableMapOf<Player, Boolean>()

        val xc: XConomyAPI by lazy { XConomyAPI() }
    }

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        server.getPluginCommand("elevator")?.setExecutor(UpdateElevator)
        server.getPluginCommand("elecheck")?.setExecutor(CheckStatus)
    }

    @EventHandler
    fun onLogin(e: PlayerJoinEvent) {
        val pl = e.getPlayer()
        if (!pl.persistentDataContainer.has(CD, PersistentDataType.INTEGER)) {
            pl.persistentDataContainer.set(CD, PersistentDataType.INTEGER, DEFAULT_CD)
        }
        if (!pl.persistentDataContainer.has(MAX_TP_HEIGHT, PersistentDataType.INTEGER)) {
            pl.persistentDataContainer.set(MAX_TP_HEIGHT, PersistentDataType.INTEGER, DEFAULT_MAX_HEIGHT)
        }
    }

    @EventHandler
    fun downElevator(e: PlayerToggleSneakEvent) {
        val pl = e.getPlayer()
        val maxHeight = pl.persistentDataContainer.get(MAX_TP_HEIGHT, PersistentDataType.INTEGER)!!
        val coolDown = pl.persistentDataContainer.get(CD, PersistentDataType.INTEGER)!!
        if (e.getPlayer().isSneaking || pl.world.getBlockAt(pl.location.subY()).type != ELEVATOR_BLOCK) return
        if (onCorner(pl.location)) return
        if (CD_LIST[pl] != null) {
            pl.sendActionBar(text("电梯冷却中..."))
            return
        }

        val minHeight = pl.world.minHeight
        val startY = pl.location.toBlockLocation().subY().y.toInt()
        pl.location.subY(MIN_TELEPORT_HEIGHT).run {
            while (startY - y <= maxHeight && y >= minHeight) {
                val result = isValidDown(this)
                if (result == 0) {
                    pl.teleport(subY())
                    pl.playSound(pl.getLocation(), TELEPORT_SOUND, 1f, 1f)
                    CD_LIST[pl] = false
                    CleanCoolDown(pl).runTaskLater(this@IronElevator, coolDown * 20L)
                    return
                } else {
                    subY(result)
                }
            }
        }
    }

    @EventHandler
    fun upElevator(e: PlayerMoveEvent) {
        val pl = e.getPlayer()
        if (pl.gameMode== GameMode.SPECTATOR) return
        val maxHeight = pl.persistentDataContainer.get(MAX_TP_HEIGHT, PersistentDataType.INTEGER)!!
        val coolDown = pl.persistentDataContainer.get(CD, PersistentDataType.INTEGER)!!
        val curBlock = e.to.block.getRelative(BlockFace.DOWN)
        if (e.to.y - e.from.y < 0.25 || curBlock.type != ELEVATOR_BLOCK) return
        if (onCorner(pl.location)) return
        if (CD_LIST[pl] != null) {
            pl.sendActionBar(text("电梯冷却中..."))
            return
        }

        val worldMaxY = pl.world.maxHeight
        val startY = pl.location.toBlockLocation().addY().y.toInt()
        pl.location.addY(MIN_TELEPORT_HEIGHT).run {
            while (y - startY <= maxHeight && y <= worldMaxY) {
                val result = isValidUp(this)
                if (result == 0) {
                    pl.teleport(addY())
                    pl.playSound(pl.getLocation(), TELEPORT_SOUND, 1f, 1f)
                    CD_LIST[pl] = false
                    CleanCoolDown(pl).runTaskLater(this@IronElevator, coolDown * 20L)
                    return
                } else {
                    addY(result)
                }
            }
        }
    }
}

fun Location.subY(d: Int = 1): Location {
    return this.subtract(0.0, d.toDouble(), 0.0)
}

fun Location.addY(d: Int = 1): Location {
    return this.add(0.0, d.toDouble(), 0.0)
}

fun isValidDown(location: Location): Int {
    return if (location.block.getRelative(BlockFace.DOWN, 2).type == ELEVATOR_BLOCK) {
        if (checkBlock(location.block.getRelative(BlockFace.DOWN))) {
            if (checkBlock(location.block)) 0 else 3
        } else 2
    } else 1
}

fun isValidUp(location: Location): Int {
    return if (location.block.type == ELEVATOR_BLOCK) {
        if (checkBlock(location.block.getRelative(BlockFace.UP))) {
            if (checkBlock(location.block.getRelative(BlockFace.UP, 2))) 0 else 3
        } else 2
    } else 1
}

fun checkBlock(block: Block): Boolean {
    return (!block.isCollidable || ALLOW_BLOCKS.contains(block.type))
}

fun onCorner(location: Location): Boolean {
    return (abs(location.x - location.x.toInt()) <= 0.2 || abs(location.z - location.z.toInt()) <= 0.2 || abs(
        location.x - location.x.toInt()
    ) >= 0.8 || abs(location.z - location.z.toInt()) >= 0.8 || abs(location.y - location.y.toInt()) > 0.5)
}

class CleanCoolDown(val pl: Player) : BukkitRunnable() {
    override fun run() {
        CD_LIST.remove(pl)
    }
}