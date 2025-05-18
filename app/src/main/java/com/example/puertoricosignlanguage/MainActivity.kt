package com.example.puertoricosignlanguage

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources

import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton

import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var searchEditText: AutoCompleteTextView
    private lateinit var searchButton: Button
    private lateinit var gifImageView: ImageView
    private lateinit var searchTermTextView: TextView
    private lateinit var voiceButton: FloatingActionButton
    private val availableGifs = mutableMapOf<String, Int>()

    private fun AutoCompleteTextView.hideKeyboard() {
        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    // Angel y Juan Jimenez
    private val speechRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.get(0)?.let { spokenText ->
                searchEditText.setText(spokenText)
                performSearch()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechRecognition()
        } else {
            Toast.makeText(
                this,
                "Permiso de micrófono necesario para esta función",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Christian
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        gifImageView = findViewById(R.id.gifImageView)
        searchTermTextView = findViewById(R.id.searchTermTextView)
        voiceButton = findViewById(R.id.voiceButton)

        // Load all available GIFs from drawable folder
        loadAvailableGifs()

        // Setup autocomplete with available GIF names
        setupAutocomplete()

        // Set up search functionality through keyboard action
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                searchEditText.hideKeyboard()
                return@setOnEditorActionListener true
            }
            false
        }

        // Set up search functionality through button click
        searchButton.setOnClickListener {
            performSearch()
            searchEditText.hideKeyboard()
        }

        // Set up voice search
        voiceButton.setOnClickListener {
            checkMicrophonePermission()
        }

        // Perform search when a suggestion is clicked
        searchEditText.setOnItemClickListener { _, _, _, _ ->
            performSearch()
            searchEditText.hideKeyboard()
        }
    }

    private fun loadAvailableGifs() {
        try {
            val drawableFields = R.drawable::class.java.fields

            for (field in drawableFields) {
                val resourceName = field.name
                try {
                    val resourceId = field.getInt(null) //

                    if (isLikelyGif(resources, resourceId)) {
                        val displayName = denormalizeString(resourceName)
                        availableGifs[displayName] = resourceId
                    }
                } catch (e: IllegalAccessException) {
                    System.err.println("Error accessing resource ID for ${field.name}: ${e.message}")
                } catch (e: Resources.NotFoundException) {
                    System.err.println("Resource not found for ID obtained from ${field.name}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            System.err.println("Failed to load available GIFs: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun isLikelyGif(res: Resources, resourceId: Int): Boolean {
        try {
            val value = TypedValue()
            res.getValue(resourceId, value, true) // true to resolve references

            // The 'value.string' field will hold the path to the resource file,
            // e.g., "res/drawable-mdpi/my_animation.gif"
            val resourcePath = value.string?.toString()

            return resourcePath != null && resourcePath.endsWith(".gif", ignoreCase = true)
        } catch (e: Resources.NotFoundException) {
            // This can happen if the ID is not a valid file-based resource
            return false
        } catch (e: Exception) {
            System.err.println("Error getting resource value for ID $resourceId: ${e.message}")
            return false
        }
    }

    private fun setupAutocomplete() {
        // Get all display names for autocomplete suggestions
        val suggestions = availableGifs.keys.toList().sorted()

        // Create a custom adapter for autocomplete with limited suggestions
        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            suggestions
        ) {
            override fun getCount(): Int {
                // Limit the number of suggestions shown at a time
                return minOf(5, super.getCount())
            }
        }

        searchEditText.setAdapter(adapter)
        searchEditText.threshold = 1  // Show suggestions after typing 1 character
    }

    // Angel y Juan Jimenez
    private fun checkMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startSpeechRecognition()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECORD_AUDIO
            ) -> {
                Toast.makeText(
                    this,
                    "Se necesita permiso para usar el micrófono",
                    Toast.LENGTH_SHORT
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Angel y Juan Jimenez
    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla para buscar una seña")
        }

        try {
            speechRecognitionLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "El reconocimiento de voz no está disponible en este dispositivo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Juan Colon y Victor
    private fun performSearch() {
        val searchTerm = searchEditText.text.toString().trim()

        if (searchTerm.isEmpty()) {
            Toast.makeText(this, "Por favor, introduce un término de búsqueda", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // First try exact match with entered text
        var resourceId = availableGifs[searchTerm]

        // If not found, try with capitalized version
        if (resourceId == null) {
            resourceId = availableGifs[capitalize(searchTerm)]
        }

        // If still not found, try with normalized version
        if (resourceId == null) {
            val normalizedTerm = normalizeString(searchTerm)
            // Get the denormalized version that might be in our map
            val possibleMatches = availableGifs.keys.filter {
                normalizeString(it) == normalizedTerm
            }

            if (possibleMatches.isNotEmpty()) {
                resourceId = availableGifs[possibleMatches.first()]
            }
        }

        if (resourceId != null) {
            // Update the search term display (keeping original input for display)
            searchTermTextView.apply {
                text = capitalize(searchTerm)
                visibility = View.VISIBLE
            }

            // If resource is found, display it using Glide to handle GIF animation
            Glide.with(this)
                .asGif()
                .load(resourceId)
                .into(gifImageView)

            searchEditText.text.clear()
        } else {
            searchTermTextView.visibility = View.GONE
            // If resource is not found, show an error message
            Toast.makeText(
                this,
                "No se encontró una seña con la palabra: $searchTerm",
                Toast.LENGTH_SHORT
            ).show()
            // Clear the current image
            gifImageView.setImageDrawable(null)
        }
    }

    // Juan Colon y Victor
    private fun normalizeString(str: String?): String {
        var word = str ?: return ""

        // Replace special characters with their standard versions (both upper and lower case)
        word = word
            .replace("á", "a").replace("Á", "a")
            .replace("é", "e").replace("É", "e")
            .replace("í", "i").replace("Í", "i")
            .replace("ó", "o").replace("Ó", "o")
            .replace("ú", "u").replace("Ú", "u")
            .replace("ñ", "n").replace("Ñ", "n")
            .replace("ü", "u").replace("Ü", "u")
            .replace(" ", "_") // replace spaces with underscores
            .replace("[^a-zA-Z0-9_]".toRegex(), "") // Remove any other special characters
            .lowercase() // convert to lowercase after handling special characters

        return word
    }

    // New method to convert normalized names back to display names
    private fun denormalizeString(str: String?): String {
        var word = str ?: return ""

        // Replace underscores with spaces
        word = word.replace("_", " ")

        // Capitalize each wordus
        return capitalize(word) ?: word
    }

    // Juan Colon y Victor
    private fun capitalize(str: String?): String? {
        if (str.isNullOrEmpty()) return str
        val words = str.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val capitalized = StringBuilder()
        for (word in words) {
            capitalized.append(word.substring(0, 1).uppercase(Locale.getDefault()))
                .append(word.substring(1)).append(" ")
        }
        return capitalized.toString().trim()
    }
}