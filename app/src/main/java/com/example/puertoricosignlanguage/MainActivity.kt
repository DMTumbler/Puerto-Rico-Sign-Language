package com.example.puertoricosignlanguage

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var gifImageView: ImageView
    private lateinit var searchTermTextView: TextView
    private lateinit var voiceButton: FloatingActionButton

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        gifImageView = findViewById(R.id.gifImageView)
        searchTermTextView = findViewById(R.id.searchTermTextView)
        voiceButton = findViewById(R.id.voiceButton)

        // Set up search functionality through keyboard action
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                return@setOnEditorActionListener true
            }
            false
        }

        // Set up search functionality through button click
        searchButton.setOnClickListener {
            performSearch()
        }

        // Set up voice search
        voiceButton.setOnClickListener {
            checkMicrophonePermission()
        }
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla para buscar un GIF")
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

        // Update the search term display (keeping original input for display)
        searchTermTextView.text = searchTerm.replaceFirstChar { it.titlecase() }

        // Normalize the search term to match gif filename format
        val normalizedTerm = searchTerm
            .lowercase() // convert to lowercase
            .replace(" ", "_") // replace spaces with underscores
            .normalize() // remove diacritical marks (tildes)
            .replace(Regex("[^a-z0-9_]"), "") // remove any other special characters

        // Try to find the drawable resource by normalized name
        val resourceId = resources.getIdentifier(normalizedTerm, "drawable", packageName)

        if (resourceId != 0) {
            // If resource is found, display it using Glide to handle GIF animation
            Glide.with(this)
                .asGif()
                .load(resourceId)
                .into(gifImageView)
        } else {
            // If resource is not found, show an error message
            Toast.makeText(
                this,
                "No se encontró ningún GIF con el nombre: $searchTerm",
                Toast.LENGTH_SHORT
            ).show()
            // Clear the current image
            gifImageView.setImageDrawable(null)
        }
    }

    private fun String.normalize(): String {
        return java.text.Normalizer
            .normalize(this, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}
