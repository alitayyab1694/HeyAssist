package com.revolution.heyassist

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.*
import java.util.Locale

class ListenService : Service(), RecognitionListener {
 private var recognizer: SpeechRecognizer? = null
 private val handler = Handler(Looper.getMainLooper())
 private var running = false
 override fun onCreate() {
  super.onCreate()
  val channel = NotificationChannel("listening","Voice listening",NotificationManager.IMPORTANCE_LOW)
  getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  val open = PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
  val notification = Notification.Builder(this,"listening").setContentTitle("Hey Assist")
   .setContentText("Experimental voice listening active").setSmallIcon(android.R.drawable.ic_btn_speak_now)
   .setContentIntent(open).setOngoing(true).build()
  startForeground(101,notification)
  running = true
  handler.post { setupAndListen() }
 }
 private fun setupAndListen() {
  if (!running || !SpeechRecognizer.isRecognitionAvailable(this)) return
  recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }
  listen()
 }
 private fun listen() {
  if (!running) return
  val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(RecognizerIntent.EXTRA_LANGUAGE,when(getSharedPreferences("heyassist",MODE_PRIVATE).getInt("language",0)) { 1->"ur-PK"; 2->"en-US"; else->"en-PK" })
   putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
  }
  try { recognizer?.startListening(intent) } catch (_: Exception) { restart() }
 }
 private fun restart() { if (running) handler.postDelayed({ if (running) { recognizer?.cancel(); listen() } },1200) }
 override fun onResults(results: Bundle?) {
  val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
  val lower = words.lowercase(Locale.ROOT)
  if (lower.contains("hey assistant") || lower.contains("ہی اسسٹنٹ")) {
   val command = lower.substringAfter("hey assistant",lower).replace("ہی اسسٹنٹ","").trim()
   if (command.isNotBlank()) AppLauncher.launch(this,command)
  }
  restart()
 }
 override fun onError(error: Int) { restart() }
 override fun onReadyForSpeech(params: Bundle?) {}
 override fun onBeginningOfSpeech() {}
 override fun onRmsChanged(rmsdB: Float) {}
 override fun onBufferReceived(buffer: ByteArray?) {}
 override fun onEndOfSpeech() {}
 override fun onPartialResults(partialResults: Bundle?) {}
 override fun onEvent(eventType: Int, params: Bundle?) {}
 override fun onBind(intent: Intent?): IBinder? = null
 override fun onDestroy() { running = false; handler.removeCallbacksAndMessages(null); recognizer?.destroy(); super.onDestroy() }
}