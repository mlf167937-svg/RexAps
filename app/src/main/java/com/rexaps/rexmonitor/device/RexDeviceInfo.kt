package com.rexaps.rexmonitor.device

import android.os.Build

data class RexDeviceInfo(
    val manufacturer: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val device: String? = null,
    val androidVersion: String? = null,
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val architecture: String? = null,
    val supportedAbis: List<String> = emptyList(),
    val is64Bit: Boolean = false
) {
    val displayName: String?
        get() {
            val m = model ?: return null
            val maker = (manufacturer ?: brand)?.replaceFirstChar { it.uppercase() }
            return if (maker != null && !m.startsWith(maker, ignoreCase = true)) "$maker $m" else m
        }
}

class RexDeviceInfoReader {
    private fun String?.clean(): String? =
        this?.takeIf { it.isNotBlank() && !it.equals(Build.UNKNOWN, ignoreCase = true) }

    fun read(): RexDeviceInfo {
        val abis = Build.SUPPORTED_ABIS?.toList().orEmpty()
        return RexDeviceInfo(
            manufacturer = Build.MANUFACTURER.clean(),
            brand = Build.BRAND.clean(),
            model = Build.MODEL.clean(),
            device = Build.DEVICE.clean(),
            androidVersion = Build.VERSION.RELEASE.clean(),
            sdkInt = Build.VERSION.SDK_INT,
            architecture = abis.firstOrNull(),
            supportedAbis = abis,
            is64Bit = Build.SUPPORTED_64_BIT_ABIS?.isNotEmpty() == true
        )
    }
}
