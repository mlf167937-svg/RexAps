package com.rexaps.rexfox

class PrivacyManager {
    fun status(settings: BrowserSettings): String =
        if (settings.trackingProtectionEnabled) {
            "Tracker blocking is not installed in this build."
        } else {
            "No tracker blocking is active."
        }

    fun canClaimTrackerBlocking(): Boolean = false
}
