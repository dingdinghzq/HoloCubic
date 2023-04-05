package com.jasonhong.holocubic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.os.Handler
import android.os.Looper
import androidx.viewpager.widget.ViewPager
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.*
import com.microsoft.cognitiveservices.speech.*
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import androidx.lifecycle.Observer
import androidx.activity.viewModels


class MainActivity : AppCompatActivity() {
    private lateinit var porcupineManager: PorcupineManager
    private lateinit var enSpeechSynthesizer: SpeechSynthesizer
    private lateinit var enSpeechRecognizer : SpeechRecognizer
    private lateinit var cnSpeechSynthesizer: SpeechSynthesizer
    private lateinit var cnSpeechRecognizer : SpeechRecognizer
    private lateinit var videoPlayer: ExoPlayer
    private lateinit var audioPlayer: ExoPlayer
    private lateinit var videoPlayerView: PlayerView
    private lateinit var viewPager : ViewPager
    private lateinit var speechTextView: TextView
    private var onAudioEnded: (() -> Unit)? = null
    private var currentLanguage : Language = Language.English

    private val chatGPTViewModel: ChatGPTViewModel by viewModels()

    private val switchBackHandler = Handler(Looper.getMainLooper())

    private val switchBackRunnable = Runnable {
        videoPlayerView.visibility = View.GONE
        videoPlayer.pause()
        speechTextView.visibility = View.GONE
        viewPager.visibility = View.VISIBLE
        enSpeechRecognizer.stopContinuousRecognitionAsync()
        cnSpeechRecognizer.stopContinuousRecognitionAsync()

    }

    enum class Language
    {
        Chinese,
        English
    }

    private fun initAzureSpeechSynthesizer() {
        val speechConfig = SpeechConfig.fromSubscription(AccessKeys.AzureSpeechKey, "westus2")
        speechConfig.speechSynthesisVoiceName = "en-US-SteffanNeural" // "en-US-AnaNeural"
        speechConfig.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Audio16Khz32KBitRateMonoMp3)
        val audioConfig = AudioConfig.fromDefaultMicrophoneInput()
        enSpeechRecognizer = SpeechRecognizer(speechConfig, audioConfig)
        enSpeechSynthesizer = SpeechSynthesizer(speechConfig)

        val cnSpeechConfig = SpeechConfig.fromSubscription(AccessKeys.AzureSpeechKey, "westus2")
        cnSpeechConfig.speechSynthesisVoiceName = "zh-CN-XiaoxiaoNeural"
        cnSpeechConfig.speechRecognitionLanguage = "zh-CN"
        cnSpeechConfig.speechSynthesisLanguage = "zh-CN"
        cnSpeechRecognizer = SpeechRecognizer(cnSpeechConfig, AudioConfig.fromDefaultMicrophoneInput())
        cnSpeechSynthesizer = SpeechSynthesizer(cnSpeechConfig)

    }



    @Throws(PorcupineException::class)
    private fun initPorcupine() {
        val keywordPath = "keywords/Holo-Cubic_en_android_v2_1_0.ppn"
        val chineseKeywordPath = "keywords/shaw-shway-jing_en_android_v2_1_0.ppn"// Replace 'holocubic.ppn' with the name of the keyword file you added to the assets folder.
        porcupineManager = PorcupineManager.Builder()
            .setKeywordPaths(arrayOf(keywordPath, chineseKeywordPath))
            .setSensitivities(floatArrayOf(0.5f, 0.5f))
            .setAccessKey(AccessKeys.PorcupineAccessKey)
            .build(applicationContext, object : PorcupineManagerCallback {
                override fun invoke(keywordIndex: Int) {
                    if (keywordIndex == 0) {
                        speakText("Hello, what can I do for you?", Language.English)
                        currentLanguage = Language.English
                        onAudioEnded = {
                            enSpeechRecognizer.startContinuousRecognitionAsync()
                            switchBackHandler.postDelayed(switchBackRunnable, 25000)
                        }
                    }
                    else {
                        speakText("你好，主人", Language.Chinese)
                        currentLanguage = Language.Chinese

                        onAudioEnded = {
                            cnSpeechRecognizer.startContinuousRecognitionAsync()
                            switchBackHandler.postDelayed(switchBackRunnable, 25000)
                        }
                    }

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
            })

        porcupineManager.start()
    }

    private fun speakText(text: String, language: Language) {
        val result = if (language == Language.English) {
            enSpeechSynthesizer.SpeakText(text)
        } else {
            cnSpeechSynthesizer.SpeakText(text)
        }

        if (result.reason == ResultReason.Canceled) {
            val cancellationDetails = SpeechSynthesisCancellationDetails.fromResult(result)
            Log.e("MainActivity", "Error: ${cancellationDetails.errorDetails}")
        } else {
            switchBackHandler.removeCallbacks(switchBackRunnable)
            playAudioFromByte(result.audioData)
        }
    }

    private fun playAudioFromByte(audioData: ByteArray) {
        val byteArrayDataSource = ByteArrayDataSource(audioData)
        val dataSpec =
            DataSpec(RawResourceDataSource.buildRawResourceUri(C.LENGTH_UNSET))
        byteArrayDataSource.open(dataSpec)

        val dataSourceFactory: DataSource.Factory =
            DataSource.Factory { byteArrayDataSource }
        val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(RawResourceDataSource.buildRawResourceUri(C.LENGTH_UNSET))

        audioPlayer.setMediaSource(audioSource)
        audioPlayer.prepare()
        audioPlayer.play()
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

        audioPlayer = ExoPlayer.Builder(this).build()
        audioPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                if (state == Player.STATE_ENDED) {
                    onAudioEnded?.invoke()
                }
            }
        })

        videoPlayerView.player = videoPlayer
        videoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_ALL

        speechTextView = findViewById<TextView>(R.id.speechTextView)

        // Observe changes to the chatGPTReply LiveData and update the UI
        chatGPTViewModel.chatGPTReply.observe(this, Observer { chatGPTReply ->
            // Update the UI with the chatGPTReply
            speechTextView.setTextColor(Color.WHITE)
            speechTextView.text = chatGPTReply
            speakText(chatGPTReply, currentLanguage)
            onAudioEnded = {
                if (currentLanguage == Language.English) {
                    enSpeechRecognizer.startContinuousRecognitionAsync()
                }
                else
                {
                    cnSpeechRecognizer.startContinuousRecognitionAsync()
                }
                switchBackHandler.postDelayed(switchBackRunnable, 25000)
            }
        })


        enSpeechRecognizer.recognizing.addEventListener { _,event ->
            runOnUiThread {
                videoPlayerView.visibility = View.GONE
                videoPlayer.pause()
                viewPager.visibility = View.GONE
                speechTextView.visibility = View.VISIBLE
                speechTextView.text = event.result.text
                switchBackHandler.removeCallbacks(switchBackRunnable)
            }
        }

        cnSpeechRecognizer.recognizing.addEventListener { _, event ->
            runOnUiThread {
                videoPlayerView.visibility = View.GONE
                videoPlayer.pause()
                viewPager.visibility = View.GONE
                speechTextView.visibility = View.VISIBLE
                speechTextView.text = event.result.text
                switchBackHandler.removeCallbacks(switchBackRunnable)
            }
        }


        enSpeechRecognizer.recognized.addEventListener { _, event ->
            if (event.result.reason == ResultReason.RecognizedSpeech) {
                runOnUiThread {
                    speechTextView.text = event.result.text
                    enSpeechRecognizer.stopContinuousRecognitionAsync()
                    chatGPTViewModel.sendMessageToChatGPT(event.result.text)
                }
            }
        }

        cnSpeechRecognizer.recognized.addEventListener { _, event ->
            if (event.result.reason == ResultReason.RecognizedSpeech) {
                runOnUiThread {
                    speechTextView.text = event.result.text
                    cnSpeechRecognizer.stopContinuousRecognitionAsync()
                    chatGPTViewModel.sendMessageToChatGPT(event.result.text)
                }
            }
        }

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
        enSpeechSynthesizer.close()
        enSpeechRecognizer.close()
        cnSpeechRecognizer.close()
        cnSpeechSynthesizer.close()
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

