package com.raumfeld.wamplibrary

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

typealias TopicPattern = String

class RandomIdGenerator {

    private val usedIds = Collections.newSetFromMap<Long>(ConcurrentHashMap())

    private val sequence = generateSequence(0L) { Identifier.acceptableRange.random() }

    fun newRandomId() = sequence.first { isValid(it) }.also { usedIds.add(it) }

    private fun isValid(id: Long) = Identifier.isValid(id) && !hasId(id)

    private fun hasId(id: Long) = id in usedIds

    fun releaseId(id: Long) = usedIds.remove(id)
}

object Identifier {
    internal val acceptableRange = 1L..2.toDouble().pow(53).toLong()

    fun isValid(id: Long) = id in acceptableRange
}