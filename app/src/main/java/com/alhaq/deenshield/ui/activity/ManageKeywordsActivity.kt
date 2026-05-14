package com.alhaq.deenshield.ui.activity

import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.ActivityManageKeywordsBinding
import com.alhaq.deenshield.databinding.DialogAddKeywordBinding
import com.alhaq.deenshield.utils.KeywordPackManager


class ManageKeywordsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageKeywordsBinding
    lateinit var savedKeywordsList: ArrayList<String>
    private lateinit var keywordAdapter: KeywordAdapter
    private var oldSize = 0
    private lateinit var keywordPackManager: KeywordPackManager

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { exportKeywords(it) }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { importKeywords(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate
        val sharedPreferences = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val themeStyle = sharedPreferences.getString("theme_style", "default")
        if (themeStyle == "gradient") {
            setTheme(R.style.Theme_DeenShield_Gradient)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityManageKeywordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        keywordPackManager = KeywordPackManager(this)
        savedKeywordsList = intent.getStringArrayListExtra("PRE_SAVED_KEYWORDS") ?: arrayListOf()
        oldSize = savedKeywordsList.size

        keywordAdapter = KeywordAdapter(savedKeywordsList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = keywordAdapter

        binding.confirmSelectionKeywords.setOnClickListener {
            val resultIntent = intent.apply {
                putStringArrayListExtra("SELECTED_KEYWORDS", savedKeywordsList)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
        binding.btnAddKeyword.setOnClickListener { makeAddKeywordDialog() }
        binding.btnExportKeywords.setOnClickListener { showExportDialog() }
        binding.btnImportKeywords.setOnClickListener { showImportDialog() }
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Show a confirmation dialog
                if (oldSize != savedKeywordsList.size) {
                    showExitDialog()
                } else {
                    finish()
                }
            }
        })
    }

    private var exportPackName = "My Keywords"
    private var exportPackDesc = "Custom keyword pack"

    private fun showExportDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_export_keywords, null)
        val packNameInput = dialogView.findViewById<TextInputEditText>(R.id.pack_name_input)
        val packDescInput = dialogView.findViewById<TextInputEditText>(R.id.pack_desc_input)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.export_keywords)
            .setView(dialogView)
            .setPositiveButton(R.string.export) { _, _ ->
                exportPackName = packNameInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: "My Keywords"
                exportPackDesc = packDescInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: "Custom keyword pack"
                exportLauncher.launch("$exportPackName.json")
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showImportDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.import_keywords)
            .setMessage("Import a keyword pack from a JSON file. This will add keywords to your existing list.")
            .setPositiveButton(R.string.import_pack) { _, _ ->
                importLauncher.launch(arrayOf("application/json", "text/plain"))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun exportKeywords(uri: Uri) {
        if (savedKeywordsList.isEmpty()) {
            Toast.makeText(this, "No keywords to export", Toast.LENGTH_SHORT).show()
            return
        }

        val success = keywordPackManager.exportKeywordPack(
            uri = uri,
            name = exportPackName,
            description = exportPackDesc,
            keywords = savedKeywordsList.toSet()
        )

        if (success) {
            Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importKeywords(uri: Uri) {
        val pack = keywordPackManager.importKeywordPack(uri)
        
        if (pack == null) {
            Toast.makeText(this, R.string.invalid_pack, Toast.LENGTH_SHORT).show()
            return
        }

        // Show confirmation dialog with pack details
        MaterialAlertDialogBuilder(this)
            .setTitle("Import \"${pack.name}\"?")
            .setMessage("Description: ${pack.description}\n\nKeywords: ${pack.keywords.size}\n\nThis will add ${pack.keywords.size} keywords to your list.")
            .setPositiveButton(R.string.import_pack) { _, _ ->
                // Add keywords, avoiding duplicates
                var addedCount = 0
                pack.keywords.forEach { keyword ->
                    if (!savedKeywordsList.contains(keyword)) {
                        savedKeywordsList.add(keyword)
                        addedCount++
                    }
                }
                keywordAdapter.notifyDataSetChanged()
                Toast.makeText(
                    this,
                    getString(R.string.import_success, addedCount),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }


    private fun showExitDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.discard_changes))
            .setMessage(getString(R.string.are_you_sure_you_want_to_discard_all_changes_and_exit))
            .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                dialog.dismiss()
                // Allow back press
                finish()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                // Do nothing, stay on the screen
            }
            .show()
    }
    private fun makeAddKeywordDialog() {
        val dialogBinding = DialogAddKeywordBinding.inflate(layoutInflater)


        val filter = InputFilter { source, _, _, _, _, _ ->
            // Allow Unicode letters and digits (but not special characters or spaces)
            if (source.contains(" ")) {
                "" // Reject the input
            } else {
                source // Accept the input
            }
        }

        // Apply the filter
        dialogBinding.keywordInput.filters = arrayOf(filter)
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_a_new_keyword))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.add)) { dialog, _ ->
                var keyword = dialogBinding.keywordInput.text.toString().trim()
                if (keyword.isEmpty()) {
                    return@setPositiveButton
                }
                if (Patterns.WEB_URL.matcher(keyword).matches()) {
                    val regex = Regex("^(?:https?://)?(?:www\\.)?([\\w-]+)\\.")
                    keyword = regex.find(keyword)?.groupValues?.get(1) ?: ""
                    Toast.makeText(
                        this,
                        "Cannot add links, converted and added as a word.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (savedKeywordsList.contains(keyword)) {
                    Toast.makeText(this, "Same Keyword already exists", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                savedKeywordsList.add(keyword)
                keywordAdapter.notifyItemInserted(savedKeywordsList.size - 1)

                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    inner class KeywordAdapter(private val keywords: ArrayList<String>) :
        RecyclerView.Adapter<KeywordAdapter.KeywordViewHolder>() {

        inner class KeywordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val keywordTextView: TextView = itemView.findViewById(R.id.keyword_txt)
            val removeBtn: Button = itemView.findViewById(R.id.btn_remove_keyword)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeywordViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.keyword_item, parent, false)
            return KeywordViewHolder(view)
        }

        override fun onBindViewHolder(holder: KeywordViewHolder, position: Int) {
            holder.keywordTextView.text = keywords[position]
            holder.removeBtn.setOnClickListener {
                savedKeywordsList.removeAt(position)
                notifyItemRemoved(position)
            }
        }

        override fun getItemCount(): Int = keywords.size
    }
}
