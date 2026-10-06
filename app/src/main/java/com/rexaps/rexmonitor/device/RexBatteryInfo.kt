package com.rexaps.rexmonitor.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

enum class RexChargeType { NONE, AC, USB, WIRELESS, OTHER }

data class RexBatteryInfo(
    val percent: Int? = null,
    val isCharging: Boolean? = null,
    val chargeType: RexChargeType = RexChargeType.NONE,
    val temperatureC: Float? = null,
    val health: String? = null,
    val voltageMv: Int? = null,
    /** Arus baterai (mA) sesuai laporan perangkat; tanda/akurasi bergantung vendor. */
    val currentMa: Int? = null,
    val chargeRemainingMs: Long? = null
)

class RexBatteryReader(context: Context) {
    private val app = context.applicationContext
    private val bm = app.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    fun read(): RexBatteryInfo {
        val i: Intent = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return RexBatteryInfo()
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else null

        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL -> true
            BatteryManager.BATTERY_STATUS_DISCHARGING, BatteryManager.BATTERY_STATUS_NOT_CHARGING -> false
            else -> null
        }
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val chargeType = when {
            plugged == 0 -> RexChargeType.NONE
            plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> RexChargeType.AC
            plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> RexChargeType.USB
            plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> RexChargeType.WIRELESS
            else -> RexChargeType.OTHER
        }
        val tempTenths = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temp = if (tempTenths != Int.MIN_VALUE) tempTenths / 10f else null
        val health = when (i.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> null
        }
        val voltage = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1).takeIf { it > 0 }

        val currentUa = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            ?.takeIf { it != Int.MIN_VALUE && it != 0 }
        val remaining = if (charging == true) {
            bm?.computeChargeTimeRemaining()?.takeIf { it > 0 }
        } else null

        return RexBatteryInfo(
            percent = percent,
            isCharging = charging,
            chargeType = chargeType,
            temperatureC = temp,
            health = health,
            voltageMv = voltage,
            currentMa = currentUa?.let { it / 1000 },
            chargeRemainingMs = remaining
        )
    }
}
