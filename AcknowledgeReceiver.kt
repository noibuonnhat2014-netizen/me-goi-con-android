package vn.megoi.con

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.database.ServerValue

class AcknowledgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val room = intent.getStringExtra("room") ?: return
        val pending = goAsync()
        FirebaseConfig.database().child("rooms").child(room).child("request")
            .updateChildren(mapOf(
                "active" to false,
                "acknowledged" to true,
                "acknowledgedAt" to ServerValue.TIMESTAMP
            ))
            .addOnCompleteListener { pending.finish() }
    }
}
