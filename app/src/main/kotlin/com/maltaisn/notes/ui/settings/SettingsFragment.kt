/*
 * Copyright 2020 Nicolas Maltais
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maltaisn.notes.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import com.maltaisn.notes.App
import com.maltaisn.notes.BuildConfig
import com.maltaisn.notes.R
import com.maltaisn.notes.ui.EventObserver
import com.maltaisn.notes.ui.common.ConfirmDialog
import javax.inject.Inject


class SettingsFragment : PreferenceFragmentCompat(), ConfirmDialog.Callback {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: SettingsViewModel by viewModels { viewModelFactory }


    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        (requireContext().applicationContext as App).appComponent.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // Observers
        viewModel.messageEvent.observe(viewLifecycleOwner, EventObserver { messageId ->
            Snackbar.make(view, messageId, Snackbar.LENGTH_SHORT).show()
        })

        viewModel.exportDataEvent.observe(viewLifecycleOwner, EventObserver { data ->
            val title = getString(R.string.pref_data_export_title)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/json"
            intent.putExtra(Intent.EXTRA_SUBJECT, title)
            intent.putExtra(Intent.EXTRA_TITLE, title)
            intent.putExtra(Intent.EXTRA_TEXT, data)
            startActivity(Intent.createChooser(intent, null))
        })
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs, rootKey)

        requirePreference<DropDownPreference>(PreferenceHelper.THEME)
                .setOnPreferenceChangeListener { _, theme ->
                    (requireContext().applicationContext as App).changeTheme(theme as String)
                    true
                }

        requirePreference<Preference>(PreferenceHelper.EXPORT_DATA)
                .setOnPreferenceClickListener {
                    viewModel.exportData()
                    true
                }

        requirePreference<Preference>(PreferenceHelper.CLEAR_DATA)
                .setOnPreferenceClickListener {
                    ConfirmDialog.newInstance(
                            title = R.string.pref_data_clear,
                            message = R.string.pref_data_clear_confirm_message,
                            btnPositive = R.string.action_clear
                    ).show(childFragmentManager, CLEAR_DATA_DIALOG_TAG)
                    true
                }

        requirePreference<Preference>(PreferenceHelper.PRIVACY_POLICY)
                .setOnPreferenceClickListener {
                    // TODO
                    Toast.makeText(requireContext(), "Privacy policy", Toast.LENGTH_SHORT).show()
                    true
                }

        requirePreference<Preference>(PreferenceHelper.VIEW_SOURCE)
                .setOnPreferenceClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse(PreferenceHelper.VIEW_SOURCE_URL)))
                    true
                }

        requirePreference<Preference>(PreferenceHelper.VIEW_LICENSES)
                .setOnPreferenceClickListener {
                    // TODO
                    Toast.makeText(requireContext(), "Open source licenses", Toast.LENGTH_SHORT).show()
                    true
                }

        // Set version name as summary text for version preference
        requirePreference<Preference>(PreferenceHelper.VERSION).summary = BuildConfig.VERSION_NAME
    }

    private fun <T : Preference> requirePreference(key: CharSequence) =
            checkNotNull(findPreference<T>(key)) { "Could not find preference with key '$key'." }


    override fun onDialogConfirmed(tag: String?) {
        if (tag == CLEAR_DATA_DIALOG_TAG) {
            viewModel.clearData()
        }
    }

    companion object {
        private const val CLEAR_DATA_DIALOG_TAG = "clear_data_dialog"
    }

}
