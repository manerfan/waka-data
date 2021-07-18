package com.manerfan.waka.data.models

/**
 * waka time
 *
 * @author Maner.Fan
 * @date 2021/3/16
 */

data class WakaData(
    val summaries: List<WakaSummary>?,
    val durations: List<WakaDuration>?
)

data class WakaSummary(
    val categories: List<WakaSummaryNode>?,
    val editors: List<WakaSummaryNode>?,
    val grandTotal: WakaSummaryNode?,
    val languages: List<WakaSummaryNode>?,
    val operatingSystems: List<WakaSummaryNode>?,
    val range: Range
)

data class WakaSummaryNode(
    val name: String?,
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0,
    val totalSeconds: Double = 0.0
)

data class WakaDuration(
    val time: Long,
    val duration: Double = 0.0
)
