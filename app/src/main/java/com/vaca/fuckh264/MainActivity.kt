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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.*
import java.nio.ByteBuffer
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var dicByteArray: ByteArray
    val dataScope = CoroutineScope(Dispatchers.IO)
    companion object {
        private const val TAG = "StudyCamera"
        private const val FRAME_RATE = 15
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
                setupDecoder(binding.ga.holder.surface, MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720)
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
        val format = MediaFormat.createVideoFormat(mime, width, height)
        mMediaDecoder = MediaCodec.createDecoderByType(mime)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1280 *720)
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, 720)
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, 1280)
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



    private suspend fun readH264FromFile() {
        try {
            val dictionaryStream: InputStream = assets.open("w.h264")
            var len = dictionaryStream.available()
            val fileBytes=ByteArray(len)
            dictionaryStream.read(fileBytes)


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
                            delay(100)
                            break@end
                        }
                    }
                }
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
}