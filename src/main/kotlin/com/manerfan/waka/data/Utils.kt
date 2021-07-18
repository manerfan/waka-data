package com.manerfan.waka.data

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import java.time.ZoneOffset

/**
 * Utils
 *
 * @author yongyong.fan
 * @date 2021/7/18
 */


val mapper = jacksonObjectMapper().apply {
    propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}


fun Long.toLocalDateTime() =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.ofHours(8)).toLocalDateTime()

fun Long.toLocalDate() =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.ofHours(8)).toLocalDate()
