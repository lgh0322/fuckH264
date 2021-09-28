package com.vaca.fuckh264

import android.content.Context
import android.hardware.camera2.*
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import com.vaca.fuckh264.databinding.ActivityMainBinding
import com.vaca.fuckh264.record.VideoRecorder
import com.vaca.fuckh264.record.genData
import com.vaca.fuckh264.record.safeList
import com.vaca.fuckh264.util.PathUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import kotlin.experimental.and

class MainActivity : AppCompatActivity() {

    private var mMediaHead: ByteArray? = null

    lateinit var binding: ActivityMainBinding

    lateinit var mySurface: Surface
    val dataScope = CoroutineScope(Dispatchers.IO)


    private val mCameraId = "0"
    private var mCameraDevice: CameraDevice? = null
    lateinit var mHandler: Handler
    lateinit var mCaptureSession: CameraCaptureSession
    lateinit var mPreviewBuilder: CaptureRequest.Builder
    private var mHandlerThread: HandlerThread? = null
    private val recorderThread by lazy {
        Executors.newFixedThreadPool(3)
    }


    companion object {
        private const val TAG = "StudyCamera"
    }


    private var mMediaDecoder: MediaCodec? = null


    private fun setupDecoder(surface: Surface?, mime: String, width: Int, height: Int): Boolean {
        Log.d(TAG, "setupDecoder surface:$surface mime:$mime w:$width h:$height")
        val format = MediaFormat.createVideoFormat(mime, width, height)
        mMediaDecoder = MediaCodec.createDecoderByType(mime)
//        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1080*1920)
//        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, 1920)
//        format.setInteger(MediaFormat.KEY_MAX_WIDTH, 1080)
//        format.setByteBuffer("csd-0", ByteBuffer.wrap(mSps))
//        format.setByteBuffer("csd-1", ByteBuffer.wrap(mPps))

        mMediaDecoder!!.configure(format, surface, null, 0)
        mMediaDecoder!!.start()
        return true
    }


    var poolx: ByteArray = ByteArray(10000000) {
        0.toByte()
    }

    var poolIndex = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PathUtil.initVar(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        binding.start.setOnClickListener {
            startBackgroundThread()
            start(720, 1280, 8000000, surfaceCallback = {
                mySurface = it
                openCamera()
            })
        }


        binding.end.setOnClickListener {
            isRecording.clear()
            recorderThread.shutdown()
            dataScope.launch {
                delay(1000)
                poolx= add(VideoRecorder.pps,poolx)
                poolx= add(VideoRecorder.sps,poolx)
                File(PathUtil.getPathX("fuck.h264")).writeBytes(poolx!!.copyOfRange(0,poolIndex))
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
        width: Int, height: Int, bitRate: Int, frameRate: Int = 7,
        frameInterval: Int = 5,
        surfaceCallback: (surface: Surface) -> Unit
    ) {
        isRecording.add(1)
        val videoRecorder = VideoRecorder(width, height, bitRate, frameRate,
            frameInterval, isRecording, surfaceCallback, { frame, timeStamp, bufferInfo, data ->
                val byteArray = data.genData()
                val type=byteArray[4].toUByte().toInt().and(0x1f)
                if(type==7){
                    Log.e("gagah","fuckSps")
                }
                else
                if(type==8){
                    Log.e("gagah","fuckPps")
                }
                else
                {
                    Log.e("gagah","fuckYou   ${type}")
                }

                if (poolIndex < 5000000) {
                    System.arraycopy(byteArray, 0, poolx, poolIndex, byteArray.size)
                    poolIndex += byteArray.size
                }
            }) {
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