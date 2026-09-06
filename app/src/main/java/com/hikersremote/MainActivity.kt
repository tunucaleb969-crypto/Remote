package com.hikersremote

import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private var connection: AdbConnection? = null
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

    private fun getCrypto(): AdbCrypto {
        val base64 = object : AdbBase64 {
            override fun encodeToString(data: ByteArray): String {
                return Base64.encodeToString(data, Base64.NO_WRAP)
            }
        }
        val privKey = File(filesDir, "adbkey")
        val pubKey = File(filesDir, "adbkey.pub")
        return if (privKey.exists() && pubKey.exists()) {
            AdbCrypto.loadAdbKeyPair(base64, privKey, pubKey)
        } else {
            val crypto = AdbCrypto.generateAdbKeyPair(base64)
            crypto.saveAdbKeyPair(privKey, pubKey)
            crypto
        }
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
                val conn = withContext(Dispatchers.IO) {
                    val crypto = getCrypto()
                    val socket = Socket(ip, 5555)
                    val c = AdbConnection.create(socket, crypto)
                    c.connect()
                    c
                }
                connection = conn
                tvStatus.text = "Connected to $ip"
            } catch (e: Exception) {
                tvStatus.text = "Failed: ${e.message}"
                Toast.makeText(this@MainActivity, "Could not connect: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendKey(code: Int) {
        val conn = connection
        if (conn == null) {
            Toast.makeText(this, "Connect to the TV first", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = conn.open("shell:input keyevent $code")
                    stream.close()
                }
            } catch (e: Exception) {
                tvStatus.text = "Error: ${e.message}"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { connection?.close() } catch (_: Exception) {}
    }
}
