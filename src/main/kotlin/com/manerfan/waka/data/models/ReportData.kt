package com.manerfan.waka.data.models

import java.time.ZonedDateTime

/**
 * ReportData
 *
 * <pre>
 *      报告
 * </pre>
 *
 * @author  maner.fan
 * @date 2021/10/3
 */
data class ReportData(
    /**
     * 报告内容
     */
    val content: String,

    /**
     * 统计粒度
     */
    val grading: Grading,

    /**
     * 统计时间
     */
    val statAt: ZonedDateTime
)