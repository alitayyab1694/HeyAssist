package com.revolution.heyassist

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
 private val bg = Color.rgb(10,15,28)
 private val panel = Color.rgb(23,31,48)
 private val muted = Color.rgb(153,166,188)
 private val accent = Color.rgb(100,114,255)
 private lateinit var status: TextView
 private lateinit var history: LinearLayout
 private lateinit var modeButton: TextView
 private lateinit var commandInput: EditText
 private val prefs by lazy { getSharedPreferences("heyassist", MODE_PRIVATE) }
 private var tts: TextToSpeech? = null
 private var ttsReady = false
 private val speechRequest = 45

 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.statusBarColor = bg
  window.navigationBarColor = bg
  window.decorView.systemUiVisibility = 0
  tts = TextToSpeech(this,this)
  buildUi()
  if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),7)
 }
 private fun dp(n:Int) = (resources.displayMetrics.density*n).toInt()
 private fun shape(color:Int, radius:Int=22): GradientDrawable = GradientDrawable().apply {
  setColor(color); cornerRadius=dp(radius).toFloat()
 }
 private fun text(s:String, size:Float=16f, color:Int=Color.WHITE, bold:Boolean=false): TextView =
  TextView(this).apply { text=s; textSize=size; setTextColor(color); if(bold) setTypeface(null,Typeface.BOLD) }
 private fun column():LinearLayout = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
 private fun pad(v:View, all:Int=16) { v.setPadding(dp(all),dp(all),dp(all),dp(all)) }
 private fun gap(parent:LinearLayout, h:Int=12) { parent.addView(View(this),LinearLayout.LayoutParams(1,dp(h))) }
 private fun panelBox(parent:LinearLayout):LinearLayout {
  val box=column(); box.background=shape(panel); pad(box,18)
  parent.addView(box,LinearLayout.LayoutParams(-1,-2)); return box
 }
 private fun action(label:String, color:Int=accent, onClick:()->Unit):TextView =
  text(label,16f,Color.WHITE,true).apply {
   gravity=Gravity.CENTER; background=shape(color,18); setPadding(dp(16),dp(15),dp(16),dp(15))
   setOnClickListener { onClick() }
  }
 private fun buildUi() {
  val scroll=ScrollView(this).apply { isFillViewport=true; setBackgroundColor(bg); clipToPadding=false }
  val root=column(); root.setPadding(dp(22),dp(28),dp(22),dp(36)); scroll.addView(root)
  root.addView(text("HEY ASSIST  •  VOICE LAUNCHER",12f,Color.rgb(155,166,255),true))
  gap(root,9)
  root.addView(text("Your voice.\nYour shortcuts.",32f,Color.WHITE,true))
  gap(root,10)
  root.addView(text("Open apps instantly in English or Urdu.",14f,muted))
  gap(root,24)

  val hero=panelBox(root)
  val eyebrow=text("VOICE CONTROL",12f,Color.rgb(158,169,255),true); eyebrow.gravity=Gravity.CENTER
  hero.addView(eyebrow); gap(hero,12)
  val mic=action("🎙   TAP TO SPEAK",accent) { speak() }
  mic.textSize=21f; mic.setPadding(dp(14),dp(30),dp(14),dp(30))
  hero.addView(mic); gap(hero,12)
  status=text("Ready when you are",14f,muted); status.gravity=Gravity.CENTER
  hero.addView(status); gap(hero,14)
  modeButton=action("▶  Start Hey Assistant",Color.rgb(44,57,82)) { startListening() }
  hero.addView(modeButton); gap(hero,8)
  hero.addView(action("■  Stop background listening",Color.rgb(38,45,64)) {
   stopService(Intent(this,ListenService::class.java))
   prefs.edit().putBoolean("listening",false).apply()
   updateMode(); status.text="Background listening stopped"
  })
  updateMode()
  gap(root,22)

  root.addView(text("QUICK LAUNCH",13f,muted,true)); gap(root,12)
  val shortcuts=panelBox(root)
  val apps=listOf("WhatsApp","YouTube","Instagram","Chrome","Camera","Settings")
  for (i in apps.indices step 2) {
   val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
   for (name in apps.subList(i,minOf(i+2,apps.size))) {
    val b=action(name,Color.rgb(39,50,75)) { execute(name) }
    val lp=LinearLayout.LayoutParams(0,dp(56),1f).apply { marginEnd=dp(8); bottomMargin=dp(8) }
    row.addView(b,lp)
   }
   shortcuts.addView(row)
  }
  gap(root,22)

  root.addView(text("TYPE A COMMAND",13f,muted,true)); gap(root,12)
  val typed=panelBox(root)
  commandInput=EditText(this).apply {
   hint="e.g. WhatsApp kholo"; setHintTextColor(muted); setTextColor(Color.WHITE)
   textSize=16f; isSingleLine=true
   background=shape(Color.rgb(33,43,64),12); setPadding(dp(14),dp(12),dp(14),dp(12))
  }
  typed.addView(commandInput); gap(typed,10)
  typed.addView(action("Run command  →") {
   val cmd=commandInput.text.toString().trim()
   if(cmd.isNotBlank()) { execute(cmd); commandInput.text.clear() }
  })
  gap(root,22)

  root.addView(text("PREFERENCES",13f,muted,true)); gap(root,12)
  val options=panelBox(root)
  options.addView(text("Speech language",15f,Color.WHITE,true)); gap(options,8)
  val languages=listOf("English (Pakistan)","Urdu (Pakistan)","English (US)")
  val spinner=Spinner(this)
  spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,languages)
  spinner.setSelection(prefs.getInt("language",0))
  spinner.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener {
   override fun onNothingSelected(parent:android.widget.AdapterView<*>?) {}
   override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:View?,position:Int,id:Long) {
    prefs.edit().putInt("language",position).apply()
   }
  }
  options.addView(spinner); gap(options,8)
  val feedback=Switch(this).apply {
   text="Speak confirmation"; setTextColor(Color.WHITE); isChecked=prefs.getBoolean("feedback",false)
   setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("feedback",checked).apply() }
  }
  options.addView(feedback)
  gap(root,22)

  val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
  header.addView(text("RECENT COMMANDS",13f,muted,true),LinearLayout.LayoutParams(0,-2,1f))
  header.addView(text("Clear",13f,Color.rgb(155,166,255)).apply {
   setOnClickListener { prefs.edit().remove("history").apply(); renderHistory() }
  })
  root.addView(header); gap(root,12)
  history=panelBox(root); renderHistory()
  gap(root,16)
  root.addView(text("Tip: Say “Hey Assistant, open WhatsApp”. Background wake listening is experimental and may stop when Android limits microphone access.",12f,muted))
  setContentView(scroll)
 }
 private fun updateMode() {
  modeButton.text=if(prefs.getBoolean("listening",false)) "●  Restart Hey Assistant" else "▶  Start Hey Assistant"
 }
 private fun languageCode():String=when(prefs.getInt("language",0)) { 1->"ur-PK";2->"en-US";else->"en-PK" }
 private fun speak() {
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
   requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),8); return
  }
  val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(RecognizerIntent.EXTRA_LANGUAGE,languageCode())
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,languageCode())
   putExtra(RecognizerIntent.EXTRA_PROMPT,"Say an app name")
  }
  status.text="Listening…"
  try { startActivityForResult(intent,speechRequest) }
  catch(e:Exception) { status.text="Speech recognition unavailable" }
 }
 private fun startListening() {
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
   requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),8); return
  }
  try {
   startForegroundService(Intent(this,ListenService::class.java))
   prefs.edit().putBoolean("listening",true).apply()
   updateMode(); status.text="Wake listening started (experimental)"
  } catch(e:Exception) { status.text="Could not start wake listening" }
 }
 private fun execute(command:String) {
  val result=AppLauncher.launch(this,command)
  status.text=result
  val existing=prefs.getString("history","").orEmpty().split("\n").filter { it.isNotBlank() }
  prefs.edit().putString("history",(listOf(command)+existing.filter { it!=command }).take(8).joinToString("\n")).apply()
  renderHistory()
  if(prefs.getBoolean("feedback",false)&&ttsReady) tts?.speak(result,TextToSpeech.QUEUE_FLUSH,null,"feedback")
  Toast.makeText(this,result,Toast.LENGTH_SHORT).show()
 }
 private fun renderHistory() {
  history.removeAllViews()
  val entries=prefs.getString("history","").orEmpty().split("\n").filter { it.isNotBlank() }
  if(entries.isEmpty()) history.addView(text("No commands yet. Try “Open YouTube”.",14f,muted))
  else entries.forEachIndexed { index,cmd ->
   val item=text("↗   $cmd",15f).apply {
    setPadding(dp(6),dp(10),dp(6),dp(10))
    setOnClickListener { execute(cmd) }
   }
   history.addView(item)
   if(index!=entries.lastIndex) gap(history,4)
  }
 }
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
  super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==speechRequest) {
   if(resultCode==RESULT_OK) {
    val heard=data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
    if(heard.isNotBlank()) execute(heard) else status.text="Nothing recognized"
   } else status.text="Voice input cancelled"
  }
 }
 override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray) {
  super.onRequestPermissionsResult(requestCode,permissions,grantResults)
  if(requestCode==8&&grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED) status.text="Microphone ready. Tap to speak."
 }
 override fun onInit(status:Int) { ttsReady=status==TextToSpeech.SUCCESS; if(ttsReady) tts?.language=Locale.ENGLISH }
 override fun onDestroy() { tts?.stop(); tts?.shutdown(); super.onDestroy() }
}