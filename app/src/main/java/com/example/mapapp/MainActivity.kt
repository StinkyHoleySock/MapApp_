package com.example.mapapp

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
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
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: SharedPreferences
    private var data = ""

    private val client = TcpClient(object : OnMessageReceived {
        override fun messageReceived(message: String?) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this@MainActivity, "mes: $message", Toast.LENGTH_SHORT).show()
            }
        }

        override fun saveFileInStorage(text: String) {
            saveFile(file = "saint_p.xml", text = text)
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
                            client.sendMessage(fileName) //fixme: it must be a file, not a filename
                        }
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
        val pointsList = parseFile(this, fileName)

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
//        var list: Array<String>?
        try {
            val path = filesDir.toString()
            Log.d("develop", "Path: $path")
            val directory = File(path)
            val list = directory.listFiles()
            Log.d("develop", "Size: " + (files?.size ?: -1))
            if (list != null) {
                for (i in list.indices) {
                    files.add(list[i].name)
                    Log.d("develop", "FileName:" + list[i].name)
                }
            }

//            Log.d("develop", "path: ${filesDir.absolutePath}")
//            list = assets.list(path)
//            if (list!!.isNotEmpty()) {
//                for (file in list) {
//                    files.add(file)
//                    Log.d("develop", "files: $file")
//
//                }
//            }
        } catch (e: IOException) {
            return mutableListOf()
        }
        return files
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //initialize YandexMaps
        MapKitInitializer.initialize(
            apiKey = "18c3a22c-a43b-4e3b-a5e3-4cf4da322159",
            context = this
        )

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
            readFile("123.xml")
//            MenuDialog().show(supportFragmentManager, "DialogFragment")

        }

        //setup recyclerView
        ordersAdapter.setData(listFiles("xml")) // FIXME: тут должен быть лист имен файлов с директории сохранения
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

        CoroutineScope(IO).launch {
            Log.d("develop", "starting")
            client.run()
        }
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

    private fun saveFile(file: String, text: String) {
        try {
            Log.d("develop", "All text in activity: $text")
            val fos: FileOutputStream = openFileOutput(file, MODE_PRIVATE)
            fos.write(text.toByteArray())
            fos.close()
            Log.d("develop", "Saved!: $text")
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