package ir.kitgroup.formula

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.room.Room
import com.google.android.material.bottomnavigation.BottomNavigationView
import ir.huri.jcal.JalaliCalendar
import ir.kitgroup.formula.database.AppDatabase
import ir.kitgroup.formula.databinding.ActivityMainBinding
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var restoreLauncher: ActivityResultLauncher<Intent>
    private lateinit var todayDate: String

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        val jalaliDate = JalaliCalendar()
        todayDate = "${jalaliDate.year}_${jalaliDate.month}_${jalaliDate.day}"
        binding.tvDate.text = todayDate


        // مقداردهی قبل از RESUMED شدن اکتیویتی
        restoreLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        restoreDatabaseFromZip(this, uri)
                    }
                }
            }

        binding.ivBackup.setOnClickListener {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // برای اندروید 9 و پایین‌تر
                if (ActivityCompat.checkSelfPermission(
                        this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    backupDatabaseAsZip(this)
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_CODE_PERMISSION
                    )
                }
            } else { // برای اندروید 10 و بالاتر
                backupDatabaseAsZip(this)
            }
        }

        binding.ivShare.setOnClickListener {
            val zipFile = backupDatabaseAsZip(this)
            zipFile?.let {
                shareZipBackup(this, it)
            }
        }

        binding.ivBackupRestore.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*" // یا application/octet-stream
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            restoreLauncher.launch(Intent.createChooser(intent, "انتخاب فایل بکاپ"))

        }
    }

    private fun backupDatabaseAsZip(context: Context): File? {
        return try {
            val dbFile = context.getDatabasePath("app_database")
            val walFile = File("${dbFile.absolutePath}-wal")
            val shmFile = File("${dbFile.absolutePath}-shm")

            val backupDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

            val timeFormat = SimpleDateFormat("HH_mm", Locale.getDefault())
            val time = timeFormat.format(Date())

            val formattedDate = "$todayDate _ $time"

            val zipFileName = "backup_$formattedDate.zip"

            val zipFile = File(backupDir, zipFileName)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                listOf(dbFile, walFile, shmFile).forEach { file ->
                    if (file.exists()) {
                        FileInputStream(file).use { fis ->
                            val entry = ZipEntry(file.name)
                            zos.putNextEntry(entry)
                            fis.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
                }
            }
            Toast.makeText(context, "بکاپ ZIP با موفقیت ذخیره شد!", Toast.LENGTH_LONG).show()
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطا در بکاپ ZIP: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun shareZipBackup(context: Context, zipFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", zipFile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری فایل بکاپ ZIP"))
    }

    private fun restoreDatabaseFromZip(context: Context, zipUri: Uri) {
        val dbFile = context.getDatabasePath("app_database")
        val walFile = File("${dbFile.absolutePath}-wal")
        val shmFile = File("${dbFile.absolutePath}-shm")

        try {
            // بستن دیتابیس و حذف فایل‌های قدیمی
            AppDatabase.getDatabase(context).close()
            dbFile.delete()
            walFile.delete()
            shmFile.delete()

            val inputStream = context.contentResolver.openInputStream(zipUri)
            if (inputStream == null) {
                Toast.makeText(context, "فایل ZIP نامعتبر است", Toast.LENGTH_LONG).show()
                return
            }

            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    val outFile = when (entry!!.name) {
                        "app_database" -> dbFile
                        "app_database-wal" -> walFile
                        "app_database-shm" -> shmFile
                        else -> continue
                    }

                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    zis.closeEntry()
                }
            }

            // حذف کش Room
            AppDatabase.destroyInstance()

            // تست باز کردن دیتابیس
            Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
                .close()

            Toast.makeText(
                context,
                "بازیابی موفق بود! لطفاً برنامه را ریستارت کنید",
                Toast.LENGTH_LONG
            ).show()

            Handler(Looper.getMainLooper()).postDelayed({
                android.os.Process.killProcess(android.os.Process.myPid())
            }, 1000)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطا در بازیابی ZIP: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                backupDatabaseAsZip(this)
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSION = 1001
    }

}