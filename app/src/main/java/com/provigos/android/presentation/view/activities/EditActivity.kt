/* MIT License
 *
 * Copyright 2025 Provigos

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

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.provigos.android.data.local.SharedPreferenceManager
import com.provigos.android.data.model.custom.CustomItemModel
import com.provigos.android.databinding.ActivityEditBinding
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditActivity: AppCompatActivity() {

    private val viewModel: DashboardViewModel by viewModel<DashboardViewModel>()

    private val sharedPrefs = SharedPreferenceManager.get()
    private lateinit var binding: ActivityEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val name = intent.extras?.getString("name") ?: ""
        val customNameField = binding.customNameField
        customNameField.setText(name)

        val label = intent.extras?.getString("label") ?: ""
        val customLabelField = binding.customLabelField
        customLabelField.setText(label)

        val units = intent.extras?.getString("units") ?: ""
        val customUnitsField = binding.customUnitsField
        customUnitsField.setText(units)

        val customValueField = binding.customValueField

        val customValue = intent.extras?.getBoolean("value")
        customValueField.visibility = if (customValue!!) View.VISIBLE else View.GONE

        binding.customCancel.setOnClickListener {
            finish()
        }

        binding.customContinue.setOnClickListener {
            if (customNameField.text.isNullOrBlank() ||
                customLabelField.text.isNullOrBlank() ||
                customUnitsField.text.isNullOrBlank()
            ) {
                Toast.makeText(this, "All fields are mandatory!", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    val customItem = CustomItemModel(
                        customNameField.text.toString(),
                        customUnitsField.text.toString(),
                        customLabelField.text.toString()
                    )
                    val success = if (customValue) {
                        viewModel.writeCustomData(customItem, customValueField.text.toString())
                            .also {
                                if (it) sharedPrefs.setAllowCustomItem(customItem.name, true)
                            }
                    } else {
                        viewModel.updateCustomKeys(customItem)
                    }
                    if(success) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            }
        }
    }

    private fun setupTextChangedListeners() {
        binding.customNameField.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                TODO("Not yet implemented")
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                TODO("Not yet implemented")
            }
            override fun afterTextChanged(string: Editable?) {
                if(string.isNullOrEmpty()) {
                    binding.customNameLayout.error = "This field is required"
                } else {
                    binding.customNameLayout.error = null
                }
            }
        })

        binding.customLabelField.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                TODO("Not yet implemented")
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                TODO("Not yet implemented")
            }
            override fun afterTextChanged(string: Editable?) {
                if(string.isNullOrEmpty()) {
                    binding.customLabelLayout.error = "This field is required"
                } else {
                    binding.customLabelLayout.error = null
                }
            }
        })

        binding.customUnitsField.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                TODO("Not yet implemented")
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                TODO("Not yet implemented")
            }
            override fun afterTextChanged(string: Editable?) {
                if(string.isNullOrEmpty()) {
                    binding.customUnitsLayout.error = "This field is required"
                } else {
                    binding.customUnitsLayout.error = null
                }
            }
        })
    }
}