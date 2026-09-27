package com.aura.jarvis

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import java.util.*

class AURAOverlayService : Service(), TextToSpeech.OnInitListener {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingIcon: ImageView
    private var overlayView: View? = null
    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isAvatarVisible = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        tts = TextToSpeech(this, this)
        createNotificationChannel()
        startForeground(1, createNotification())
        createFloatingIcon()
        startWakeWordListening()
    }

    private fun createNotificationChannel(){
        val channel = NotificationChannel("aura_channel", "AURA Listening", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    private fun createNotification(): Notification {
        return Notification.Builder(this, "aura_channel")
            .setContentTitle("AURA sun raha hai...")
            .setContentText("Wake word: 'AURA' bolo")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    private fun createFloatingIcon(){
        floatingIcon = ImageView(this)
        floatingIcon.setImageResource(android.R.drawable.sym_def_app_icon) // baad me tumhara purple icon lagega
        floatingIcon.setOnClickListener { showAvatar() }

        val params = WindowManager.LayoutParams(
            150, 150,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0; params.y = 300

        // drag karne ke liye touch
        floatingIcon.setOnTouchListener(object: View.OnTouchListener{
            var initialX=0; var initialY=0; var initialTouchX=0f; var initialTouchY=0f
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when(event?.action){
                    MotionEvent.ACTION_DOWN -> { initialX=params.x; initialY=params.y; initialTouchX=event.rawX; initialTouchY=event.rawY; return true }
                    MotionEvent.ACTION_MOVE -> { params.x = initialX + (event.rawX - initialTouchX).toInt(); params.y = initialY + (event.rawY - initialTouchY).toInt(); windowManager.updateViewLayout(floatingIcon, params); return true }
                    MotionEvent.ACTION_UP -> { if(kotlin.math.abs(event.rawX - initialTouchX) < 10) showAvatar(); return true }
                }
                return false
            }
        })
        windowManager.addView(floatingIcon, params)
    }

    private fun showAvatar(){
        if(isAvatarVisible) return
        isAvatarVisible = true
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.webViewClient = WebViewClient()
        // Tumhara AURA Avatar HTML yahan load hoga
        webView.loadUrl("https://claude.ai/artifact/GMTRdF3NJEUo7eyPSPiXY") // pehle ka preview, baad me local file banayenge
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        overlayView = webView
        windowManager.addView(overlayView, params)
        speak("Ji sir, bataiye kya karna hai")

        // 2 sec baad voice input start
        webView.postDelayed({ startVoiceInput() }, 1500)
    }

    private fun hideAvatar(){
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        isAvatarVisible = false
        startWakeWordListening()
    }

    private fun startWakeWordListening(){
        // Simple wake word - "AURA" sunte hi showAvatar()
        // Iska pura code SpeechRecognizer se hota hai, yahan short rakha hai
        // Jab "aura" detect hoga to showAvatar() call hoga
    }

    private fun startVoiceInput(){
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        speechRecognizer?.startListening(intent)
        // Result aane par Gemini ko bhejna + agar "bye" bola to hideAvatar()
    }

    private fun speak(text: String){
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if(status == TextToSpeech.SUCCESS){
            tts.language = Locale("hi","IN")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try{ windowManager.removeView(floatingIcon) }catch(e:Exception){}
        try{ overlayView?.let{ windowManager.removeView(it) } }catch(e:Exception){}
        speechRecognizer?.destroy()
        tts.shutdown()
    }
}
