package com.revolution.heyassist

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
 private lateinit var status: TextView
 private val speechRequest = 45
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48,80,48,48) }
  root.addView(TextView(this).apply { text = "Hey Assist"; textSize = 30f })
  status = TextView(this).apply { text = "Tap the microphone or start Hey Assistant listening."; textSize = 18f }
  root.addView(status)
  root.addView(Button(this).apply { text = "🎤 Tap to Speak"; setOnClickListener { speak() } })
  root.addView(Button(this).apply { text = "Start Hey Assistant"; setOnClickListener { startListening() } })
  root.addView(Button(this).apply { text = "Stop Listening"; setOnClickListener { stopService(Intent(this@MainActivity, ListenService::class.java)); status.text = "Stopped" } })
  setContentView(root)
  if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
 }
 private fun speak() {
  if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
   requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 8); return
  }
  val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-PK")
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-PK")
   putExtra(RecognizerIntent.EXTRA_PROMPT, "Say open WhatsApp / WhatsApp kholo")
  }
  try { startActivityForResult(i, speechRequest) } catch (e: Exception) { status.text = "Speech recognition unavailable: ${e.message}" }
 }
 private fun startListening() {
  if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
   requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 8); return
  }
  try { startForegroundService(Intent(this, ListenService::class.java)); status.text = "Listening for Hey Assistant (experimental)" }
  catch (e: Exception) { status.text = "Cannot start: ${e.message}" }
 }
 override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  super.onActivityResult(requestCode,resultCode,data)
  if (requestCode == speechRequest && resultCode == RESULT_OK) {
   val heard = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
   status.text = "Heard: $heard"
   val result = AppLauncher.launch(this,heard)
   Toast.makeText(this,result,Toast.LENGTH_LONG).show()
  }
 }
}