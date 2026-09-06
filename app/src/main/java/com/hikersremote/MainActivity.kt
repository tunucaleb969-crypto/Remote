package com.hikersremote

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.flyfishxu.kadb.Kadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private var kadb: Kadb? = null
    private lateinit var etIp: EditText
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etIp = findViewById(R.id.etIp)
        tvStatus = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnConnect).setOnClickListener { connect() }
        findViewById<Button>(R.id.btnPower).setOnClickListener { sendKey(26) }
        findViewById<Button>(R.id.btnVolUp).setOnClickListener { sendKey(24) }
        findViewById<Button>(R.id.btnVolDown).setOnClickListener { sendKey(25) }
        findViewById<Button>(R.id.btnChUp).setOnClickListener { sendKey(166) }
        findViewById<Button>(R.id.btnChDown).setOnClickListener { sendKey(167) }
        findViewById<Button>(R.id.btnUp).setOnClickListener { sendKey(19) }
        findViewById<Button>(R.id.btnDown).setOnClickListener { sendKey(20) }
        findViewById<Button>(R.id.btnLeft).setOnClickListener { sendKey(21) }
        findViewById<Button>(R.id.btnRight).setOnClickListener { sendKey(22) }
        findViewById<Button>(R.id.btnOk).setOnClickListener { sendKey(23) }
        findViewById<Button>(R.id.btnHome).setOnClickListener { sendKey(3) }
        findViewById<Button>(R.id.btnBack).setOnClickListener { sendKey(4) }
        findViewById<Button>(R.id.btnMenu).setOnClickListener { sendKey(82) }
    }

    private fun connect() {
        val ip = etIp.text.toString().trim()
        if (ip.isEmpty()) {
            Toast.makeText(this, "Enter the TV's IP address", Toast.LENGTH_SHORT).show()
            return
        }
        tvStatus.text = "Connecting..."
        lifecycleScope.launch {
            try {
                val connection = withContext(Dispatchers.IO) { Kadb.create(ip, 5555) }
                kadb = connection
                tvStatus.text = "Connected to $ip"
            } catch (e: Exception) {
                tvStatus.text = "Failed: ${e.message}"
                Toast.makeText(this@MainActivity, "Could not connect: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendKey(code: Int) {
        val connection = kadb
        if (connection == null) {
            Toast.makeText(this, "Connect to the TV first", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { connection.shell("input keyevent $code") }
            } catch (e: Exception) {
                tvStatus.text = "Error: ${e.message}"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { kadb?.close() } catch (_: Exception) {}
    }
}
