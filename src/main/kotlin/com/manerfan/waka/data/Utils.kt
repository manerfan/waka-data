package com.manerfan.waka.data

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.reactivex.Single
import java.time.Instant

/**
 * Utils
 *
 * @author yongyong.fan
 * @date 2021/7/18
 */

fun <R : Any> List<Single<R>>.chain() = this.reduce { acc, single -> acc.flatMap { single } }

val mapper = jacksonObjectMapper().apply {
    // propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}


fun Long.toLocalDateTime() =
    Instant.ofEpochMilli(this).atZone(DEF_ZONEID).toLocalDateTime()

fun Long.toLocalDate() =
    Instant.ofEpochMilli(this).atZone(DEF_ZONEID).toLocalDate()
