package com.manerfan.waka.data.models

import com.fasterxml.jackson.annotation.JsonProperty

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
    @JsonProperty("grand_total")
    val grandTotal: WakaSummaryNode?,
    val languages: List<WakaSummaryNode>?,
    @JsonProperty("operating_systems")
    val operatingSystems: List<WakaSummaryNode>?,
    val projects: List<WakaSummaryNode>?,
    val range: WakaRange
)

data class WakaSummaryNode(
    val name: String?,
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0,
    @JsonProperty("total_seconds")
    val totalSeconds: Double = 0.0
)

data class WakaDuration(
    val project: String?,
    val time: Long,
    val duration: Double = 0.0
)

data class WakaRange(
    val date: String,
    val start: String,
    val end: String
)
