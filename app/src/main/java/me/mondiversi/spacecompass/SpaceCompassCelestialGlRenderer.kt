package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** One quad, analytic sphere/rings and a cached texture: no CPU bitmap rebuild per animation frame. */
internal class SpaceCompassCelestialGlRenderer(private val context: Context, private val body: SpaceCompassCelestialBody,
    private val onFailure: (Throwable) -> Unit) : GLSurfaceView.Renderer {
    @Volatile var geometry = SpaceCompassCelestialRotation().geometry()
    @Volatile var viewport = SpaceCompassCelestialViewportState()
    @Volatile var inspectShadows = true
    private var program = 0
    private var texture = 0
    private var positionAttribute = -1
    private val uniformLocations = IntArray(12)
    private var aspect = 1f
    private var failed = false
    private val vertices = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        .apply { put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)); position(0) }
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            glClearColor(0.015f, 0.022f, 0.045f, 1f)
            program = link(VERTEX, FRAGMENT)
            // Handles belong to the current GL context; resolve them once after each link.
            positionAttribute = glGetAttribLocation(program, "position")
            for (index in UNIFORM_NAMES.indices) uniformLocations[index] = glGetUniformLocation(program, UNIFORM_NAMES[index])
            val ids = IntArray(1); glGenTextures(1, ids, 0); texture = ids[0]
            glBindTexture(GL_TEXTURE_2D, texture)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            val bitmap = loadTexture()
            try { GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0) }
            finally { bitmap.recycle() }
            glGenerateMipmap(GL_TEXTURE_2D)
            check(glGetError() == GL_NO_ERROR) { "Celestial texture upload failed" }
        } catch (error: Exception) { failed = true; onFailure(error) }
        catch (error: OutOfMemoryError) { failed = true; onFailure(error) }
    }
    private fun loadTexture(): Bitmap {
        val asset = body.viewerTexture ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(if (body == SpaceCompassCelestialBody.POLARIS) 0xfffff3d6.toInt() else 0xff9c4f3a.toInt()) }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open("celestial/$asset").use { BitmapFactory.decodeStream(it, null, bounds) }
        val graphicsLimit = IntArray(1)
        glGetIntegerv(GL_MAX_TEXTURE_SIZE, graphicsLimit, 0)
        val width = spaceCompassCelestialTextureWidth(bounds.outWidth, graphicsLimit[0])
        val height = (width / 2).coerceAtLeast(1)
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = spaceCompassCelestialTextureSampleSize(bounds.outWidth, width * 2)
        }
        val decoded = context.assets.open("celestial/$asset").use {
            requireNotNull(BitmapFactory.decodeStream(it, null, options)) { "Invalid celestial map: $asset" }
        }
        // GLES2 repeat requires a power-of-two texture; cap memory even for the large NASA Pluto map.
        if (decoded.width == width && decoded.height == height) return decoded
        return try { Bitmap.createScaledBitmap(decoded, width, height, true) }
        finally { decoded.recycle() }
    }
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height); aspect = width.toFloat() / height.coerceAtLeast(1)
    }
    override fun onDrawFrame(gl: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT)
        if (failed || program == 0) return
        val current = geometry
        val framing = viewport
        glUseProgram(program)
        val position = positionAttribute
        glEnableVertexAttribArray(position)
        glVertexAttribPointer(position, 2, GL_FLOAT, false, 0, vertices)
        glUniformMatrix3fv(uniformLocations[0], 1, false, current.matrix(), 0)
        glUniform3f(uniformLocations[1], current.light.x.toFloat(), current.light.y.toFloat(), current.light.z.toFloat())
        glUniform1f(uniformLocations[2], aspect)
        glUniform1f(uniformLocations[3], framing.zoom.toFloat())
        glUniform2f(uniformLocations[4], framing.panX.toFloat(), framing.panY.toFloat())
        glUniform1f(uniformLocations[5],
            (if (body == SpaceCompassCelestialBody.SATURN) 2.65f else 1.30f) / aspect.coerceAtMost(1f))
        glUniform1f(uniformLocations[6], if (body == SpaceCompassCelestialBody.SUN || body == SpaceCompassCelestialBody.POLARIS) 1f else 0f)
        // Visual fill reveals the observer-facing terrain even at new phase. This is not
        // measured illumination: the geometric light vector and reported phase remain untouched.
        glUniform1f(uniformLocations[7], if (inspectShadows) .24f else .012f)
        glUniform1f(uniformLocations[8], if (body == SpaceCompassCelestialBody.SATURN) 1f else 0f)
        glUniform1f(uniformLocations[9], body.textureLongitudeOffset.toFloat())
        glUniform1f(uniformLocations[10], if (body.textureHasUnmappedAreas) 1f else 0f)
        glUniform1i(uniformLocations[11], 0)
        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, texture)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        glDisableVertexAttribArray(position)
    }
    private fun shader(type: Int, source: String): Int {
        val shader = glCreateShader(type); glShaderSource(shader, source); glCompileShader(shader)
        val status = IntArray(1); glGetShaderiv(shader, GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val message = glGetShaderInfoLog(shader); glDeleteShader(shader)
            error("Celestial shader: $message")
        }
        return shader
    }
    private fun link(vertex: String, fragment: String): Int {
        val v = shader(GL_VERTEX_SHADER, vertex); val f = shader(GL_FRAGMENT_SHADER, fragment)
        val result = glCreateProgram(); glAttachShader(result, v); glAttachShader(result, f); glLinkProgram(result)
        glDeleteShader(v); glDeleteShader(f)
        val status = IntArray(1); glGetProgramiv(result, GL_LINK_STATUS, status, 0)
        if (status[0] == 0) { val message = glGetProgramInfoLog(result); glDeleteProgram(result); error(message) }
        return result
    }
    companion object {
        private val UNIFORM_NAMES = arrayOf("bodyToCamera", "light", "aspect", "zoom", "pan", "extent", "emissive", "shadowFill", "rings", "mapOffset", "unmapped", "map")
        private const val VERTEX = """
            attribute vec2 position; varying vec2 point;
            void main() { point = position; gl_Position = vec4(position, 0.0, 1.0); }
        """
        // Body frame: +Z north; +X prime meridian. Raster X increases eastward; offset sets its seam.
        private const val FRAGMENT = """
            precision mediump float;
            varying vec2 point; uniform mat3 bodyToCamera;
            uniform vec3 light; uniform sampler2D map;
            uniform vec2 pan; uniform float zoom;
            uniform float aspect, extent, emissive, rings, mapOffset, unmapped, shadowFill;
            void main() {
                vec2 p = (point - pan) * vec2(aspect, 1.0) * extent / zoom;
                float rr = dot(p,p); float surfaceZ = rr <= 1.0 ? sqrt(max(0.0,1.0-rr)) : -10.0;
                vec3 color = vec3(0.015,0.022,0.045);
                if (rr <= 1.0) {
                    vec3 normal = vec3(p,surfaceZ);
                    vec3 n = vec3(dot(bodyToCamera[0],normal),dot(bodyToCamera[1],normal),dot(bodyToCamera[2],normal));
                    vec2 uv = vec2(fract(atan(n.y,n.x)/6.2831853+mapOffset),acos(clamp(n.z,-1.0,1.0))/3.14159265);
                    vec3 albedo = texture2D(map,uv).rgb;
                    // Unmapped reference-mosaic pixels are unknown terrain, not physical black craters.
                    if (unmapped > 0.5 && dot(albedo,albedo) < 0.0001) albedo = vec3(0.22,0.20,0.18);
                    float diffuse = max(dot(normal,normalize(light)),0.0);
                    float brightness = emissive > 0.5 ? 0.58+0.42*surfaceZ : shadowFill+(1.0-shadowFill)*pow(diffuse,0.65);
                    color = albedo * brightness;
                }
                if (rings > 0.5) {
                    vec3 pole = bodyToCamera[2];
                    if (abs(pole.z) > 0.002) {
                        float z = -(p.x*pole.x+p.y*pole.y)/pole.z;
                        vec3 ringPoint = vec3(p,z); float radius = length(ringPoint);
                        if (radius > 1.24 && radius < 2.30 && (rr > 1.0 || z > surfaceZ)) {
                            float shade = 0.52+0.35*abs(dot(pole,normalize(light)));
                            float bands = 0.72+0.17*sin(radius*110.0)+0.08*sin(radius*310.0);
                            if (radius > 1.94 && radius < 2.02) bands *= 0.18;
                            color = vec3(0.80,0.73,0.59)*shade*bands;
                        }
                    }
                }
                gl_FragColor = vec4(color,1.0);
            }
        """
    }
}
