package com.example.tokoplastik

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import com.example.tokoplastik.data.UserPreferences
import com.example.tokoplastik.ui.auth.AuthActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val userPreferences = UserPreferences(this)
        userPreferences.authToken.asLiveData().observe(this, Observer {
            Toast.makeText(this, it ?: "Token Is Null", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AuthActivity::class.java))
        })

//        finish()
//        startActivity(Intent(this, AuthActivity::class.java))

    }
}