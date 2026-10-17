package com.revolution.heyassist

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object AppLauncher {
 fun launch(context: Context, raw: String): String {
  val clean = raw.lowercase().replace(Regex("(?i)hey\\s+assistant"),"")
   .replace(Regex("(?i)\\b(open|launch|start|please|kholo|khol|chalao|chala|karo|kar|app|the)\\b"),"")
   .replace("کھولو","").replace("کھولیں","").replace("چلاؤ","").trim()
  if (clean.isBlank()) return "Say an app name, such as WhatsApp"
  val aliases = mapOf("واٹس ایپ" to "whatsapp","یوٹیوب" to "youtube","انسٹاگرام" to "instagram",
   "کیمرہ" to "camera","فیس بک" to "facebook","گوگل" to "google")
  val name = aliases.entries.fold(clean) { acc, entry -> acc.replace(entry.key,entry.value) }.trim()
  val pm = context.packageManager
  val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  val apps = pm.queryIntentActivities(query,PackageManager.MATCH_ALL)
  val match = apps.firstOrNull { it.loadLabel(pm).toString().lowercase() == name }
   ?: apps.firstOrNull { it.loadLabel(pm).toString().lowercase().contains(name) }
   ?: apps.firstOrNull { name.contains(it.loadLabel(pm).toString().lowercase()) }
  if (match == null) return "No installed app found for: $name"
  val intent = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
   ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(match.activityInfo.packageName,match.activityInfo.name)
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  return try { context.startActivity(intent); "Opening ${match.loadLabel(pm)}" } catch (e: Exception) { "Unable to open ${match.loadLabel(pm)}" }
 }
}