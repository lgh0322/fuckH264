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
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import com.vaca.fuckh264.databinding.ActivityMainBinding
import com.vaca.fuckh264.record.*
import com.vaca.fuckh264.util.PathUtil
import kotlinx.coroutines.*
import java.io.File
import java.lang.Runnable
import java.util.*
import java.util.concurrent.Executors

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
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 720 *1280)
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, 1280)
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, 720)
/*        format.setByteBuffer("csd-0", ByteBuffer.wrap(mSps))
        format.setByteBuffer("csd-1", ByteBuffer.wrap(mPps))*/
        if (mMediaDecoder == null) {
            Log.e("DecodeActivity", "createDecoderByType fail!")
            return false
        }

        mMediaDecoder!!.configure(format, surface, null, 0)
        mMediaDecoder!!.start()
        return true
    }





    private fun offerDecoder(input: ByteArray, length: Int) {
        try {
            val inputBuffers = mMediaDecoder!!.inputBuffers
            val inputBufferIndex = mMediaDecoder!!.dequeueInputBuffer(-1)
            if (inputBufferIndex >= 0) {
                val inputBuffer = inputBuffers[inputBufferIndex]
                inputBuffer.put(input, 0, length)
                inputBuffer.clear()
                inputBuffer.limit(length)
                mMediaDecoder!!.queueInputBuffer(inputBufferIndex, 0, length, 0, MediaCodec.BUFFER_FLAG_SYNC_FRAME)
            }
            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = mMediaDecoder!!.dequeueOutputBuffer(bufferInfo, 0)
            while (outputBufferIndex >= 0) {
                Log.d(TAG, "offerDecoder OutputBufSize:" + bufferInfo.size + " bytes written")
                //If a valid surface was specified when configuring the codec,
                //passing true renders this output buffer to the surface.
                mMediaDecoder!!.releaseOutputBuffer(outputBufferIndex, true)
                outputBufferIndex = mMediaDecoder!!.dequeueOutputBuffer(bufferInfo, 0)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }














    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PathUtil.initVar(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val holder=binding.ga.holder
        holder.addCallback(object: SurfaceHolder.Callback{
            override fun surfaceCreated(p0: SurfaceHolder) {

            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
                setupDecoder(binding.ga.holder.surface, MediaFormat.MIMETYPE_VIDEO_AVC, 720, 1280)
                startBackgroundThread()
                start(720, 1280, 8000000, surfaceCallback = {
                    mySurface = it
                    openCamera()
                })
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {

            }

        })

   /*     MainScope().launch {
            delay(5000)
            val fileBytes=pool!!
            start@ for (i in 0 until fileBytes.size) {
                if (fileBytes[i] == 0.toByte() && fileBytes[i + 1] == 0.toByte() && fileBytes[i + 2] == 0.toByte() && fileBytes[i + 3] == 1.toByte()
                ) {
                    end@ for (j in i + 4 until fileBytes.size) {
                        if (fileBytes[j] == 0.toByte() && fileBytes[j + 1] == 0.toByte() && fileBytes[j + 2] == 0.toByte() && fileBytes[j + 3] == 1.toByte()
                        ) {
                            val temp: ByteArray = Arrays.copyOfRange(
                                fileBytes, i, j
                            )
                            offerDecoder(temp,temp.size)
                            break@end
                        }
                    }
                }
            }
        }*/



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
    val audioFormats = safeList<MediaFormat>()


    var pool:ByteArray?=null

    fun start(
        width: Int, height: Int, bitRate: Int, frameRate: Int = 7,
        frameInterval: Int = 5,
        surfaceCallback: (surface: Surface) -> Unit
    ) {
        isRecording.add(1)
        val videoRecorder = VideoRecorder(width, height, bitRate, frameRate,
            frameInterval, isRecording, surfaceCallback, { frame, timeStamp, bufferInfo, data ->
                val fileBytes = data.genData()


                pool=add(pool,fileBytes)

             /*   if(byteArray[0]==0.toByte() && byteArray[1]==0.toByte() && byteArray[2]==0.toByte()&& byteArray[3]==1.toByte()&&byteArray[4]==0x65.toByte()){
                    Log.e("fuckGG","fuckfcuk")
                }
*/

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