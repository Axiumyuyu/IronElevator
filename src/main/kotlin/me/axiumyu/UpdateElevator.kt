package me.axiumyu

import me.axiumyu.IronElevator.Companion.DEFAULT_CD
import me.axiumyu.IronElevator.Companion.DEFAULT_MAX_HEIGHT
import me.axiumyu.IronElevator.Companion.MAX_TP_HEIGHT
import me.axiumyu.IronElevator.Companion.xc
import net.kyori.adventure.text.Component.text
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import kotlin.math.max
import kotlin.random.Random

object UpdateElevator : CommandExecutor {
    override fun onCommand(
        p0: CommandSender, p1: Command, p2: String,
        p3: Array<out String>?
    ): Boolean {
        if (p0 !is Player) return false
        if (p3 == null || p3.isEmpty()) return false
        val levels = p3[1].toIntOrNull() ?: 1
        if (levels==0) return false
        var cost = 0
        if (p3[0].lowercase() == "height") {
            var height = p0.persistentDataContainer.get(MAX_TP_HEIGHT, PersistentDataType.INTEGER)!!
            if (levels+height > 1280) {
                p0.sendActionBar(text("你似乎在做一些没有意义的事情呢"))
                return false
            }
            for (i in 1..levels) {
                if (height >= 512) {
                    cost += (height - DEFAULT_MAX_HEIGHT).div(4).toInt() - Random.nextInt(30)
                } else if (height >= 256) {
                    cost += (height - DEFAULT_MAX_HEIGHT).div(3).toInt() - Random.nextInt(20)
                } else {
                    cost += max(1, (height - DEFAULT_MAX_HEIGHT).div(2).toInt() - Random.nextInt(10))
                }
                height++
            }
            if (xc.getPlayerData(p0.uniqueId).balance >= cost.toBigDecimal()) {
                p0.persistentDataContainer.set(MAX_TP_HEIGHT, PersistentDataType.INTEGER, height)
                xc.changePlayerBalance(p0.uniqueId, p0.name, cost.toBigDecimal(), false)
                p0.sendActionBar(text("你的电梯高度升级到了$height 层,花费了$cost"))
                return true
            } else {
                p0.sendActionBar(text("你的余额不足，需要$cost"))
                return false
            }
        } else if (p3[0].lowercase() == "cd") {
            var cd = p0.persistentDataContainer.get(IronElevator.CD, PersistentDataType.INTEGER)!!
            if (cd - levels <= 0) {
                p0.sendActionBar(text("电梯冷却时间不能小于1"))
                return false
            }
            for (i in 1..levels) {
                cost += DEFAULT_CD - cd + Random.nextInt(30) + 35
                cd--
            }
            if (xc.getPlayerData(p0.uniqueId).balance >= cost.toBigDecimal()) {
                p0.persistentDataContainer.set(IronElevator.CD, PersistentDataType.INTEGER, cd)
                xc.changePlayerBalance(p0.uniqueId, p0.name, cost.toBigDecimal(), false)
                p0.sendActionBar(text("你的电梯冷却升级到了$cd 秒,花费了$cost"))
                return true
            } else {
                p0.sendActionBar(text("你的余额不足，需要$cost"))
                return false
            }
        }
        return false
    }
}