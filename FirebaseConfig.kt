package vn.megoi.con

import com.google.firebase.database.FirebaseDatabase

object FirebaseConfig {
    const val DATABASE_URL = "https://megoicon-d5104-default-rtdb.asia-southeast1.firebasedatabase.app"
    fun database() = FirebaseDatabase.getInstance(DATABASE_URL).reference
}
