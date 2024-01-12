package com.example.pettysms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_fragment, container, false)

        val appBar = view.findViewById<Toolbar>(R.id.preferences_toolbar)
        (activity as AppCompatActivity).setSupportActionBar(appBar)

        // Load the preferences fragment into the container
        childFragmentManager.beginTransaction()
            .replace(R.id.settingsContainer, SettingsPreferencesFragment())
            .commit()

        return view
    }
}

class SettingsPreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}
