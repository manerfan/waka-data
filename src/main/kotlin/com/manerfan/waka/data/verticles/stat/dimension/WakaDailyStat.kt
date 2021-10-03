package com.manerfan.waka.data.verticles.stat.dimension

import com.manerfan.waka.data.DEF_ZONEID
import com.manerfan.waka.data.mapper
import com.manerfan.waka.data.models.*
import com.manerfan.waka.data.toLocalDateTime
import com.manerfan.waka.data.verticles.oss.OssAccessorVerticle
import com.manerfan.waka.data.verticles.oss.OssFileType
import com.manerfan.waka.data.verticles.stat.*
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors

/**
 * 日维度统计
 *
 * @author maner.fan
 * @date 2021/7/28
 */
class WakaDailyStat(private val ossObject: OssObject) : WakaStat {
    override fun stat(date: ZonedDateTime): StatData? {
        val (bucketName, ossClient) = ossObject

        val fileKey = OssAccessorVerticle.format(date, OssFileType.META)
        if (!ossClient.oss.doesObjectExist(bucketName, fileKey)) {
            return null
        }

        val wakaData = ossClient.oss.getObject(bucketName, fileKey).let {
            mapper.readValue(it.objectContent, WakaData::class.java)
        }

        return stat(wakaData)
    }

    fun stat(wakaData: WakaData): StatData? {
        val summary = wakaData.summaries?.asSequence()?.sortedByDescending { it.range.date }?.first() ?: return null
        val durations = wakaData.durations.orEmpty().parallelStream().flatMap { duration ->
            // 开始时间
            val start = duration.time.times(1000).toLocalDateTime()
            // 通过持续时间计算出结束时间
            val end = start.plus(duration.duration.times(1000).toLong(), ChronoUnit.MILLIS)
            // 生成StatDurationNode
            StatDurationNode.from(start, end).stream()
        }.collect(
            // 按时间段period分组后对编码时间duration计总和
            Collectors.groupingBy(
                StatDurationNode::period,
                Collectors.summingLong(StatDurationNode::duration)
            )
        ).asSequence()
            .map { (period, duration) -> StatDurationNode(period, duration) }
            .sortedBy { duration -> duration.period }
            .toList()

        val totalSeconds = summary.grandTotal?.totalSeconds ?: 0.0

        if (durations.isEmpty()) {
            return null
        }

        return StatData(
            Grading.DAILY,
            Range(summary.range.date, summary.range.date),
            StatSummary(
                summary.categories.toStatSummaryNodeList(),
                summary.editors.toStatSummaryNodeList(),
                summary.languages.toStatSummaryNodeList(),
                summary.operatingSystems.toStatSummaryNodeList(),
                summary.projects.toStatSummaryNodeList()
            ),
            durations,
            listOf(
                StatDurationNode(
                    summary.range.date,
                    totalSeconds.times(1000).toLong()
                )
            ),
            Stat(
                MostHardDay(summary.range.date, totalSeconds.times(1000).toLong()),
                durations.latest(summary.range.date),
                durations.earliest(summary.range.date),
                durations.favoritePeriod(),
                FavoritePeriod(
                    durations.firstOrNull()?.period ?: "LOST",
                    durations.lastOrNull()?.period ?: "LOST",
                    totalSeconds.times(1000).toLong()
                ),
                totalSeconds.times(1000).toLong()
            ),
            OssAccessorVerticle.format(
                LocalDate.parse(summary.range.date, DateTimeFormatter.ISO_DATE).atStartOfDay(DEF_ZONEID),
                Grading.DAILY.ossFileType, "html"
            )
        )
    }
}


