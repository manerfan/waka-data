package com.manerfan.waka.data.models

import com.manerfan.waka.data.verticles.oss.OssFileType
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * waka statistics
 *
 * @author yongyong.fan
 * @date 2021/7/18
 */

data class StatData(
    /**
     * 统计粒度
     */
    val grading: Grading,

    /**
     * 统计范围
     */
    val range: Range,

    /**
     * 统计摘要
     */
    val summaries: StatSummary,

    /**
     * 分钟维度时长统计
     */
    val durations: List<StatDurationNode>,

    /**
     * 天维度时长统计
     */
    val contributions: List<StatDurationNode>,

    /**
     * 统计
     */
    val stat: Stat
)

/**
 * 摘要
 */
data class StatSummary(
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
    val operatingSystems: List<StatSummaryNode>,

    /**
     * 项目
     */
    val projects: List<StatSummaryNode>
)

data class StatSummaryNode(
    /**
     * 分类
     */
    val name: String,

    /**
     * 该分类下的总时间
     */
    var totalDuration: Long
)

data class StatDurationNode(
    /**
     * 时间段 HHmm
     * 五分钟一段
     * 01:00 01:00 ~ 01:05
     * 01:10 01:10 ~ 01:15
     * 01:25 01:25 ~ 01:30
     * 01:50 01:50 ~ 01:55
     * 01:55 01:55 ~ 02:00
     */
    val period: String,

    /**
     * 编码时间/毫秒
     */
    var duration: Long
) {
    companion object {
        const val step = 5

        fun from(start: LocalDateTime, end: LocalDateTime): List<StatDurationNode> {
            if (end.isBefore(start)) {
                return emptyList()
            }

            // 取到年月日时分，分钟放到5(分钟)段内
            // 11:32 -> 11:30 11:05 -> 11:05 11:59 -> 11:55
            var cursorPre = start
            var cursorPost = LocalDateTime.of(
                start.year,
                start.month,
                start.dayOfMonth,
                start.hour,
                start.minute.div(step).times(step)
            ).plusMinutes(step.toLong())

            val statDurationNodes = mutableListOf<StatDurationNode>()
            while (true) {
                if (cursorPost.isAfter(end)) {
                    statDurationNodes.add(of(cursorPre, end))
                    break
                } else {
                    statDurationNodes.add(of(cursorPre, cursorPost))
                }

                // 每次加5分钟
                cursorPre = cursorPost
                cursorPost = cursorPre.plusMinutes(step.toLong())
            }

            return statDurationNodes
        }

        private fun of(start: LocalDateTime, end: LocalDateTime): StatDurationNode {
            return StatDurationNode(
                localDateTime2PeriodStr(end),
                start.until(end, ChronoUnit.MILLIS)
            )
        }

        fun localDateTime2PeriodStr(ldt: LocalDateTime): String {
            // 回退1秒再格式化是精华
            // 11:00:02 -> 11:00:01 12:00:00 -> 11:59:59
            val date = ldt.minusSeconds(1)
            val hour = date.hour
            // 12:00:00 -> 11:59 -> 11:55
            val minute = date.minute.div(step).times(step)
            return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        }
    }
}

data class Range(
    /**
     * 开始时间
     */
    val start: String,

    /**
     * 结束时间
     */
    val end: String
) {
    companion object {
        private val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.SIMPLIFIED_CHINESE)
        fun from(start: ZonedDateTime, end: ZonedDateTime) = Range(dtf.format(start), dtf.format(end))
    }
}

data class Stat(
    /**
     * 最辛苦的一天
     */
    val mostHardDay: MostHardDay?,

    /**
     * 工作最晚的一天
     */
    val mostLateDay: MostLateDay?,

    /**
     * 工作最早的一天
     */
    val mostEarlyDay: MostEarlyDay?,

    /**
     * 最喜欢的时间段
     */
    val favoritePeriod: FavoritePeriod?,

    /**
     * 针对日维度统计
     */
    val dailyPeriod: FavoritePeriod?,

    /**
     * 工作日平均每天工作时长
     */
    val averageDurationsOnWorkDays: Long,
)

data class MostHardDay(
    val date: String,
    val totalDuration: Long
)

data class MostLateDay(
    val date: String,
    val time: String,
)

data class MostEarlyDay(
    val date: String,
    val time: String
)

data class FavoritePeriod(
    val from: String,
    val end: String,
    val totalDuration: Long
)

enum class Grading(val desc: String, val ossFileType: OssFileType) {
    DAILY("日统计", OssFileType.STAT_DAILY),
    WEEK("周统计", OssFileType.STAT_WEEK),
    MONTH("月度统计", OssFileType.STAT_MONTH),
    QUARTER("季度统计", OssFileType.STAT_QUARTER),
    HALF_YEAR("半年统计", OssFileType.STAT_HALF_YEAR),
    YEAR("全年统计", OssFileType.STAT_YEAR)
}
