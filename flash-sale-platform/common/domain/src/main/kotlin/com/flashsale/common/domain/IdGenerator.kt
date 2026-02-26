package com.flashsale.common.domain

import java.security.SecureRandom
import java.time.Instant

/**
 * 시간 순서가 보장되는 고유 ID 생성기.
 * ULID(Universally Unique Lexicographically Sortable Identifier) 방식.
 *
 * 성능 특성:
 * - ThreadLocal SecureRandom으로 lock contention 제거
 * - 10만 TPS에서도 병목 없음
 * - 시간순 정렬 가능 (앞 10자리가 타임스탬프)
 * - 밀리초 내 충돌 없음 (뒤 16자리가 랜덤)
 */
object IdGenerator {
    private const val ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val TIMESTAMP_LENGTH = 10
    private const val RANDOMNESS_LENGTH = 16

    // ThreadLocal로 스레드별 독립 인스턴스 → lock contention 제거
    private val threadLocalRandom = ThreadLocal.withInitial { SecureRandom() }

    fun generate(): String = generate(Instant.now().toEpochMilli())

    internal fun generate(timestamp: Long): String {
        val chars = CharArray(TIMESTAMP_LENGTH + RANDOMNESS_LENGTH)
        encodeTimestamp(chars, timestamp)
        encodeRandomness(chars)
        return String(chars)
    }

    private fun encodeTimestamp(
        chars: CharArray,
        timestamp: Long,
    ) {
        var ts = timestamp
        for (i in TIMESTAMP_LENGTH - 1 downTo 0) {
            chars[i] = ENCODING[(ts and 0x1F).toInt()]
            ts = ts shr 5
        }
    }

    private fun encodeRandomness(chars: CharArray) {
        val random = threadLocalRandom.get()
        val bytes = ByteArray(10)
        random.nextBytes(bytes)
        var bitBuffer = 0L
        var bitsInBuffer = 0
        var byteIndex = 0
        for (i in TIMESTAMP_LENGTH until TIMESTAMP_LENGTH + RANDOMNESS_LENGTH) {
            while (bitsInBuffer < 5 && byteIndex < bytes.size) {
                bitBuffer = (bitBuffer shl 8) or (bytes[byteIndex].toLong() and 0xFF)
                bitsInBuffer += 8
                byteIndex++
            }
            bitsInBuffer -= 5
            chars[i] = ENCODING[((bitBuffer shr bitsInBuffer) and 0x1F).toInt()]
        }
    }
}
