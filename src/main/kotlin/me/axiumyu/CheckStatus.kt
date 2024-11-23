package me.axiumyu

import me.axiumyu.IronElevator.Companion.CD
import me.axiumyu.IronElevator.Companion.MAX_TP_HEIGHT
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.TextColor.color
import org.bukkit.Bukkit.getPlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType.INTEGER

object CheckStatus : CommandExecutor {
    override fun onCommand(
        p0: CommandSender, p1: Command, p2: String,
        p3: Array<out String>?
    ): Boolean {
        if (p3 == null || p3.isEmpty()) return false
        val player = if (p3.size == 1) p0 else getPlayer(p3[1])
        if (player !is Player) {
            p0.sendMessage(text().content("需要指定一个玩家名或玩家不存在").color(color(0xdd6666)))
            return false
        }
        when (p3[0]) {
            "height" -> player.sendMessage(printHeight(player))
            "cd" -> player.sendMessage(printCd(player))
        }
        return true
    }
}

fun printHeight(pl: Player): ComponentLike {
    return text().content("你能使用电梯最大改变高度${pl.persistentDataContainer.get(MAX_TP_HEIGHT, INTEGER)}格")
        .color(color(0x5ce6e1))
}

fun printCd(pl: Player): ComponentLike {
    return text().content("你使用电梯的冷却时间为${pl.persistentDataContainer.get(CD, INTEGER)}秒")
        .color(color(0x5ce6e1))
}