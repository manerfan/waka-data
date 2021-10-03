package com.manerfan.waka.data.verticles.report

import com.google.common.hash.Hashing
import com.manerfan.waka.data.mapper
import com.manerfan.waka.data.models.*
import com.manerfan.waka.data.toHoursPart
import com.manerfan.waka.data.toMinutesPart
import freemarker.cache.StringTemplateLoader
import freemarker.template.Configuration
import freemarker.template.Template
import java.io.StringWriter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * WakaReport
 *
 * <pre>
 *     报告
 * </pre>
 *
 * @author maner.fan
 * @date 2021/10/3
 */
interface WakaReport {
    companion object {
        private val dtf = DateTimeFormatter.ofPattern(
            "yyyy-MM-yyyy", Locale.SIMPLIFIED_CHINESE
        )

        private val cfg = Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).apply {
            templateLoader = StringTemplateLoader()
            defaultEncoding = "UTF-8"
        }

        private val template = WakaReport::class.java.classLoader
            .getResourceAsStream("report/report.ftl")?.readAllBytes()?.decodeToString()
    }

    /**
     * 根据日期出报告
     */
    fun report(date: ZonedDateTime): ReportData?

    fun doReport(
        allStatContent: StatData?,
        statAtContent: StatData?,
        statAt: ZonedDateTime,
        grading: Grading
    ): ReportData? = statAtContent?.let { stat ->
        val templateParam = mapOf(
            "title" to "Waka Waki - ${grading.type} - ${stat.range.start} - ${stat.range.end}",
            "grading" to object {
                val name = grading.name
                val type = grading.type
                val desc = grading.desc
            },
            "range" to object {
                val year = statAt.year
                val start = stat.range.start
                val end = stat.range.end
            },
            "contributions" to object {
                val year = statAt.year
                val max = (allStatContent?.contributions ?: statAtContent.contributions).asSequence().maxOf(
                    StatDurationNode::duration
                )
                val data = mapper.writeValueAsString(
                    (allStatContent?.contributions ?: statAtContent.contributions).asSequence()
                        .sortedBy(StatDurationNode::period)
                        .map {
                            listOf(it.period, it.duration)
                        }.toList()
                )
            },
            "durations" to object {
                val start = statAt.with(ChronoField.HOUR_OF_DAY, 0)
                    .with(ChronoField.MINUTE_OF_HOUR, 0)
                    .with(ChronoField.SECOND_OF_MINUTE, 0)
                val end = statAt.plusDays(1).with(ChronoField.HOUR_OF_DAY, 0)
                    .with(ChronoField.MINUTE_OF_HOUR, 0)
                    .with(ChronoField.SECOND_OF_MINUTE, 0)
                val dataMap = stat.durations.stream()
                    .collect(Collectors.toMap(StatDurationNode::period, StatDurationNode::duration))
                val axis = Stream.iterate(start, { s -> s.isBefore(end) }) {
                    it.plusMinutes(5)
                }.map {
                    val hour = it.hour
                    // 12:00:00 -> 11:59 -> 11:55
                    val minute = it.minute.div(5).times(5)
                    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
                }.toList()
                val dataAxis = mapper.writeValueAsString(axis + listOf("00:00"))
                val data = mapper.writeValueAsString(axis.asSequence().map { dataMap[it] ?: 0 }.toList() + listOf(0))
            },
            "languages" to object {
                val total = stat.summaries.languages.sumOf(StatSummaryNode::totalDuration) * 1.0
                val data = mapper.writeValueAsString(stat.summaries.languages.asSequence().map {
                    object {
                        val name = "${it.name} (${String.format("%.1f", it.totalDuration / total * 100)}%)"
                        val value = it.totalDuration
                    }
                }.toList())
            },
            "editors" to object {
                val total = stat.summaries.editors.sumOf(StatSummaryNode::totalDuration) * 1.0
                val data = mapper.writeValueAsString(stat.summaries.editors.asSequence().map {
                    object {
                        val name = "${it.name} (${String.format("%.1f", it.totalDuration / total * 100)}%)"
                        val value = it.totalDuration
                    }
                }.toList())
            },
            "projects" to object {
                val total = stat.summaries.projects.sumOf(StatSummaryNode::totalDuration) * 1.0
                val data = mapper.writeValueAsString(stat.summaries.projects.asSequence().map {
                    object {
                        val name = "${Hashing.sha256().hashBytes(it.name.toByteArray()).toString().substring(0..6)} (${String.format("%.1f", it.totalDuration / total * 100)}%)"
                        val value = it.totalDuration
                    }
                }.toList())
            },
            "categories" to object {
                val total = stat.summaries.categories.sumOf(StatSummaryNode::totalDuration) * 1.0
                val data = mapper.writeValueAsString(stat.summaries.categories.asSequence().map {
                    object {
                        val name = "${it.name} (${String.format("%.1f", it.totalDuration / total * 100)}%)"
                        val value = it.totalDuration
                    }
                }.toList())
            },
            "mostHardDay" to stat.stat.mostHardDay?.let {
                object {
                    val date = it.date
                    val hour = it.totalDuration.toHoursPart()
                    val minute = it.totalDuration.toMinutesPart()
                }
            },
            "mostEarlyDay" to stat.stat.mostEarlyDay,
            "mostLateDay" to stat.stat.mostLateDay,
            "averageDurationsOnWorkDays" to object {
                val hour = stat.stat.averageDurationsOnWorkDays.toHoursPart()
                val minute = stat.stat.averageDurationsOnWorkDays.toMinutesPart()
            },
            "favoritePeriod" to stat.stat.favoritePeriod

        )

        val content = StringWriter().apply {
            Template(null, template, cfg).process(templateParam, this)
        }.toString()

        return ReportData(content, grading, statAt)
    }
}