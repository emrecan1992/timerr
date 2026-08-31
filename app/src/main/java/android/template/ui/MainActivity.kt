package android.template.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Calendar

class MyAdminReceiver : DeviceAdminReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "LOCK_SCREEN_NOW") {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (dpm.isAdminActive(ComponentName(context, MyAdminReceiver::class.java))) {
                dpm.lockNow()
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TimerAppScreen(
                    onSetAlarm = { minutes, desc -> setQuickAlarm(this, minutes, desc) },
                    onSetLockTimer = { minutes -> startLockTimer(this, minutes) }
                )
            }
        }
    }
}

fun setQuickAlarm(context: Context, minutesToAdd: Int, description: String) {
    val calendar = Calendar.getInstance().apply { add(Calendar.MINUTE, minutesToAdd) }
    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_HOUR, calendar.get(Calendar.HOUR_OF_DAY))
        putExtra(AlarmClock.EXTRA_MINUTES, calendar.get(Calendar.MINUTE))
        putExtra(AlarmClock.EXTRA_MESSAGE, description)
        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
    }
    context.startActivity(intent)
    Toast.makeText(context, "$minutesToAdd dakika sonrasına alarm kuruldu.", Toast.LENGTH_SHORT).show()
}

fun startLockTimer(context: Context, minutes: Int) {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val compName = ComponentName(context, MyAdminReceiver::class.java)

    if (!dpm.isAdminActive(compName)) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "YouTube izlerken uyuya kaldığınızda ekranı kilitleyebilmek için bu izne ihtiyaç var.")
        }
        context.startActivity(intent)
        Toast.makeText(context, "Lütfen önce yönetici iznini etkinleştirin.", Toast.LENGTH_LONG).show()
        return
    }

    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, MyAdminReceiver::class.java).apply { action = "LOCK_SCREEN_NOW" }
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000)
    am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    Toast.makeText(context, "Uyku modu aktif: $minutes dakika sonra ekran kilitlenecek.", Toast.LENGTH_LONG).show()
}

@Composable
fun TimerAppScreen(onSetAlarm: (Int, String) -> Unit, onSetLockTimer: (Int) -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Hızlı Alarm", "Uyku Modu")
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }, text = { Text(title) })
            }
        }
        Surface(modifier = Modifier.weight(1f).padding(16.dp)) {
            if (selectedTabIndex == 0) AlarmSection(onSetAlarm) else LockScreenSection(onSetLockTimer)
        }
    }
}

@Composable
fun AlarmSection(onSetAlarm: (Int, String) -> Unit) {
    var customMinutes by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Hazır Süreler", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PresetButton("5dk") { customMinutes = "5" }
            PresetButton("10dk") { customMinutes = "10" }
            PresetButton("30dk") { customMinutes = "30" }
            PresetButton("1sa") { customMinutes = "60" }
            PresetButton("2sa") { customMinutes = "120" }
        }
        OutlinedTextField(
            value = customMinutes,
            onValueChange = { customMinutes = it.filter { char -> char.isDigit() } },
            label = { Text("Manuel Süre (Dakika)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Açıklama (Opsiyonel)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                val mins = customMinutes.toIntOrNull() ?: 0
                if (mins > 0) {
                    onSetAlarm(mins, description)
                    customMinutes = ""
                    description = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Alarmı Kur")
        }
    }
}

@Composable
fun LockScreenSection(onSetLockTimer: (Int) -> Unit) {
    var customMinutes by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("YouTube Uyku Modu", style = MaterialTheme.typography.titleMedium)
        Text(
            "Belirlenen süre sonunda telefon otomatik kilitlenir.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PresetButton("15dk") { customMinutes = "15" }
            PresetButton("30dk") { customMinutes = "30" }
            PresetButton("1sa") { customMinutes = "60" }
            PresetButton("2sa") { customMinutes = "120" }
        }
        OutlinedTextField(
            value = customMinutes,
            onValueChange = { customMinutes = it.filter { char -> char.isDigit() } },
            label = { Text("Kilitlenme Süresi (Dakika)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                val mins = customMinutes.toIntOrNull() ?: 0
                if (mins > 0) {
                    onSetLockTimer(mins)
                    customMinutes = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Zamanlayıcıyı Başlat")
        }
    }
}

@Composable
fun PresetButton(text: String, onClick: () -> Unit) {
    ElevatedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
        Text(text)
    }
}
