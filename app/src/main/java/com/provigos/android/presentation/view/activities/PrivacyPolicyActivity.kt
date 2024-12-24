/* MIT License
 *
 * Copyright 2024 Provigos

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.provigos.android.presentation.view.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import com.provigos.android.R
import com.provigos.android.data.SharedPreferenceDataSource
import com.provigos.android.databinding.ActivityPrivacyPolicyBinding

class PrivacyPolicyActivity: AppCompatActivity(R.layout.activity_privacy_policy) {

    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        val view = binding.root
        if(SharedPreferenceDataSource(this).isPrivacyPolicy()) {
            startHealthConnectIntent()
            finish()
        }
        setContentView(view)
        initListeners()
    }

    private fun initListeners() {

        binding.ppCancelButton.setOnClickListener {
            finish()
        }

        binding.ppContinueButton.setOnClickListener {
            if(binding.ppCheckbox.isChecked) {
                SharedPreferenceDataSource(this).setPrivacyPolicy(true)
                startHealthConnectIntent()
                finish()
            } else Toast.makeText(this, "You didn't accept the Privacy Policy",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun startHealthConnectIntent() {
        val healthConnectActivity = Intent()
        healthConnectActivity.action = HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
        this.startActivity(healthConnectActivity)
    }

}