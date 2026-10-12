package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.GLES20
import java.nio.ByteBuffer

/** Each export owns and releases its context/surface; the display is shared with live GL views. */
internal fun renderSpaceCompassCelestialOffscreen(context: Context, body: SpaceCompassCelestialBody,
    geometry: SpaceCompassCelestialViewGeometry, viewport: SpaceCompassCelestialViewportState,
    current: Boolean, width: Int, height: Int): Bitmap {
    val display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    check(display!=EGL14.EGL_NO_DISPLAY && EGL14.eglInitialize(display,IntArray(2),0,IntArray(2),0))
    val configs=arrayOfNulls<android.opengl.EGLConfig>(1)
    val attributes=intArrayOf(EGL14.EGL_SURFACE_TYPE,EGL14.EGL_PBUFFER_BIT,EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_NONE)
    val count=IntArray(1)
    check(EGL14.eglChooseConfig(display,attributes,0,configs,0,1,count,0) && count[0]>0)
    val config=requireNotNull(configs[0])
    val glContext=EGL14.eglCreateContext(display,config,EGL14.EGL_NO_CONTEXT,
        intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE),0)
    check(glContext!=EGL14.EGL_NO_CONTEXT)
    var surface=EGL14.EGL_NO_SURFACE
    try {
        surface=EGL14.eglCreatePbufferSurface(display,config,intArrayOf(EGL14.EGL_WIDTH,width,EGL14.EGL_HEIGHT,height,EGL14.EGL_NONE),0)
        check(surface!=EGL14.EGL_NO_SURFACE && EGL14.eglMakeCurrent(display,surface,surface,glContext))
        var failure: Throwable?=null
        val renderer=SpaceCompassCelestialGlRenderer(context,body) { failure=it }
        renderer.geometry=geometry;renderer.viewport=viewport;renderer.inspectShadows=current
        renderer.onSurfaceCreated(null,null);failure?.let { throw it }
        renderer.onSurfaceChanged(null,width,height);renderer.onDrawFrame(null)
        val rgba=ByteBuffer.allocateDirect(width*height*4)
        GLES20.glReadPixels(0,0,width,height,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,rgba)
        check(GLES20.glGetError()==GLES20.GL_NO_ERROR)
        val pixels=IntArray(width*height)
        repeat(height) { row -> repeat(width) { column ->
            val offset=(row*width+column)*4
            val r=rgba.get(offset).toInt() and 255;val g=rgba.get(offset+1).toInt() and 255
            val b=rgba.get(offset+2).toInt() and 255;val a=rgba.get(offset+3).toInt() and 255
            pixels[(height-1-row)*width+column]=(a shl 24) or (r shl 16) or (g shl 8) or b
        } }
        // The caption renderer needs a mutable target; the IntArray overload is immutable.
        return Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels,0,width,0,0,width,height)
        }
    } finally {
        EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT)
        if (surface!=EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display,surface)
        EGL14.eglDestroyContext(display,glContext)
        EGL14.eglReleaseThread()
        // eglTerminate would also tear down the display used by the on-screen observatory.
    }
}
