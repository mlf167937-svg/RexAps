package com.rexaps.rexmonitor.booster

enum class RexCheckStatus { OK, APPLIED, WARNING, INFO }

data class RexCheck(
    val title: String,
    val status: RexCheckStatus,
    val detail: String
)

/** Hanya berisi hasil terukur. Tidak ada persentase "boost" buatan. */
data class RexOptimizationResult(
    val checks: List<RexCheck>,
    val freedBytes: Long,
    val ramBeforeAvailableBytes: Long?,
    val ramAfterAvailableBytes: Long?,
    val batteryTempBeforeC: Float?,
    val batteryTempAfterC: Float?,
    val recommendations: List<String>,
    val suggestStorageSettings: Boolean
) {
    val completedChecks: Int get() = checks.size
    val appliedCount: Int get() = checks.count { it.status == RexCheckStatus.APPLIED }

    val ramDeltaBytes: Long?
        get() = if (ramBeforeAvailableBytes != null && ramAfterAvailableBytes != null)
            ramAfterAvailableBytes - ramBeforeAvailableBytes else null

    val tempDeltaC: Float?
        get() = if (batteryTempBeforeC != null && batteryTempAfterC != null)
            batteryTempAfterC - batteryTempBeforeC else null

    val ramChangedSignificantly: Boolean
        get() = ramDeltaBytes?.let { kotlin.math.abs(it) >= 50L * 1024 * 1024 } == true

    val tempChangedSignificantly: Boolean
        get() = tempDeltaC?.let { kotlin.math.abs(it) >= 0.5f } == true

    val noSignificantChange: Boolean
        get() = !ramChangedSignificantly && !tempChangedSignificantly && freedBytes <= 0L
}
