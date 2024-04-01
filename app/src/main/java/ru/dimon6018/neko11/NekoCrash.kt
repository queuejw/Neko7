package ru.dimon6018.neko11

import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NekoCrash: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.neko_crash)
        val copy = findViewById<MaterialButton>(R.id.buttonCopy)
        val show = findViewById<MaterialButton>(R.id.buttonShow)
        val model = "Model: " + Build.MODEL + "\n"
        val brand = "Brand: " + Build.BRAND + "\n"
        val error = "Detected critical error.\n " + model + brand + intent.extras?.getString("stacktrace")
        show.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setMessage(error)
                .setNegativeButton(R.string.copy
                ) { _: DialogInterface?, _: Int -> copyError(error) }
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        copy.setOnClickListener {
            copyError(error)
        }
    }
    private fun copyError(code: String) {
        val clipbrd = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("error", code)
        clipbrd.setPrimaryClip(clip)
    }
}