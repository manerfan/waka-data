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
import java.time.ZonedDateTime

/**
 * WakaDailyReport
 *
 * <pre>
 *     日报告
 * </pre>
 *
 * @author maner.fan
 * @date 2021/10/3
 */
class WakaDailyReport(private val ossObject: OssObject, private val allStatContent: StatData?) : WakaReport {
    override fun report(date: ZonedDateTime): ReportData? {
        val (bucketName, ossClient) = ossObject

        logger.info("==> Waka Data Report [${Grading.DAILY}]")

        val statAt = date.minusDays(1)
        val statAtFileKey = OssAccessorVerticle.format(statAt, OssFileType.STAT_DAILY)
        val statAtContent = if (ossClient.oss.doesObjectExist(bucketName, statAtFileKey)) {
            ossClient.oss.getObject(bucketName, statAtFileKey).let { oss ->
                mapper.readValue(oss.objectContent, StatData::class.java)
            }
        } else null

        return doReport(allStatContent, statAtContent, statAt, Grading.DAILY)
    }
}