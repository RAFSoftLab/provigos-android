package com.provigos.android.presentation.view.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.provigos.android.R
import com.provigos.android.presentation.viewmodel.HealthConnectViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class UserInputActivity: AppCompatActivity(R.layout.activity_userinput) {


    private val viewModel by viewModel<HealthConnectViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}