package com.infinity.suite.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import com.infinity.suite.utils.ImageUtils
import com.infinity.suite.utils.PortraitSegmenter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SearchIndexable
class WallpaperDepth : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var customImagePickerPref: Preference? = null
    private var generateSubjectPref: Preference? = null

    private val customImagePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imgUri = result.data?.data
            if (imgUri != null) {
                val cr = requireContext().contentResolver
                val mimeType = cr.getType(imgUri) ?: ""
                val fileName = getFileName(imgUri) ?: ""
                val hasValidExtension = fileName.endsWith(".jpg", ignoreCase = true) ||
                        fileName.endsWith(".jpeg", ignoreCase = true) ||
                        fileName.endsWith(".png", ignoreCase = true)
                val hasValidMime = mimeType == "image/jpeg" || mimeType == "image/png"

                if (hasValidMime || hasValidExtension) {
                    val path = ImageUtils.saveImageToInternalStorage(
                        requireContext(), imgUri, "depthwallpaper", "DEPTH_WALLPAPER_SUBJECT"
                    )
                    if (path != null) {
                        Settings.System.putStringForUser(
                            requireContext().contentResolver,
                            "depth_wallpaper_subject_image_uri",
                            path,
                            UserHandle.USER_CURRENT
                        )
                        Toast.makeText(requireContext(), "Custom wallpaper subject updated", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Only JPG or PNG files are accepted", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val generateSubjectPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val cr = requireContext().contentResolver
                val mimeType = cr.getType(uri) ?: ""
                val fileName = getFileName(uri) ?: ""
                val hasValidExtension = fileName.endsWith(".jpg", ignoreCase = true) ||
                        fileName.endsWith(".jpeg", ignoreCase = true) ||
                        fileName.endsWith(".png", ignoreCase = true)
                val hasValidMime = mimeType == "image/jpeg" || mimeType == "image/png"

                if (hasValidMime || hasValidExtension) {
                    generateCutout(uri)
                } else {
                    Toast.makeText(requireContext(), "Only JPG or PNG files are accepted", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.wallpaper_depth)

        customImagePickerPref = findPreference("depth_wallpaper_subject_image_uri")
        generateSubjectPref = findPreference("depth_wallpaper_generate_subject")

        customImagePickerPref?.setOnPreferenceClickListener {
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
                }
                customImagePicker.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No file picker app found", Toast.LENGTH_LONG).show()
            }
            true
        }

        generateSubjectPref?.setOnPreferenceClickListener {
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
                }
                generateSubjectPicker.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No file picker app found", Toast.LENGTH_LONG).show()
            }
            true
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun generateCutout(uri: Uri) {
        scope.launch {
            Toast.makeText(requireContext(), "Generating subject cutout...", Toast.LENGTH_SHORT).show()
            val savedPath = withContext(Dispatchers.IO) {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap == null) return@withContext null

                    val segmenter = PortraitSegmenter(requireContext())
                    segmenter.init()
                    if (!segmenter.isReady()) {
                        Log.e(TAG, "Segmenter models failed to load")
                        return@withContext null
                    }

                    val foreground = segmenter.segment(bitmap)
                    bitmap.recycle()
                    if (foreground == null) {
                        Log.e(TAG, "Failed to segment image")
                        return@withContext null
                    }

                    val baseDir = Environment.getExternalStorageDirectory()
                    val outputDir = File(baseDir, "InfinityResources")
                    if (!outputDir.exists() && !outputDir.mkdirs()) {
                        foreground.recycle()
                        Log.e(TAG, "Failed to create InfinityResources directory")
                        return@withContext null
                    }

                    // Clean existing cutout files in /sdcard/InfinityResources
                    outputDir.listFiles { _, name ->
                        name.startsWith("DEPTH_WALLPAPER_SUBJECT") && name.endsWith(".png")
                    }?.forEach { it.delete() }

                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val outputFile = File(outputDir, "DEPTH_WALLPAPER_SUBJECT_${stamp}.png")

                    FileOutputStream(outputFile).use { out ->
                        if (!foreground.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                            foreground.recycle()
                            return@withContext null
                        }
                    }
                    foreground.recycle()
                    outputFile.absolutePath
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating cutout", e)
                    null
                }
            }

            if (savedPath != null) {
                Toast.makeText(requireContext(), "Subject image generated and saved to /sdcard/InfinityResources successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Failed to generate depth wallpaper subject image", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        return false
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    companion object {
        private const val TAG = "WallpaperDepth"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.wallpaper_depth) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                return super.getNonIndexableKeys(context)
            }
        }
    }
}
