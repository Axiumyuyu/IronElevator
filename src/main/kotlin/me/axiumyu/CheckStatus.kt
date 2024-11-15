package me.axiumyu

import me.axiumyu.IronElevator.Companion.CD
import me.axiumyu.IronElevator.Companion.MAX_TP_HEIGHT
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextColor.color
import org.bukkit.Bukkit.getPlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType.*

object CheckStatus : CommandExecutor {
    override fun onCommand(
        p0: CommandSender, p1: Command, p2: String,
        p3: Array<out String>?
    ): Boolean {
        if (p3?.isNotEmpty() == true && p3.size == 1) {
            val nameNeeded by lazy { text("需要指定一个玩家名").color(color(0xdd6666)) }
            if (p3[0] == "height") {
                if (p0 is Player) {
                    p0.sendMessage(printHeight(p0))
                } else {
                    p0.sendMessage(nameNeeded)
                }
            } else if (p3[0] == "cd") {
                if (p0 is Player) {
                    p0.sendMessage(printCd(p0))
                } else {
                    p0.sendMessage(nameNeeded)
                }
            }
        } else if (p3?.isNotEmpty() == true && p3.size == 2) {
            val playerNotFound by lazy { text("玩家不存在").color(color(0xdd6666)) }
            if (p3[0] == "height") {
                getPlayer(p3[1])?.let {
                    it.sendMessage(printHeight(it))
                } ?: p0.sendMessage(playerNotFound)
            } else if (p3[0] == "cd") {
                getPlayer(p3[1])?.let {
                    it.sendMessage(printCd(it))
                } ?: p0.sendMessage(playerNotFound)
            }
        }
        return true
    }
}

fun printHeight(pl: Player): Component {
    return text("你能使用电梯最大改变高度${pl.persistentDataContainer.get(MAX_TP_HEIGHT, INTEGER)}格").color(color(0x5ce6e1))
}

fun printCd(pl: Player): Component {
    return text("你使用电梯的冷却时间为${pl.persistentDataContainer.get(CD, INTEGER)}秒").color(color(0x5ce6e1))
}
