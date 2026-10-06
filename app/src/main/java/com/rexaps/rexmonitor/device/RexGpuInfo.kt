package com.rexaps.rexmonitor.device

import android.content.Context
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import java.io.File

data class RexGpuInfo(
    val renderer: String? = null,
    val vendor: String? = null,
    val glVersion: String? = null,
    val vulkanVersion: String? = null,
    /** Hanya terisi bila sistem menyediakan counter yang dapat dibaca (mis. Adreno kgsl). */
    val usagePercent: Int? = null
)

class RexGpuReader(private val context: Context) {
    private var cached: RexGpuInfo? = null

    fun read(): RexGpuInfo {
        val base = cached ?: queryStatic().also { cached = it }
        return base.copy(usagePercent = readUsage())
    }

    private fun readUsage(): Int? = runCatching {
        File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage").readText()
            .filter { it.isDigit() }.toIntOrNull()?.takeIf { it in 0..100 }
    }.getOrNull()

    private fun queryStatic(): RexGpuInfo {
        val vulkan = vulkanVersion()
        val gl = queryGl()
        return gl.copy(vulkanVersion = vulkan)
    }

    private fun vulkanVersion(): String? = runCatching {
        val pm = context.packageManager
        val feature = pm.systemAvailableFeatures
            .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
            ?: return null
        val v = feature.version
        "${v shr 22}.${(v shr 12) and 0x3ff}"
    }.getOrNull()

    private fun queryGl(): RexGpuInfo {
        var display = EGL14.EGL_NO_DISPLAY
        var surface = EGL14.EGL_NO_SURFACE
        var ctx = EGL14.EGL_NO_CONTEXT
        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return RexGpuInfo()
            val ver = IntArray(2)
            if (!EGL14.eglInitialize(display, ver, 0, ver, 1)) return RexGpuInfo()
            val attribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val num = IntArray(1)
            EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, num, 0)
            val config = configs[0] ?: return RexGpuInfo()
            ctx = EGL14.eglCreateContext(
                display, config, EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
            )
            surface = EGL14.eglCreatePbufferSurface(
                display, config,
                intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0
            )
            if (!EGL14.eglMakeCurrent(display, surface, surface, ctx)) return RexGpuInfo()
            val renderer = GLES20.glGetString(GLES20.GL_RENDERER)
            val vendor = GLES20.glGetString(GLES20.GL_VENDOR)
            val version = GLES20.glGetString(GLES20.GL_VERSION)
            val shortVersion = version?.let { Regex("OpenGL ES \\d+(\\.\\d+)?").find(it)?.value }
            return RexGpuInfo(renderer = renderer, vendor = vendor, glVersion = shortVersion ?: version)
        } catch (_: Throwable) {
            return RexGpuInfo()
        } finally {
            runCatching {
                if (display != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(
                        display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
                    )
                    if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
                    if (ctx != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, ctx)
                }
            }
        }
    }
}
