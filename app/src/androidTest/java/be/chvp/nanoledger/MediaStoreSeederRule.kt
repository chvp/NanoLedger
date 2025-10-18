package be.chvp.nanoledger

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MediaStoreSeederRule(
    private val context: Context = ApplicationProvider.getApplicationContext(),
) : TestRule {
    private val createdUris = mutableListOf<Uri>()

    fun insertIntoDownloads(
        context: Context,
        displayName: String,
        mimeType: String,
        data: ByteArray,
    ) {
        val resolver = context.contentResolver

        val externalUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            }

        val uri = resolver.insert(externalUri, values) ?: error("Failed to insert into MediaStore: $displayName")

        resolver.openOutputStream(uri)?.use { os ->
            os.write(data)
            os.flush()
        } ?: error("Failed to open output stream for $uri")

        createdUris += uri
    }

    private fun cleanup() {
        val resolver = context.contentResolver
        createdUris.forEach { runCatching { resolver.delete(it, null, null) } }
        createdUris.clear()
    }

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } finally {
                    cleanup()
                }
            }
        }
}
