import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin ("jvm") version "1.5.30"
  application
  id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.manerfan"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenLocal()
  mavenCentral()
  jcenter()
}

val vertxVersion = "4.1.1"
val junitJupiterVersion = "5.7.0"

val wakaApiKey: String by project
val ossEndpoint: String by project
val ossAccessKeyId: String by project
val ossAccessKeySecret: String by project
val ossBucketName: String by project
val dingRobotWebhook: String by project
val reportHomeUrl: String by project

val launcherClassName = "com.manerfan.waka.data.AppActions"

application {
  mainClassName = launcherClassName
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-web-client")
  implementation("io.vertx:vertx-rx-java2")
  //implementation("io.vertx:vertx-mysql-client")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.4")

  implementation("io.vertx:vertx-lang-kotlin")
  implementation(kotlin("stdlib-jdk8"))

  implementation("com.aliyun.oss:aliyun-sdk-oss:3.10.2")
  implementation("javax.xml.bind:jaxb-api:2.3.0")
  implementation("com.sun.xml.bind:jaxb-core:2.3.0")
  implementation("com.sun.xml.bind:jaxb-impl:2.3.0")
  implementation("javax.activation:activation:1.1.1")

  implementation(fileTree("libs"))
  //implementation("com.dingtalk.open:dingtalk-openapi-sdk:20201230")
  implementation("net.steppschuh.markdowngenerator:markdowngenerator:1.3.1.1")

  implementation("ch.qos.logback:logback-classic:1.1.8")

  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "11"

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    // attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf("run",
    "--wakaApiKey=$wakaApiKey",
    "--ossEndpoint=$ossEndpoint", "--ossAccessKeyId=$ossAccessKeyId", "--ossAccessKeySecret=$ossAccessKeySecret", "--ossBucketName=$ossBucketName",
    "--dingRobotWebhook=$dingRobotWebhook", "--reportHomeUrl=$reportHomeUrl",
    "--illegal-access=deny"
  )
}
