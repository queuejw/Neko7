/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package ru.dimon6018.neko11

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.dimon6018.neko11.NekoService.Companion.setupNotificationChannels
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class NekoLand : AppCompatActivity(), PrefState.PrefsListener {
    private var mPrefs: PrefState? = null
    private var mAdapter: CatAdapter? = null
    private var mPendingShareCat: Cat? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.neko_activity)
        mPrefs = PrefState(this)
        if(!mPrefs!!.isConfigured()) {
            mPrefs!!.setConf(true)
            setupNotificationChannels(this)
            val cn = ComponentName(this, NekoTile::class.java)
            packageManager.setComponentEnabledSetting(
                cn, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        setSupportActionBar(findViewById(R.id.Ntoolbar))
        if (actionBar != null) {
            supportActionBar!!.setLogo(Cat.create(this))
            supportActionBar!!.setDisplayUseLogoEnabled(false)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
        window.statusBarColor = ContextCompat.getColor(this, R.color.actionBar)
        mPrefs!!.setListener(this)
        val recyclerView: RecyclerView = findViewById(R.id.holder)
        val view = findViewById<LinearLayout>(R.id.frame)
        mAdapter = CatAdapter()
        recyclerView.setAdapter(mAdapter)
        recyclerView.setLayoutManager(GridLayoutManager(this, 3))
        updateCats()
        applyWindowInsets(view)
    }

    override fun onStart() {
        super.onStart()
        if(!areNotificationsEnabled(NotificationManagerCompat.from(this))) {
            notificationsDialog()
        }
    }
    private fun areNotificationsEnabled(noman: NotificationManagerCompat) = when {
        noman.areNotificationsEnabled().not() -> false
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
            noman.notificationChannels.firstOrNull { channel ->
                channel.importance == NotificationManager.IMPORTANCE_NONE
            } == null
        }
        else -> true
    }
    private fun notificationsDialog() {
        MaterialAlertDialogBuilder(this)
            .setCancelable(false)
            .setMessage(R.string.notifications_warning)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                openSettings()
            }.show()
    }
    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.setData(uri)
        startActivity(intent)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.dishMenu) {
            val mDialog = NekoDialog(this)
            mDialog.show()
        }
        if (item.itemId == R.id.aboutMenu) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.about))
                .setMessage(R.string.aboutText)
                .setPositiveButton(getString(android.R.string.ok), null).show()
        }
        return true
    }
    private fun applyWindowInsets(target: View) {
        ViewCompat.setOnApplyWindowInsetsListener(target) { view, insets ->
            val paddingBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val paddingTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(0, paddingTop, 0, paddingBottom)
            WindowInsetsCompat.CONSUMED
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        mPrefs!!.setListener(null)
    }

    private fun updateCats(): Int {
        val cats: Array<Cat?>
        if (CAT_GEN) {
            cats = arrayOfNulls(50)
            for (i in cats.indices) {
                cats[i] = Cat.create(this)
            }
        } else {
            val hsv = FloatArray(3)
            val list = mPrefs!!.cats
            list.sortedWith { cat, cat2 ->
                Color.colorToHSV(cat.bodyColor, hsv)
                val bodyH1 = hsv[0]
                Color.colorToHSV(cat2.bodyColor, hsv)
                val bodyH2 = hsv[0]
                bodyH1.compareTo(bodyH2)
            }
            cats = list.toTypedArray<Cat?>()
        }
        mAdapter!!.setCats(cats)
        return cats.size
    }

    private fun onCatClick(cat: Cat?) {
        if (CAT_GEN) {
            if (cat != null) {
                mPrefs!!.addCat(cat)
            }
            MaterialAlertDialogBuilder(this@NekoLand)
                .setTitle("Cat added")
                .setPositiveButton(android.R.string.ok, null)
                .show()
        } else {
            showNameDialog(cat)
        }
    }

    private fun onCatRemove(cat: Cat?) {
        if (cat != null) {
            mPrefs!!.removeCat(cat)
        }
    }

    private fun showNameDialog(cat: Cat?) {
        val context: Context = ContextThemeWrapper(
            this,
            com.google.android.material.R.style.Theme_MaterialComponents_Dialog_Alert
        )
        val view: View = LayoutInflater.from(context).inflate(R.layout.edit_text, null)
        val text: EditText = view.findViewById<View>(android.R.id.edit) as EditText
        text.setText(cat!!.name)
        text.setSelection(cat.name!!.length)
        val size = context.resources
            .getDimensionPixelSize(android.R.dimen.app_icon_size)
        val catIcon: Drawable? = cat.createIcon(size, size).loadDrawable(this)
        MaterialAlertDialogBuilder(context)
            .setTitle(" ")
            .setIcon(catIcon)
            .setView(view)
            .setPositiveButton(
                android.R.string.ok
            ) { _, _ ->
                cat.name = text.getText().toString().trim { it <= ' ' }
                mPrefs!!.addCat(cat)
            }.show()
    }

    override fun onPrefsChanged() {
        updateCats()
    }

    companion object {
        @JvmField
        var CHAN_ID: String = "EGG"

        var DEBUG: Boolean = false

        @JvmField
        var DEBUG_NOTIFICATIONS: Boolean = false

        private const val EXPORT_BITMAP_SIZE = 600

        private const val STORAGE_PERM_REQUEST = 123

        private const val CAT_GEN = false
    }

    private inner class CatAdapter : RecyclerView.Adapter<CatAdapter.CatHolder?>() {
        private lateinit var mCats: Array<Cat?>

        fun setCats(cats: Array<Cat?>) {
            mCats = cats
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatHolder {
            return CatHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.cat_view, parent, false)
            )
        }

        override fun getItemCount(): Int {
            return mCats.size
        }

        private fun setContextGroupVisible(holder: CatHolder, vis: Boolean) {
            val group = holder.contextGroup
            if (vis && group.visibility != View.VISIBLE) {
                group.alpha = 0f
                group.visibility = View.VISIBLE
                group.animate().alpha(1.0f).setDuration(333)
                val hideAction = Runnable { setContextGroupVisible(holder, false) }
                group.tag = hideAction
                group.postDelayed(hideAction, 5000)
            } else if (!vis && group.visibility == View.VISIBLE) {
                group.removeCallbacks(group.tag as Runnable)
                group.animate().alpha(0f).setDuration(250)
                    .withEndAction { group.visibility = View.INVISIBLE }
            }
        }

        override fun onBindViewHolder(holder: CatHolder, position: Int) {
            val context: Context = holder.itemView.context
            val size = context.resources.getDimensionPixelSize(R.dimen.neko_display_size)
            holder.imageView.setImageIcon(mCats[position]!!.createIcon(size, size))
            holder.textView.text = mCats[position]!!.name
            holder.itemView.setOnClickListener { onCatClick(mCats[holder.getAdapterPosition()]) }
            holder.itemView.setOnLongClickListener {
                setContextGroupVisible(holder, true)
                true
            }
            holder.delete.setOnClickListener {
                setContextGroupVisible(holder, false)
                MaterialAlertDialogBuilder(this@NekoLand)
                    .setTitle(getString(R.string.confirm_delete, mCats[position]!!.name))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(
                        android.R.string.ok
                    ) { _, _ -> onCatRemove(mCats[holder.getAdapterPosition()]) }
                    .show()
            }
            holder.share.setOnClickListener(View.OnClickListener {
                setContextGroupVisible(holder, false)
                val cat = mCats[holder.getAdapterPosition()]
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    mPendingShareCat = cat
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        STORAGE_PERM_REQUEST
                    )
                    return@OnClickListener
                }
                shareCat(cat)
            })
        }

        private fun shareCat(cat: Cat?) {
            CoroutineScope(Dispatchers.IO).launch {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    getString(R.string.directory_name)
                )
                if (!dir.exists() && !dir.mkdirs()) {
                    Log.e("NekoLand", "save: error: can't create Pictures directory")
                    cancel("can't create Pictures directory")
                }
                val png = File(dir, cat!!.name!!.replace("[/ #:]+".toRegex(), "_") + ".png")
                val bitmap = cat.createBitmap(EXPORT_BITMAP_SIZE, EXPORT_BITMAP_SIZE)
                try {
                    val os: OutputStream = FileOutputStream(png)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, os)
                    os.close()
                    MediaScannerConnection.scanFile(
                        this@NekoLand,
                        arrayOf(png.toString()),
                        arrayOf("image/png"),
                        null
                    )
                    Log.v("Neko", "cat file: $png")
                    val uri: Uri =
                        FileProvider.getUriForFile(
                            this@NekoLand,
                            "com.android.egg.fileprovider",
                            png
                        )
                    Log.v("Neko", "cat uri: $uri")
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.putExtra(Intent.EXTRA_SUBJECT, cat.name)
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    intent.setType("image/png")
                    runOnUiThread {
                        startActivity(Intent.createChooser(intent, null))
                    }
                } catch (e: IOException) {
                    Log.e("NekoLand", "save: error: $e")
                    runOnUiThread {
                        MaterialAlertDialogBuilder(this@NekoLand)
                            .setTitle("Error")
                            .setMessage("Details: $e")
                            .setNegativeButton(android.R.string.ok, null)
                            .show()
                    }
                }
            }
        }
        private inner class CatHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById<View>(android.R.id.icon) as ImageView
            val textView: TextView = itemView.findViewById<View>(android.R.id.title) as TextView
            val contextGroup: View = itemView.findViewById(R.id.contextGroup)
            val delete: View = itemView.findViewById(android.R.id.closeButton)
            val share: View = itemView.findViewById(android.R.id.shareText)
        }
    }
}
