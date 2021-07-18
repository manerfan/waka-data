package com.manerfan.waka.data.verticles.stat

import com.manerfan.waka.data.logger
import com.manerfan.waka.data.models.ObjectCodec
import com.manerfan.waka.data.models.StatDurationNode
import com.manerfan.waka.data.models.WakaData
import com.manerfan.waka.data.toLocalDateTime
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors

/**
 * WakaStatVerticle
 *
 * @author yongyong.fan
 * @date 2021/7/18
 */
class WakaStatVerticle : AbstractVerticle() {

    companion object {
        const val WAKA_STAT_DAILY = "waka.stat.daily"
    }

    override fun start(stopFuture: Promise<Void>) {
        logger.info("==> Waka Data Statistics [Daily]")

        // 日统计
        vertx.eventBus().registerDefaultCodec(WakaData::class.java, ObjectCodec(WakaData::class.java))
        vertx.eventBus().consumer<WakaData>(WAKA_STAT_DAILY).handler { message ->
            val wakaData = message.body()
            val summary = wakaData.summaries.orEmpty().asSequence().sortedByDescending { it.range.date }.first()

            val durations = wakaData.durations.orEmpty().parallelStream().map { duration ->
                val start = duration.time.times(1000).toLocalDateTime()
                val end = start.plus(duration.duration.times(1000).toLong(), ChronoUnit.MILLIS)
                StatDurationNode.from(start, end)
            }.collect(Collectors.toList())

            message.reply("DONE")
        }

        super.start(stopFuture)
    }

    override fun stop(stopFuture: Promise<Void>) {
        super.stop(stopFuture)
    }
}
