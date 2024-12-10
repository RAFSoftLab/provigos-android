package com.provigos.android.presentation.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.provigos.android.R

class SettingsFragment: Fragment(R.layout.fragment_settings) {

    private lateinit var settingsView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingsView = inflater.inflate(R.layout.fragment_settings, container, false)
        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.view_settings, SettingsFragmentCompat())
            .commit()
        return settingsView
    }
}