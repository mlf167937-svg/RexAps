package com.rexaps.rexcoder.model

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*

fun RexCoderState.__split_removeGroup_4(id: Int) {
        if (groups.size > 1) {
            groups.removeAll { it.id == id }
            if (focusedGroupId == id) focusedGroupId = groups.first().id
            equalizeGroups()
        }
    }

/**
     * Drag the divider that sits between groups[leftIndex] and groups[leftIndex + 1].
     * [deltaPx] is the finger movement along the split axis, [totalPx] the pixels available to all groups.
     */
    fun RexCoderState.__split_resizeGroups_5(leftIndex: Int, deltaPx: Float, totalPx: Float) {
        val a = groups.getOrNull(leftIndex) ?: return
        val b = groups.getOrNull(leftIndex + 1) ?: return
        if (totalPx <= 0f) return
        val total = groups.sumOf { it.weight.toDouble() }.toFloat()
        val min = total * MIN_GROUP_SHARE
        val pair = a.weight + b.weight
        if (pair < min * 2f) return
        val next = (a.weight + deltaPx / totalPx * total).coerceIn(min, pair - min)
        a.weight = next
        b.weight = pair - next
    }
