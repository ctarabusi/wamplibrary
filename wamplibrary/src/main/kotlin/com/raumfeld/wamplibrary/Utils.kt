package com.raumfeld.wamplibrary

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

class RandomIdGenerator {

    private val acceptableRange = 1L..2.toDouble().pow(53).toLong()

    private val usedIds = Collections.newSetFromMap<Long>(ConcurrentHashMap())

    private val sequence = generateSequence(0L) { acceptableRange.random() }

    fun newRandomId() = sequence.first { isValid(it) }.also { usedIds.add(it) }

    fun releaseId(id: Long) = usedIds.remove(id)

    private fun isValid(id: Long) = id in acceptableRange && !hasId(id)

    private fun hasId(id: Long) = id in usedIds
}