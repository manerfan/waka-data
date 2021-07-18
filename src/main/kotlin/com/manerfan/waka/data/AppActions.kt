@file:JvmName("AppActions")

package com.manerfan.waka.data

import com.manerfan.waka.data.verticles.collect.WakaCollectVerticle
import com.manerfan.waka.data.verticles.oss.OssAccessorVerticle
import com.manerfan.waka.data.verticles.stat.WakaStatVerticle
import io.reactivex.Single
import io.vertx.core.cli.CLI
import io.vertx.core.cli.annotations.CLIConfigurator
import io.vertx.core.cli.annotations.Name
import io.vertx.core.cli.annotations.Option
import io.vertx.core.cli.annotations.Summary
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import org.slf4j.LoggerFactory

/**
 * AppActions
 *
 * @author Maner.Fan
 * @date 2021/3/19
 */

val logger = LoggerFactory.getLogger("waka-data")

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    logger.info("==> Start Action")

    //// parse commands
    //// 解析命令行参数

    val cli = CLI.create(WakaCli::class.java)
    val command = cli.parse(args.asList(), false)

    // display help
    if (!command.isValid || command.isAskingForHelp) {
        val builder = StringBuilder()
        cli.usage(builder)
        println(builder.toString())
        return vertx.close()
    }

    //// prepare vertex shared data
    //// 将命令行参数转为vertx shared data供各Verticle使用

    val wakaCli = WakaCli()
    CLIConfigurator.inject(command, wakaCli)
    wakaCli.toSharedData(vertx)

    //// deploy & collect & statistics
    listOf(
        vertx.rxDeployVerticle(WakaCollectVerticle()).doOnSubscribe { logger.info("==> Deploy WakaCollectVerticle") },
        vertx.rxDeployVerticle(OssAccessorVerticle()).doOnSubscribe { logger.info("==> Deploy OssAccessorVerticle") },
        vertx.rxDeployVerticle(WakaStatVerticle()).doOnSubscribe { logger.info("==> Deploy WakaStatVerticle") }
    ).chain().subscribe { _ ->
        listOf(
            // waka data collect
            vertx.eventBus().rxRequest(WakaCollectVerticle.WAKA_COLLECT, 7L),
            // TODO waka data statistics
            Single.just<Message<String>>(Message(null)).doOnSubscribe { logger.info("==> Waka Data Statistics") }
        ).chain().doFinally {
            logger.info("<== Close Action")
            vertx.close()
        }.subscribe()
    }
}

@Name("waka")
@Summary("waka time data collect & statistics.")
class WakaCli {
    var help: Boolean = false
        @Option(
            shortName = "h",
            longName = "help",
            help = true,
            flag = true
        ) set

    var apiKey: String = ""
        @Option(
            longName = "wakaApiKey",
            argName = "waka time api key",
            required = true
        ) set

    var ossEndpoint: String = ""
        @Option(
            longName = "ossEndpoint",
            argName = "oss end point",
            required = true
        ) set

    var ossAccessKeyId: String = ""
        @Option(
            longName = "ossAccessKeyId",
            argName = "oss access key id",
            required = true
        ) set

    var ossAccessKeySecret: String = ""
        @Option(
            longName = "ossAccessKeySecret",
            argName = "oss access key secret",
            required = true
        ) set

    var ossBucketName: String = ""
        @Option(
            longName = "ossBucketName",
            argName = "oss bucket name",
            required = true
        ) set

    var dingRobotWebhook: String = ""
        @Option(
            longName = "dingRobotWebhook",
            argName = "dingding robot webhook",
            required = false
        ) set

    var dingRobotSecret: String = ""
        @Option(
            longName = "dingRobotSecret",
            argName = "dingding robot secret",
            required = false
        ) set

    fun toSharedData(vertx: Vertx) {
        vertx.sharedData().getLocalMap<String, String>(WAKA_CONFIG_KEY).put(WAKA_API_KEY, apiKey)
        vertx.sharedData().getLocalMap<String, String>(OSS_CONFIG_KEY).apply {
            put(OSS_ENDPOINT, ossEndpoint)
            put(OSS_ACCESS_KEY_ID, ossAccessKeyId)
            put(OSS_ACCESS_KEY_SECRET, ossAccessKeySecret)
            put(OSS_BUCKET_NAME, ossBucketName)
        }
        vertx.sharedData().getLocalMap<String, String>(DING_ROBOT_CONFIG_KEY).apply {
            put(DING_ROBOT_WEB_HOOK, dingRobotWebhook)
            put(DING_ROBOT_SECRET, dingRobotSecret)
        }
    }
}

fun <R : Any> List<Single<R>>.chain() = this.reduce { acc, single -> acc.flatMap { single } }
