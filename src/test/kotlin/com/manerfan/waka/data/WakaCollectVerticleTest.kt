package com.manerfan.waka.data

import com.manerfan.waka.data.collect.WakaCollectVerticle
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * WakaCollectVerticleTest
 *
 * @author Maner.Fan
 * @date 2021/3/17
 */
@ExtendWith(VertxExtension::class)
class WakaCollectVerticleTest {
    @BeforeEach
    fun deploy_verticle(vertx: Vertx, testContext: VertxTestContext) {
        vertx.sharedData().getLocalMap<String, String>(WAKA_CONFIG_KEY)[WAKA_API_KEY] = "your waka time api key"
        vertx.deployVerticle(WakaCollectVerticle(), testContext.succeedingThenComplete())
    }

    @Test
    fun verticle_deployed(vertx: Vertx, testContext: VertxTestContext) {
        vertx.eventBus().request<String>(WakaCollectVerticle.WAKA_COLLECT, 7L) {
            println(it.result().body())
            testContext.completeNow()
        }
    }
}