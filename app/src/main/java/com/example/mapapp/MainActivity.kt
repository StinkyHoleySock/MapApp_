package com.example.mapapp

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.*
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.PopupMenu.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapapp.TcpClient.OnMessageReceived
import com.example.mapapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.MapObjectTapListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: SharedPreferences
    private var data = ""

    private val client = TcpClient(object : OnMessageReceived {
        override fun messageReceived(message: String?) {
            Log.d("develop", "Received message: $message")
        }

        override fun saveFileInStorage(text: String) {
            val fileName: String = SimpleDateFormat("dd-MM-yyyy_hh:mm:ss", Locale.US).format(Date())
            saveFile(file = "$fileName.xml", text = text)
        }
    })

    //adapter for list of files
    private val ordersAdapter by lazy {
        FilesAdapter() { fileName, view ->
            val popup = PopupMenu(this, view)
            popup.inflate(R.menu.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.open -> {
                        openPointsOnMap(fileName)
                    }
                    R.id.send -> {
                        CoroutineScope(IO).launch {
                            client.sendMessage(fileName)
                        }
                    }
                }
                true
            }
            popup.show()
        }
    }

    //listener for points on the map
    private val pointListener =
        MapObjectTapListener { _, point ->
            when (preferences.getString("test", "")) {
                "wgs84Degree" -> {
                    Toast.makeText(
                        this,
                        "Coordinates in WGS-84 (Degrees): ${point.latitude}, ${point.longitude}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                "wgs84Radian" -> {
                    Toast.makeText(
                        this,
                        "Coordinates in WGS-84 (Radian): ${point.latitude.toRadian()}, ${point.longitude.toRadian()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                "sk42" -> {
                    Toast.makeText(
                        this,
                        "Coordinates in SK-42: ${
                            WGS84ToSK42Meters(
                                point.latitude,
                                point.longitude,
                                1.0
                            ).toList()
                        }",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    Toast.makeText(
                        this, "Wrong coordinate system", Toast.LENGTH_LONG
                    ).show()
                }
            }
            true
        }

    //this function displays points on the map
    private fun openPointsOnMap(fileName: String) {

        binding.map.map.mapObjects.clear()
        try {
            val pointsList = parseFile(this, fileName)

            for (point in pointsList) {
                binding.map.map.mapObjects.addPlacemark(
                    Point(
                        point.latitude.toDouble(),
                        point.longitude.toDouble(),
                    )
                ).addTapListener(pointListener)
            }
        } catch (e: Exception) {
            val snackbar = Snackbar.make(binding.root, "Invalid file!", Snackbar.LENGTH_LONG)
            snackbar.view.setBackgroundColor(Color.RED)
            snackbar.show()
            Log.e("parse", "error: ", e)
        }

    }

    //battery level receiver
    private val mBatInfoReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context?, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val battery = level * 100 / scale.toFloat()
            binding.tvBattery.text = "$battery%"
        }
    }

    //full screen mode
    private fun setFullScreen() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController

            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //initialize YandexMaps
        MapKitInitializer.initialize(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        requestPermissions()
        setFullScreen()

        preferences = getSharedPreferences("test", MODE_PRIVATE)

        //choose coordinates system
        binding.menuCoordinates.setOnClickListener {
            MenuDialog().show(supportFragmentManager, "DialogFragment")
        }

        //setup recyclerView
        ordersAdapter.setData(getListOfFiles())
        binding.rvFiles.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = ordersAdapter
        }

        //receiver for battery level
        this.registerReceiver(this.mBatInfoReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val mWifiManager = (this.getSystemService(WIFI_SERVICE) as WifiManager)
        val info = mWifiManager.connectionInfo

        binding.tvNetwork.text = info.ssid
        binding.tvWifiLevel.text = "Wifi level: ${WifiManager.calculateSignalLevel(info.rssi, 5)}/5"

        //run tcp connection
        CoroutineScope(IO).launch {
            Log.d("develop", "starting")
            client.run()
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PackageManager.PERMISSION_GRANTED
        )
    }

    override fun onStart() {
        binding.map.onStart()
        MapKitFactory.getInstance().onStart()
        super.onStart()
    }

    override fun onStop() {
        binding.map.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroy() {
        client.stopClient()
        super.onDestroy()
    }

    private fun getListOfFiles(): MutableList<String> {
        val files: MutableList<String> = mutableListOf()
        try {
            val path = filesDir.toString()
            val directory = File(path)
            val list = directory.listFiles()
            if (list != null) {
                for (i in list.indices) {
                    files.add(list[i].name)
                    Log.d("develop", "FileName:" + list[i].name)
                }
            }

        } catch (e: IOException) {
            return mutableListOf()
        }
        return files
    }

    private fun saveFile(file: String, text: String) {
        try {
            Log.d("develop", "All text in activity: $text")
            val fos: FileOutputStream = openFileOutput(file, MODE_PRIVATE)
            fos.write(text.toByteArray())
            fos.close()
            Log.d("develop", "Saved!: $text")
            val listOfFiles = getListOfFiles()
            ordersAdapter.setData(listOfFiles)
        } catch (e: Exception) {
            Log.e("TCP", "SAVE_FILE: Error", e)
        }
    }

    private fun readFile(file: String) {
        val path = filesDir.toString()
        Log.d("develop", "Path: $path")
        val directory = File(path)
        val files = directory.listFiles()
        Log.d("develop", "Size: " + (files?.size ?: -1))
        if (files != null) {
            for (i in files.indices) {
                Log.d("develop", "FileName:" + files[i].name)
            }
        }
        var text = ""
        try {
            val fis: FileInputStream = openFileInput(file)
            val buffer: ByteArray = byteArrayOf()
            fis.read(buffer)
            fis.close()
            text = buffer.toString()
//            Log.d("develop", "text read: $text")
        } catch (e: Exception) {
            Log.e("TCP", "READ_FILE: Error", e)
        }
    }
}