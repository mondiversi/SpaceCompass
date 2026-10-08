package me.mondiversi.spacecompass

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** One textured full-screen quad; phone movement changes uniforms, never rebuilds a bitmap. */
internal class SpaceCompassStarGlRenderer(context: Context) {
    private var program = 0
    private var texture = 0
    private val vertices = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer()
        .apply { put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)); position(0) }
    private val locations = IntArray(8)
    private var position = -1

    init {
        try {
            fun shader(type: Int, source: String): Int {
                val id = glCreateShader(type)
                glShaderSource(id, source); glCompileShader(id)
                val ok = IntArray(1); glGetShaderiv(id, GL_COMPILE_STATUS, ok, 0)
                if (ok[0] == 0) { val message = glGetShaderInfoLog(id); glDeleteShader(id); error(message) }
                return id
            }
            val vertex = shader(GL_VERTEX_SHADER, VERTEX)
            val fragment = try { shader(GL_FRAGMENT_SHADER, FRAGMENT) } catch (error: Throwable) { glDeleteShader(vertex); throw error }
            program = glCreateProgram()
            try { glAttachShader(program, vertex); glAttachShader(program, fragment); glLinkProgram(program) }
            finally { glDeleteShader(vertex); glDeleteShader(fragment) }
            val ok = IntArray(1); glGetProgramiv(program, GL_LINK_STATUS, ok, 0)
            check(ok[0] != 0) { glGetProgramInfoLog(program) }
            position = glGetAttribLocation(program, "position")
            NAMES.forEachIndexed { i, name -> locations[i] = glGetUniformLocation(program, name) }
            val ids = IntArray(1); glGenTextures(1, ids, 0); texture = ids[0]
            glBindTexture(GL_TEXTURE_2D, texture)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            val limit = IntArray(1)
            glGetIntegerv(GL_MAX_TEXTURE_SIZE, limit, 0)
            val bitmap = loadSpaceCompassStarAtlas(context, limit[0])
            try { GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0) } finally { bitmap.recycle() }
            glGenerateMipmap(GL_TEXTURE_2D)
            check(glGetError() == GL_NO_ERROR) { "Star map upload failed" }
        } catch (error: Throwable) { release(); throw error }
    }

    fun draw(width: Int, height: Int, drawing: SpaceCompassStarDrawing?) {
        glViewport(0, 0, width, height)
        glClearColor(0f, 0f, 0f, 0f); glClear(GL_COLOR_BUFFER_BIT)
        if (drawing == null || drawing.opacity <= 0) return
        glUseProgram(program)
        glEnableVertexAttribArray(position); glVertexAttribPointer(position, 2, GL_FLOAT, false, 0, vertices)
        glUniform2f(locations[0], width.toFloat(), height.toFloat())
        val frame = drawing.frame
        glUniform3f(locations[1], (frame.left + frame.width / 2).toFloat(),
            (frame.top + frame.height / 2).toFloat(), spaceCompassSunProjectionFocalLength(frame.height).toFloat())
        fun vector(index: Int, vector: SpaceCompassViewVector) = glUniform3f(locations[index], vector.x.toFloat(), vector.y.toFloat(), vector.z.toFloat())
        vector(2, drawing.right); vector(3, drawing.screenUp); vector(4, drawing.forward); vector(5, drawing.worldUp)
        glUniform1f(locations[6], drawing.opacity)
        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, texture); glUniform1i(locations[7], 0)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        glDisableVertexAttribArray(position)
    }

    fun release() {
        if (texture != 0) { glDeleteTextures(1, intArrayOf(texture), 0); texture = 0 }
        if (program != 0) { glDeleteProgram(program); program = 0 }
    }

    private companion object {
        val NAMES = listOf("size", "lens", "rightEqj", "upEqj", "forwardEqj", "worldUp", "opacity", "map")
        const val VERTEX = "attribute vec2 position; varying mediump vec2 point; void main(){ point=position; gl_Position=vec4(position,0.0,1.0); }"
        const val FRAGMENT = """
            #ifdef GL_FRAGMENT_PRECISION_HIGH
            precision highp float;
            #else
            precision mediump float;
            #endif
            varying mediump vec2 point;
            uniform vec2 size;
            uniform vec3 lens, rightEqj, upEqj, forwardEqj, worldUp;
            uniform float opacity;
            uniform sampler2D map;
            void main() {
                vec2 pixel=vec2((point.x+1.0)*0.5*size.x,(1.0-point.y)*0.5*size.y);
                vec2 delta=vec2((pixel.x-lens.x)/lens.z,-(pixel.y-lens.y)/lens.z);
                vec3 q=forwardEqj+rightEqj*delta.x+upEqj*delta.y;
                float up=(worldUp.z+worldUp.x*delta.x+worldUp.y*delta.y)/length(q);
                if(up<=0.0){ gl_FragColor=vec4(0.0); return; }
                q=normalize(q);
                float ra=(q.x*q.x+q.y*q.y<0.000000000001)?0.0:atan(q.y,q.x);
                vec2 uv=vec2(fract(0.5-ra/6.28318530718),0.5-asin(clamp(q.z,-1.0,1.0))/3.14159265359);
                vec3 color=texture2D(map,uv).rgb;
                float fade=opacity*smoothstep(0.0,0.12,up);
                float alpha=max(max(color.r,color.g),color.b)*fade;
                gl_FragColor=vec4(color*fade,alpha);
            }
        """
    }
}
