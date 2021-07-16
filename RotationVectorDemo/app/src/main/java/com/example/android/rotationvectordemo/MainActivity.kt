package com.example.android.rotationvectordemo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.OrientationEventListener
import androidx.appcompat.app.AppCompatActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class MainActivity : AppCompatActivity() {

    private lateinit var sensorManager: SensorManager
    private lateinit var gLSurfaceView: GLSurfaceView
    private val renderer = MyRenderer()
    private val listener = MySensorEventListener(renderer.rotationMatrix)
    private lateinit var orientationListener: OrientationEventListener

    class MyOrientationListener(context: Context) : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            Log.d("THIAGO", "Orientation changed: $orientation")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gLSurfaceView = GLSurfaceView(this)
        gLSurfaceView.setRenderer(renderer)
        setContentView(gLSurfaceView)

        orientationListener = MyOrientationListener(this)
        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        }

    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), 10000)
        gLSurfaceView.onResume()

    }

    override fun onPause() {
        sensorManager.unregisterListener(listener)
        gLSurfaceView.onPause()
        super.onPause()
    }

    class MySensorEventListener(private val rotationMatrix: FloatArray) : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent?) {
            // we received a sensor event. it is a good practice to check that we received the proper event
            event?.let { sensorEvent ->
                if (sensorEvent.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    // convert the rotation-vector to a 4x4 matrix. the matrix is interpreted by Open GL as the
                    // inverse of the rotation-vector, which is what we want.
//                    Log.d("THIAGO", "x*sin(ϴ/2) = ${sensorEvent.values[0]}")
//                    Log.d("THIAGO", "y*sin(ϴ/2) = ${sensorEvent.values[1]}")
//                    Log.d("THIAGO", "z*sin(ϴ/2) = ${sensorEvent.values[2]}")
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values)
                }
            }
        }
    }

    class MyRenderer(private val cube: Cube = Cube()) : GLSurfaceView.Renderer {

        val rotationMatrix = FloatArray(16)

        init {
            // initialize the rotation matrix to identity
            rotationMatrix[0] = 1f
            rotationMatrix[4] = 1f
            rotationMatrix[8] = 1f
            rotationMatrix[12] = 1f
        }

        /**
         *The system calls this method on each redraw of the GLSurfaceView
         */
        override fun onDrawFrame(gl: GL10?) {
            gl?.let {
                // clear screen
                it.glClear(GL10.GL_COLOR_BUFFER_BIT)
                // set-up modelview matrix
                it.glMatrixMode(GL10.GL_MODELVIEW)
                it.glLoadIdentity()
                it.glTranslatef(0f, 0f, -3.0f)
                it.glMultMatrixf(rotationMatrix, 0)
                // draw our object
                it.glEnableClientState(GL10.GL_VERTEX_ARRAY)
                it.glEnableClientState(GL10.GL_COLOR_ARRAY)
                cube.draw(it)
            }
        }

        /**
         * The system calls this method when the GLSurfaceView geometry changes
         */
        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            gl?.let {
                // set view-port
                it.glViewport(0, 0, width, height)
                // set projection matrix
                val ratio = width.toFloat() / height
                it.glMatrixMode(GL10.GL_PROJECTION)
                it.glLoadIdentity()
                it.glFrustumf(-ratio, ratio, -1f, 1f, 1f, 10f)
            }
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            // dither is enabled by default, we don't need it
            gl?.glDisable(GL10.GL_DITHER)
            // clear screen in white
            gl?.glClearColor(1f, 1f, 1f, 1f)
        }

        class Cube {
            // initialize our cube
            private var mVertexBuffer: FloatBuffer
            private var mColorBuffer: FloatBuffer
            private var mIndexBuffer: ByteBuffer

            init {
                val vertices = floatArrayOf(
                    -1f, -1f, -1f, 1f, -1f, -1f,
                    1f, 1f, -1f, -1f, 1f, -1f,
                    -1f, -1f, 1f, 1f, -1f, 1f,
                    1f, 1f, 1f, -1f, 1f, 1f
                )
                val colors = floatArrayOf(
                    0f, 0f, 0f, 1f, 1f, 0f, 0f, 1f,
                    1f, 1f, 0f, 1f, 0f, 1f, 0f, 1f,
                    0f, 0f, 1f, 1f, 1f, 0f, 1f, 1f,
                    1f, 1f, 1f, 1f, 0f, 1f, 1f, 1f
                )
                val indices = byteArrayOf(
                    0, 4, 5, 0, 5, 1,
                    1, 5, 6, 1, 6, 2,
                    2, 6, 7, 2, 7, 3,
                    3, 7, 4, 3, 4, 0,
                    4, 7, 6, 4, 6, 5,
                    3, 0, 1, 3, 1, 2
                )

                val vbb: ByteBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
                vbb.order(ByteOrder.nativeOrder())
                mVertexBuffer = vbb.asFloatBuffer()
                mVertexBuffer.put(vertices)
                mVertexBuffer.position(0)

                val cbb: ByteBuffer = ByteBuffer.allocateDirect(colors.size * 4)
                cbb.order(ByteOrder.nativeOrder())
                mColorBuffer = cbb.asFloatBuffer()
                mColorBuffer.put(colors)
                mColorBuffer.position(0)


                mIndexBuffer = ByteBuffer.allocateDirect(indices.size)
                mIndexBuffer.put(indices)
                mIndexBuffer.position(0)
            }

            fun draw(gl: GL10) {
                gl.glEnable(GL10.GL_CULL_FACE)
                gl.glFrontFace(GL10.GL_CW)
                gl.glShadeModel(GL10.GL_SMOOTH)
                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer)
                gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuffer)
                gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE, mIndexBuffer)
            }
        }
    }
}
