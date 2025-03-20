package com.provigos.android.presentation.view.activities

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.provigos.android.R
import com.provigos.android.databinding.ActivityLicenseBinding

class LicenseActivity: AppCompatActivity(R.layout.activity_license) {

    private lateinit var binding: ActivityLicenseBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLicenseBinding.inflate(layoutInflater)

        setContentView(binding.root)
        
        binding.continueButton.setOnClickListener {
            finish()
        }
    }
}