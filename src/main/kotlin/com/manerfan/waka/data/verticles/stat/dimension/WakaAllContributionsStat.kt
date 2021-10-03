package com.manerfan.waka.data.verticles.stat.dimension

import com.manerfan.waka.data.logger
import com.manerfan.waka.data.mapper
import com.manerfan.waka.data.models.Grading
import com.manerfan.waka.data.models.Range
import com.manerfan.waka.data.models.StatData
import com.manerfan.waka.data.verticles.oss.OssAccessorVerticle
import com.manerfan.waka.data.verticles.oss.OssFileType
import com.manerfan.waka.data.verticles.stat.OssObject
import com.manerfan.waka.data.verticles.stat.WakaStat
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * 全年统计
 *
 * @author maner.fan
 * @date 2021/9/21
 */
class WakaAllContributionsStat(private val ossObject: OssObject) : WakaStat {
    override fun stat(date: ZonedDateTime): StatData? {
        logger.info("==> Waka Data Statistics [${Grading.ALL}]")

        val (bucketName, ossClient) = ossObject
        val statAt = date.minusDays(1)

        val allStatFileKey = OssAccessorVerticle.format(statAt, OssFileType.STAT_ALL_CONTRIBUTIONS)
        val allStatContent = if (ossClient.oss.doesObjectExist(bucketName, allStatFileKey)) {
            ossClient.oss.getObject(bucketName, allStatFileKey).let { oss ->
                mapper.readValue(oss.objectContent, StatData::class.java)
            }
        } else null

        return stat(allStatContent, date)
    }

    fun stat(allStatContent: StatData?, date: ZonedDateTime): StatData? {
        val (bucketName, ossClient) = ossObject
        val statAt = date.minusDays(1)
        val firstDayOfYear = statAt.with(TemporalAdjusters.firstDayOfYear())

        val range = Range.from(firstDayOfYear, statAt)

        val statAtFileKey = OssAccessorVerticle.format(statAt, OssFileType.STAT_DAILY)
        val statAtContent = if (ossClient.oss.doesObjectExist(bucketName, statAtFileKey)) {
            ossClient.oss.getObject(bucketName, statAtFileKey).let { oss ->
                mapper.readValue(oss.objectContent, StatData::class.java)
            }
        } else null

        // 全是 null
        if (Objects.isNull(allStatContent) && Objects.isNull(statAtContent)) {
            return null
        }

        // 有一个是 null
        if (Objects.isNull(allStatContent) || Objects.isNull(statAtContent)) {
            return Stream.of<StatData>(allStatContent, statAtContent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                .merge(Grading.ALL, Range.from(firstDayOfYear, statAt))
        }

        // 全不是 null

        val alreadyStored = allStatContent?.contributions?.stream()?.anyMatch { contribution ->
            contribution.period.equals(range.end, false)
        } ?: false

        return if (alreadyStored) {
            Stream.of<StatData>(allStatContent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                .merge(Grading.ALL, Range.from(firstDayOfYear, statAt))
        } else {
            Stream.of<StatData>(allStatContent, statAtContent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                .merge(Grading.ALL, Range.from(firstDayOfYear, statAt))
        }
    }
}


