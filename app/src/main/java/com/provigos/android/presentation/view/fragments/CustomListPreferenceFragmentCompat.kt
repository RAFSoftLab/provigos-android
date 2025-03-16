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
package com.provigos.android.presentation.view.fragments

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.provigos.android.data.model.custom.CustomItemModel
import com.provigos.android.presentation.view.activities.EditActivity
import com.provigos.android.presentation.viewmodel.DashboardViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class CustomListPreferenceFragmentCompat: PreferenceFragmentCompat() {

    private val mDashboardViewModel: DashboardViewModel by viewModel<DashboardViewModel>(ownerProducer = { requireActivity() } )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        val customList = emptyList<CustomItemModel>()

        preferenceManager.createPreferenceScreen(requireContext()).apply {
            customList.forEach { item ->
                addPreference(createPreference(item))
            }
        }
    }

    private fun createPreference(item: CustomItemModel): Preference {
        return Preference(requireContext()).apply {
            title = item.name
            summary = item.label
            setOnPreferenceClickListener {
                startActivity(Intent(requireActivity(), EditActivity::class.java).apply {
                    putExtra("name", item.name)
                    putExtra("label", item.label)
                    putExtra("units", item.units)
                    putExtra("operation", item.operation)
                })
                parentFragmentManager.popBackStack()
                true
            }
        }
    }
}