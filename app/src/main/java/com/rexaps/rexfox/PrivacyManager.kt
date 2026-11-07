package com.rexaps.rexfox

class PrivacyManager {
    fun status(settings: BrowserSettings): String =
        if (settings.trackingProtectionEnabled) {
            "GeckoView tracking protection is enabled."
        } else {
            "No tracker blocking is active."
        }

    fun canClaimTrackerBlocking(): Boolean = false
}
