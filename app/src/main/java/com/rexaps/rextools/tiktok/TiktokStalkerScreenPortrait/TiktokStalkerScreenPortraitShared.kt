package com.rexaps.rextools.tiktok

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.rexaps.rextools.ui.*

internal const val SCRAPE_JS = """

(function() {
    try {
        var el = document.getElementById('__UNIVERSAL_DATA_FOR_REHYDRATION__');
        if (el) { window.RexBridge.onResult(el.textContent); return; }
        var scripts = document.getElementsByTagName('script');
        for (var i = 0; i < scripts.length; i++) {
            if (scripts[i].id === '__UNIVERSAL_DATA_FOR_REHYDRATION__') {
                window.RexBridge.onResult(scripts[i].textContent);
                return;
            }
        }
        window.RexBridge.onError("Data TikTok tidak ditemukan.");
    } catch (e) {
        window.RexBridge.onError(e.message);
    }
})();
"""

internal const val SCRAPE_TIMEOUT_MS = 25_000L

/* ═════════════════════ KARTU FULL ═════════════════════ */

internal const val PREVIEW_COUNT = 6
