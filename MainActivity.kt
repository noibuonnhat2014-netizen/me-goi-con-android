package vn.megoi.con

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { AppScreen() }
    }
}

@Composable
private fun AppScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseConfig.database() }
    val prefs = remember { context.getSharedPreferences("me_goi_con", 0) }

    var role by remember { mutableStateOf(prefs.getString("role", "mother") ?: "mother") }
    var code by remember { mutableStateOf(prefs.getString("room", "") ?: "") }
    var connected by remember { mutableStateOf(prefs.getBoolean("connected", false)) }
    var status by remember { mutableStateOf("Chưa kết nối") }
    var busy by remember { mutableStateOf(false) }
    var requestActive by remember { mutableStateOf(false) }
    var ackListener by remember { mutableStateOf<ValueEventListener?>(null) }

    LaunchedEffect(connected, role, code) {
        if (connected && role == "child" && code.length == 8) {
            val service = Intent(context, CallMonitorService::class.java).putExtra("room", code)
            ContextCompat.startForegroundService(context, service)
        }
    }

    DisposableEffect(connected, role, code) {
        if (connected && role == "mother" && code.length == 8) {
            val ref = db.child("rooms").child(code).child("request")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val active = snapshot.child("active").getValue(Boolean::class.java) ?: false
                    val ack = snapshot.child("acknowledged").getValue(Boolean::class.java) ?: false
                    requestActive = active
                    status = when {
                        ack -> "Con đã nhận được tín hiệu ❤️"
                        active -> "Đã gửi tín hiệu – đang chờ con xác nhận"
                        else -> "Đã kết nối với điện thoại con"
                    }
                }
                override fun onCancelled(error: DatabaseError) { status = "Lỗi theo dõi: ${error.message}" }
            }
            ref.addValueEventListener(listener)
            ackListener = listener
            onDispose { ref.removeEventListener(listener); ackListener = null }
        } else onDispose { }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFFFF5964), background = Color(0xFF101216), surface = Color(0xFF20242B)
    )) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF101216)) {
            Column(
                Modifier.fillMaxSize().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(14.dp))
                Text("MẸ GỌI CON", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Một chạm để gọi người thân", color = Color.LightGray)
                if (!connected) {
                    Text("THIẾT LẬP KẾT NỐI", color = Color(0xFFFFA5AA), fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(selected = role == "mother", onClick = { role = "mother" }, label = { Text("Điện thoại mẹ") })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = role == "child", onClick = { role = "child" }, label = { Text("Điện thoại con") })
                    }
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase().filter(Char::isLetterOrDigit).take(8) },
                        label = { Text("Mã ghép đôi 8 ký tự") },
                        singleLine = true
                    )
                    if (role == "mother") {
                        OutlinedButton(onClick = {
                            code = (1..8).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[Random.nextInt(32)] }.joinToString("")
                        }) { Text("TẠO MÃ GHÉP ĐÔI") }
                    }
                    Button(enabled = code.length == 8 && !busy, onClick = {
                        scope.launch {
                            busy = true
                            try {
                                if (auth.currentUser == null) auth.signInAnonymously().await()
                                val room = db.child("rooms").child(code)
                                val uid = auth.currentUser!!.uid
                                if (role == "mother") {
                                    room.child("motherUid").setValue(uid).await()
                                } else {
                                    // Database Rules verify that this room already exists and has no child paired.
                                    // The child cannot read the room before pairing, by design.
                                    room.child("childUid").setValue(uid).await()
                                }
                                prefs.edit().putString("role", role).putString("room", code).putBoolean("connected", true).apply()
                                connected = true
                                status = "Ghép đôi thành công."
                                if (role == "child") {
                                    val service = Intent(context, CallMonitorService::class.java).putExtra("room", code)
                                    ContextCompat.startForegroundService(context, service)
                                }
                            } catch (e: Exception) {
                                status = "Không kết nối được: ${e.localizedMessage}"
                            }
                            busy = false
                        }
                    }) { Text(if (busy) "ĐANG KẾT NỐI…" else "KẾT NỐI") }
                } else if (role == "mother") {
                    Text("Khi mẹ cần giúp đỡ, chỉ cần chạm nút bên dưới.", color = Color.White, fontSize = 19.sp, textAlign = TextAlign.Center)
                    Button(
                        enabled = !requestActive,
                        onClick = {
                            scope.launch {
                                try {
                                    db.child("rooms").child(code).child("request").updateChildren(
                                        mapOf("active" to true, "acknowledged" to false, "sentAt" to System.currentTimeMillis())
                                    ).await()
                                    status = "Đã gửi tín hiệu. Đang chờ con…"
                                } catch (e: Exception) { status = "Gửi thất bại: ${e.localizedMessage}" }
                            }
                        },
                        modifier = Modifier.size(230.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCB2637))
                    ) {
                        Text(if (requestActive) "ĐÃ GỌI\nCON" else "GỌI\nCON", fontSize = 38.sp, lineHeight = 46.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    }
                    Text(status, color = Color(0xFFFFC0C4), textAlign = TextAlign.Center)
                    Text("Nút sẽ mở lại sau khi con xác nhận.", color = Color.LightGray, fontSize = 12.sp)
                } else {
                    Text("ĐIỆN THOẠI CON ĐÃ GHÉP ĐÔI", color = Color(0xFFFFA5AA), fontWeight = FontWeight.Bold)
                    Text("Dịch vụ theo dõi tín hiệu đang chạy khi thông báo thường trực xuất hiện.", color = Color.White, textAlign = TextAlign.Center)
                    Button(onClick = {
                        val service = Intent(context, CallMonitorService::class.java).putExtra("room", code)
                        ContextCompat.startForegroundService(context, service)
                        status = "Đã yêu cầu bật theo dõi."
                    }) { Text("BẬT THEO DÕI LẠI") }
                    Text(status, color = Color.LightGray, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.weight(1f))
                Text("Kết nối Internet • Firebase Spark", color = Color.Gray, fontSize = 12.sp)
                Text("Mã ghép đôi: ${if (connected) code else "Chưa thiết lập"}", color = Color.Gray, fontSize = 12.sp)
                if (connected) {
                    TextButton(onClick = {
                        if (role == "child") context.stopService(Intent(context, CallMonitorService::class.java))
                        prefs.edit().clear().apply()
                        connected = false
                        status = "Đã ngắt kết nối trên máy này."
                    }) { Text("Ngắt ghép đôi trên máy này", color = Color(0xFFFFA5AA)) }
                }
            }
        }
    }
}
