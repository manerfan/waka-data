package com.manerfan.waka.data.utils

import com.aliyun.oss.OSSClientBuilder
import com.aliyun.oss.model.PutObjectRequest
import io.vertx.core.json.JsonObject
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


/**
 * OssAccessor
 *
 * @author Maner.Fan
 * @date 2021/3/19
 */
data class OssAccessor(
    private val endpoint: String,
    private val accessKeyId: String,
    private val accessKeySecret: String,
    private val bucketName: String
) {
    private val dtf = DateTimeFormatter.ofPattern("'meta'/yyyy/MM/yyyy.MM.dd.'json'", Locale.SIMPLIFIED_CHINESE)
    private val ossClient = OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret)

    fun putMeta(date: LocalDate, data: JsonObject) {
        ossClient.putObject(
            PutObjectRequest(
                bucketName,
                date.format(dtf),
                ByteArrayInputStream(data.encodePrettily().encodeToByteArray())
            )
        )
    }

    fun close() {
        ossClient.shutdown()
    }
}
