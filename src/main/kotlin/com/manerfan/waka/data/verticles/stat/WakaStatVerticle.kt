package com.manerfan.waka.data.verticles.stat

import com.manerfan.waka.data.DEF_ZONEID
import com.manerfan.waka.data.chain
import com.manerfan.waka.data.logger
import com.manerfan.waka.data.models.Grading
import com.manerfan.waka.data.models.ObjectCodec
import com.manerfan.waka.data.models.StatData
import com.manerfan.waka.data.models.WakaData
import com.manerfan.waka.data.verticles.message.DingMessageVerticle
import com.manerfan.waka.data.verticles.oss.OssAccessorVerticle
import com.manerfan.waka.data.verticles.oss.OssFilePut
import com.manerfan.waka.data.verticles.stat.dimension.WakaDailyStat
import io.reactivex.Single
import io.vertx.core.Promise
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.core.eventbus.Message
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
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

    override fun stop(stopFuture: Promise<Void>) {
        super.stop(stopFuture)
    }

    override fun start(stopFuture: Promise<Void>) {
        vertx.eventBus().registerCodec(ObjectCodec(WakaData::class.java))
        vertx.eventBus().registerCodec(ObjectCodec(StatData::class.java))
        vertx.eventBus().registerCodec(ObjectCodec(ZonedDateTime::class.java))

        // 日统计
        vertx.eventBus().consumer<WakaData>(WAKA_STAT_DAILY).handler { message ->
            logger.info("==> Waka Data Statistics [${Grading.DAILY}]")

            val wakaData = message.body()
            val statData = WakaDailyStat(OssObject.from(vertx)).stat(wakaData)

            if (null == statData) {
                message.reply("DONE")
                return@handler
            }

            statData.handle() {
                message.reply("DONE")
            }.subscribe()
        }

        // 统计
        vertx.eventBus().consumer<ZonedDateTime>(WAKA_STAT).handler { message ->
            val ossObject = OssObject.from(vertx)
            val date = message.body()

            // 补数据用
//             patchDailyStat(
//                 ossObject,
//                 message,
//                 LocalDate.of(2021, 3, 1).atStartOfDay(DEF_ZONEID),
//                 LocalDate.of(2021, 8, 1).atStartOfDay(DEF_ZONEID)
//             )

//            patchWeekStat(
//                ossObject,
//                message,
//                LocalDate.of(2021, 3, 1).atStartOfDay(DEF_ZONEID),
//                LocalDate.of(2021, 8, 1).atStartOfDay(DEF_ZONEID)
//            )

//            patchMonthStat(
//                ossObject,
//                message,
//                LocalDate.of(2021, 3, 1).atStartOfDay(DEF_ZONEID),
//                LocalDate.of(2021, 8, 1).atStartOfDay(DEF_ZONEID)
//            )

//            patchQuarterStat(
//                ossObject,
//                message,
//                LocalDate.of(2021, 4, 1).atStartOfDay(DEF_ZONEID),
//                LocalDate.of(2021, 8, 1).atStartOfDay(DEF_ZONEID)
//            )

//            patchHalfYearStat(
//                ossObject,
//                message,
//                LocalDate.of(2021, 1, 1).atStartOfDay(DEF_ZONEID),
//                LocalDate.of(2021, 8, 1).atStartOfDay(DEF_ZONEID)
//            )

//            return@handler

            Stream.of(
                WakaWeekStat(ossObject).stat(date),
                WakaMonthStat(ossObject).stat(date),
                WakaQuarterStat(ossObject).stat(date),
                WakaHalfYearStat(ossObject).stat(date),
                WakaYearStat(ossObject).stat(date),
            ).filter(Objects::nonNull)
                .map { statData -> statData!!.handle() }
                .collect(Collectors.toList()).let {
                    if (it.isEmpty()) {
                        message.reply("DONE")
                    } else {
                        it.chain().doFinally { message.reply("DONE") }.subscribe()
                    }
                }
        }

        super.start(stopFuture)
    }

    private fun StatData.handle(
        oss: Boolean = true,
        message: Boolean = true,
        report: Boolean = true,
        handle: (() -> Unit)? = null
    ): Single<Message<String>> {
        return listOf(
            if (oss) {
                // 保存到OSS
                vertx.eventBus().rxRequest<String>(
                    OssAccessorVerticle.OSS_PUT,
                    OssFilePut(
                        this.grading.ossFileType,
                        LocalDate.parse(this.range.start, DateTimeFormatter.ISO_DATE).atStartOfDay(DEF_ZONEID),
                        this
                    ),
                    DeliveryOptions().apply { codecName = OssFilePut::class.java.simpleName }
                )
            } else Single.just(Message(null)),
            if (message) {
                // 消息推送
                vertx.eventBus().rxRequest<String>(
                    DingMessageVerticle.DING_MESSAGE,
                    this,
                    DeliveryOptions().apply { codecName = StatData::class.java.simpleName }
                )
            } else Single.just(Message(null)),
        ).chain().doFinally {
            logger.info("<== Waka Data Statistics [${this.grading}]")
            handle?.invoke()
        }
    }

    private fun patchDailyStat(
        ossObject: OssObject,
        message: Message<ZonedDateTime>, start: ZonedDateTime, end: ZonedDateTime
    ) {
        val dailyStat = WakaDailyStat(ossObject)
        val limit = start.until(end, ChronoUnit.DAYS) + 1
        Stream.iterate(start) { it.plusDays(1) }.limit(limit)
            .map { dailyStat.stat(it) }
            .filter(Objects::nonNull)
            .parallel()
            .map { statData -> statData!!.handle(message = false, report = false) }
            .collect(Collectors.toList()).chain().doFinally { message.reply("DONE") }.subscribe()
    }

    private fun patchWeekStat(
        ossObject: OssObject,
        message: Message<ZonedDateTime>, start: ZonedDateTime, end: ZonedDateTime
    ) {
        val dailyStat = WakaWeekStat(ossObject)
        val limit = start.with(saturdayOfLastWeek()).until(end, ChronoUnit.WEEKS) + 1
        Stream.iterate(start.with(saturdayOfLastWeek())) { it.plusWeeks(1) }.limit(limit)
            .map { dailyStat.stat(it) }
            .filter(Objects::nonNull)
            .parallel()
            .map { statData -> statData!!.handle(message = false, report = false) }
            .collect(Collectors.toList()).chain().doFinally { message.reply("DONE") }.subscribe()
    }

    private fun patchMonthStat(
        ossObject: OssObject,
        message: Message<ZonedDateTime>, start: ZonedDateTime, end: ZonedDateTime
    ) {
        val dailyStat = WakaMonthStat(ossObject)
        val limit = start.with(TemporalAdjusters.firstDayOfMonth()).until(end, ChronoUnit.MONTHS) + 1
        Stream.iterate(start.with(TemporalAdjusters.firstDayOfMonth())) { it.plusMonths(1) }.limit(limit)
            .map { dailyStat.stat(it) }
            .filter(Objects::nonNull)
            .parallel()
            .map { statData -> statData!!.handle(message = false, report = false) }
            .collect(Collectors.toList()).chain().doFinally { message.reply("DONE") }.subscribe()
    }

    private fun patchQuarterStat(
        ossObject: OssObject,
        message: Message<ZonedDateTime>, start: ZonedDateTime, end: ZonedDateTime
    ) {
        val dailyStat = WakaQuarterStat(ossObject)
        val limit = start.with(TemporalAdjusters.firstDayOfMonth()).until(end, ChronoUnit.MONTHS) / 3 + 1
        Stream.iterate(start.with(TemporalAdjusters.firstDayOfMonth())) { it.plusMonths(3) }.limit(limit)
            .map { dailyStat.stat(it) }
            .filter(Objects::nonNull)
            .parallel()
            .map { statData -> statData!!.handle(message = false, report = false) }
            .collect(Collectors.toList()).chain().doFinally { message.reply("DONE") }.subscribe()
    }

    private fun patchHalfYearStat(
        ossObject: OssObject,
        message: Message<ZonedDateTime>, start: ZonedDateTime, end: ZonedDateTime
    ) {
        val dailyStat = WakaHalfYearStat(ossObject)
        val limit = start.with(TemporalAdjusters.firstDayOfYear()).until(end, ChronoUnit.MONTHS) / 6 + 1
        Stream.iterate(start.with(TemporalAdjusters.firstDayOfMonth())) { it.plusMonths(6) }.limit(limit)
            .map { dailyStat.stat(it) }
            .filter(Objects::nonNull)
            .parallel()
            .map { statData -> statData!!.handle(message = false, report = false) }
            .collect(Collectors.toList()).chain().doFinally { message.reply("DONE") }.subscribe()
    }
}
