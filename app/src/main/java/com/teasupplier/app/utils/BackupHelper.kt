package com.teasupplier.app.utils

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Database export/import engine supporting SQLite database file backup/restore
 * and CSV customer directory export for safe data migration.
 */

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.teasupplier.app.data.local.dao.CustomerDao
import com.teasupplier.app.data.local.entity.CustomerEntity
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import com.teasupplier.app.data.local.AppDatabase

object BackupHelper {

    fun exportCustomersToCsv(customers: List<CustomerEntity>): String? {
        return try {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TeaSupplierBackups")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "CustomersBackup_${System.currentTimeMillis()}.csv")
            
            val writer = OutputStreamWriter(FileOutputStream(file), "UTF-8")
            // Header
            writer.write("Name,ShopName,Mobile,Address,Area,DefaultRate,OpeningBalance,Balance,Status,Notes\n")
            
            for (c in customers) {
                // Escape commas by quoting strings
                val name = "\"${c.name.replace("\"", "\"\"")}\""
                val shopName = "\"${c.shopName.replace("\"", "\"\"")}\""
                val mobile = "\"${c.mobile.replace("\"", "\"\"")}\""
                val address = "\"${c.address.replace("\"", "\"\"")}\""
                val area = "\"${c.area.replace("\"", "\"\"")}\""
                val notes = "\"${c.notes.replace("\"", "\"\"")}\""
                
                writer.write("$name,$shopName,$mobile,$address,$area,${c.defaultRate},${c.openingBalance},${c.balance},${c.status},$notes\n")
            }
            writer.flush()
            writer.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun importCustomersFromCsv(context: Context, uri: Uri, customerDao: CustomerDao): Int {
        var count = 0
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            
            reader.readLine() // Skip header
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // Simplistic CSV parsing (assuming no commas inside quotes for simplicity unless handled by advanced regex, but we will use basic split for now or a custom parser)
                // A better approach for this simple app is to just split by comma assuming the user didn't modify it heavily.
                // Let's implement a basic CSV row parser
                val tokens = parseCsvRow(line!!)
                if (tokens.size >= 10) {
                    val name = tokens[0]
                    val shopName = tokens[1]
                    val mobile = tokens[2]
                    val address = tokens[3]
                    val area = tokens[4]
                    val defaultRate = tokens[5].toDoubleOrNull() ?: 0.0
                    val openingBalance = tokens[6].toDoubleOrNull() ?: 0.0
                    val balance = tokens[7].toDoubleOrNull() ?: 0.0
                    val status = tokens[8].toBooleanStrictOrNull() ?: true
                    val notes = tokens[9]
                    
                    val customer = CustomerEntity(
                        name = name,
                        shopName = shopName,
                        mobile = mobile,
                        address = address,
                        area = area,
                        defaultRate = defaultRate,
                        openingBalance = openingBalance,
                        balance = balance,
                        status = status,
                        notes = notes,
                        createdAt = System.currentTimeMillis()
                    )
                    customerDao.insert(customer)
                    count++
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }
    
    private fun parseCsvRow(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        
        while (i < line.length) {
            val char = line[i]
            if (inQuotes) {
                if (char == '"') {
                    if (i < line.length - 1 && line[i + 1] == '"') {
                        // Escaped quote — append a literal " and skip the next char
                        current.append('"')
                        i++ // skip the second quote
                    } else {
                        inQuotes = false
                    }
                } else {
                    current.append(char)
                }
            } else {
                if (char == '"') {
                    inQuotes = true
                } else if (char == ',') {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                } else {
                    current.append(char)
                }
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    fun exportDatabase(context: Context, database: AppDatabase? = null): String? {
        return try {
            // Checkpoint WAL to flush pending writes to the main db file
            database?.let {
                try {
                    it.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
                } catch (_: Exception) { /* best-effort */ }
            }
            
            val dbFile = context.getDatabasePath("tea_supplier_db")
            if (!dbFile.exists()) return null
            
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TeaSupplierBackups")
            if (!dir.exists()) dir.mkdirs()
            
            val destFile = File(dir, "DB_Backup_${System.currentTimeMillis()}.db")
            
            // Note: In a production app, we should checkpoint the WAL before copying.
            // For simple usage, copying all three files (.db, .db-wal, .db-shm) or closing the DB is safer.
            // We will just copy the main db for now assuming it's flushed, but let's copy WAL too if it exists.
            copyFile(dbFile, destFile)
            
            val walFile = File(dbFile.absolutePath + "-wal")
            if (walFile.exists()) {
                copyFile(walFile, File(dir, destFile.name + "-wal"))
            }
            val shmFile = File(dbFile.absolutePath + "-shm")
            if (shmFile.exists()) {
                copyFile(shmFile, File(dir, destFile.name + "-shm"))
            }
            
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importDatabase(context: Context, uri: Uri): Boolean {
        return try {
            val dbFile = context.getDatabasePath("tea_supplier_db")
            
            // Delete old db files
            dbFile.delete()
            File(dbFile.absolutePath + "-wal").delete()
            File(dbFile.absolutePath + "-shm").delete()
            
            // Copy new db file
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val outputStream = FileOutputStream(dbFile)
            
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            
            // App must restart to re-initialize Room correctly
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun copyFile(source: File, dest: File) {
        val input = FileInputStream(source)
        val output = FileOutputStream(dest)
        val buffer = ByteArray(1024)
        var length: Int
        while (input.read(buffer).also { length = it } > 0) {
            output.write(buffer, 0, length)
        }
        output.flush()
        output.close()
        input.close()
    }
}
