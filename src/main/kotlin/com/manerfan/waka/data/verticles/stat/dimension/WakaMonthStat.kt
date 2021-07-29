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
 * 月维度统计
 *
 * @author yongyong.fan
 * @date 2021/7/28
 */
class WakaMonthStat(private val ossObject: OssObject) : WakaStat {
    override fun stat(date: ZonedDateTime): StatData? {
        if (!date.isMonthStat()) {
            return null
        }

        logger.info("==> Waka Data Statistics [${Grading.MONTH}]")

        val (bucketName, ossClient) = ossObject
        val dtf = OssAccessorVerticle.dtfMap[OssFileType.STAT_DAILY]!!

        val start = date.with(firstDayOfLastMonth())
        val end = start.with(TemporalAdjusters.lastDayOfMonth())
        return Stream.iterate(start) { d -> d.plusDays(1) }.limit(ChronoField.DAY_OF_MONTH.range().maximum)
            .filter { d -> !d.isAfter(end) }
            .map { d -> dtf.format(d) }
            .filter { fileKey -> ossClient.oss.doesObjectExist(bucketName, fileKey) }
            .parallel()
            .map { fileKey ->
                ossClient.oss.getObject(bucketName, fileKey).let { oss ->
                    mapper.readValue(oss.objectContent, StatData::class.java)
                }
            }
            .collect(Collectors.toList())
            .merge(Grading.MONTH, Range.from(start, end))
    }
}


