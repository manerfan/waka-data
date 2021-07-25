package com.manerfan.waka.data.verticles.message

import com.dingtalk.api.DefaultDingTalkClient
import com.dingtalk.api.DingTalkClient
import com.dingtalk.api.request.OapiRobotSendRequest
import com.manerfan.waka.data.DING_ROBOT_CONFIG_KEY
import com.manerfan.waka.data.DING_ROBOT_WEB_HOOK
import com.manerfan.waka.data.REPORT_HOME_URL
import com.manerfan.waka.data.models.Grading
import com.manerfan.waka.data.models.StatData
import com.manerfan.waka.data.models.StatSummary
import com.manerfan.waka.data.models.StatSummaryNode
import io.vertx.core.Promise
import io.vertx.reactivex.core.AbstractVerticle
import net.steppschuh.markdowngenerator.MarkdownBuilder
import net.steppschuh.markdowngenerator.MarkdownElement
import net.steppschuh.markdowngenerator.text.TextBuilder
import java.time.Duration


/**
 * 钉消息通知
 *
 * @author yongyong.fan
 * @date 2021/7/25
 */
class DingMessageVerticle : AbstractVerticle() {
    private lateinit var dingClient: DingTalkClient
    private lateinit var reportHomeUrl: String

    companion object {
        const val DING_MESSAGE = "ding.mesage"
    }

    override fun start(startFuture: Promise<Void>) {
        vertx.sharedData().getLocalMap<String, String>(DING_ROBOT_CONFIG_KEY)?.let {
            dingClient = DefaultDingTalkClient(it[DING_ROBOT_WEB_HOOK])
            reportHomeUrl = it[REPORT_HOME_URL]
        } ?: return startFuture.fail(
            """
            |Ding Robot Config must be set in [Launch Arguments] or [Environment Variables]
            |you can obtain waka api key on https://wakatime.com/settings/account
            """.trimMargin()
        )

        vertx.eventBus().consumer<StatData>(DING_MESSAGE).handler { message ->
            val statData = message.body()

            val request = OapiRobotSendRequest().apply {
                msgtype = "actionCard"
                setActionCard(OapiRobotSendRequest.Actioncard().apply {
                    title = statData.generateTitle()
                    text = statData.generateText()
                    btnOrientation = "1"
                    btns = listOf(
                        OapiRobotSendRequest.Btns().apply {
                            title = "查看详情"
                            actionURL = ""
                        },
                        OapiRobotSendRequest.Btns().apply {
                            title = "统计首页"
                            actionURL = reportHomeUrl
                        }
                    )
                })
            }

            dingClient.execute(request)
            message.reply("DONE")
        }

        super.start(startFuture)
    }

    override fun stop(stopFuture: Promise<Void>?) {
        super.stop(stopFuture)
    }

    private fun StatData.generateText(): String {
        val devoteTime = this.stat.averageSecondsOnWorkDays.humanReadable()
        return when (this.grading) {
            Grading.DAILY -> TextBuilder()
                .apply { this@generateText.generateMdHeader(this) }
                .text("---")
                .newParagraph()
                .text("从 ").bold(this.stat.mostEarlyDay?.time).text(" 一直到 ").bold(this.stat.mostLateDay?.time)
                .newParagraph()
                .text("共投入 ").bold(devoteTime)
                .newParagraph()
                .text("---")
                .apply { this@generateText.generateMdCategory(this) }
                .text("---")
                .apply { this@generateText.generateMdLanguages(this) }
                .text("---")
                .apply { this@generateText.generateMdEditors(this) }
                .text("---")
                .apply { this@generateText.generateMdFooter(this) }
                // .text("---")
                // .apply { this@generateText.generateMdBadge(this) }
                .toString()
            else -> TextBuilder()
                .apply { this@generateText.generateMdHeader(this) }
                .text("---")
                .newParagraph()
                .text("平均每天投入 ").bold(devoteTime)
                .newParagraph()
                .bold(this.stat.mostHardDay?.date).text(" 最辛苦，共投入")
                .bold(this.stat.mostHardDay?.totalSeconds?.humanReadable())
                .newParagraph()
                .bold(this.stat.mostLateDay?.date).text(" 工作最晚，一直到 ").bold(this.stat.mostLateDay?.time)
                .newParagraph()
                .bold(this.stat.mostLateDay?.date).text(" 工作最早，").bold(this.stat.mostLateDay?.time).text(" 便启动战斗模式")
                .newParagraph()
                .text("---")
                .apply { this@generateText.generateMdCategory(this) }
                .text("---")
                .apply { this@generateText.generateMdLanguages(this) }
                .text("---")
                .apply { this@generateText.generateMdEditors(this) }
                .text("---")
                .apply { this@generateText.generateMdFooter(this) }
                // .text("---")
                // .apply { this@generateText.generateMdBadge(this) }
                .toString()
        }
    }

    private fun StatData.generateTitle() = when (this.grading) {
        Grading.DAILY -> "${this.grading.desc}(${this.range.start})"
        else -> "${this.grading.desc}(${this.range.start} ~ ${this.range.end})"
    }

    private fun StatData.generateHead() = when (this.grading) {
        Grading.DAILY -> "${this.range.start} ${this.grading.rangeDesc}"
        else -> "${this.range.start} ~ ${this.range.end} ${this.grading.rangeDesc}"
    }

    private fun <T : MarkdownBuilder<T, S>, S : MarkdownElement> StatData.generateMdHeader(builder: MarkdownBuilder<T, S>) {
        builder
            .heading(this.generateTitle(), 3)
            .newParagraph()
            .heading(generateHead(), 4)
            .newParagraph()
            .text("在搬砖这件事情上")
            .newParagraph()
    }

    private fun <T : MarkdownBuilder<T, S>, S : MarkdownElement> StatData.generateMdFooter(builder: MarkdownBuilder<T, S>) {
        builder
            .newParagraph()
            .text("@Waka-Waki ")
            .link("▶ Cods On Github", "https://github.com/manerfan/waka-data")
            .newParagraph()
    }

    private fun <T : MarkdownBuilder<T, S>, S : MarkdownElement> StatData.generateMdBadge(builder: MarkdownBuilder<T, S>) {
        builder
            .newParagraph()
            .image(
                "waka-data-collect-and-statistics",
                "https://github.com/manerfan/waka-data/actions/workflows/main.yml/badge.svg"
            )
            .newParagraph()
            .image(
                "GitHub followers",
                "https://img.shields.io/github/followers/manerfan?style=social")
            .newLine()
            .image(
                "GitHub Repo stars",
                "https://img.shields.io/github/stars/manerfan/waka-data?style=social")
    }

    private fun <T : MarkdownBuilder<T, S>, S : MarkdownElement> StatData.generateMdCategory(builder: MarkdownBuilder<T, S>) =
        this.generateSummaries(builder, StatSummary::categories, "操作")

    private fun <T : MarkdownBuilder<T, S>, S : MarkdownElement> StatData.generateMdEditors(builder: MarkdownBuilder<T, S>) =
        this.generateSummaries(builder, StatSummary::editors, "编辑器")

    private fun <T : MarkdownBuilder<T, S>, S : MarkdownElement> StatData.generateMdLanguages(builder: MarkdownBuilder<T, S>) =
        this.generateSummaries(builder, StatSummary::languages, "语言")

    private fun <T : MarkdownBuilder<T, S>, S : MarkdownElement> StatData.generateSummaries(
        builder: MarkdownBuilder<T, S>,
        getter: (StatSummary) -> List<StatSummaryNode>,
        name: String
    ) {
        val summaryNodes = getter.invoke(this.summaries).asSequence()
            .sortedByDescending { summaryNode -> summaryNode.totalSeconds }
            .toList()
        val totalSeconds = summaryNodes.sumByDouble { summaryNode -> summaryNode.totalSeconds }
        builder.newParagraph().text("${name}前三占比").newParagraph()
        summaryNodes.stream().limit(3).forEach { summaryNode ->
            builder
                .text(summaryNode.name)
                .newParagraph()
                .progressWithLabel(summaryNode.totalSeconds / totalSeconds)
                .newParagraph()
        }
    }

    private fun Duration.humanReadable() = "${this.toHours()}小时${this.toMinutesPart()}分钟"
    private fun Double.humanReadable() = Duration.ofMillis(this.times(1000).toLong()).humanReadable()
    private fun Long.humanReadable() = Duration.ofMillis(this).humanReadable()
}
