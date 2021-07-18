package com.manerfan.waka.data.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.manerfan.waka.data.mapper
import io.netty.util.CharsetUtil
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec

/**
 * ObjectCodec
 *
 * @author yongyong.fan
 * @date 2021/7/18
 */
class ObjectCodec<T>(private val type: Class<T>) : MessageCodec<T, T> {
    override fun encodeToWire(buffer: Buffer, s: T) {
        val strBytes: ByteArray = mapper.writeValueAsString(s).toByteArray()
        buffer.appendInt(strBytes.size)
        buffer.appendBytes(strBytes)
    }

    override fun decodeFromWire(pos: Int, buffer: Buffer): T {
        val length = buffer.getInt(pos)
        val p = pos + 4
        val bytes = buffer.getBytes(p, p + length)
        return mapper.readValue(String(bytes, CharsetUtil.UTF_8), type)
    }

    override fun transform(s: T): T = s

    override fun name(): String = type.simpleName

    /**
     * -1 for a user codec.
     */
    override fun systemCodecID(): Byte = -1

}
