@file:JvmName("AppActions")

package com.manerfan.waka.data

import com.manerfan.waka.data.collect.WakaCollectVerticle
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

    val cli = CLI.create(WakaCli::class.java)
    val command = cli.parse(args.asList(), false)

    if (!command.isValid || command.isAskingForHelp) {
        val builder = StringBuilder()
        cli.usage(builder)
        println(builder.toString())
        return vertx.close()
    }

    //// prepare shared data

    val wakaCli = WakaCli()
    CLIConfigurator.inject(command, wakaCli)
    wakaCli.toSharedData(vertx)

    //// deploy& collect & statistics

    Single.merge(
        listOf(
            vertx.rxDeployVerticle(WakaCollectVerticle())
                .doOnSubscribe { logger.info("==> Deploy WakaCollectVerticle") }
        )
    ).switchMap {
        Single.merge(
            listOf<Single<Message<String>>>(
                // waka data collect
                vertx.eventBus().rxRequest<String>(WakaCollectVerticle.WAKA_COLLECT, 7L)
                    .doOnSubscribe { logger.info("==> Waka Data Collect") },
                // TODO waka data statistics
                Single.just<Message<String>>(Message(null)).doOnSubscribe { logger.info("==> Waka Data Statistics") },
            )
        )
    }.doFinally {
        logger.info("<== Close Action")
        vertx.close()
    }.subscribe()
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

    fun toSharedData(vertx: Vertx) {
        vertx.sharedData().getLocalMap<String, String>(WAKA_CONFIG_KEY).put(WAKA_API_KEY, apiKey)
        vertx.sharedData().getLocalMap<String, String>(OSS_CONFIG_KEY).apply {
            put(OSS_ENDPOINT, ossEndpoint)
            put(OSS_ACCESS_KEY_ID, ossAccessKeyId)
            put(OSS_ACCESS_KEY_SECRET, ossAccessKeySecret)
            put(OSS_BUCKET_NAME, ossBucketName)
        }
    }
}
