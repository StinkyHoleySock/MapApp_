package com.example.mapapp

import android.R
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.mapapp.databinding.ActivityMainBinding
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import org.xmlpull.v1.XmlPullParser
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("da2a62fc-d17b-4ff5-9690-480e7e998219")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setFullScreen()
        initView()

    }

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

    private fun initView() {

        val pointsList = parseFile(this, "test.xml")

        for (point in pointsList) {
            binding.map.map.mapObjects.addPlacemark(
                Point(
                    point.latitude.toDouble(),
                    point.longitude.toDouble()
                )
            )
        }



//        Log.d("develop", "${}")
//        CameraPosition(Point(55, 37)), Animation(Animation.Type.SMOOTH, 0), null
//        binding.tvBattery.text =
//            batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1).toString()
//
//        binding.tvTime.text = java.text.DateFormat.getDateTimeInstance().format(Date());

//        binding.tvNetwork.text = getSystemService(ConnectivityManager::class.java).activeNetwork

//        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
//            this.registerReceiver(null, it)
//        }

    }

}