package com.alhaq.amnshield.utils

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class KeywordPack(
    val name: String,
    val description: String,
    val keywords: List<String>,
    val version: Int = 1,
    val verified: Boolean = false,
    val author: String = "DeenShield Community"
)

class KeywordPackManager(private val context: Context) {
    
    private val gson = Gson()
    
    /**
     * Export keywords to a JSON file
     */
    fun exportKeywordPack(
        uri: Uri,
        name: String,
        description: String,
        keywords: Set<String>
    ): Boolean {
        return try {
            val pack = KeywordPack(
                name = name,
                description = description,
                keywords = keywords.toList(),
                version = 1,
                verified = false,
                author = "Custom Pack"
            )
            
            val json = gson.toJson(pack)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Import keywords from a JSON file
     */
    fun importKeywordPack(uri: Uri): KeywordPack? {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
            
            if (json.isNullOrEmpty()) {
                return null
            }
            
            val pack = gson.fromJson(json, KeywordPack::class.java)
            
            // Validate the pack
            if (pack.name.isBlank() || pack.keywords.isEmpty()) {
                return null
            }
            
            // Sanitize keywords - remove empty strings and trim
            val sanitizedPack = pack.copy(
                keywords = pack.keywords
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
            )
            
            sanitizedPack
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get built-in verified keyword packs
     */
    fun getVerifiedPacks(): List<KeywordPack> {
        return listOf(
            KeywordPack(
                name = "Adult Content Blocker",
                description = "Comprehensive pack to block adult and inappropriate content",
                keywords = com.alhaq.amnshield.data.blockers.KeywordPacks.adultKeywords.toList(),
                version = 1,
                verified = true,
                author = "DeenShield Official"
            )
        )
    }
    
    /**
     * Create shareable text content for keywords
     */
    fun createShareableText(keywords: Set<String>, packName: String): String {
        val pack = KeywordPack(
            name = packName,
            description = "Shared keyword pack",
            keywords = keywords.toList(),
            version = 1,
            verified = false,
            author = "DeenShield User"
        )
        return gson.toJson(pack)
    }
    
    /**
     * Parse keywords from shareable text
     */
    fun parseShareableText(text: String): KeywordPack? {
        return try {
            val pack = gson.fromJson(text, KeywordPack::class.java)
            if (pack.keywords.isEmpty()) null else pack
        } catch (e: Exception) {
            null
        }
    }
}
