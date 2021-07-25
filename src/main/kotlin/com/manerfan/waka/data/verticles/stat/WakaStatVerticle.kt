package com.manerfan.waka.data.verticles.stat

import com.manerfan.waka.data.*
import com.manerfan.waka.data.models.*
import com.manerfan.waka.data.verticles.oss.OssAccessorVerticle
import com.manerfan.waka.data.verticles.oss.OssFilePut
import com.manerfan.waka.data.verticles.oss.OssFileType
import com.manerfan.waka.data.verticles.oss.ShareableOss
import io.vertx.core.Promise
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.core.eventbus.Message
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * WakaStatVerticle
 *
 * @author yongyong.fan
 * @date 2021/7/18
 */
class WakaStatVerticle : AbstractVerticle() {

    companion object {
        const val WAKA_STAT_DAILY = "waka.stat.daily"
        const val WAKA_STAT = "waka.stat"
    }

    override fun start(stopFuture: Promise<Void>) {
        logger.info("==> Waka Data Statistics [Daily]")

        vertx.eventBus().registerCodec(ObjectCodec(WakaData::class.java))
        vertx.eventBus().registerCodec(ObjectCodec(StatData::class.java))
        vertx.eventBus().registerCodec(ObjectCodec(ZonedDateTime::class.java))

        // 日统计
        vertx.eventBus().consumer<WakaData>(WAKA_STAT_DAILY).handler { message ->
            val wakaData = message.body()
            val statData = wakaDataDailyStat(wakaData)

            if (null == statData) {
                message.reply("DONE")
                return@handler
            }

            // 日统计数据保存
            vertx.eventBus().request<String>(
                OssAccessorVerticle.OSS_PUT,
                OssFilePut(
                    OssFileType.STAT_DAILY,
                    LocalDate.parse(statData.range.start, DateTimeFormatter.ISO_DATE).atStartOfDay(DEF_ZONEID),
                    statData
                )
            ) {
                logger.info("--> Waka Data Daily Stat: put to oss")
                message.reply("DONE")
            }
        }

        // 统计
        vertx.eventBus().consumer<ZonedDateTime>(WAKA_STAT).handler { message ->
            val date = message.body()

            // 补数据用 - 日维度
            patchDailyStat(
                message,
                LocalDate.of(2021, 3, 1).atStartOfDay(DEF_ZONEID),
                LocalDate.of(2021, 7, 25).atStartOfDay(DEF_ZONEID)
            )
        }

        super.start(stopFuture)
    }

    private fun patchDailyStat(message: Message<ZonedDateTime>, start: ZonedDateTime, end: ZonedDateTime) {
        val limit = start.until(end, ChronoUnit.DAYS) + 1
        Stream.iterate(start) { it.plusDays(1) }.limit(limit)
            .map { dailyStat(it) }
            .filter(Objects::nonNull)
            .parallel()
            .map { statData ->
                vertx.eventBus().rxRequest<String>(
                    OssAccessorVerticle.OSS_PUT,
                    OssFilePut(
                        OssFileType.STAT_DAILY,
                        LocalDate.parse(statData!!.range.start, DateTimeFormatter.ISO_DATE).atStartOfDay(DEF_ZONEID),
                        statData
                    )
                ).doOnSubscribe { _ -> logger.info("--> Waka Data Daily Stat: ${statData.range.start}") }
            }
            .collect(Collectors.toList()).chain().doFinally { message.reply("DONE") }.subscribe()
    }

    private fun dailyStat(date: ZonedDateTime): StatData? {
        val bucketName = vertx.sharedData().getLocalMap<String, String>(OSS_CONFIG_KEY)[OSS_BUCKET_NAME]!!
        val ossClient = vertx.sharedData().getLocalMap<String, ShareableOss>(OSS_CLIENT)[OSS_CLIENT]!!

        val fileKey = OssAccessorVerticle.dtfMap[OssFileType.META]!!.format(date)
        if (!ossClient.oss.doesObjectExist(bucketName, fileKey)) {
            return null
        }

        val wakaData = ossClient.oss.getObject(bucketName, fileKey).let {
            mapper.readValue(it.objectContent, WakaData::class.java)
        }

        return wakaDataDailyStat(wakaData)
    }

    /**
     * waka data 日维度统计
     */
    private fun wakaDataDailyStat(wakaData: WakaData): StatData? {
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
                MostHardDay(summary.range.date, totalSeconds),
                durations.latest(summary.range.date),
                durations.earliest(summary.range.date),
                durations.favoritePeriod(),
                totalSeconds
            )
        )
    }

    override fun stop(stopFuture: Promise<Void>) {
        super.stop(stopFuture)
    }

    private fun List<WakaSummaryNode>?.toStatSummaryNodeList() = this.orEmpty().asSequence().map { wakaSummaryNode ->
        StatSummaryNode(wakaSummaryNode.name ?: "unknown", wakaSummaryNode.totalSeconds)
    }.toList()

    /**
     * 一天中找到最忙的时间段
     */
    private fun List<StatDurationNode>?.favoritePeriod(): FavoritePeriod? = this?.let { durations ->
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
    private fun List<StatDurationNode>?.totalBetween(start: String, end: String): FavoritePeriod = this?.let {
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
    private fun List<StatDurationNode>?.latest(date: String): MostLateDay? = this?.let {
        val durations = it.asSequence().sortedBy { duration -> duration.period }
        (durations.filter { duration -> duration.period < "04:00" }.firstOrNull() ?: durations.lastOrNull())
            ?.let { duration ->
                MostLateDay(date, duration.period)
            }
    }

    /**
     * 一天中找到最早的时间段
     */
    private fun List<StatDurationNode>?.earliest(date: String): MostEarlyDay? = this?.let {
        it.asSequence()
            .sortedBy { duration -> duration.period }
            .filter { duration -> duration.period >= "04:00" }
            .firstOrNull()
            ?.let { duration ->
                MostEarlyDay(date, duration.period)
            }
    }
}
