package com.jasonhong.holocubic

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.os.Handler
import android.os.Looper
import androidx.viewpager.widget.ViewPager
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.*
import com.microsoft.cognitiveservices.speech.*


class MainActivity : AppCompatActivity() {
    private lateinit var porcupineManager: PorcupineManager
    private lateinit var speechSynthesizer: SpeechSynthesizer
    private lateinit var videoPlayer: ExoPlayer
    private lateinit var videoPlayerView: PlayerView
    private lateinit var viewPager : ViewPager
    private val switchBackHandler = Handler(Looper.getMainLooper())

    private val switchBackRunnable = Runnable {
        videoPlayerView.visibility = View.GONE
        videoPlayer.pause()
        viewPager.visibility = View.VISIBLE

    }

    private fun initAzureSpeechSynthesizer() {
        val speechConfig = SpeechConfig.fromSubscription(AccessKeys.AzureSpeechKey, "westus2")
        speechConfig.speechSynthesisVoiceName = "en-US-SteffanNeural" // "en-US-AnaNeural"
        speechConfig.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Audio16Khz32KBitRateMonoMp3)
        speechSynthesizer = SpeechSynthesizer(speechConfig)
    }

    @Throws(PorcupineException::class)
    private fun initPorcupine() {
        val keywordPath = "keywords/Holo-Cubic_en_android_v2_1_0.ppn" // Replace 'holocubic.ppn' with the name of the keyword file you added to the assets folder.
        porcupineManager = PorcupineManager.Builder()
            .setKeywordPath(keywordPath)
            .setSensitivity(0.5f)
            .setAccessKey(AccessKeys.PorcupineAccessKey)
            .build(applicationContext, object : PorcupineManagerCallback {
                override fun invoke(keywordIndex: Int) {
                    val result = speechSynthesizer.SpeakText("Hello, what can I do for you?")
                    if (result.reason == ResultReason.Canceled) {
                        val cancellationDetails = SpeechSynthesisCancellationDetails.fromResult(result)
                        Log.e("MainActivity", "Error: ${cancellationDetails.errorDetails}")
                    } else {

                        switchBackHandler.removeCallbacks(switchBackRunnable)
                        val audioData = result.audioData

                        val byteArrayDataSource = ByteArrayDataSource(audioData)
                        val dataSpec =
                            DataSpec(RawResourceDataSource.buildRawResourceUri(C.LENGTH_UNSET))
                        byteArrayDataSource.open(dataSpec)

                        val dataSourceFactory: DataSource.Factory =
                            DataSource.Factory { byteArrayDataSource }
                        val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(RawResourceDataSource.buildRawResourceUri(C.LENGTH_UNSET))

                        val player: ExoPlayer = ExoPlayer.Builder(this@MainActivity).build()
                        player.setMediaSource(audioSource)
                        player.prepare()
                        player.play()

                        videoPlayerView.visibility = View.VISIBLE
                        viewPager.visibility = View.GONE

                        val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.wormhole}")
                        val videoSource = ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this@MainActivity))
                            .createMediaSource(MediaItem.fromUri(videoUri))
                        videoPlayer.setMediaSource(videoSource)
                        videoPlayer.prepare()
                        videoPlayer.playWhenReady = true

                        switchBackHandler.postDelayed(switchBackRunnable, 15000) // 15 seconds

                    }
                }
            })

        porcupineManager.start()
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        } else {
            try {
                initApp()
            } catch (e: PorcupineException) {
                Log.e("MainActivity", "Error initializing Porcupine: ${e.message}")
            }
        }
    }

    private fun initApp() {
        initAzureSpeechSynthesizer()
        try {
            initPorcupine()
        } catch (e: PorcupineException) {
            Log.e("MainActivity", "Error initializing Porcupine: ${e.message}")
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        requestAudioPermission()

        videoPlayerView = findViewById(R.id.videoPlayerView)
        val trackSelector = DefaultTrackSelector(this)
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
        videoPlayer = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        videoPlayerView.player = videoPlayer
        videoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_ALL

        viewPager = findViewById<ViewPager>(R.id.viewPager)

        val imageUrls = listOf(
            "https://cdn.shopify.com/s/files/1/0059/8835/2052/products/Okame_Cherry_FGT_1024x1024.jpg?v=1567030775",
            "https://img.thrfun.com/img/229/574/oriole_tx2.jpg",
            "https://2.bp.blogspot.com/-gj7W66B3aHE/WRg1CdbcNSI/AAAAAAAADAg/fCYhtnehyM4i6Crr1_SKYbVMBWgWzcCKgCEw/s1600/03262017_de+Onrust_01713924.jpg",
            "https://wallpapercave.com/wp/hogH5l6.jpg",
            "https://wallpapercave.com/wp/MVvcYVb.jpg",
            "https://wallpapercave.com/wp/VxY1aG0.jpg"

            // Add more image URLs here
        )

        

        val handler = Handler(Looper.getMainLooper())
        val update = object : Runnable {
            override fun run() {
                val nextItem = (viewPager.currentItem + 1) % imageUrls.size
                viewPager.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 3000) // Change images every 3 seconds
            }
        }

        handler.postDelayed(update, 3000) // Start the slideshow with a 3-second delay

        val adapter = ImageSlideAdapter(this, imageUrls)
        viewPager.adapter = adapter

    }

    override fun onDestroy() {
        super.onDestroy()
        porcupineManager.stop()
        porcupineManager.delete()
        speechSynthesizer.close()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            RECORD_AUDIO_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        initApp()
                    } catch (e: PorcupineException) {
                        Log.e("MainActivity", "Error initializing Porcupine: ${e.message}")
                    }
                } else {
                    Toast.makeText(this, "Permission to record audio is required", Toast.LENGTH_LONG).show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    companion object {
        private const val RECORD_AUDIO_REQUEST_CODE = 101
    }

}

