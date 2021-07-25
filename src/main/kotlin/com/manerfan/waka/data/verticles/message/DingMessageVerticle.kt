package com.manerfan.waka.data.verticles.message

import com.dingtalk.api.DefaultDingTalkClient
import com.dingtalk.api.DingTalkClient
import com.dingtalk.api.request.OapiRobotSendRequest
import com.manerfan.waka.data.DING_ROBOT_CONFIG_KEY
import com.manerfan.waka.data.DING_ROBOT_WEB_HOOK
import com.manerfan.waka.data.models.Grading
import com.manerfan.waka.data.models.StatData
import io.vertx.core.Promise
import io.vertx.reactivex.core.AbstractVerticle
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

    companion object {
        const val DING_MESSAGE = "ding.mesage"
    }

    override fun start(startFuture: Promise<Void>) {
        vertx.sharedData().getLocalMap<String, String>(DING_ROBOT_CONFIG_KEY)?.let {
            dingClient = DefaultDingTalkClient(it[DING_ROBOT_WEB_HOOK])
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
                    btnOrientation = "0"
                    btns = listOf(
                        OapiRobotSendRequest.Btns().apply {
                            title = "查看详情"
                            actionURL = ""
                        },
                        OapiRobotSendRequest.Btns().apply {
                            title = "统计首页"
                            actionURL = ""
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

    private fun StatData.generateTitle() = when (this.grading) {
        Grading.DAILY -> "${this.grading.desc}(${this.range.start})"
        else -> "${this.grading.desc}(${this.range.start} - ${this.range.end})"
    }

    private fun StatData.generateHead() = when (this.grading) {
        Grading.DAILY -> "${this.range.start} ${this.grading.rangeDesc}"
        else -> "${this.range.start} - ${this.range.end} ${this.grading.rangeDesc}"
    }

    private fun StatData.generateText(): String {
        val devoteTime = this.stat.averageSecondsOnWorkDays.humanReadable()
        return when (this.grading) {
            Grading.DAILY -> TextBuilder()
                .heading(this.generateTitle(), 3)
                .newParagraph()
                .heading(generateHead(), 4)
                .newParagraph()
                .text("在编码这件事情上")
                .newParagraph()
                .text("---")
                .newParagraph()
                .text("从").bold(this.stat.mostEarlyDay?.time).text("一直到").bold(this.stat.mostLateDay?.time)
                .newParagraph()
                .text("共投入 ").bold(devoteTime)
                .newParagraph()
                .text("---")
                .newParagraph()
                .text("@Waka-Waki ")
                .link("▶ Cods On Github", "https://github.com/manerfan/waka-data")
                .newParagraph()
                .toString()
            else -> TextBuilder()
                .heading(this.generateTitle(), 3)
                .newParagraph()
                .heading(generateHead(), 4)
                .newParagraph()
                .text("在编码这件事情上")
                .newParagraph()
                .text("---")
                .newParagraph()
                .text("平均每天投入 ").bold(devoteTime)
                .newParagraph()
                .bold(this.stat.mostHardDay?.date).text("最辛苦，共投入").bold(this.stat.mostHardDay?.totalSeconds?.humanReadable())
                .newParagraph()
                .bold(this.stat.mostLateDay?.date).text("工作最晚，一直到").bold(this.stat.mostLateDay?.time)
                .newParagraph()
                .bold(this.stat.mostLateDay?.date).text("工作最早，").bold(this.stat.mostLateDay?.time).text("便启动战斗模式")
                .newParagraph()
                .text("---")
                .newParagraph()
                .text("@Waka-Waki ")
                .link("▶ Cods On Github", "https://github.com/manerfan/waka-data")
                .newParagraph()
                .toString()
        }
    }

    private fun Duration.humanReadable() = "${this.toHours()}小时${this.toMinutesPart()}分钟"
    private fun Double.humanReadable() = Duration.ofMillis(this.times(1000).toLong()).humanReadable()
    private fun Long.humanReadable() = Duration.ofMillis(this).humanReadable()
}
