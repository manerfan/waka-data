package com.manerfan.waka.data.verticles.report

import com.manerfan.waka.data.DEF_ZONEID
import com.manerfan.waka.data.chain
import com.manerfan.waka.data.logger
import com.manerfan.waka.data.mapper
import com.manerfan.waka.data.models.ReportData
import com.manerfan.waka.data.models.StatData
import com.manerfan.waka.data.verticles.oss.OssAccessorVerticle
import com.manerfan.waka.data.verticles.oss.OssFilePut
import com.manerfan.waka.data.verticles.oss.OssFileType
import com.manerfan.waka.data.verticles.report.dimension.*
import com.manerfan.waka.data.verticles.stat.OssObject
import io.reactivex.Single
import io.vertx.core.Promise
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.core.eventbus.Message
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * 报告
 *
 * @author maner.fan
 * @date 2021/7/25
 */
class WakaReportVerticle : AbstractVerticle() {
    companion object {
        const val WAKA_REPORT = "waka.report"
    }

    override fun stop(stopFuture: Promise<Void>) {
        super.stop(stopFuture)
    }

    override fun start(stopFuture: Promise<Void>) {
        vertx.eventBus().consumer<ZonedDateTime>(WAKA_REPORT).handler { message ->
            val date = message.body()

//            patchDailyReport(
//                message,
//                LocalDate.of(2021, 3, 1).atStartOfDay(DEF_ZONEID),
//                LocalDate.of(2021, 10, 5).atStartOfDay(DEF_ZONEID)
//            )

            doReport(date).let {
                if (it.isEmpty()) {
                    message?.reply("DONE")
                } else {
                    it.chain().doFinally { message?.reply("DONE") }.subscribe()
                }
            }
        }

        super.start(stopFuture)
    }

    private fun doReport(date: ZonedDateTime): List<Single<Message<String>>> {
        val ossObject = OssObject.from(vertx)
        val (bucketName, ossClient) = ossObject

        val statAt = date.minusDays(1)
        val allStatFileKey = OssAccessorVerticle.format(statAt, OssFileType.STAT_ALL_CONTRIBUTIONS)
        val allStatContent = if (ossClient.oss.doesObjectExist(bucketName, allStatFileKey)) {
            ossClient.oss.getObject(bucketName, allStatFileKey).let { oss ->
                mapper.readValue(oss.objectContent, StatData::class.java)
            }
        } else null

        return Stream.of(
            WakaDailyReport(ossObject, allStatContent).report(date),
            WakaWeekReport(ossObject, allStatContent).report(date),
            WakaMonthReport(ossObject, allStatContent).report(date),
            WakaQuarterReport(ossObject, allStatContent).report(date),
            WakaHalfYearReport(ossObject, allStatContent).report(date),
            WakaYearReport(ossObject, allStatContent).report(date),
        ).filter(Objects::nonNull)
            .map { content -> content!!.handle() }
            .collect(Collectors.toList())
    }

    private fun ReportData.handle(
        oss: Boolean = true,
        file: Boolean = true,
        handle: (() -> Unit)? = null
    ): Single<Message<String>> {
        return listOf(
            if (oss) {
                // 保存到OSS
                vertx.eventBus().rxRequest<String>(
                    OssAccessorVerticle.OSS_PUT,
                    OssFilePut(
                        this.grading.ossFileType,
                        this.statAt,
                        this.content,
                        "html"
                    ),
                    DeliveryOptions().apply { codecName = OssFilePut::class.java.simpleName }
                )
            } else Single.just(Message(null)),
            if (file) {
                // 保存到系统
                val filePath = OssAccessorVerticle.format(this.statAt, this.grading.ossFileType, "html")
                val target = Path.of(System.getProperty("user.dir"), "docs", filePath).toFile()
                target.parentFile.mkdirs()
                target.writeText(this.content)
                Single.just(Message(null))
            } else Single.just(Message(null)),
        ).chain().doFinally {
            logger.info("<== Waka Data Report [${this.grading}]")
            handle?.invoke()
        }
    }

    private fun patchDailyReport(
        message: Message<ZonedDateTime>, start: ZonedDateTime, end: ZonedDateTime
    ) {
        val limit = start.until(end, ChronoUnit.DAYS) + 1
        Stream.iterate(start) { it.plusDays(1) }.limit(limit)
            .flatMap { doReport(it).stream() }
            .filter(Objects::nonNull)
            .collect(Collectors.toList()).let {
                if (it.isEmpty()) {
                    message.reply("DONE")
                } else {
                    it.chain().doFinally { message.reply("DONE") }.subscribe()
                }
            }
    }
}
