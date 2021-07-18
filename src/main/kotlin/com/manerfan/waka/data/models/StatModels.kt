package com.manerfan.waka.data.models

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * waka statistics
 *
 * @author yongyong.fan
 * @date 2021/7/18
 */

/**
 * 摘要
 */
data class StatSummary(
    /**
     * 总时间
     */
    val grandTotalSeconds: Double,

    /**
     * 分类
     */
    val categories: List<StatSummaryNode>,

    /**
     * 编辑器
     */
    val editors: List<StatSummaryNode>,

    /**
     * 语言
     */
    val languages: List<StatSummaryNode>,

    /**
     * 操作系统
     */
    val operatingSystems: List<StatSummaryNode>
)

data class StatSummaryNode(
    /**
     * 分类
     */
    val name: String,

    /**
     * 该分类下的总时间
     */
    val totalSeconds: Double
)

data class StatDuration(
    /**
     * 统计范围
     */
    val range: Range,

    /**
     * 时间段统计
     */
    val durations: List<StatDuration>
)

data class StatDurationNode(
    /**
     * 时间段 HHmm
     * 十分钟一段
     * 0100: 01:00 ~ 01:10
     * 0110: 01:10 ~ 01:20
     * 0120: 01:20 ~ 01:30
     * 0150: 01:50 ~ 02:00
     */
    val period: String,

    /**
     * 编码时间/毫秒
     */
    val duration: Long
) {
    companion object {
        private const val step = 10

        fun from(start: LocalDateTime, end: LocalDateTime): List<StatDurationNode> {
            if (end.isBefore(start)) {
                return emptyList()
            }

            // 取到年月日时分，分钟放到10段内
            // 11:32 -> 11:30 11:05 -> 11:00 11:59 -> 11:50
            var cursorPre = LocalDateTime.of(
                start.year,
                start.month,
                start.dayOfMonth,
                start.hour,
                start.minute.div(step).times(step)
            )
            var cursorPost = cursorPre.plusMinutes(step.toLong())

            val statDurationNodes = mutableListOf<StatDurationNode>()
            while (true) {
                if (cursorPost.isAfter(end)) {
                    statDurationNodes.add(of(cursorPre, end))
                    break
                } else {
                    statDurationNodes.add(of(cursorPre, cursorPost))
                }

                // 每次加10分钟
                cursorPre = cursorPost
                cursorPost = cursorPre.plusMinutes(step.toLong())
            }

            return statDurationNodes
        }

        private fun of(start: LocalDateTime, end: LocalDateTime): StatDurationNode {
            // 回退1秒再格式化是精华
            // 11:00:02 -> 11:00:01 12:00:00 -> 11:59:59
            val date = end.minusSeconds(1)
            val hour = date.hour
            // 12:00:00 -> 11:59 -> 11:50 -> 1150
            val minute = date.minute.div(step).times(step)
            return StatDurationNode("$hour$minute", start.until(end, ChronoUnit.MILLIS))
        }
    }
}

data class Range(
    val date: String,

    /**
     * 开始时间
     */
    val start: String,

    /**
     * 结束时间
     */
    val end: String
)
