package com.manerfan.waka.data.verticles.stat

import com.manerfan.waka.data.models.*
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.util.*
import java.util.stream.Collectors

/**
 * 统计
 *
 * @author yongyong.fan
 * @date 2021/7/28
 */
interface WakaStat {
    /**
     * 根据日期统计
     */
    fun stat(date: ZonedDateTime): StatData?

    /**
     * 合并StatData列表
     */
    fun List<StatData>?.merge(grading: Grading, range: Range): StatData? = if (this.isNullOrEmpty()) null else {

        // ====== 将所有的数据按类型分类整合 ======

        val categoriesGroup = mutableListOf<List<StatSummaryNode>>()
        val editorsGroup = mutableListOf<List<StatSummaryNode>>()
        val languagesGroup = mutableListOf<List<StatSummaryNode>>()
        val operatingSystemsGroup = mutableListOf<List<StatSummaryNode>>()
        val projectsGroup = mutableListOf<List<StatSummaryNode>>()

        val durationsGroup = mutableListOf<List<StatDurationNode>>()
        val contributionsGroup = mutableListOf<List<StatDurationNode>>()

        val mostLateDayGroup = mutableListOf<MostLateDay>()
        val mostEarlyDayGroup = mutableListOf<MostEarlyDay>()

        this.forEach { statData ->
            categoriesGroup.add(statData.summaries.categories)
            editorsGroup.add(statData.summaries.editors)
            languagesGroup.add(statData.summaries.languages)
            operatingSystemsGroup.add(statData.summaries.operatingSystems)
            projectsGroup.add(statData.summaries.projects)

            durationsGroup.add(statData.durations)
            contributionsGroup.add(statData.contributions)

            statData.stat.mostLateDay?.let { mostLateDayGroup.add(it) }
            statData.stat.mostEarlyDay?.let { mostEarlyDayGroup.add(it) }
        }

        // ====== 分组用的key，组内叠加的value ======

        val statSummaryNodeKeyGetter = { ssn: StatSummaryNode -> ssn.name }
        val statSummaryNodeValueGetter = { ssn: StatSummaryNode -> ssn.totalDuration }
        val statSummaryNodeValueAdder =
            { ssn: StatSummaryNode, totalDurations: Long -> ssn.totalDuration += totalDurations }

        val statDurationNodeKeyGetter = { sdn: StatDurationNode -> sdn.period }
        val statDurationNodeValueGetter = { sdn: StatDurationNode -> sdn.duration }
        val statDurationNodeValueAdder =
            { sdn: StatDurationNode, duration: Long -> sdn.duration += duration }

        // ====== 通过以上信息merge数据 ======

        val categories =
            categoriesGroup.merge(statSummaryNodeKeyGetter, statSummaryNodeValueGetter, statSummaryNodeValueAdder)
        val editors =
            editorsGroup.merge(statSummaryNodeKeyGetter, statSummaryNodeValueGetter, statSummaryNodeValueAdder)
        val languages =
            languagesGroup.merge(statSummaryNodeKeyGetter, statSummaryNodeValueGetter, statSummaryNodeValueAdder)
        val operatingSystems =
            operatingSystemsGroup.merge(statSummaryNodeKeyGetter, statSummaryNodeValueGetter, statSummaryNodeValueAdder)
        val projects =
            projectsGroup.merge(statSummaryNodeKeyGetter, statSummaryNodeValueGetter, statSummaryNodeValueAdder)

        val durations =
            durationsGroup.merge(statDurationNodeKeyGetter, statDurationNodeValueGetter, statDurationNodeValueAdder)
        val contributions =
            contributionsGroup.merge(statDurationNodeKeyGetter, statDurationNodeValueGetter, statDurationNodeValueAdder)

        // ====== 构造StatData =====

        if (durations.isEmpty()) null
        else StatData(
            grading,
            range,
            StatSummary(categories, editors, languages, operatingSystems, projects),
            durations,
            contributions,
            Stat(
                contributions.maxByOrNull(StatDurationNode::duration)?.let { MostHardDay(it.period, it.duration) },
                mostLateDayGroup.minByOrNull(MostLateDay::time)?.let {
                    // 00:00 ~ 04:00 算最最晚
                    if (it.time <= "04:00") it else null
                } ?: mostLateDayGroup.maxByOrNull(MostLateDay::time),
                mostEarlyDayGroup.minByOrNull(MostEarlyDay::time),
                durations.favoritePeriod(),
                null,
                contributions.stream()
                    .filter { Objects.nonNull(it) && it.duration > 0 }
                    .collect(Collectors.averagingLong(StatDurationNode::duration))
                    .toLong()
            )
        )
    }
}

/**
 * 把多个list合并成一个list
 */
fun <T, V : Comparable<V>> List<List<T>>.merge(
    keyGetter: (T) -> String,
    valueGetter: (T) -> V,
    valueAdder: (T, V) -> Unit
): List<T> {
    if (this.isEmpty()) {
        return emptyList()
    }

    if (this.size == 1) {
        return this.first()
    }

    // 拿到第一条数据
    val genesis = this.first().toMutableList()
    // 按照key封装为map，便于索引
    val genesisMap = genesis.stream().collect(Collectors.toMap(keyGetter) { g -> g })

    // 遍历余下的数据
    this.stream().skip(1)
        .flatMap { subjects -> subjects.stream() }
        .forEach { subject ->
            // 拿到key
            val key = keyGetter.invoke(subject)
            // 拿到value
            val value = valueGetter.invoke(subject)
            if (genesisMap.containsKey(key)) {
                // 存在，则直接加值
                valueAdder.invoke(genesisMap[key]!!, value)
            } else {
                // 不存在，则add进列表
                genesis.add(subject)
                genesisMap[key] = subject
            }
        }

    return genesis.asSequence().sortedByDescending(valueGetter).toList()
}

fun saturdayOfLastWeek() = { temporal: Temporal ->
    temporal.with(ChronoField.DAY_OF_WEEK, 6).minus(1, ChronoUnit.WEEKS)
}

fun firstDayOfLastMonth() = { temporal: Temporal ->
    temporal.with(ChronoField.DAY_OF_MONTH, 1).minus(1, ChronoUnit.MONTHS)
}

fun firstDayOfLastYear() = { temporal: Temporal ->
    temporal.with(ChronoField.DAY_OF_YEAR, 1).minus(1, ChronoUnit.YEARS)
}
