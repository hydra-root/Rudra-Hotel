package com.teasupplier.app.utils

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Production-grade PDF Document Generation engine supporting bilingual branding
 * (Marathi & English), Table Order Bills with UPI Payment QR Codes, Detailed Customer Statements,
 * Daily/Monthly Sales Reports, and Hotel Expense Sheets.
 */

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.teasupplier.app.data.local.entity.CustomerEntity
import com.teasupplier.app.data.local.entity.ExpenseEntity
import com.teasupplier.app.data.local.entity.SaleEntity
import com.teasupplier.app.data.local.entity.TableOrderEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.teasupplier.app.domain.usecase.DailyReportRow
import com.teasupplier.app.domain.usecase.MonthlyReportRow
import com.teasupplier.app.domain.usecase.CustomerTransactionRow
import com.teasupplier.app.domain.usecase.TransactionType

object PdfGenerator {

    // Color Palette based on requested Yellow & Warm Cream theme
    private val THEME_COLOR = Color.parseColor("#D97706") // Warm Saffron Amber
    private val COLOR_PRIMARY = Color.parseColor("#D97706") // Warm Saffron Amber
    private val COLOR_HEADER_BG = Color.parseColor("#4E342E") // Espresso Dark Brown
    private val COLOR_YELLOW_ACCENT = Color.parseColor("#FEF08A") // Soft Pastel Yellow swatch (Top)
    private val COLOR_CREAM_BG = Color.parseColor("#FFFDF2") // Warm Soft Cream swatch (Bottom)
    private val COLOR_BORDER_YELLOW = Color.parseColor("#FDE047") // Pastel Golden Yellow border
    private val COLOR_DIVIDER = Color.parseColor("#F3E8C8") // Warm light divider
    private val COLOR_TEXT_PRIMARY = Color.parseColor("#1F2937") // Crisp Dark Text
    private val COLOR_TEXT_SECONDARY = Color.parseColor("#6B7280") // Subtle Gray Text

    fun generateBill(
        context: Context,
        customer: CustomerEntity,
        sales: List<SaleEntity>,
        billingPeriodStr: String,
        shopNameMarathi: String = "हॉटेल रुद्रा अमृततुल्य",
        shopNameEnglish: String = "Hotel Rudra Amruttulya",
        shopAddress: String = "कोतकर वस्ती, एमआयडीसी, अहिल्यानगर - ४१४१११",
        shopPhone: String = "मोबा. नं.: 8788138197 / 8830404860",
        billDate: Long = System.currentTimeMillis(),
        upiId: String = ""
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Changed to Portrait for more rows

        drawCustomerBillPages(context, pdfDocument, pageInfo, customer, sales, billingPeriodStr, shopNameMarathi, shopNameEnglish, shopAddress, shopPhone, billDate, upiId)

        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val directory = File(baseDir, "TeaSupplierBills")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val safeCustomerName = customer.shopName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val file = File(directory, "Bill_${safeCustomerName}.pdf")
        
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
        
        return file
    }

    fun generateMasterBill(
        context: Context,
        customersWithSales: Map<CustomerEntity, List<SaleEntity>>,
        billingPeriodStr: String,
        shopNameMarathi: String = "हॉटेल रुद्रा अमृततुल्य",
        shopNameEnglish: String = "Hotel Rudra Amruttulya",
        shopAddress: String = "कोतकर वस्ती, एमआयडीसी, अहिल्यानगर - ४१४१११",
        shopPhone: String = "मोबा. नं.: 8788138197 / 8830404860",
        billDate: Long = System.currentTimeMillis(),
        upiId: String = ""
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        
        val activeCustomers = customersWithSales.filter { it.value.isNotEmpty() }
        if (activeCustomers.isEmpty()) return null

        for ((customer, sales) in activeCustomers) {
            drawCustomerBillPages(context, pdfDocument, pageInfo, customer, sales, billingPeriodStr, shopNameMarathi, shopNameEnglish, shopAddress, shopPhone, billDate, upiId)
        }

        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val directory = File(baseDir, "TeaSupplierBills")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, "Master_Bill.pdf")
        
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
        
        return file
    }

    private fun drawCustomerBillPages(
        context: Context,
        pdfDocument: PdfDocument,
        pageInfo: PdfDocument.PageInfo,
        customer: CustomerEntity,
        sales: List<SaleEntity>,
        billingPeriodStr: String,
        shopNameMarathi: String,
        shopNameEnglish: String,
        shopAddress: String,
        shopPhone: String,
        billDate: Long = System.currentTimeMillis(),
        upiId: String = ""
    ) {
        val paint = Paint()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date(billDate))

        val rowHeight = 30f
        val footerHeight = 200f
        val bottomMargin = 50f
        
        // Portrait coordinates
        val col1X = 60f // Sr. No
        val col3X = 330f // Qty
        val col4X = 420f // Rate
        val col5X = 510f // Amount

        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = drawHeader(context, canvas, paint, pageInfo, shopNameMarathi, shopNameEnglish, shopAddress, shopPhone, currentDate, billingPeriodStr, customer, col1X, col3X, col4X, col5X)
        
        var totalQuantity = 0.0
        var grandTotal = 0.0

        // Aggregate sales by product name and rate
        data class AggregatedSale(val productName: String, val rate: Double, val quantity: Double, val amount: Double)
        
        val aggregatedSales = sales.groupBy { Pair(it.productName, it.rate) }
            .map { (key, salesGroup) ->
                AggregatedSale(
                    productName = key.first,
                    rate = key.second,
                    quantity = salesGroup.sumOf { it.quantity },
                    amount = salesGroup.sumOf { it.amount }
                )
            }
            .sortedBy { it.productName }

        if (aggregatedSales.isEmpty()) {
            paint.color = COLOR_TEXT_PRIMARY
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 10.5f
            drawTableRow(canvas, paint, currentY, rowHeight, pageInfo.pageWidth,
                "", "", "", "", "₹0.00", col1X, col3X, col4X, col5X, false)
            currentY += rowHeight
        } else {
            aggregatedSales.forEachIndexed { index, sale ->
                // Check pagination
                if (currentY + rowHeight > pageInfo.pageHeight - footerHeight - bottomMargin) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = drawHeader(context, canvas, paint, pageInfo, shopNameMarathi, shopNameEnglish, shopAddress, shopPhone, currentDate, billingPeriodStr, customer, col1X, col3X, col4X, col5X)
                }

                drawTableRow(canvas, paint, currentY, rowHeight, pageInfo.pageWidth,
                    (index + 1).toString(),
                    sale.productName,
                    if (sale.quantity % 1.0 == 0.0) sale.quantity.toInt().toString() else sale.quantity.toString(),
                    "₹${if (sale.rate % 1.0 == 0.0) sale.rate.toInt() else sale.rate}",
                    "₹${if (sale.amount % 1.0 == 0.0) sale.amount.toInt() else sale.amount}",
                    col1X, col3X, col4X, col5X,
                    isOddRow = index % 2 != 0
                )
                
                totalQuantity += sale.quantity
                grandTotal += sale.amount
                currentY += rowHeight
            }
        }
        
        // Draw Footer
        // Ensure space for footer
        if (currentY + footerHeight > pageInfo.pageHeight - bottomMargin) {
            pdfDocument.finishPage(page)
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = drawHeader(context, canvas, paint, pageInfo, shopNameMarathi, shopNameEnglish, shopAddress, shopPhone, currentDate, billingPeriodStr, customer, col1X, col3X, col4X, col5X)
        }

        drawFooter(canvas, paint, currentY, pageInfo, grandTotal, shopNameEnglish, upiId)
        
        pdfDocument.finishPage(page)
    }

    private fun drawHeader(
        context: Context, canvas: Canvas, paint: Paint, pageInfo: PdfDocument.PageInfo,
        shopNameMarathi: String, shopNameEnglish: String, shopAddress: String, shopPhone: String,
        currentDate: String, billingPeriodStr: String, customer: CustomerEntity,
        col1X: Float, col3X: Float, col4X: Float, col5X: Float
    ): Float {
        val center = pageInfo.pageWidth / 2f
        val rightMargin = pageInfo.pageWidth - 40f
        
        // 1. Top Accent Band
        paint.color = COLOR_PRIMARY
        paint.style = Paint.Style.FILL
        canvas.drawRect(40f, 20f, rightMargin, 25f, paint)

        // 2. Titles
        paint.textAlign = Paint.Align.CENTER
        val marathiTypeface = try {
            Typeface.createFromAsset(context.assets, "fonts/YatraOne-Regular.ttf")
        } catch (e: Exception) {
            try {
                Typeface.createFromAsset(context.assets, "fonts/Mukta-ExtraBold.ttf")
            } catch (e2: Exception) {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }
        paint.typeface = marathiTypeface
        paint.textSize = 26f
        paint.color = COLOR_PRIMARY
        canvas.drawText(shopNameMarathi, center, 62f, paint)
        
        paint.color = COLOR_HEADER_BG
        paint.textSize = 16f
        canvas.drawText(shopNameEnglish, center, 86f, paint)

        // 3. Address and Phone
        paint.color = COLOR_TEXT_SECONDARY
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(shopAddress, center, 105f, paint)
        canvas.drawText(shopPhone, center, 120f, paint)
        
        // Dashed line
        paint.color = COLOR_DIVIDER
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
        canvas.drawLine(40f, 135f, rightMargin, 135f, paint)
        paint.pathEffect = null
        paint.style = Paint.Style.FILL

        // 4. Customer Info Box (Warm Cream with Yellow Accent)
        var currentY = 150f
        paint.color = COLOR_CREAM_BG
        canvas.drawRoundRect(android.graphics.RectF(40f, currentY, rightMargin, currentY + 54f), 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = COLOR_BORDER_YELLOW
        paint.strokeWidth = 1f
        canvas.drawRoundRect(android.graphics.RectF(40f, currentY, rightMargin, currentY + 54f), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Left accent bar
        paint.color = COLOR_PRIMARY
        canvas.drawRoundRect(android.graphics.RectF(40f, currentY, 46f, currentY + 54f), 4f, 4f, paint)

        paint.color = COLOR_TEXT_PRIMARY
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 11.5f
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Customer:", 55f, currentY + 22f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(" ${customer.shopName}", 55f + paint.measureText("Customer:"), currentY + 22f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Bill Period:", 55f, currentY + 42f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(" $billingPeriodStr", 55f + paint.measureText("Bill Period:"), currentY + 42f, paint)

        val dateText = currentDate
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(dateText, rightMargin - 12f, currentY + 22f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Bill Date: ", rightMargin - 12f - paint.measureText(dateText), currentY + 22f, paint)
        
        currentY += 72f
        
        // 5. Table Header (Rich Espresso Brown)
        paint.color = COLOR_HEADER_BG
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(android.graphics.RectF(40f, currentY, rightMargin, currentY + 30f), 6f, 6f, paint)
        
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        paint.textAlign = Paint.Align.CENTER
        
        canvas.drawText("Sr.", col1X, currentY + 20f, paint)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Item Description", 100f, currentY + 20f, paint)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Qty", col3X, currentY + 20f, paint)
        canvas.drawText("Rate", col4X, currentY + 20f, paint)
        canvas.drawText("Amount", col5X, currentY + 20f, paint)
        
        return currentY + 30f
    }

    private fun drawFooter(
        canvas: Canvas, paint: Paint, startY: Float, pageInfo: PdfDocument.PageInfo,
        grandTotal: Double,
        hotelNameEnglish: String = "Hotel Rudra Amruttulya",
        upiId: String = ""
    ) {
        val rightMargin = pageInfo.pageWidth - 40f
        var currentY = startY + 40f
        
        paint.style = Paint.Style.FILL
        paint.color = COLOR_TEXT_SECONDARY
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Subtotal:", 450f, currentY, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = COLOR_TEXT_PRIMARY
        canvas.drawText("₹$grandTotal", rightMargin - 10f, currentY, paint)

        currentY += 22f
        paint.color = COLOR_TEXT_SECONDARY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Tax (0%):", 450f, currentY, paint)
        canvas.drawText("₹0.00", rightMargin - 10f, currentY, paint)

        // TOTAL box (Pastel Yellow with Golden Stroke)
        currentY += 14f
        paint.color = COLOR_YELLOW_ACCENT
        canvas.drawRoundRect(android.graphics.RectF(280f, currentY, rightMargin, currentY + 32f), 8f, 8f, paint)
        
        paint.style = Paint.Style.STROKE
        paint.color = COLOR_PRIMARY
        paint.strokeWidth = 1f
        canvas.drawRoundRect(android.graphics.RectF(280f, currentY, rightMargin, currentY + 32f), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = COLOR_HEADER_BG
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("TOTAL:", 450f, currentY + 21f, paint)
        canvas.drawText("₹$grandTotal", rightMargin - 12f, currentY + 21f, paint)
        
        // Amount in words box (Soft Cream)
        currentY += 45f
        paint.color = COLOR_CREAM_BG
        canvas.drawRoundRect(android.graphics.RectF(40f, currentY, rightMargin, currentY + 32f), 8f, 8f, paint)
        
        paint.style = Paint.Style.STROKE
        paint.color = COLOR_BORDER_YELLOW
        canvas.drawRoundRect(android.graphics.RectF(40f, currentY, rightMargin, currentY + 32f), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Left accent bar
        paint.color = COLOR_PRIMARY
        canvas.drawRoundRect(android.graphics.RectF(40f, currentY, 46f, currentY + 32f), 4f, 4f, paint)

        paint.color = COLOR_TEXT_PRIMARY
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Amount in Words: ", 55f, currentY + 21f, paint)

        paint.color = COLOR_TEXT_SECONDARY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val words = "Rupees " + com.teasupplier.app.utils.NumberToWordsConverter.convert(grandTotal.toLong()) + " Only"
        canvas.drawText(words, 55f + paint.measureText("Amount in Words: "), currentY + 21f, paint)
        
        currentY += 60f
        // Dashed line
        paint.color = COLOR_DIVIDER
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
        canvas.drawLine(40f, currentY, rightMargin, currentY, paint)
        paint.pathEffect = null
        paint.style = Paint.Style.FILL

        currentY += 25f

        // Thank you
        paint.color = COLOR_PRIMARY
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("Thank you!", pageInfo.pageWidth / 2f, currentY + 50f, paint)

        // Signature
        paint.color = COLOR_TEXT_SECONDARY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        
        val signatureText = "For $hotelNameEnglish"
        val signatureCenterX = rightMargin - (paint.measureText(signatureText) / 2f)
        paint.textAlign = Paint.Align.CENTER
        
        canvas.drawText("Authorized Signature", signatureCenterX, currentY, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = COLOR_TEXT_PRIMARY
        canvas.drawText(signatureText, signatureCenterX, currentY + 45f, paint)

        // ─── UPI Payment QR Code Section (Bottom Left) ───
        val actualUpi = upiId.trim().ifBlank { "8788138197@ybl" }
        val qrBitmap = QrCodeGenerator.generateUpiPaymentQr(
            upiId = actualUpi,
            payeeName = hotelNameEnglish.ifBlank { "Hotel Rudra Amruttulya" },
            amount = grandTotal,
            note = "Bill Payment",
            sizePx = 250
        )

        if (qrBitmap != null) {
            val qrSize = 64f
            val qrLeft = 40f
            val qrTop = currentY - 5f

            // White background card for QR
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawRoundRect(android.graphics.RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), 4f, 4f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = COLOR_BORDER_YELLOW
            paint.strokeWidth = 1f
            canvas.drawRoundRect(android.graphics.RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), 4f, 4f, paint)
            paint.style = Paint.Style.FILL

            // Draw QR Bitmap
            val srcRect = android.graphics.Rect(0, 0, qrBitmap.width, qrBitmap.height)
            val dstRect = android.graphics.RectF(qrLeft + 2f, qrTop + 2f, qrLeft + qrSize - 2f, qrTop + qrSize - 2f)
            canvas.drawBitmap(qrBitmap, srcRect, dstRect, null)

            // Text info beside QR
            val textLeft = qrLeft + qrSize + 10f
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            paint.color = COLOR_HEADER_BG
            canvas.drawText("Scan & Pay via UPI", textLeft, qrTop + 13f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12.5f
            paint.color = Color.parseColor("#2E7D32")
            canvas.drawText("₹ ${String.format(Locale.getDefault(), "%.2f", grandTotal)}", textLeft, qrTop + 28f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f
            paint.color = COLOR_TEXT_SECONDARY
            canvas.drawText("UPI: $actualUpi", textLeft, qrTop + 42f, paint)

            paint.textSize = 7.5f
            canvas.drawText("GPay • PhonePe • Paytm • BHIM", textLeft, qrTop + 55f, paint)
        } else {
            // Fallback plain text if QR not available
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#2E7D32")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10.5f
            canvas.drawText("UPI Payment ID: $actualUpi", 40f, currentY + 45f, paint)
        }
    }
    
    private fun drawTableRow(
        canvas: Canvas,
        textPaint: Paint,
        currentY: Float,
        rowHeight: Float,
        pageWidth: Int,
        srNo: String,
        desc: String,
        qty: String,
        rate: String,
        amount: String,
        col1X: Float,
        col3X: Float,
        col4X: Float,
        col5X: Float,
        isOddRow: Boolean = false
    ) {
        val rightMargin = pageWidth.toFloat() - 40f

        // Zebra striping using user's soft cream swatch
        if (isOddRow) {
            val bgPaint = Paint().apply {
                color = COLOR_CREAM_BG
                style = Paint.Style.FILL
            }
            canvas.drawRect(40f, currentY, rightMargin, currentY + rowHeight, bgPaint)
        }

        textPaint.color = COLOR_TEXT_PRIMARY
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 10.5f
        
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(srNo, col1X, currentY + 18f, textPaint)
        
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(desc, 100f, currentY + 18f, textPaint)
        
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(qty, col3X, currentY + 18f, textPaint)
        canvas.drawText(rate, col4X, currentY + 18f, textPaint)
        canvas.drawText(amount, col5X, currentY + 18f, textPaint)
        
        // Faint bottom border
        val dividerPaint = Paint().apply {
            color = COLOR_DIVIDER
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        canvas.drawLine(40f, currentY + rowHeight, rightMargin, currentY + rowHeight, dividerPaint)
    }

    /**
     * Generates a "Detailed Bill" with day-by-day breakdown showing Morning / Evening quantities.
     * Table format: Date | Item | Morning | Evening | Total | Rate | Amount
     * Morning = sale timestamp hour < 12, Evening = >= 12
     */
    fun generateDetailedBill(
        context: Context,
        customer: CustomerEntity,
        sales: List<SaleEntity>,
        billingPeriodStr: String,
        shopNameMarathi: String = "हॉटेल रुद्रा अमृततुल्य",
        shopNameEnglish: String = "Hotel Rudra Amruttulya",
        shopAddress: String = "कोतकर वस्ती, एमआयडीसी, अहिल्यानगर - ४१४१११",
        shopPhone: String = "मोबा. नं.: 8788138197 / 8830404860",
        billDate: Long = System.currentTimeMillis(),
        upiId: String = ""
    ): File? {
        if (sales.isEmpty()) return null

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val paint = Paint()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val currentDate = dateFormat.format(Date(billDate))
        val rightMargin = pageInfo.pageWidth - 40f

        val rowHeight = 26f
        val footerHeight = 200f
        val bottomMargin = 50f

        val calendar = java.util.Calendar.getInstance()

        // Helper to normalize timestamp to start of day
        fun getDayStart(ts: Long): Long {
            calendar.timeInMillis = ts
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
        
        data class DayProductKey(val dayStartMs: Long, val productName: String, val rate: Double)
        data class DetailRow(
            val date: Long,
            val productName: String,
            val morningQty: Double,
            val eveningQty: Double,
            val totalQty: Double,
            val rate: Double,
            val amount: Double
        )

        // Group sales by Day + Product + Rate
        val groupedSales = sales.groupBy {
            DayProductKey(
                dayStartMs = getDayStart(it.date),
                productName = it.productName,
                rate = it.rate
            )
        }

        val detailRows = groupedSales.map { (key, salesList) ->
            var mQty = 0.0
            var eQty = 0.0
            var amt = 0.0
            for (s in salesList) {
                calendar.timeInMillis = s.date
                val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                if (hour < 12) {
                    mQty += s.quantity
                } else {
                    eQty += s.quantity
                }
                amt += s.amount
            }
            DetailRow(
                date = key.dayStartMs,
                productName = key.productName,
                morningQty = mQty,
                eveningQty = eQty,
                totalQty = mQty + eQty,
                rate = key.rate,
                amount = amt
            )
        }.sortedWith(compareBy({ it.date }, { it.productName }))

        // Column X coordinates
        val colDate = 50f
        val colItem = 120f
        val colMorning = 280f
        val colEvening = 345f
        val colTotal = 410f
        val colRate = 465f
        val colAmount = 530f

        // --- Start first page ---
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Header branding
        var currentY = drawDetailedHeader(context, canvas, paint, pageInfo, shopNameMarathi, shopNameEnglish, shopAddress, shopPhone, currentDate, billingPeriodStr, customer)

        // Table header
        currentY = drawDetailedTableHeader(canvas, paint, currentY, rightMargin, colDate, colItem, colMorning, colEvening, colTotal, colRate, colAmount)

        var grandTotalMorning = 0.0
        var grandTotalEvening = 0.0
        var grandTotalQty = 0.0
        var grandTotalAmount = 0.0
        var lastDate = -1L
        var rowIndex = 0

        for (row in detailRows) {
            // Check pagination
            if (currentY + rowHeight > pageInfo.pageHeight - footerHeight - bottomMargin) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = drawDetailedHeader(context, canvas, paint, pageInfo, shopNameMarathi, shopNameEnglish, shopAddress, shopPhone, currentDate, billingPeriodStr, customer)
                currentY = drawDetailedTableHeader(canvas, paint, currentY, rightMargin, colDate, colItem, colMorning, colEvening, colTotal, colRate, colAmount)
                lastDate = -1L
                rowIndex = 0
            }

            // Draw date separator when date changes
            val showDate = row.date != lastDate
            if (showDate && lastDate != -1L) {
                // Light separator line between days
                val sepPaint = Paint().apply {
                    color = COLOR_DIVIDER
                    style = Paint.Style.STROKE
                    strokeWidth = 1.2f
                }
                canvas.drawLine(40f, currentY, rightMargin, currentY, sepPaint)
            }
            lastDate = row.date

            // Zebra striping background for odd rows
            val isOddRow = rowIndex % 2 != 0
            if (isOddRow) {
                val bgPaint = Paint().apply {
                    color = COLOR_CREAM_BG
                    style = Paint.Style.FILL
                }
                canvas.drawRect(40f, currentY, rightMargin, currentY + rowHeight, bgPaint)
            }
            rowIndex++

            paint.color = COLOR_TEXT_PRIMARY
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f

            // Date column (only show on first row of each date)
            paint.textAlign = Paint.Align.LEFT
            if (showDate) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(dayFormat.format(Date(row.date)), colDate, currentY + 16f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            // Item
            canvas.drawText(row.productName, colItem, currentY + 16f, paint)

            // Morning / Evening / Total
            paint.textAlign = Paint.Align.CENTER
            val mStr = if (row.morningQty > 0) fmtQty(row.morningQty) else "-"
            val eStr = if (row.eveningQty > 0) fmtQty(row.eveningQty) else "-"
            canvas.drawText(mStr, colMorning, currentY + 16f, paint)
            canvas.drawText(eStr, colEvening, currentY + 16f, paint)
            
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(fmtQty(row.totalQty), colTotal, currentY + 16f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            // Rate / Amount
            canvas.drawText("₹${fmtNum(row.rate)}", colRate, currentY + 16f, paint)
            canvas.drawText("₹${fmtNum(row.amount)}", colAmount, currentY + 16f, paint)

            // Faint row divider
            val divPaint = Paint().apply {
                color = COLOR_DIVIDER
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }
            canvas.drawLine(40f, currentY + rowHeight, rightMargin, currentY + rowHeight, divPaint)

            grandTotalMorning += row.morningQty
            grandTotalEvening += row.eveningQty
            grandTotalQty += row.totalQty
            grandTotalAmount += row.amount
            currentY += rowHeight
        }

        // Totals row (Pastel Yellow Highlight Banner)
        currentY += 6f
        paint.color = COLOR_YELLOW_ACCENT
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(android.graphics.RectF(40f, currentY, rightMargin, currentY + 28f), 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = COLOR_PRIMARY
        paint.strokeWidth = 1f
        canvas.drawRoundRect(android.graphics.RectF(40f, currentY, rightMargin, currentY + 28f), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = COLOR_HEADER_BG
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("TOTAL", colDate + 6f, currentY + 18f, paint)

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(fmtQty(grandTotalMorning), colMorning, currentY + 18f, paint)
        canvas.drawText(fmtQty(grandTotalEvening), colEvening, currentY + 18f, paint)
        canvas.drawText(fmtQty(grandTotalQty), colTotal, currentY + 18f, paint)
        canvas.drawText("₹${fmtNum(grandTotalAmount)}", colAmount, currentY + 18f, paint)

        currentY += 28f

        // Footer
        if (currentY + footerHeight > pageInfo.pageHeight - bottomMargin) {
            pdfDocument.finishPage(page)
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = 60f
        }
        drawFooter(canvas, paint, currentY, pageInfo, grandTotalAmount, shopNameEnglish, upiId)
        pdfDocument.finishPage(page)

        // Save file
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val directory = File(baseDir, "TeaSupplierBills")
        if (!directory.exists()) directory.mkdirs()
        val safeCustomerName = customer.shopName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val file = File(directory, "DetailedBill_${safeCustomerName}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
        return file
    }

    private fun drawDetailedHeader(
        context: Context, canvas: Canvas, paint: Paint, pageInfo: PdfDocument.PageInfo,
        shopNameMarathi: String, shopNameEnglish: String, shopAddress: String, shopPhone: String,
        currentDate: String, billingPeriodStr: String, customer: CustomerEntity
    ): Float {
        val center = pageInfo.pageWidth / 2f
        val rightMargin = pageInfo.pageWidth - 40f

        // Top Orange Band
        paint.color = COLOR_PRIMARY
        paint.style = Paint.Style.FILL
        canvas.drawRect(40f, 20f, rightMargin, 25f, paint)

        // Titles
        paint.textAlign = Paint.Align.CENTER
        val marathiTypeface = try {
            Typeface.createFromAsset(context.assets, "fonts/YatraOne-Regular.ttf")
        } catch (e: Exception) {
            try {
                Typeface.createFromAsset(context.assets, "fonts/Mukta-ExtraBold.ttf")
            } catch (e2: Exception) {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }
        paint.typeface = marathiTypeface
        paint.textSize = 24f
        paint.color = COLOR_PRIMARY
        canvas.drawText(shopNameMarathi, center, 56f, paint)

        paint.color = COLOR_HEADER_BG
        paint.textSize = 14f
        canvas.drawText(shopNameEnglish, center, 78f, paint)

        paint.color = COLOR_TEXT_SECONDARY
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(shopAddress, center, 95f, paint)
        canvas.drawText(shopPhone, center, 108f, paint)

        // Dashed line
        paint.color = COLOR_DIVIDER
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
        canvas.drawLine(40f, 118f, rightMargin, 118f, paint)
        paint.pathEffect = null
        paint.style = Paint.Style.FILL

        // Customer Info (Warm Cream with Yellow Accent)
        var currentY = 128f
        paint.color = COLOR_CREAM_BG
        canvas.drawRoundRect(android.graphics.RectF(40f, currentY, rightMargin, currentY + 50f), 8f, 8f, paint)
        
        paint.style = Paint.Style.STROKE
        paint.color = COLOR_BORDER_YELLOW
        paint.strokeWidth = 1f
        canvas.drawRoundRect(android.graphics.RectF(40f, currentY, rightMargin, currentY + 50f), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Left accent bar
        paint.color = COLOR_PRIMARY
        canvas.drawRoundRect(android.graphics.RectF(40f, currentY, 46f, currentY + 50f), 4f, 4f, paint)

        paint.color = COLOR_TEXT_PRIMARY
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Customer:", 55f, currentY + 20f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(" ${customer.shopName}", 55f + paint.measureText("Customer:"), currentY + 20f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Period:", 55f, currentY + 38f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(" $billingPeriodStr", 55f + paint.measureText("Period:"), currentY + 38f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(currentDate, rightMargin - 12f, currentY + 20f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Bill Date: ", rightMargin - 12f - paint.measureText(currentDate), currentY + 20f, paint)

        return currentY + 60f
    }

    private fun drawDetailedTableHeader(
        canvas: Canvas, paint: Paint, startY: Float, rightMargin: Float,
        colDate: Float, colItem: Float, colMorning: Float, colEvening: Float,
        colTotal: Float, colRate: Float, colAmount: Float
    ): Float {
        // Table header background (Rich Espresso Brown)
        paint.color = COLOR_HEADER_BG
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(android.graphics.RectF(40f, startY, rightMargin, startY + 28f), 6f, 6f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Date", colDate, startY + 18f, paint)
        canvas.drawText("Item", colItem, startY + 18f, paint)

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Morning", colMorning, startY + 18f, paint)
        canvas.drawText("Evening", colEvening, startY + 18f, paint)
        canvas.drawText("Total", colTotal, startY + 18f, paint)
        canvas.drawText("Rate", colRate, startY + 18f, paint)
        canvas.drawText("Amount", colAmount, startY + 18f, paint)

        return startY + 28f
    }

    private fun fmtQty(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
    private fun fmtNum(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.2f", v)

    fun generateDailyReportPdf(
        context: Context, dateStr: String, items: List<DailyReportRow>, totalQty: Double, totalAmount: Double
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.color = THEME_COLOR
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Daily Report - $dateStr", 50f, 50f, paint)

        var y = 100f
        paint.textSize = 12f
        paint.color = Color.BLACK
        items.forEach { row ->
            canvas.drawText(row.customer.shopName, 50f, y, paint)
            canvas.drawText(row.sale.quantity.toString(), 250f, y, paint)
            canvas.drawText("₹" + row.sale.amount.toString(), 350f, y, paint)
            y += 20f
        }
        
        y += 20f
        paint.color = THEME_COLOR
        canvas.drawText("Total Qty: $totalQty", 50f, y, paint)
        canvas.drawText("Total Amount: ₹$totalAmount", 250f, y, paint)

        pdfDocument.finishPage(page)
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val dir = File(baseDir, "TeaSupplierBills")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "DailyReport.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch(e: Exception) { e.printStackTrace() }
        pdfDocument.close()
        return file
    }

    fun generateMonthlyReportPdf(
        context: Context, monthStr: String, items: List<MonthlyReportRow>, totalQty: Double, totalRevenue: Double, totalPaid: Double, totalOutstanding: Double
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.color = THEME_COLOR
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Monthly Report - $monthStr", 50f, 50f, paint)

        var y = 100f
        paint.textSize = 10f
        paint.color = Color.BLACK
        items.forEach { row ->
            canvas.drawText(row.customer.shopName, 50f, y, paint)
            canvas.drawText("Qty: " + row.totalQuantity, 200f, y, paint)
            canvas.drawText("Amt: ₹" + row.totalAmount, 280f, y, paint)
            canvas.drawText("Paid: ₹" + row.totalPaid, 370f, y, paint)
            canvas.drawText("Pend: ₹" + row.outstanding, 460f, y, paint)
            y += 20f
        }
        
        y += 20f
        paint.color = THEME_COLOR
        canvas.drawText("Total Qty: $totalQty", 50f, y, paint)
        canvas.drawText("Total Rev: ₹$totalRevenue", 150f, y, paint)
        canvas.drawText("Total Paid: ₹$totalPaid", 280f, y, paint)
        canvas.drawText("Total Pend: ₹$totalOutstanding", 400f, y, paint)

        pdfDocument.finishPage(page)
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val dir = File(baseDir, "TeaSupplierBills")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "MonthlyReport.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch(e: Exception) { e.printStackTrace() }
        pdfDocument.close()
        return file
    }

    fun generateCustomerStatementPdf(
        context: Context, customer: CustomerEntity, items: List<CustomerTransactionRow>, totalQty: Double, totalAmount: Double, totalPaid: Double, outstanding: Double
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.color = THEME_COLOR
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Customer Statement - " + customer.shopName, 50f, 50f, paint)

        var y = 100f
        paint.textSize = 12f
        paint.color = Color.BLACK
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        items.forEach { row ->
            canvas.drawText(df.format(Date(row.date)), 50f, y, paint)
            if (row.type == TransactionType.SALE) {
                canvas.drawText("Sale Qty: " + row.quantity, 150f, y, paint)
                canvas.drawText("Amt: ₹" + row.amount, 280f, y, paint)
            } else {
                canvas.drawText("Payment", 150f, y, paint)
                canvas.drawText("Paid: ₹" + row.amount, 280f, y, paint)
            }
            y += 20f
        }
        
        y += 20f
        paint.color = THEME_COLOR
        canvas.drawText("Total Qty: $totalQty", 50f, y, paint)
        canvas.drawText("Total Bill: ₹$totalAmount", 180f, y, paint)
        canvas.drawText("Total Paid: ₹$totalPaid", 320f, y, paint)
        canvas.drawText("Pend: ₹$outstanding", 460f, y, paint)

        pdfDocument.finishPage(page)
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val dir = File(baseDir, "TeaSupplierBills")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "CustomerStatement.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch(e: Exception) { e.printStackTrace() }
        pdfDocument.close()
        return file
    }

    fun generateTableBill(
        context: Context,
        tableNumber: Int,
        orders: List<TableOrderEntity>,
        grandTotal: Double,
        hotelNameMarathi: String = "हॉटेल रुद्रा अमृततुल्य",
        hotelNameEnglish: String = "Hotel Rudra Amruttulya",
        hotelAddress: String = "",
        hotelPhone: String = "",
        upiId: String = ""
    ): File? {
        val pdfDocument = PdfDocument()
        val pageHeight = maxOf(760, 320 + orders.size * 25 + 240)
        val pageWidth = 420
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Background
        paint.color = Color.parseColor("#FFFDF7")
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        // Top Amber accent bar
        paint.color = COLOR_PRIMARY
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 8f, paint)

        // Outer border
        paint.color = COLOR_BORDER_YELLOW
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(15f, 15f, (pageWidth - 15).toFloat(), (pageHeight - 15).toFloat(), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        val center = pageWidth / 2f
        var y = 45f

        // Marathi Font
        val marathiTypeface = try {
            Typeface.createFromAsset(context.assets, "fonts/YatraOne-Regular.ttf")
        } catch (e: Exception) {
            try {
                Typeface.createFromAsset(context.assets, "fonts/Mukta-ExtraBold.ttf")
            } catch (e2: Exception) {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }

        // Hotel Name Marathi
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marathiTypeface
        paint.textSize = 24f
        paint.color = COLOR_PRIMARY
        canvas.drawText(hotelNameMarathi.ifBlank { "हॉटेल रुद्रा अमृततुल्य" }, center, y, paint)
        y += 24f

        // Hotel Name English
        if (hotelNameEnglish.isNotBlank()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 14f
            paint.color = COLOR_HEADER_BG
            canvas.drawText(hotelNameEnglish, center, y, paint)
            y += 18f
        }

        // Address & Phone
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = COLOR_TEXT_SECONDARY
        if (hotelAddress.isNotBlank()) {
            canvas.drawText(hotelAddress, center, y, paint)
            y += 15f
        }
        if (hotelPhone.isNotBlank()) {
            canvas.drawText(hotelPhone, center, y, paint)
            y += 15f
        }

        // Dashed divider line
        y += 5f
        paint.color = COLOR_DIVIDER
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 4f), 0f)
        canvas.drawLine(25f, y, (pageWidth - 25).toFloat(), y, paint)
        paint.pathEffect = null
        paint.style = Paint.Style.FILL

        y += 22f
        // Table Number badge + Date
        paint.color = COLOR_PRIMARY
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(25f, y - 16f, 150f, y + 10f, 6f, 6f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText("TABLE  #$tableNumber", 38f, y + 2f, paint)

        // Date right-aligned
        paint.textAlign = Paint.Align.RIGHT
        paint.color = COLOR_TEXT_PRIMARY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        canvas.drawText(sdf.format(Date()), (pageWidth - 30).toFloat(), y + 2f, paint)

        y += 25f

        // Table Header Box
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FEF3C7")
        canvas.drawRoundRect(25f, y, (pageWidth - 25).toFloat(), y + 26f, 4f, 4f, paint)

        paint.color = COLOR_HEADER_BG
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Item / वस्तू", 35f, y + 17f, paint)

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Qty", 230f, y + 17f, paint)
        canvas.drawText("Rate", 305f, y + 17f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Amount", (pageWidth - 35).toFloat(), y + 17f, paint)

        y += 32f

        // Items List
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        var totalQty = 0.0

        orders.forEach { item ->
            totalQty += item.quantity
            paint.textAlign = Paint.Align.LEFT
            paint.color = COLOR_TEXT_PRIMARY
            canvas.drawText(item.productName, 35f, y + 14f, paint)

            paint.textAlign = Paint.Align.CENTER
            paint.color = COLOR_TEXT_SECONDARY
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            canvas.drawText(qtyStr, 230f, y + 14f, paint)
            canvas.drawText("₹${String.format(Locale.getDefault(), "%.0f", item.rate)}", 305f, y + 14f, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.color = COLOR_TEXT_PRIMARY
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", item.amount)}", (pageWidth - 35).toFloat(), y + 14f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            y += 24f
            paint.color = Color.parseColor("#F3F4F6")
            paint.strokeWidth = 0.8f
            canvas.drawLine(35f, y - 4f, (pageWidth - 35).toFloat(), y - 4f, paint)
        }

        // Summary Divider
        y += 8f
        paint.color = COLOR_PRIMARY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(4f, 3f), 0f)
        canvas.drawLine(25f, y, (pageWidth - 25).toFloat(), y, paint)
        paint.pathEffect = null
        paint.style = Paint.Style.FILL

        y += 18f
        // Total Qty Row
        paint.textAlign = Paint.Align.LEFT
        paint.color = COLOR_TEXT_SECONDARY
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val totalQtyStr = if (totalQty % 1.0 == 0.0) totalQty.toInt().toString() else totalQty.toString()
        canvas.drawText("Total Items: ${orders.size} (Qty: $totalQtyStr)", 35f, y, paint)

        y += 22f
        // Grand Total Box
        paint.color = Color.parseColor("#FEF3C7")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(25f, y - 16f, (pageWidth - 25).toFloat(), y + 20f, 6f, 6f, paint)

        paint.color = COLOR_PRIMARY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(25f, y - 16f, (pageWidth - 25).toFloat(), y + 20f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.textAlign = Paint.Align.LEFT
        paint.color = COLOR_HEADER_BG
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GRAND TOTAL", 35f, y + 7f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = COLOR_PRIMARY
        paint.textSize = 18f
        canvas.drawText("₹ ${String.format(Locale.getDefault(), "%.2f", grandTotal)}", (pageWidth - 35).toFloat(), y + 7f, paint)

        y += 36f

        // ─── UPI Payment QR Code Section ───
        val actualUpi = upiId.trim().ifBlank { "8788138197@ybl" }
        val qrBitmap = QrCodeGenerator.generateUpiPaymentQr(
            upiId = actualUpi,
            payeeName = hotelNameEnglish.ifBlank { "Hotel Rudra Amruttulya" },
            amount = grandTotal,
            note = "Table $tableNumber Bill",
            sizePx = 300
        )

        if (qrBitmap != null) {
            val cardLeft = 30f
            val cardRight = (pageWidth - 30).toFloat()
            val cardHeight = 155f
            val cardTop = y

            // Card background (pure white)
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawRoundRect(android.graphics.RectF(cardLeft, cardTop, cardRight, cardTop + cardHeight), 8f, 8f, paint)

            // Card border (warm amber / gold)
            paint.style = Paint.Style.STROKE
            paint.color = COLOR_BORDER_YELLOW
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(android.graphics.RectF(cardLeft, cardTop, cardRight, cardTop + cardHeight), 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            // Top Header strip of card
            paint.color = Color.parseColor("#FEF3C7")
            canvas.drawRoundRect(android.graphics.RectF(cardLeft, cardTop, cardRight, cardTop + 26f), 8f, 8f, paint)
            canvas.drawRect(cardLeft, cardTop + 18f, cardRight, cardTop + 26f, paint)

            paint.color = COLOR_HEADER_BG
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("SCAN & PAY WITH ANY UPI APP / स्कॅन करून बिल भरा", center, cardTop + 17f, paint)

            // QR code in center of card
            val qrSize = 85f
            val qrLeft = center - (qrSize / 2f)
            val qrTop = cardTop + 33f
            val srcRect = android.graphics.Rect(0, 0, qrBitmap.width, qrBitmap.height)
            val dstRect = android.graphics.RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
            canvas.drawBitmap(qrBitmap, srcRect, dstRect, null)

            // UPI ID and payment apps below QR
            paint.color = COLOR_TEXT_PRIMARY
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            canvas.drawText("UPI ID: $actualUpi", center, cardTop + 130f, paint)

            paint.color = COLOR_TEXT_SECONDARY
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f
            canvas.drawText("Google Pay  •  PhonePe  •  Paytm  •  BHIM", center, cardTop + 144f, paint)

            y += cardHeight + 16f
        } else {
            // Fallback text if QR couldn't be generated
            paint.textAlign = Paint.Align.CENTER
            paint.color = COLOR_TEXT_SECONDARY
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Pay via UPI: $actualUpi", center, y, paint)
            y += 20f
        }

        // Thank you footer
        y += 10f
        paint.textAlign = Paint.Align.CENTER
        paint.color = COLOR_PRIMARY
        paint.typeface = marathiTypeface
        paint.textSize = 14f
        canvas.drawText("धन्यवाद ! पुन्हा भेट द्या", center, y, paint)

        y += 16f
        paint.color = COLOR_TEXT_SECONDARY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        canvas.drawText("Thank you! Visit again.", center, y, paint)

        pdfDocument.finishPage(page)

        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val directory = File(baseDir, "TeaSupplierBills")
        if (!directory.exists()) directory.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(directory, "Bill_Table_${tableNumber}_$timeStamp.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }

        return file
    }

    fun generateExpenseSheet(
        context: Context,
        expenses: List<ExpenseEntity>,
        startMs: Long,
        endMs: Long,
        hotelNameMarathi: String = "हॉटेल रुद्रा अमृततुल्य",
        hotelNameEnglish: String = "Hotel Rudra Amruttulya",
        hotelAddress: String = "",
        hotelPhone: String = ""
    ): File? {
        if (expenses.isEmpty()) return null

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val leftMargin = 36f
        val rightMargin = (pageWidth - 36).toFloat()

        val marathiTypeface = try {
            Typeface.createFromAsset(context.assets, "fonts/YatraOne-Regular.ttf")
        } catch (e: Exception) {
            try {
                Typeface.createFromAsset(context.assets, "fonts/Mukta-ExtraBold.ttf")
            } catch (e2: Exception) {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }

        val dfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dfRange = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val periodStr = "${dfRange.format(Date(startMs))} - ${dfRange.format(Date(endMs))}"
        val totalAmount = expenses.sumOf { it.amount }

        val categoryBreakdown = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val rowsPerPage = 22
        val totalPages = maxOf(1, (expenses.size + rowsPerPage - 1) / rowsPerPage)

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Background
            paint.color = Color.parseColor("#FFFDF7")
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

            // Top Amber Accent Line
            paint.color = COLOR_PRIMARY
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, paint)

            var y = 35f

            if (pageIndex == 0) {
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = marathiTypeface
                paint.textSize = 22f
                paint.color = COLOR_PRIMARY
                canvas.drawText(hotelNameMarathi.ifBlank { "हॉटेल रुद्रा अमृततुल्य" }, pageWidth / 2f, y, paint)
                y += 20f

                if (hotelNameEnglish.isNotBlank()) {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 13f
                    paint.color = COLOR_HEADER_BG
                    canvas.drawText(hotelNameEnglish, pageWidth / 2f, y, paint)
                    y += 16f
                }

                if (hotelAddress.isNotBlank() || hotelPhone.isNotBlank()) {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 9.5f
                    paint.color = COLOR_TEXT_SECONDARY
                    val details = listOfNotNull(hotelAddress.ifBlank { null }, hotelPhone.ifBlank { null }).joinToString("  •  ")
                    canvas.drawText(details, pageWidth / 2f, y, paint)
                    y += 14f
                }

                y += 6f
                // Title Banner
                paint.color = COLOR_HEADER_BG
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(android.graphics.RectF(leftMargin, y, rightMargin, y + 26f), 6f, 6f, paint)

                paint.color = Color.WHITE
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 11f
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("HOTEL EXPENSE SHEET / खर्च पत्रक", leftMargin + 12f, y + 17f, paint)

                paint.textAlign = Paint.Align.RIGHT
                paint.textSize = 10f
                canvas.drawText("Period: $periodStr", rightMargin - 12f, y + 17f, paint)
                y += 34f

                // Summary Stats Card
                val summaryCardHeight = 44f
                paint.color = Color.parseColor("#FEF3C7")
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(android.graphics.RectF(leftMargin, y, rightMargin, y + summaryCardHeight), 6f, 6f, paint)

                paint.color = COLOR_BORDER_YELLOW
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(android.graphics.RectF(leftMargin, y, rightMargin, y + summaryCardHeight), 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                paint.color = COLOR_TEXT_PRIMARY
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 11f
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Total Items: ${expenses.size}", leftMargin + 16f, y + 26f, paint)

                val topCats = categoryBreakdown.take(3).joinToString(", ") { "${it.first}: ₹${it.second.toInt()}" }
                if (topCats.isNotBlank()) {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 9.5f
                    paint.color = COLOR_TEXT_SECONDARY
                    canvas.drawText("Top: $topCats", leftMargin + 115f, y + 26f, paint)
                }

                paint.textAlign = Paint.Align.RIGHT
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 13f
                paint.color = Color.parseColor("#B91C1C")
                canvas.drawText("Grand Total: ₹ ${String.format(Locale.getDefault(), "%.2f", totalAmount)}", rightMargin - 16f, y + 27f, paint)

                y += summaryCardHeight + 12f
            } else {
                paint.textAlign = Paint.Align.LEFT
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 11f
                paint.color = COLOR_PRIMARY
                canvas.drawText("${hotelNameEnglish.ifBlank { "Hotel Rudra Amruttulya" }} — Expense Sheet (Contd.)", leftMargin, y, paint)

                paint.textAlign = Paint.Align.RIGHT
                paint.textSize = 9.5f
                paint.color = COLOR_TEXT_SECONDARY
                canvas.drawText(periodStr, rightMargin, y, paint)
                y += 18f
            }

            // Table Header
            val colSrNo = leftMargin + 6f
            val colDate = leftMargin + 38f
            val colCategory = leftMargin + 115f
            val colDesc = leftMargin + 220f
            val colAmount = rightMargin - 10f

            paint.color = COLOR_HEADER_BG
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(android.graphics.RectF(leftMargin, y, rightMargin, y + 24f), 4f, 4f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("#", colSrNo, y + 16f, paint)
            canvas.drawText("Date", colDate, y + 16f, paint)
            canvas.drawText("Category", colCategory, y + 16f, paint)
            canvas.drawText("Description", colDesc, y + 16f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Amount (₹)", colAmount, y + 16f, paint)

            y += 24f

            val startIndex = pageIndex * rowsPerPage
            val endIndex = minOf(startIndex + rowsPerPage, expenses.size)

            paint.textSize = 9.5f
            for (i in startIndex until endIndex) {
                val exp = expenses[i]
                val rowHeight = 22f

                if (i % 2 == 1) {
                    paint.color = Color.parseColor("#FFFBEB")
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(leftMargin, y, rightMargin, y + rowHeight, paint)
                }

                paint.color = COLOR_DIVIDER
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.5f
                canvas.drawLine(leftMargin, y + rowHeight, rightMargin, y + rowHeight, paint)
                paint.style = Paint.Style.FILL

                paint.color = COLOR_TEXT_SECONDARY
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("${i + 1}", colSrNo, y + 15f, paint)

                paint.color = COLOR_TEXT_PRIMARY
                canvas.drawText(dfDate.format(Date(exp.date)), colDate, y + 15f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = COLOR_PRIMARY
                canvas.drawText(exp.category.take(16), colCategory, y + 15f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = COLOR_TEXT_PRIMARY
                val descText = if (exp.description.length > 32) exp.description.take(29) + "..." else exp.description
                canvas.drawText(descText, colDesc, y + 15f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = COLOR_TEXT_PRIMARY
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("₹ ${String.format(Locale.getDefault(), "%.2f", exp.amount)}", colAmount, y + 15f, paint)

                y += rowHeight
            }

            if (pageIndex == totalPages - 1) {
                y += 6f
                paint.color = COLOR_YELLOW_ACCENT
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(android.graphics.RectF(leftMargin, y, rightMargin, y + 26f), 4f, 4f, paint)

                paint.color = COLOR_BORDER_YELLOW
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(android.graphics.RectF(leftMargin, y, rightMargin, y + 26f), 4f, 4f, paint)
                paint.style = Paint.Style.FILL

                paint.color = COLOR_HEADER_BG
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10.5f
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Total (${expenses.size} expenses)", leftMargin + 12f, y + 17f, paint)

                paint.textAlign = Paint.Align.RIGHT
                paint.textSize = 11.5f
                paint.color = Color.parseColor("#B91C1C")
                canvas.drawText("₹ ${String.format(Locale.getDefault(), "%.2f", totalAmount)}", rightMargin - 10f, y + 17f, paint)
            }

            paint.color = COLOR_TEXT_SECONDARY
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f
            paint.textAlign = Paint.Align.LEFT
            val genDateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("Generated on $genDateStr", leftMargin, pageHeight - 20f, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Page ${pageIndex + 1} of $totalPages", rightMargin, pageHeight - 20f, paint)

            pdfDocument.finishPage(page)
        }

        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val directory = File(baseDir, "TeaSupplierBills")
        if (!directory.exists()) directory.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(directory, "Expense_Sheet_$timeStamp.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }

        return file
    }
}

