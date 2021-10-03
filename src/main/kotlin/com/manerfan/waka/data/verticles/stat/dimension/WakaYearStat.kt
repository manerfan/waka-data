package com.manerfan.waka.data.verticles.stat

import com.manerfan.waka.data.logger
import com.manerfan.waka.data.mapper
import com.manerfan.waka.data.models.Grading
import com.manerfan.waka.data.models.Range
import com.manerfan.waka.data.models.StatData
import com.manerfan.waka.data.verticles.oss.OssAccessorVerticle
import com.manerfan.waka.data.verticles.oss.OssFileType
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * 季度维度统计
 *
 * @author maner.fan
 * @date 2021/7/28
 */
class WakaYearStat(private val ossObject: OssObject) : WakaStat {
    override fun stat(date: ZonedDateTime): StatData? {
        if (!date.isYearStat()) {
            return null
        }

        logger.info("==> Waka Data Statistics [${Grading.YEAR}]")

        val (bucketName, ossClient) = ossObject

        val start = date.with(firstDayOfLastYear())
        val end = start.with(TemporalAdjusters.lastDayOfYear())
        return Stream.iterate(start) { d -> d.plusMonths(1) }.limit(ChronoField.MONTH_OF_YEAR.range().maximum)
            .filter { d -> !d.isAfter(end) }
            .map { d -> OssAccessorVerticle.format(d, OssFileType.STAT_MONTH) }
            .filter { fileKey -> ossClient.oss.doesObjectExist(bucketName, fileKey) }
            .parallel()
            .map { fileKey ->
                ossClient.oss.getObject(bucketName, fileKey).let { oss ->
                    mapper.readValue(oss.objectContent, StatData::class.java)
                }
            }
            .collect(Collectors.toList())
            .merge(Grading.YEAR, Range.from(start, end))
    }
}


