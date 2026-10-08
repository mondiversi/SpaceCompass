package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.view.TextureView
import java.util.concurrent.atomic.AtomicBoolean

/** TextureView composes below clouds and controls on every supported Android version. */
internal class SpaceCompassStarTextureView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {
    private val thread = HandlerThread("SpaceCompass stars").apply { start() }
    private val worker = Handler(thread.looper)
    private val closed = AtomicBoolean(false)
    private var session: Session? = null
    @Volatile private var drawing: SpaceCompassStarDrawing? = null
    @Volatile private var extentWidth = 1
    @Volatile private var extentHeight = 1
    private val render = Runnable {
        if (!closed.get()) try {
            session?.let { current ->
                check(EGL14.eglMakeCurrent(current.display, current.surface, current.surface, current.context))
                current.renderer.draw(extentWidth, extentHeight, drawing)
                check(EGL14.eglSwapBuffers(current.display, current.surface))
            }
        } catch (error: Exception) { fail(error) }
        catch (error: OutOfMemoryError) { fail(error) }
    }

    init {
        isOpaque = false
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        surfaceTextureListener = this
    }

    fun update(next: SpaceCompassStarDrawing) {
        drawing = next
        if (!closed.get()) { worker.removeCallbacks(render); worker.post(render) }
    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        extentWidth = width; extentHeight = height
        if (!worker.post {
            if (closed.get()) { texture.release(); return@post }
            try {
                session?.close()
                session = null
                session = Session(context.applicationContext, texture)
                render.run()
            } catch (error: Exception) { texture.release(); fail(error) }
            catch (error: OutOfMemoryError) { texture.release(); fail(error) }
        }) texture.release()
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        extentWidth = width; extentHeight = height
        if (!closed.get()) { worker.removeCallbacks(render); worker.post(render) }
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        // EGL must relinquish the native buffer before the SurfaceTexture is released.
        if (!worker.post {
            if (session?.texture === texture) { session?.close(); session = null }
            else texture.release()
        }) texture.release()
        return false
    }

    private fun fail(error: Throwable) {
        SpaceCompassErrorLog.record(context.applicationContext, "starfield:renderer", error)
        session?.close(); session = null
    }

    fun release() {
        if (closed.compareAndSet(false, true)) {
            worker.removeCallbacks(render)
            worker.post { session?.close(); session = null; thread.quitSafely() }
        }
    }

    private class Session(application: Context, val texture: SurfaceTexture) {
        private var closed = false
        val display: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        var context: EGLContext = EGL14.EGL_NO_CONTEXT
            private set
        var surface: EGLSurface = EGL14.EGL_NO_SURFACE
            private set
        lateinit var renderer: SpaceCompassStarGlRenderer
            private set
        init {
            try {
                check(display != EGL14.EGL_NO_DISPLAY && EGL14.eglInitialize(display, IntArray(2), 0, IntArray(2), 0))
                val attributes = intArrayOf(EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE)
                val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
                val count = IntArray(1)
                check(EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0) && count[0] > 0)
                val config = requireNotNull(configs[0])
                context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT,
                    intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
                check(context != EGL14.EGL_NO_CONTEXT)
                surface = EGL14.eglCreateWindowSurface(display, config, texture, intArrayOf(EGL14.EGL_NONE), 0)
                check(surface != EGL14.EGL_NO_SURFACE && EGL14.eglMakeCurrent(display, surface, surface, context))
                renderer = SpaceCompassStarGlRenderer(application)
            } catch (error: Throwable) { close(); throw error }
        }
        fun close() {
            if (closed) return
            closed = true
            if (display != EGL14.EGL_NO_DISPLAY) {
                if (surface != EGL14.EGL_NO_SURFACE && context != EGL14.EGL_NO_CONTEXT &&
                    EGL14.eglMakeCurrent(display, surface, surface, context) && ::renderer.isInitialized) renderer.release()
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (surface != EGL14.EGL_NO_SURFACE) { EGL14.eglDestroySurface(display, surface); surface = EGL14.EGL_NO_SURFACE }
                if (context != EGL14.EGL_NO_CONTEXT) { EGL14.eglDestroyContext(display, context); context = EGL14.EGL_NO_CONTEXT }
                EGL14.eglReleaseThread(); EGL14.eglTerminate(display)
            }
            texture.release()
        }
    }
}
