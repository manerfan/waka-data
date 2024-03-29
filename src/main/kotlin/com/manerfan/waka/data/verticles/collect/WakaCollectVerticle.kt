package com.manerfan.waka.data.verticles.collect

import com.manerfan.waka.data.*
import com.manerfan.waka.data.models.WakaData
import com.manerfan.waka.data.verticles.oss.OssAccessorVerticle
import com.manerfan.waka.data.verticles.oss.OssFilePut
import com.manerfan.waka.data.verticles.oss.OssFileType
import com.manerfan.waka.data.verticles.report.WakaReportVerticle
import com.manerfan.waka.data.verticles.stat.WakaStatVerticle
import io.reactivex.Flowable
import io.reactivex.Single
import io.vertx.core.Promise
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.ext.web.client.webClientOptionsOf
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.web.client.HttpRequest
import io.vertx.reactivex.ext.web.client.HttpResponse
import io.vertx.reactivex.ext.web.client.WebClient
import io.vertx.reactivex.ext.web.codec.BodyCodec
import java.nio.file.Paths
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * WakaCollectVerticle
 *
 * @author Maner.Fan
 * @date 2021/3/16
 */
class WakaCollectVerticle : AbstractVerticle() {
    private lateinit var apiKey: String
    private lateinit var webClient: WebClient

    companion object {
        const val WAKA_COLLECT = "waka.collect"
        private val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.SIMPLIFIED_CHINESE)
        private const val uriVersion = "/api/v1"
    }

    override fun start(startFuture: Promise<Void>) {

        //// waka time api key

        apiKey = vertx.sharedData().getLocalMap<String, String>(WAKA_CONFIG_KEY)[WAKA_API_KEY]
            ?: return startFuture.fail(
                """
                |waka api key must be set in [Launch Arguments] or [Environment Variables]
                |you can obtain waka api key on https://wakatime.com/settings/account
                """.trimMargin()
            )

        //// web client

        webClient = WebClient.create(
            vertx, webClientOptionsOf(
                defaultHost = "wakatime.com",
                defaultPort = 443,
                ssl = true,
                trustAll = true
            )
        )

        //// waka time data collect event consumer

        vertx.eventBus().consumer<Long>(WAKA_COLLECT).handler { message ->
            logger.info("==> Waka Data Collect")

            val intervalDays = message.body()
            val start = ZonedDateTime.now(DEF_ZONEID).minusDays(intervalDays)
            val end = ZonedDateTime.now(DEF_ZONEID).minusDays(1)

            Flowable.fromArray(
                this::user,
                this::summaries,
                this::lastWeekStats,
                this::durations,
                this::allTimeSineToday
            ).parallel()
                .flatMap { func ->
                    func(start, end)
                        .doOnSubscribe { logger.info("--> Waka Data Collect: collect ${func.name}") }
                        .toFlowable()
                }
                .reduce(JsonObject::mergeIn)
                .retry(3)
                .subscribe {
                    listOf(
                        // 原始数据保存
                        vertx.eventBus()
                            .rxRequest<String>(OssAccessorVerticle.OSS_PUT, OssFilePut(OssFileType.META, end, it))
                            .doOnSubscribe { logger.info("--> Waka Data Collect: put to oss") },
                        // 日维度统计
                        vertx.eventBus().rxRequest<String>(
                            WakaStatVerticle.WAKA_STAT_DAILY,
                            mapper.readValue(it.encode(), WakaData::class.java),
                            DeliveryOptions().apply {
                                codecName = WakaData::class.java.simpleName
                            }
                        ),
                        // 其他维度统计
                        vertx.eventBus().rxRequest<String>(
                            WakaStatVerticle.WAKA_STAT,
                            LocalDate.now().atStartOfDay(DEF_ZONEID),
                            DeliveryOptions().apply {
                                codecName = ZonedDateTime::class.java.simpleName
                                sendTimeout = 5 * 60 * 1000
                            }
                        ),
                        // 生成报告
                        vertx.eventBus().rxRequest<String>(
                            WakaReportVerticle.WAKA_REPORT,
                            LocalDate.now().atStartOfDay(DEF_ZONEID),
                            DeliveryOptions().apply {
                                codecName = ZonedDateTime::class.java.simpleName
                                sendTimeout = 5 * 60 * 1000
                            }
                        )
                    ).chain().doFinally {
                        message.reply("DONE").also { logger.info("<== Waka Data Collect") }
                    }.subscribe()
                }
        }

        super.start(startFuture)
    }

    override fun stop(stopFuture: Promise<Void>) {
        super.stop(stopFuture)
    }

    /**
     * [user](https://wakatime.com/developers#users)
     */
    @Suppress("UNUSED_PARAMETER")
    private fun user(start: ZonedDateTime, end: ZonedDateTime) =
        getInfos<JsonObject>(node = "user")

    /**
     * [summaries](https://wakatime.com/developers#summaries)
     */
    private fun summaries(start: ZonedDateTime, end: ZonedDateTime) =
        getInfos<JsonArray>(path = "summaries", withPeriod = true, start = start, end = end)

    /**
     * [stats](https://wakatime.com/developers#stats)
     */
    @Suppress("UNUSED_PARAMETER")
    private fun lastWeekStats(start: ZonedDateTime, end: ZonedDateTime) =
        getInfos<JsonObject>(path = "stats/last_7_days", node = "lastWeekStats")

    /**
     * [durations](https://wakatime.com/developers#durations)
     */
    @Suppress("UNUSED_PARAMETER")
    private fun durations(start: ZonedDateTime, end: ZonedDateTime) =
        getInfos<JsonArray>(path = "durations", withDate = true, date = end)

    /**
     * [all_time_since_today](https://wakatime.com/developers#all_time_since_today)
     */
    @Suppress("UNUSED_PARAMETER")
    private fun allTimeSineToday(start: ZonedDateTime, end: ZonedDateTime) =
        getInfos<JsonArray>(path = "all_time_since_today", node = "allTimeSineToday")

    /**
     * unified interface for information
     * TODO 多页的情况需要考虑
     */
    private fun <T> getInfos(
        path: String = "", node: String = path,
        withDate: Boolean = false,
        date: ZonedDateTime = ZonedDateTime.now(DEF_ZONEID).minusDays(1),
        withPeriod: Boolean = false,
        start: ZonedDateTime = ZonedDateTime.now(DEF_ZONEID).minusDays(1),
        end: ZonedDateTime = ZonedDateTime.now(DEF_ZONEID).minusDays(1)
    ): Single<JsonObject> {
        val request = webClient.get(Paths.get(uriVersion, "/users/current/", path).toUri().path).addNecessaryParams()

        if (withDate) {
            request.addDateParams(date)
        }
        if (withPeriod) {
            request.addPeriodParams(start, end)
        }

        return request.`as`(BodyCodec.jsonObject())
            .rxSend()
            .map(HttpResponse<JsonObject>::body)
            .map { body ->
                JsonObject().put(node, body.get<T>("data") ?: throw IllegalStateException("Data is null on $path!"))
            }
    }

    /**
     * add timezone & api_key & ... more necessary parameters
     */
    private fun <T> HttpRequest<T>.addNecessaryParams() =
        this.addQueryParam("timezone", "Asia/Shanghai")
            .addQueryParam("api_key", apiKey)

    /**
     * add start & end parameters
     */
    private fun <T> HttpRequest<T>.addPeriodParams(start: ZonedDateTime, end: ZonedDateTime) =
        this.addQueryParam("start", start.format(dtf))
            .addQueryParam("end", end.format(dtf))

    /**
     * add date parameters
     */
    private fun <T> HttpRequest<T>.addDateParams(date: ZonedDateTime) =
        this.addQueryParam("date", date.format(dtf))
}
