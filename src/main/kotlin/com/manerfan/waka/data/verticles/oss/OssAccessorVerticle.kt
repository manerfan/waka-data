package com.manerfan.waka.data.verticles.oss

import com.aliyun.oss.OSS
import com.aliyun.oss.OSSClientBuilder
import com.aliyun.oss.model.PutObjectRequest
import com.manerfan.waka.data.*
import com.manerfan.waka.data.models.ObjectCodec
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
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
        private val dtfMap = mapOf(
            OssFileType.META to DateTimeFormatter.ofPattern("'meta'/yyyy/MM/yyyy.MM.dd.'json'", Locale.SIMPLIFIED_CHINESE)
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
            )
        } ?: return startFuture.fail(
            """
                |waka api key must be set in [Launch Arguments] or [Environment Variables]
                |you can obtain waka api key on https://wakatime.com/settings/account
                """.trimMargin()
        )

        //// oss meta put event consumer

        vertx.eventBus().registerDefaultCodec(OssFilePut::class.java, ObjectCodec(OssFilePut::class.java))
        vertx.eventBus().consumer<OssFilePut>(OSS_PUT).handler { message ->
            val content = message.body()
            //put(content)
            message.reply("DONE")
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
                ByteArrayInputStream(data.encodePrettily().encodeToByteArray())
            )
        )
    }
}

data class OssFilePut(
    val type: OssFileType,
    val date: ZonedDateTime,
    val data: JsonObject
)

enum class OssFileType {
    /**
     * 元数据
     */
    META,
}
