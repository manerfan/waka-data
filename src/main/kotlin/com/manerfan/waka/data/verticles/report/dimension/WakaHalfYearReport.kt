package com.manerfan.waka.data.verticles.report.dimension

import com.manerfan.waka.data.logger
import com.manerfan.waka.data.mapper
import com.manerfan.waka.data.models.Grading
import com.manerfan.waka.data.models.ReportData
import com.manerfan.waka.data.models.StatData
import com.manerfan.waka.data.verticles.oss.OssAccessorVerticle
import com.manerfan.waka.data.verticles.oss.OssFileType
import com.manerfan.waka.data.verticles.report.WakaReport
import com.manerfan.waka.data.verticles.stat.OssObject
import com.manerfan.waka.data.verticles.stat.isHalfYearStat
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * WakaDailyReport
 *
 * <pre>
 *     半年报告
 * </pre>
 *
 * @author maner.fan
 * @date 2021/10/3
 */
class WakaHalfYearReport(private val ossObject: OssObject, private val allStatContent: StatData?) : WakaReport {
    override fun report(date: ZonedDateTime): ReportData? {
        if (!date.isHalfYearStat()) {
            return null
        }

        logger.info("==> Waka Data Report [${Grading.HALF_YEAR}]")

        val (bucketName, ossClient) = ossObject

        val statAt = date.with(TemporalAdjusters.firstDayOfYear())
        val statAtFileKey = OssAccessorVerticle.format(statAt, OssFileType.STAT_HALF_YEAR)
        val statAtContent = if (ossClient.oss.doesObjectExist(bucketName, statAtFileKey)) {
            ossClient.oss.getObject(bucketName, statAtFileKey).let { oss ->
                mapper.readValue(oss.objectContent, StatData::class.java)
            }
        } else null

        return doReport(allStatContent, statAtContent, statAt, Grading.HALF_YEAR)
    }
}