package com.vaca.fuckh264

import android.media.MediaCodec
import android.media.MediaFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import com.vaca.fuckh264.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*

class MainActivity : AppCompatActivity() {
    lateinit var dicByteArray: ByteArray
    val dataScope = CoroutineScope(Dispatchers.IO)
    companion object {
        private const val TAG = "StudyCamera"
        private const val FRAME_RATE = 15
        private const val REMOTE_HOST = "192.168.1.105"
        private const val REMOTE_HOST_PORT: Short = 5000
        private const val H264FILE = "/sdcard/test.h264"
    }


    private var mMediaDecoder: MediaCodec? = null
    private var mFrameIndex = 0

    lateinit var binding:ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    /*    val dictionaryStream: InputStream = assets.open("w.h264")
        val size: Int = dictionaryStream.available()
        dicByteArray = ByteArray(size)
        dictionaryStream.read(dicByteArray)
*/

        val holder=binding.ga.holder
        holder.addCallback(object:SurfaceHolder.Callback{
            override fun surfaceCreated(p0: SurfaceHolder) {

            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
                setupDecoder(binding.ga.holder.surface, "video/avc", 1280, 720)
                dataScope.launch {
                    readH264FromFile()
                }
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {

            }

        })




    }

    private fun setupDecoder(surface: Surface?, mime: String, width: Int, height: Int): Boolean {
        Log.d(TAG, "setupDecoder surface:$surface mime:$mime w:$width h:$height")
        val mediaFormat = MediaFormat.createVideoFormat(mime, width, height)
        mMediaDecoder = MediaCodec.createDecoderByType(mime)
        if (mMediaDecoder == null) {
            Log.e("DecodeActivity", "createDecoderByType fail!")
            return false
        }

        mMediaDecoder!!.configure(mediaFormat, surface, null, 0)
        mMediaDecoder!!.start()
        return true
    }



    private fun readH264FromFile() {
        try {
            val dictionaryStream: InputStream = assets.open("w.h264")
            var len = 0
            val fis = dictionaryStream
            val buf = ByteArray(1024)
            while (fis.read(buf).also { len = it } > 0) {
                offerDecoder(buf, len)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return
    }

    private fun offerDecoder(input: ByteArray, length: Int) {
        try {
            val inputBuffers = mMediaDecoder!!.inputBuffers
            val inputBufferIndex = mMediaDecoder!!.dequeueInputBuffer(-1)
            if (inputBufferIndex >= 0) {
                val inputBuffer = inputBuffers[inputBufferIndex]
                val timestamp = (mFrameIndex++ * 1000000 / FRAME_RATE).toLong()
                Log.d(TAG, "offerDecoder timestamp: $timestamp inputSize: $length bytes")
                inputBuffer.clear()
                inputBuffer.put(input, 0, length)
                mMediaDecoder!!.queueInputBuffer(inputBufferIndex, 0, length, timestamp, 0)
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
}