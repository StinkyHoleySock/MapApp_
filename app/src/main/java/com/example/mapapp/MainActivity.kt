package com.example.mapapp

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
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
import com.example.mapapp.databinding.ActivityMainBinding
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.MapObjectTapListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: SharedPreferences
    private var data = ""

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
                        Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            popup.show()
        }
    }

    //listener for points on the map
    private val listener =
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
                        this,
                        "Wrong coordinate system",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            true
        }

    //this function displays points on the map
    private fun openPointsOnMap(fileName: String) {

        binding.map.map.mapObjects.clear()
        val pointsList = parseFile(this, "xml/$fileName")

        for (point in pointsList) {
            binding.map.map.mapObjects.addPlacemark(
                Point(
                    point.latitude.toDouble(),
                    point.longitude.toDouble(),
                )
            ).addTapListener(listener)
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

    //find local files
    private fun listFiles(path: String): MutableList<String> {
        val files: MutableList<String> = mutableListOf()
        val list: Array<String>?
        try {
            list = assets.list(path)
            if (list!!.isNotEmpty()) {
                for (file in list) {
                    files.add(file)
                    Log.d("develop", "files: $file")

                }
            }
        } catch (e: IOException) {
            return mutableListOf()
        }
        return files
    }

    suspend fun client(host: String, port: Int) {

        val socket = Socket(host, port)
        val writer = socket.getOutputStream()
        writer.write(1)
        val reader = Scanner(socket.getInputStream())

        while (true) {
            var input = ""
            input = reader.nextLine()
            if (data.length < 300) data += "\n$input" else data = input
            Log.d("develop", "inp: $input")
        }

        reader.close()
        writer.close()
        socket.close()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //initialize YandexMaps
        MapKitInitializer.initialize(
            apiKey = "18c3a22c-a43b-4e3b-a5e3-4cf4da322159",
            context = this
        )
        CoroutineScope(IO).launch {
            client("192.168.0.2", 4440)
        }

        preferences = getSharedPreferences("test", MODE_PRIVATE)

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PackageManager.PERMISSION_GRANTED
        )


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setFullScreen()

        //setup button for dialog fragment
        binding.menuCoordinates.setOnClickListener {
            MenuDialog().show(supportFragmentManager, "DialogFragment")
        }

        //setup recyclerView
        ordersAdapter.setData(listFiles("xml"))
        binding.rvFiles.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = ordersAdapter
        }

        //receiver for battery level
        this.registerReceiver(this.mBatInfoReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val mWifiManager = (this.getSystemService(WIFI_SERVICE) as WifiManager)
        val info = mWifiManager.connectionInfo
        Log.d("develop", "info: $info")

        binding.tvNetwork.text = info.ssid
        binding.tvWifiLevel.text = "Wifi level: ${WifiManager.calculateSignalLevel(info.rssi, 5)}/5"

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

}