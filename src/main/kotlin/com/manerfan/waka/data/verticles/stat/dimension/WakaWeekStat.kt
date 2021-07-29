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
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * 周维度统计
 *
 * @author yongyong.fan
 * @date 2021/7/28
 */
class WakaWeekStat(private val ossObject: OssObject) : WakaStat {
    override fun stat(date: ZonedDateTime): StatData? {
        if (!date.isWeekStat()) {
            return null
        }

        logger.info("==> Waka Data Statistics [${Grading.WEEK}]")

        val (bucketName, ossClient) = ossObject
        val dtf = OssAccessorVerticle.dtfMap[OssFileType.STAT_DAILY]!!

        val start = date.with(saturdayOfLastWeek())
        val end = start.plusDays(ChronoField.DAY_OF_WEEK.range().maximum - 1)
        return Stream.iterate(start) { d -> d.plusDays(1) }.limit(ChronoField.DAY_OF_WEEK.range().maximum)
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
            .merge(Grading.WEEK, Range.from(start, end))
    }
}


