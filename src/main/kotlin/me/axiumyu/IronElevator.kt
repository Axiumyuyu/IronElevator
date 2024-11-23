package me.axiumyu

import me.axiumyu.IronElevator.Companion.SAFE_BLOCK
import me.axiumyu.IronElevator.Companion.CD_LIST
import me.axiumyu.IronElevator.Companion.ELEVATOR_BLOCK
import me.yic.xconomy.api.XConomyAPI
import net.kyori.adventure.text.Component.text
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.Sound.*
import org.bukkit.NamespacedKey
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
        //axiumyu:max_tp_height
        @JvmField
        val MAX_TP_HEIGHT = NamespacedKey("axiumyu", "max_tp_height")

        //axiumyu:cd
        @JvmField
        val CD = NamespacedKey("axiumyu", "cd")

        const val DEFAULT_MAX_HEIGHT = 25

        const val MIN_TELEPORT_HEIGHT = 3

        const val DEFAULT_CD = 60

        @JvmField
        val ELEVATOR_BLOCK = IRON_BLOCK

        @JvmField
        val TELEPORT_SOUND = ENTITY_IRON_GOLEM_ATTACK

        //允许的方块列表，传送到以下方块被认为是安全的
        @JvmField
        val SAFE_BLOCK: Set<Material> = setOf(
            AIR,
            CAVE_AIR,
            VOID_AIR,
            WATER,
            WHITE_CARPET,
            LIGHT_GRAY_CARPET,
            GRAY_CARPET,
            BLACK_CARPET,
            BLUE_CARPET,
            BROWN_CARPET,
            CYAN_CARPET,
            GREEN_CARPET,
            LIGHT_BLUE_CARPET,
            LIME_CARPET,
            MAGENTA_CARPET,
            ORANGE_CARPET,
            PINK_CARPET,
            PURPLE_CARPET,
            RED_CARPET,
            OAK_TRAPDOOR,
            SPRUCE_TRAPDOOR,
            BIRCH_TRAPDOOR,
            JUNGLE_TRAPDOOR,
            ACACIA_TRAPDOOR,
            DARK_OAK_TRAPDOOR,
            CRIMSON_TRAPDOOR,
            WARPED_TRAPDOOR,
            OAK_FENCE_GATE,
            SPRUCE_FENCE_GATE,
            BIRCH_FENCE_GATE,
            JUNGLE_FENCE_GATE,
            ACACIA_FENCE_GATE,
            DARK_OAK_FENCE_GATE,
            CRIMSON_FENCE_GATE,
            WARPED_FENCE_GATE,
            OAK_DOOR,
            SPRUCE_DOOR,
            BIRCH_DOOR,
            JUNGLE_DOOR,
            ACACIA_DOOR,
            DARK_OAK_DOOR,
            CRIMSON_DOOR,
            WARPED_DOOR,
            MOSS_CARPET
        )

        //记录冷却
        @JvmField
        val CD_LIST = mutableMapOf<Player, Boolean>()

        val xc: XConomyAPI by lazy { XConomyAPI() }
    }

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        server.getPluginCommand("elevator")?.setExecutor(UpdateElevator)
        server.getPluginCommand("elecheck")?.setExecutor(CheckStatus)
    }

    //初始化玩家的PDC
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

        //主体部分
        val minHeight = pl.world.minHeight
        val startY = pl.location.toBlockLocation().subY().y.toInt()
        pl.location.subY(MIN_TELEPORT_HEIGHT).run {
            while (startY - y <= maxHeight && y >= minHeight) {
                val result = isValidDown(this)
                if (result == 0) {
                    pl.teleport(subY())
                    pl.playSound(pl.location, TELEPORT_SOUND, 1f, 1f)
                    CD_LIST[pl] = false     //false和true的效果应该是一样的
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
        if (pl.gameMode == GameMode.SPECTATOR) return
        val maxHeight = pl.persistentDataContainer.get(MAX_TP_HEIGHT, PersistentDataType.INTEGER)!!
        val coolDown = pl.persistentDataContainer.get(CD, PersistentDataType.INTEGER)!!
        val curBlock = e.to.block.getRelative(BlockFace.DOWN)
        if (e.to.y - e.from.y < 0.25 || curBlock.type != ELEVATOR_BLOCK) return
        if (onCorner(pl.location)) return
        if (CD_LIST[pl] != null) {
            pl.sendActionBar(text("电梯冷却中..."))
            return
        }

        //主体部分
        val worldMaxY = pl.world.maxHeight
        val startY = pl.location.toBlockLocation().addY().y.toInt()
        pl.location.addY(MIN_TELEPORT_HEIGHT).run {
            while (y - startY <= maxHeight && y <= worldMaxY) {
                val result = isValidUp(this)
                if (result == 0) {
                    pl.teleport(addY())
                    pl.playSound(pl.location, TELEPORT_SOUND, 1f, 1f)
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

/**
 * 将Location的Y减少或增加一个值
 * @param d 需要减少或增加的高度，默认为1
 */
private fun Location.subY(d: Int = 1): Location {
    return this.subtract(0.0, d.toDouble(), 0.0)
}

private fun Location.addY(d: Int = 1): Location {
    return this.add(0.0, d.toDouble(), 0.0)
}

/**
 * @return 需要减少的高度，0则认为找到正确位置
 */
private fun isValidDown(location: Location): Int {
    return if (location.block.getRelative(BlockFace.DOWN, 2).type == ELEVATOR_BLOCK) {
        if (checkBlock(location.block.getRelative(BlockFace.DOWN))) {
            if (checkBlock(location.block)) 0 else 3
        } else 2
    } else 1
}

/**
 * @return 需要增加的高度，0则认为找到正确位置
 */
private fun isValidUp(location: Location): Int {
    return if (location.block.type == ELEVATOR_BLOCK) {
        if (checkBlock(location.block.getRelative(BlockFace.UP))) {
            if (checkBlock(location.block.getRelative(BlockFace.UP, 2))) 0 else 3
        } else 2
    } else 1
}

/**
 * @return 是否为安全方块
 */
private fun checkBlock(block: Block): Boolean {
    return (!block.isCollidable || SAFE_BLOCK.contains(block.type))
}

/**
 *  @return 是否在方块边缘，如是则不传送
 */
private fun onCorner(location: Location): Boolean {
    return (abs(location.x - location.x.toInt()) <= 0.2 || abs(location.z - location.z.toInt()) <= 0.2 || abs(
        location.x - location.x.toInt()
    ) >= 0.8 || abs(location.z - location.z.toInt()) >= 0.8 || abs(location.y - location.y.toInt()) > 0.5)
}

/**
 * 去除冷却
 */
class CleanCoolDown(val pl: Player) : BukkitRunnable() {
    override fun run() {
        CD_LIST.remove(pl)
    }
}