package com.manerfan.waka.data.models

import com.fasterxml.jackson.core.type.TypeReference
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
class ObjectTypeCodec<T> : MessageCodec<T, T> {
    private val type: TypeReference<T> = object : TypeReference<T>() {}

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

    override fun name(): String = "OssPutMetaCodec"

    /**
     * -1 for a user codec.
     */
    override fun systemCodecID(): Byte = -1

}
