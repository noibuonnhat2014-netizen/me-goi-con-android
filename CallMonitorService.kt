package vn.megoi.con

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class CallMonitorService : Service() {
    private var requestListener: ValueEventListener? = null
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var roomCode: String? = null
    private var ringing = false

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(9100, monitoringNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val room = intent?.getStringExtra("room") ?: getSharedPreferences("me_goi_con", 0).getString("room", null)
        if (room.isNullOrBlank()) { stopSelf(); return START_NOT_STICKY }
        if (roomCode != room) {
            removeListener()
            roomCode = room
            val ref = FirebaseConfig.database().child("rooms").child(room).child("request")
            requestListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val active = snapshot.child("active").getValue(Boolean::class.java) ?: false
                    val acknowledged = snapshot.child("acknowledged").getValue(Boolean::class.java) ?: false
                    if (active && !acknowledged) startAlarm() else stopAlarm()
                }
                override fun onCancelled(error: DatabaseError) {
                    updateStatusNotification("Mất kết nối Firebase: ${error.message}")
                }
            }
            ref.addValueEventListener(requestListener!!)
        }
        return START_STICKY
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            val monitor = NotificationChannel("monitor_service", "Duy trì kết nối Mẹ Gọi Con", NotificationManager.IMPORTANCE_LOW)
            val alarm = NotificationChannel("mother_alarm", "Báo động từ mẹ", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Cảnh báo lặp lại khi mẹ cần giúp đỡ"
                enableVibration(true)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(monitor)
            manager.createNotificationChannel(alarm)
        }
    }

    private fun monitoringNotification(): Notification =
        NotificationCompat.Builder(this, "monitor_service")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MẸ GỌI CON đang hoạt động")
            .setContentText("Đang theo dõi tín hiệu từ mẹ qua Internet")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun updateStatusNotification(message: String) {
        val n = NotificationCompat.Builder(this, "monitor_service")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MẸ GỌI CON")
            .setContentText(message)
            .setOngoing(true).build()
        getSystemService(NotificationManager::class.java).notify(9100, n)
    }

    @Suppress("DEPRECATION")
    private fun startAlarm() {
        if (ringing) return
        ringing = true
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        try {
            player = MediaPlayer().apply {
                setDataSource(this@CallMonitorService, sound)
                isLooping = true
                setVolume(1f, 1f)
                prepare()
                start()
            }
        } catch (_: Exception) { }
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else (getSystemService(VIBRATOR_SERVICE) as Vibrator)
        if (vibrator?.hasVibrator() == true) {
            val pattern = longArrayOf(0, 700, 350, 700, 900)
            if (Build.VERSION.SDK_INT >= 26) vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            else vibrator?.vibrate(pattern, 0)
        }
        showAlarmNotification()
    }

    private fun showAlarmNotification() {
        val room = roomCode ?: return
        val ackIntent = Intent(this, AcknowledgeReceiver::class.java).putExtra("room", room)
        val ackPending = PendingIntent.getBroadcast(this, 77, ackIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val openIntent = PendingIntent.getActivity(this, 78, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, "mother_alarm")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("MẸ ĐANG CẦN GIÚP ĐỠ!")
            .setContentText("Mẹ vừa nhấn nút GỌI CON. Hãy liên hệ với mẹ.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Mẹ vừa nhấn nút GỌI CON. Chạm ĐÃ NHẬN sau khi tiếp nhận tín hiệu."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true).setAutoCancel(false)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_call, "ĐÃ NHẬN", ackPending)
            .build()
        getSystemService(NotificationManager::class.java).notify(7001, n)
    }

    private fun stopAlarm() {
        if (!ringing) return
        ringing = false
        try { player?.stop() } catch (_: Exception) {}
        player?.release()
        player = null
        vibrator?.cancel()
        getSystemService(NotificationManager::class.java).cancel(7001)
        updateStatusNotification("Đã xác nhận hoặc không có yêu cầu đang chờ")
    }

    private fun removeListener() {
        val room = roomCode ?: return
        requestListener?.let {
            FirebaseConfig.database().child("rooms").child(room).child("request").removeEventListener(it)
        }
        requestListener = null
    }

    override fun onDestroy() {
        removeListener()
        stopAlarm()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
