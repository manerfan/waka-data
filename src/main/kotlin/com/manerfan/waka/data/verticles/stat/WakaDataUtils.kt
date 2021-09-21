package com.manerfan.waka.data.verticles.stat

import com.manerfan.waka.data.OSS_BUCKET_NAME
import com.manerfan.waka.data.OSS_CLIENT
import com.manerfan.waka.data.OSS_CONFIG_KEY
import com.manerfan.waka.data.models.*
import com.manerfan.waka.data.verticles.oss.ShareableOss
import io.vertx.reactivex.core.Vertx
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

/**
 * 统计工具
 *
 * @author yongyong.fan
 * @date 2021/7/28
 */

data class OssObject(
    val bucketName: String,
    val ossClient: ShareableOss
) {
    companion object {
        fun from(vertx: Vertx) = OssObject(
            vertx.sharedData().getLocalMap<String, String>(OSS_CONFIG_KEY)[OSS_BUCKET_NAME]!!,
            vertx.sharedData().getLocalMap<String, ShareableOss>(OSS_CLIENT)[OSS_CLIENT]!!
        )
    }
}

/**
 * 原始的 WakaSummaryNode 列表 -> 统计用 StatSummaryNode 列表
 */
fun List<WakaSummaryNode>?.toStatSummaryNodeList() = this.orEmpty().asSequence().map { wakaSummaryNode ->
    StatSummaryNode(wakaSummaryNode.name ?: "unknown", wakaSummaryNode.totalSeconds.times(1000).toLong())
}.sortedByDescending(StatSummaryNode::totalDuration).toList()

/**
 * 一天中找到最忙的时间段
 */
fun List<StatDurationNode>?.favoritePeriod(): FavoritePeriod? = this?.let { durations ->
    val endTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 0, 0))
    val ds = durations.sortedBy { duration -> duration.period }
    // 从 00:00 开始，每隔 StatDurationNode.step 时间生成一个
    Stream.iterate(LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0, 0))) {
        it.plus(StatDurationNode.step.toLong(), ChronoUnit.MINUTES)
    }
        .limit(24L * 60 / StatDurationNode.step)
        // 只取 23:00 之前的
        .filter { ldt -> !ldt.isAfter(endTime) }
        .map { ldt ->
            // 将每一个时间段的总时长计算出来
            ds.totalBetween(
                StatDurationNode.localDateTime2PeriodStr(ldt),
                StatDurationNode.localDateTime2PeriodStr(ldt.plus(1, ChronoUnit.HOURS))
            )
        }
        // 找到总时长最长的一个时间段
        .sorted { fp1, fp2 -> -1 * fp1.totalDuration.compareTo(fp2.totalDuration) }
        .findFirst()
        .orElse(null)
}

/**
 * 在时间段之间的总时长
 */
fun List<StatDurationNode>?.totalBetween(start: String, end: String): FavoritePeriod = this?.let {
    FavoritePeriod(
        start, end,
        it.asSequence()
            .filter { duration -> duration.period >= start && duration.period < end }
            .sumOf { duration -> duration.duration }
    )
} ?: FavoritePeriod(start, end, 0)

/**
 * 一天中找到最晚的时间段
 */
fun List<StatDurationNode>?.latest(date: String, criticalTime: String = "04:00"): MostLateDay? = this?.let {
    val durations = it.asSequence().sortedBy { duration -> duration.period }
    val latestDuration = durations.filter { duration ->
        duration.period <= criticalTime
    }.maxByOrNull { duration -> duration.period } ?: durations.lastOrNull()
    return latestDuration?.let { duration -> MostLateDay(date, duration.period) }
}

/**
 * 一天中找到最早的时间段
 */
fun List<StatDurationNode>?.earliest(date: String, criticalTime: String = "04:00"): MostEarlyDay? = this?.let {
    it.asSequence()
        .sortedBy { duration -> duration.period }
        .filter { duration -> duration.period >= criticalTime }
        .firstOrNull()
        ?.let { duration -> MostEarlyDay(date, duration.period) }
}

// 周六周统计
fun ZonedDateTime.isWeekStat() = DayOfWeek.SATURDAY == this.dayOfWeek

// 1号月统计
fun ZonedDateTime.isMonthStat() = 1 == this.dayOfMonth

// 1、4、7、10月1号季统计
fun ZonedDateTime.isQuarterStat() =
    this.isMonthStat() && setOf(Month.JANUARY, Month.APRIL, Month.JULY, Month.OCTOBER).contains(this.month)

// 7月1号半年统计
fun ZonedDateTime.isHalfYearStat() = this.isMonthStat() && Month.JULY == this.month

// 1月1号年统计
fun ZonedDateTime.isYearStat() = this.isMonthStat() && Month.JANUARY == this.month
