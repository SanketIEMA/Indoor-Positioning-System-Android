package com.example.indoornavigation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var resultText: TextView
    private lateinit var mapImage: ImageView
    private lateinit var gridView: GridOverlayView

    private lateinit var widthInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var setSizeBtn: Button

    private var selectedX = 0f
    private var selectedY = 0f

    private var mapWidth = 5
    private var mapHeight = 5

    // 📸 Image Picker
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                mapImage.setImageURI(it)
                Toast.makeText(this, "Map Loaded ✅", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI
        val scanBtn = findViewById<Button>(R.id.scanBtn)
        val loadBtn = findViewById<Button>(R.id.loadBtn)
        val exportBtn = findViewById<Button>(R.id.exportBtn)
        val startScanBtn = findViewById<Button>(R.id.startScanBtn)

        resultText = findViewById(R.id.resultText)
        mapImage = findViewById(R.id.mapImage)
        gridView = findViewById(R.id.gridView)

        widthInput = findViewById(R.id.widthInput)
        heightInput = findViewById(R.id.heightInput)
        setSizeBtn = findViewById(R.id.setSizeBtn)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        requestPermissions()

        // 📂 Load Map
        loadBtn.setOnClickListener {
            imagePicker.launch("image/*")
        }

        // 📐 Set Grid Size
        setSizeBtn.setOnClickListener {

            mapWidth = widthInput.text.toString().toIntOrNull() ?: 1
            mapHeight = heightInput.text.toString().toIntOrNull() ?: 1

            gridView.setGridSize(mapWidth, mapHeight)

            Toast.makeText(
                this,
                "Grid set: ${mapWidth} x ${mapHeight}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 📍 Tap on map
        mapImage.setOnTouchListener { _, event ->
            selectedX = event.x
            selectedY = event.y

            Toast.makeText(this, "Point: ($selectedX, $selectedY)", Toast.LENGTH_SHORT).show()
            true
        }

        // 📡 Scan WiFi
        scanBtn.setOnClickListener {
            scanWifi()
        }

        // 📍 Open Google Maps
        startScanBtn.setOnClickListener {
            val uri = Uri.parse("geo:0,0?q=Current Location")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        }

        // 📤 Export CSV
        exportBtn.setOnClickListener {
            exportCSV()
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            100
        )
    }

    private fun scanWifi() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            resultText.text = "Permission required"
            return
        }

        val scanCount = 5   // 🔥 number of scans
        val wifiMap = mutableMapOf<String, MutableList<Int>>()

        for (i in 1..scanCount) {

            wifiManager.startScan()

            Thread.sleep(500)   // 👈 ADD HERE

            val results = wifiManager.scanResults

            for (result in results) {

                val ssid = result.SSID
                val rssi = result.level

                if (!wifiMap.containsKey(ssid)) {
                    wifiMap[ssid] = mutableListOf()
                }

                wifiMap[ssid]?.add(rssi)
            }
        }

        val builder = StringBuilder()

        for ((ssid, values) in wifiMap) {

            val avg = values.average().toInt()

            val line = "$selectedX,$selectedY,$ssid,$avg\n"

            builder.append(line)
            saveToFile(line)
        }

        resultText.text = builder.toString()
    }

    private fun saveToFile(data: String) {
        try {
            val file = File(getExternalFilesDir(null), "wifi_data.csv")
            val isNew = !file.exists()

            val writer = FileWriter(file, true)

            if (isNew) {
                writer.append("X,Y,SSID,RSSI\n")
            }

            writer.append(data)
            writer.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun exportCSV() {

        val file = File(getExternalFilesDir(null), "wifi_data.csv")

        if (!file.exists()) {
            Toast.makeText(this, "No file found", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(Intent.createChooser(intent, "Export CSV"))
    }
}