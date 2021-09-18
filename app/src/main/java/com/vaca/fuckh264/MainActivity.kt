package com.vaca.fuckh264

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaFormat
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import com.vaca.fuckh264.databinding.ActivityMainBinding
import com.vaca.fuckh264.record.VideoRecorder
import com.vaca.fuckh264.record.genData
import com.vaca.fuckh264.record.safeList
import com.vaca.fuckh264.util.PathUtil
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    lateinit var mySurface: Surface


    private val mCameraId = "0"
    private var mCameraDevice: CameraDevice? = null
    lateinit var mHandler: Handler
    lateinit var mCaptureSession: CameraCaptureSession
    lateinit var mPreviewBuilder: CaptureRequest.Builder
    private var mHandlerThread: HandlerThread? = null
    private val recorderThread by lazy {
        Executors.newFixedThreadPool(3)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PathUtil.initVar(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ga.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                startBackgroundThread()
                start(1080, 1920, 80000000, surfaceCallback = {

                    Log.e("fuckfuck", "了艰苦撒旦看风景卢卡斯大量进口")
                    mySurface = it
                    openCamera()
                })
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {


            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

            }

        }
    }


    private fun startBackgroundThread() {
        mHandlerThread = HandlerThread("fuck")
        mHandlerThread!!.start()
        mHandler = Handler(mHandlerThread!!.looper)
    }


    private val mCameraDeviceStateCallback: CameraDevice.StateCallback =
        object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                mCameraDevice = camera
                startPreview(camera)
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.e("fuckCamera", "a1")
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.e("fuckCamera", "a2")
                camera.close()
            }

            override fun onClosed(camera: CameraDevice) {
                Log.e("fuckCamera", "a3")
                camera.close()
            }
        }

    private fun openCamera() {
        try {
            val cameraManager =
                getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val isRecording = safeList<Int>()
    val videoFormats = safeList<MediaFormat>()

    fun start(
        width: Int, height: Int, bitRate: Int, frameRate: Int = 24,
        frameInterval: Int = 5,
        surfaceCallback: (surface: Surface) -> Unit
    ) {
        isRecording.add(1)
        val videoRecorder = VideoRecorder(width, height, bitRate, frameRate,
            frameInterval, isRecording, surfaceCallback, { frame, timeStamp, bufferInfo, data ->
                val byteArray = data.genData()
                Log.e("fuckfuck", "gg   " + byteArray.size)

            }){
            videoFormats.add(it)
        }
        recorderThread.execute(videoRecorder)

    }

    private val mSessionStateCallback: CameraCaptureSession.StateCallback =
        object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                mCaptureSession = session
                updatePreview()

            }

            override fun onConfigureFailed(session: CameraCaptureSession) {}
        }

    private fun startPreview(camera: CameraDevice) {
        mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        mPreviewBuilder.addTarget(mySurface)
        camera.createCaptureSession(
            Arrays.asList(mySurface),
            mSessionStateCallback,
            mHandler
        )
    }


    private fun updatePreview() {
        mHandler.post(Runnable {
            try {
                mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        })
    }

    override fun onPause() {
        closeCamera()
        super.onPause()

    }

    private fun closeCamera() {
        mCaptureSession.stopRepeating()
        mCaptureSession.close()
        mCameraDevice!!.close()
        stopBackgroundThread()
    }

    private fun stopBackgroundThread() {
        try {
            if (mHandlerThread != null) {
                mHandlerThread!!.quitSafely()
                mHandlerThread!!.join()
                mHandlerThread = null
            }
            mHandler.removeCallbacksAndMessages(null)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

}