package com.manerfan.waka.data

import java.time.ZoneId

/**
 * Configs
 *
 * @author Maner.Fan
 * @date 2021/3/16
 */


val DEF_ZONEID = ZoneId.of("Asia/Shanghai")

/**
 * wakatime config
 */
const val WAKA_CONFIG_KEY = "WAKA_CONFIG"

/**
 * secret api key
 *
 * here to [obtain](https://wakatime.com/settings/account)
 */
const val WAKA_API_KEY = "API_KEY"

/**
 * oss config
 */
const val OSS_CONFIG_KEY = "OSS_CONFIG"

/**
 * oss endpoint
 *
 * for aliyun, here to [obtain](https://oss.console.aliyun.com)
 */
const val OSS_ENDPOINT = "ENDPOINT"

/**
 * oss access key id
 */
const val OSS_ACCESS_KEY_ID = "ACCESS_KEY_ID"

/**
 * oss access key secret
 */
const val OSS_ACCESS_KEY_SECRET = "ACCESS_KEY_SECRET"

/**
 * oss bucket name
 */
const val OSS_BUCKET_NAME = "BUCKET_NAME"

const val OSS_CLIENT = "OSS_CLIENT"

/**
 * ding robot
 *
 * here to [obtain](https://developers.dingtalk.com/document/app/custom-robot-access)
 */
const val DING_ROBOT_CONFIG_KEY = "DING_ROBOT_CONFIG_KEY"

/**
 * webhook for ding robot message
 */
const val DING_ROBOT_WEB_HOOK = "DING_ROBOT_WEB_HOOK"

/**
 * report home
 */
const val REPORT_HOME_URL = "REPORT_HOME_URL"
