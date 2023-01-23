package com.example.mapapp

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.example.mapapp.databinding.ChooseSystemDialogBinding


class MenuDialog : DialogFragment() {

    private var _binding: ChooseSystemDialogBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = ChooseSystemDialogBinding.inflate(inflater, container, false)
        preferences = context?.getSharedPreferences("test", AppCompatActivity.MODE_PRIVATE)!!

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (preferences.getString("test", "wgs84Degree")) {
            "wgs84Degree" -> {
                binding.wgs84Degree.isChecked = true
            }
            "wgs84Radian" -> {
                binding.wgs84Radian.isChecked = true
            }
            "sk42" -> {
                binding.sk42.isChecked = true
            }
            else -> {
                binding.wgs84Degree.isChecked = true
            }
        }

        binding.wgs84Degree.setOnClickListener {
            Log.d("develop", "wgs84Degree")
            preferences.edit()
                .putString("test", "wgs84Degree")
                .apply()
        }
        binding.wgs84Radian.setOnClickListener {
            Log.d("develop", "wgs84Radian")
            preferences.edit()
                .putString("test", "wgs84Radian")
                .apply()
        }
        binding.sk42.setOnClickListener {
            Log.d("develop", "sk42")
            binding.sk42.isChecked = true
            preferences.edit()
                .putString("test", "sk42")
                .apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}