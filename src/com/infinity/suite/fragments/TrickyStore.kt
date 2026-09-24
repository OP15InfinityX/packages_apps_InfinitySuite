/*
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package com.infinity.suite.fragments

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.regex.Pattern
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrickyStore : SettingsPreferenceFragment() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val keyboxPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val bytes = requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: ByteArray(0)
                    val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    Settings.Secure.putString(
                        requireContext().contentResolver,
                        KEYBOX_KEY,
                        encoded
                    )
                    killGms()
                    toast(getString(R.string.ts_keybox_imported))
                    refreshStatus()
                } catch (e: Exception) {
                    toast(getString(R.string.ts_failed, e.message ?: ""))
                }
            }
        }
    }

    private val targetPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val text = requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(StandardCharsets.UTF_8)
                    } ?: ""
                    Settings.Secure.putString(
                        requireContext().contentResolver,
                        TARGET_KEY,
                        text
                    )
                    toast(getString(R.string.ts_target_list_imported))
                    refreshStatus()
                } catch (e: Exception) {
                    toast(getString(R.string.ts_failed, e.message ?: ""))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.tricky_store)

        findPreference<Preference>("ts_download_latest_keybox")?.setOnPreferenceClickListener {
            downloadLatestKeybox()
            true
        }

        findPreference<Preference>("ts_import_keybox")?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            keyboxPicker.launch(intent)
            true
        }

        findPreference<Preference>("ts_delete_keybox")?.setOnPreferenceClickListener {
            showDeleteKeyboxDialog()
            true
        }

        findPreference<Preference>("ts_security_patch")?.setOnPreferenceClickListener {
            showPatchDateDialog()
            true
        }

        findPreference<Preference>("ts_import_targets")?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/*"
            }
            targetPicker.launch(intent)
            true
        }

        refreshStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        checkLoadedKeyboxStatusOnOpen()
    }

    private fun refreshStatus() {
        val keyboxExists = !Settings.Secure.getString(
            requireContext().contentResolver, KEYBOX_KEY
        ).isNullOrEmpty()

        val targetContent = Settings.Secure.getString(requireContext().contentResolver, TARGET_KEY)
        val targetCount = if (!targetContent.isNullOrEmpty()) {
            targetContent.lines().count { it.isNotBlank() }
        } else 0

        findPreference<Preference>("ts_import_keybox")?.summary =
            if (keyboxExists) getString(R.string.ts_keybox_installed)
            else getString(R.string.ts_no_keybox)

        findPreference<Preference>("ts_delete_keybox")?.isEnabled = keyboxExists

        findPreference<Preference>("ts_manage_targets")?.summary =
            if (targetCount > 0) getString(R.string.ts_target_apps_count, targetCount)
            else getString(R.string.ts_no_targets)

        val patchDate = Settings.Secure.getString(requireContext().contentResolver, PATCH_KEY)
        findPreference<Preference>("ts_security_patch")?.summary =
            if (!patchDate.isNullOrEmpty()) patchDate
            else getString(R.string.ts_no_patch)

        findPreference<Preference>("ts_verification_mode")?.summary = buildVerificationSummary()
    }

    private fun buildVerificationSummary(): String {
        val content = Settings.Secure.getString(
            requireContext().contentResolver, TARGET_KEY
        ) ?: return getString(R.string.ts_verification_mode_auto)

        var auto = 0; var cert = 0; var leaf = 0
        content.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) when {
                trimmed.endsWith("!") -> cert++
                trimmed.endsWith("?") -> leaf++
                else                  -> auto++
            }
        }

        if (auto == 0 && cert == 0 && leaf == 0)
            return getString(R.string.ts_verification_mode_auto)

        return buildList {
            if (auto > 0) add(getString(R.string.ts_verification_auto_count, auto))
            if (cert > 0) add(getString(R.string.ts_verification_cert_count, cert))
            if (leaf > 0) add(getString(R.string.ts_verification_leaf_count, leaf))
        }.joinToString(" · ")
    }

    private fun showDeleteKeyboxDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.ts_delete_keybox_title)
            .setMessage(R.string.ts_delete_keybox_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                try {
                    Settings.Secure.putString(
                        requireContext().contentResolver, KEYBOX_KEY, "")
                    toast(getString(R.string.ts_keybox_deleted))
                    refreshStatus()
                } catch (e: Exception) {
                    toast(getString(R.string.ts_failed, e.message ?: ""))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showPatchDateDialog() {
        val current = Settings.Secure.getString(requireContext().contentResolver, PATCH_KEY) ?: ""
        val input = android.widget.EditText(requireContext()).apply {
            setText(current)
            hint = getString(R.string.ts_patch_date_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(48, 24, 48, 24)
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.ts_security_patch)
            .setView(input)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.delete) { _, _ ->
                Settings.Secure.putString(
                    requireContext().contentResolver, PATCH_KEY, "")
                refreshStatus()
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = input.text.toString().trim()
                if (value.isNotEmpty() && !value.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
                    toast(getString(R.string.ts_invalid_patch_date))
                    return@setOnClickListener
                }
                Settings.Secure.putString(
                    requireContext().contentResolver, PATCH_KEY, value)
                refreshStatus()
                dialog.dismiss()
            }
            if (current.isEmpty()) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = false
            }
        }

        dialog.show()
    }

    private fun killGms() {
        try {
            val am = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.forceStopPackage(VENDING_PACKAGE)
            am.forceStopPackage(DROIDGUARD_PACKAGE)
            am.forceStopPackage(GMS_PACKAGE)
            am.forceStopPackage(RKPD_PACKAGE)
            // Clear Play Store's cached attestation results so the new
            // keybox/config takes effect immediately (mirrors Specter's gms.sh).
            requireContext().packageManager.clearApplicationUserData(
                VENDING_PACKAGE, null)
        } catch (_: Exception) {}
    }

    private enum class KeyboxStatus { VALID, REVOKED, UNKNOWN }

    private fun downloadLatestKeybox() {
        val downloadPref = findPreference<Preference>("ts_download_latest_keybox") ?: return
        downloadPref.isEnabled = false

        scope.launch {
            val success = withContext(Dispatchers.IO) {
                downloadLatestKeyboxFile()
            }

            toast(
                getString(
                    if (success) R.string.ts_download_keybox_success
                    else R.string.ts_download_keybox_error
                )
            )

            if (success) {
                val file = File(Environment.getExternalStorageDirectory(), "InfinityResources/keybox.xml")
                if (file.exists()) {
                    val xmlContent = withContext(Dispatchers.IO) {
                        try {
                            file.readText(Charsets.UTF_8)
                        } catch (_: Exception) {
                            ""
                        }
                    }
                    if (xmlContent.isNotEmpty()) {
                        val status = checkKeyboxStatus(xmlContent)
                        when (status) {
                            KeyboxStatus.VALID -> toast(getString(R.string.ts_fetched_keybox_valid))
                            KeyboxStatus.REVOKED -> toast(getString(R.string.ts_fetched_keybox_revoked))
                            else -> {}
                        }
                    }
                }
            }

            downloadPref.isEnabled = true
        }
    }

    private fun checkLoadedKeyboxStatusOnOpen() {
        val loadedKeybox = Settings.Secure.getString(requireContext().contentResolver, KEYBOX_KEY)
        if (loadedKeybox.isNullOrEmpty()) return

        scope.launch {
            val decodedXml = withContext(Dispatchers.IO) {
                decodeKeybox(loadedKeybox)
            } ?: return@launch

            val status = checkKeyboxStatus(decodedXml)
            when (status) {
                KeyboxStatus.REVOKED -> toast(getString(R.string.ts_imported_keybox_revoked))
                else -> {}
            }
        }
    }

    private fun decodeKeybox(payload: String): String? {
        val trimmed = payload.trim()
        if (trimmed.startsWith("<")) return trimmed
        return try {
            val decoded = Base64.decode(trimmed, Base64.DEFAULT)
            val asXml = String(decoded, Charsets.UTF_8).trim()
            if (asXml.startsWith("<")) asXml else null
        } catch (_: Exception) { null }
    }

    private fun extractCertSerials(xml: String): List<String> {
        val serials = mutableListOf<String>()
        try {
            val factory = CertificateFactory.getInstance("X.509")
            val matcher = Pattern.compile(
                "-----BEGIN CERTIFICATE-----([\\s\\S]+?)-----END CERTIFICATE-----"
            ).matcher(xml)
            while (matcher.find()) {
                try {
                    val der = Base64.decode(
                        matcher.group(1)!!.replace("\\s".toRegex(), ""), Base64.DEFAULT)
                    val cert = factory.generateCertificate(ByteArrayInputStream(der)) as X509Certificate
                    serials.add(cert.serialNumber.toString(16).uppercase())
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return serials
    }

    private fun fetchRevocationJson(): JSONObject? = try {
        val conn = URL(REVOCATION_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                JSONObject(reader.readText())
            }
        } else null
    } catch (_: Exception) { null }

    private suspend fun checkKeyboxStatus(xmlContent: String): KeyboxStatus = withContext(Dispatchers.IO) {
        try {
            val serials = extractCertSerials(xmlContent)
            if (serials.isEmpty()) return@withContext KeyboxStatus.UNKNOWN

            val json = fetchRevocationJson() ?: return@withContext KeyboxStatus.UNKNOWN

            val entries = json.optJSONObject("entries")
            if (entries != null) {
                for (serial in serials) {
                    val entry = entries.optJSONObject(serial) ?: continue
                    val status = entry.optString("status", "").uppercase()
                    if (status == "REVOKED" || status == "SUSPENDED") {
                        return@withContext KeyboxStatus.REVOKED
                    }
                }
            }

            KeyboxStatus.VALID
        } catch (_: Exception) {
            KeyboxStatus.UNKNOWN
        }
    }

    private fun downloadLatestKeyboxFile(): Boolean {
        val baseDir = Environment.getExternalStorageDirectory()
        val outputDir = File(baseDir, "InfinityResources")
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            return false
        }

        val outputFile = File(outputDir, "keybox.xml")
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(KEYBOX_DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = KEYBOX_DOWNLOAD_TIMEOUT_MS
                readTimeout = KEYBOX_DOWNLOAD_TIMEOUT_MS
                instanceFollowRedirects = true
                doInput = true
            }
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return false
            }

            val startTime = SystemClock.elapsedRealtime()
            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        if (SystemClock.elapsedRealtime() - startTime > KEYBOX_DOWNLOAD_TIMEOUT_MS) {
                            throw IOException("Download timed out")
                        }
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            true
        } catch (_: Exception) {
            if (outputFile.exists()) {
                outputFile.delete()
            }
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.INFINITY

    companion object {
        private const val KEYBOX_KEY = "spoof_trickystore_keybox"
        private const val TARGET_KEY = TrickyStoreAppSettings.TARGET_KEY
        internal const val PATCH_KEY = "spoof_trickystore_patch"
        private const val VENDING_PACKAGE = "com.android.vending"
        private const val DROIDGUARD_PACKAGE = "com.google.android.gms.unstable"
        private const val GMS_PACKAGE = "com.google.android.gms"
        private val KEYBOX_DOWNLOAD_URL: String by lazy {
        String(
        Base64.decode(
            "aHR0cHM6Ly9naXQuZXZvbHV0aW9uLXgub3JnL0V2b1gva2V5Ym94L3Jhdy9icmFuY2gvbWFpbi9rZXlib3gueG1s",
            Base64.DEFAULT
        )
    )
}
        private const val KEYBOX_DOWNLOAD_TIMEOUT_MS = 10_000
        private const val RKPD_PACKAGE = "com.google.android.rkpdapp"
        private const val REVOCATION_URL = "https://android.googleapis.com/attestation/status?encrypted=0"
    }
}
