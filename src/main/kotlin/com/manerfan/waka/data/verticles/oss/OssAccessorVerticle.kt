package com.manerfan.waka.data.verticles.oss

import com.aliyun.oss.OSS
import com.aliyun.oss.OSSClientBuilder
import com.aliyun.oss.model.PutObjectRequest
import com.manerfan.waka.data.*
import com.manerfan.waka.data.models.ObjectCodec
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.shareddata.Shareable
import java.io.ByteArrayInputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


/**
 * OssAccessor
 *
 * @author Maner.Fan
 * @date 2021/3/19
 */
class OssAccessorVerticle : AbstractVerticle() {
    private lateinit var ossClient: OSS
    private lateinit var bucketName: String

    companion object {
        const val OSS_PUT = "oss.put"
        val dtfMap = mapOf(
            OssFileType.META to DateTimeFormatter.ofPattern(
                "'meta'/yyyy/MM/yyyy.MM.dd.'json'",
                Locale.SIMPLIFIED_CHINESE
            ),
            OssFileType.STAT_DAILY to DateTimeFormatter.ofPattern(
                "'stat/daily'/yyyy/MM/yyyy.MM.dd.'json'",
                Locale.SIMPLIFIED_CHINESE
            ),
            OssFileType.STAT_WEEK to DateTimeFormatter.ofPattern(
                "'stat/month'/yyyy/MM/yyyy.MM.'W'F.'json'",
                Locale.SIMPLIFIED_CHINESE
            ),
            OssFileType.STAT_MONTH to DateTimeFormatter.ofPattern(
                "'stat/month'/yyyy/MM/yyyy.MM.'json'",
                Locale.SIMPLIFIED_CHINESE
            ),
            OssFileType.STAT_QUARTER to DateTimeFormatter.ofPattern(
                "'stat/year'/yyyy/yyyy.'Q'Q.'json'",
                Locale.SIMPLIFIED_CHINESE
            ),
            OssFileType.STAT_HALF_YEAR to DateTimeFormatter.ofPattern(
                "'stat/year'/yyyy/yyyy.MM.'json'",
                Locale.SIMPLIFIED_CHINESE
            ),
            OssFileType.STAT_YEAR to DateTimeFormatter.ofPattern(
                "'stat/year'/yyyy/yyyy.'json'",
                Locale.SIMPLIFIED_CHINESE
            ),
        )
    }

    override fun start(startFuture: Promise<Void>) {
        //// oss

        vertx.sharedData().getLocalMap<String, String>(OSS_CONFIG_KEY)?.let { ossConfig ->
            bucketName = ossConfig[OSS_BUCKET_NAME]!!
            ossClient = OSSClientBuilder().build(
                ossConfig[OSS_ENDPOINT],
                ossConfig[OSS_ACCESS_KEY_ID],
                ossConfig[OSS_ACCESS_KEY_SECRET]
            ).also {
                vertx.sharedData()
                    .getLocalMap<String, ShareableOss>(OSS_CLIENT)
                    .put(OSS_CLIENT, ShareableOss(it))
            }
        } ?: return startFuture.fail(
            """
                |oss config must be set in [Launch Arguments] or [Environment Variables]
                |you can obtain waka api key on https://wakatime.com/settings/account
                """.trimMargin()
        )

        //// oss meta put event consumer

        vertx.eventBus().registerDefaultCodec(OssFilePut::class.java, ObjectCodec(OssFilePut::class.java))
        vertx.eventBus().consumer<OssFilePut>(OSS_PUT).handler { message ->
            val content = message.body()
            put(content)
            message.reply("${content.type}:${content.date}")
        }

        super.start(startFuture)
    }

    override fun stop(stopFuture: Promise<Void>?) {
        ossClient.shutdown()
        super.stop(stopFuture)
    }

    fun put(putMeta: OssFilePut) = with(putMeta) {
        ossClient.putObject(
            PutObjectRequest(
                bucketName,
                date.format(dtfMap[type]),
                ByteArrayInputStream(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data))
            )
        )
    }
}

data class OssFilePut(
    val type: OssFileType,
    val date: ZonedDateTime,
    val data: Any
)

class ShareableOss(val oss: OSS) : Shareable

enum class OssFileType {
    /**
     * 元数据
     */
    META,

    /**
     * 日维度统计数据
     */
    STAT_DAILY,

    /**
     * 周维度统计数据
     */
    STAT_WEEK,

    /**
     * 月维度统计数据
     */
    STAT_MONTH,

    /**
     * 季度维度统计
     */
    STAT_QUARTER,

    /**
     * 半年维度统计
     */
    STAT_HALF_YEAR,

    /**
     * 年维度统计
     */
    STAT_YEAR

}
