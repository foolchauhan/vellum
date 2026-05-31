package com.example.vellum.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.vellum.data.local.AccountEntity
import com.example.vellum.data.local.CategoryEntity
import com.example.vellum.data.local.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

object ExportManager {

    fun exportToCsv(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append("Date,Type,Amount,Category,Account,Note\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (tx in transactions) {
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val typeStr = tx.type
            val amountStr = String.format(Locale.US, "%.2f", tx.amount)
            val categoryStr = escapeCsvField(tx.categoryName)
            val accountStr = escapeCsvField(tx.accountName)
            val noteStr = escapeCsvField(tx.note)
            sb.append("$dateStr,$typeStr,$amountStr,$categoryStr,$accountStr,$noteStr\n")
        }
        return sb.toString()
    }

    private fun escapeCsvField(field: String): String {
        val escaped = field.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    fun exportToPdf(
        context: Context,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>,
        accountFilter: String,
        periodLabel: String,
        currencySymbol: String
    ): ByteArray {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - 2 * margin

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        var currentY = margin

        // Calculations
        var totalIncome = 0.0
        var totalExpense = 0.0
        for (tx in transactions) {
            if (tx.type == "INCOME") {
                totalIncome += tx.amount
            } else {
                totalExpense += tx.amount
            }
        }
        val netBalance = totalIncome - totalExpense

        val expenses = transactions.filter { it.type == "EXPENSE" }
        val categoryBreakdown = expenses.groupBy { it.categoryId }
            .map { (catId, txs) ->
                val catName = txs.firstOrNull()?.categoryName ?: categories.find { it.id == catId }?.name ?: "Unknown"
                val amount = txs.sumOf { it.amount }
                val colorHex = categories.find { it.id == catId }?.chartColor ?: "#4E3C30"
                Triple(catName, amount, colorHex)
            }
            .sortedByDescending { it.second }

        // Helper to draw Header on page 1
        fun drawHeaderPage1() {
            // Elegant Blackboard Background
            val blackboardBitmap = android.graphics.BitmapFactory.decodeResource(
                context.resources,
                com.example.vellum.R.drawable.blackboard_background_01
            )
            if (blackboardBitmap != null) {
                val srcRect = android.graphics.Rect(0, 0, blackboardBitmap.width, blackboardBitmap.height)
                val destRect = android.graphics.RectF(margin, currentY, pageWidth - margin, currentY + 70f)
                canvas.drawBitmap(blackboardBitmap, srcRect, destRect, paint)
                blackboardBitmap.recycle()
            } else {
                // Fallback Dark Slate Green Header bar
                paint.color = Color.parseColor("#1B2A27")
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 70f, paint)
            }

            // Draw Logo (load real app icon)
            val appIconBitmap = android.graphics.BitmapFactory.decodeResource(
                context.resources,
                com.example.vellum.R.mipmap.ic_launcher
            )
            if (appIconBitmap != null) {
                val destRect = android.graphics.RectF(margin + 10f, currentY + 15f, margin + 50f, currentY + 55f)
                canvas.drawBitmap(appIconBitmap, null, destRect, paint)
                appIconBitmap.recycle()
            } else {
                // Fallback circle with 'V'
                paint.color = Color.parseColor("#4CAF50")
                canvas.drawCircle(margin + 30f, currentY + 35f, 20f, paint)
                paint.color = Color.WHITE
                paint.textSize = 18f
                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("V", margin + 30f, currentY + 41f, paint)
            }

            // App Name Text
            paint.textSize = 22f
            paint.color = Color.WHITE
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("VELLUM", margin + 65f, currentY + 34f, paint)

            // Subtitle
            paint.textSize = 10f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#B0BEC5")
            canvas.drawText("FINANCIAL STATEMENT & STATEMENT REPORT", margin + 65f, currentY + 52f, paint)

            // Date generated (Right aligned)
            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.WHITE
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
            canvas.drawText("Generated: $dateStr", pageWidth - margin - 15f, currentY + 40f, paint)

            currentY += 70f
        }

        // Helper to draw page footer
        fun drawFooter(pageNum: Int, totalPagesPlaceholder: Boolean = false) {
            paint.color = Color.parseColor("#9E9E9E")
            paint.textSize = 9f
            paint.textAlign = Paint.Align.LEFT
            paint.isFakeBoldText = false
            canvas.drawText("Vellum Finance", margin, pageHeight - 25f, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Page $pageNum", pageWidth - margin, pageHeight - 25f, paint)

            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(margin, pageHeight - 38f, pageWidth - margin, pageHeight - 38f, paint)
        }

        // Start drawing Page 1
        drawHeaderPage1()
        currentY += 15f

        // Metadata block (Filters selected)
        paint.color = Color.parseColor("#F5F5F5")
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 32f, paint)

        paint.color = Color.parseColor("#37474F")
        paint.textSize = 10f
        paint.textAlign = Paint.Align.LEFT
        paint.isFakeBoldText = true
        canvas.drawText("Filters Selected", margin + 10f, currentY + 20f, paint)

        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#546E7A")
        val filterStr = "Account: $accountFilter   |   Period: $periodLabel   |   Transactions: ${transactions.size}"
        canvas.drawText(filterStr, margin + 110f, currentY + 20f, paint)

        currentY += 32f
        currentY += 20f

        // Overview title
        paint.color = Color.parseColor("#1B2A27")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Financial Summary", margin, currentY, paint)
        currentY += 8f

        // Draw horizontal divider line
        paint.color = Color.parseColor("#CFD8DC")
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint)
        currentY += 12f

        // Overview Cards (3 side-by-side)
        val cardWidth = (contentWidth - 24f) / 3f // spacing = 12f
        val cardHeight = 55f

        // Income Card (Index 0)
        var cardLeft = margin
        var rect = RectF(cardLeft, currentY, cardLeft + cardWidth, currentY + cardHeight)
        paint.color = Color.parseColor("#E8F5E9") // Very light green
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.color = Color.parseColor("#4CAF50") // Green outline
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#2E7D32")
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("TOTAL INCOME", cardLeft + 10f, currentY + 18f, paint)
        paint.textSize = 13f
        canvas.drawText(String.format(Locale.US, "%s%.2f", currencySymbol, totalIncome), cardLeft + 10f, currentY + 40f, paint)

        // Expense Card (Index 1)
        cardLeft += cardWidth + 12f
        rect = RectF(cardLeft, currentY, cardLeft + cardWidth, currentY + cardHeight)
        paint.color = Color.parseColor("#FFEBEE") // Very light red
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.color = Color.parseColor("#EF5350") // Red outline
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#C62828")
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("TOTAL EXPENSE", cardLeft + 10f, currentY + 18f, paint)
        paint.textSize = 13f
        canvas.drawText(String.format(Locale.US, "%s%.2f", currencySymbol, totalExpense), cardLeft + 10f, currentY + 40f, paint)

        // Net Balance Card (Index 2)
        cardLeft += cardWidth + 12f
        rect = RectF(cardLeft, currentY, cardLeft + cardWidth, currentY + cardHeight)
        val balanceColor = if (netBalance >= 0) "#E3F2FD" else "#FBE9E7" // Blue vs Orange-red
        val balanceOutline = if (netBalance >= 0) "#42A5F5" else "#FF7043"
        val balanceText = if (netBalance >= 0) "#1565C0" else "#D84315"
        paint.color = Color.parseColor(balanceColor)
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.color = Color.parseColor(balanceOutline)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor(balanceText)
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("NET BALANCE", cardLeft + 10f, currentY + 18f, paint)
        paint.textSize = 13f
        canvas.drawText(String.format(Locale.US, "%s%.2f", currencySymbol, netBalance), cardLeft + 10f, currentY + 40f, paint)

        currentY += cardHeight
        currentY += 24f

        // Category Breakdown
        if (categoryBreakdown.isNotEmpty()) {
            paint.color = Color.parseColor("#1B2A27")
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("Spending by Category", margin, currentY, paint)
            currentY += 8f

            paint.color = Color.parseColor("#CFD8DC")
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint)
            currentY += 15f

            // Limit breakdown to top 5 categories for page 1 space constraints
            val topCategories = categoryBreakdown.take(5)
            for ((catName, amount, colorHex) in topCategories) {
                paint.color = Color.parseColor("#37474F")
                paint.textSize = 10f
                paint.isFakeBoldText = true
                canvas.drawText(catName, margin, currentY + 12f, paint)

                paint.textAlign = Paint.Align.RIGHT
                paint.color = Color.parseColor("#263238")
                val pct = if (totalExpense > 0) (amount / totalExpense) * 100 else 0.0
                canvas.drawText(
                    String.format(Locale.US, "%s%.2f (%.1f%%)", currencySymbol, amount, pct),
                    pageWidth - margin,
                    currentY + 12f,
                    paint
                )
                paint.textAlign = Paint.Align.LEFT

                // Progress Bar Background
                currentY += 18f
                paint.color = Color.parseColor("#ECEFF1")
                canvas.drawRoundRect(RectF(margin, currentY, pageWidth - margin, currentY + 6f), 3f, 3f, paint)

                // Colored progress bar
                paint.color = try {
                    Color.parseColor(colorHex)
                } catch (e: Exception) {
                    Color.parseColor("#4E3C30")
                }
                val fillWidth = if (totalExpense > 0) (amount / totalExpense).toFloat() * contentWidth else 0f
                canvas.drawRoundRect(RectF(margin, currentY, margin + fillWidth, currentY + 6f), 3f, 3f, paint)

                currentY += 18f
            }
            currentY += 10f
        }

        // Detailed Transactions
        paint.color = Color.parseColor("#1B2A27")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Detailed Transactions", margin, currentY, paint)
        currentY += 8f

        paint.color = Color.parseColor("#CFD8DC")
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint)
        currentY += 12f

        // Table Header
        paint.color = Color.parseColor("#F5F5F5")
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 20f, paint)

        paint.color = Color.parseColor("#37474F")
        paint.textSize = 9f
        paint.isFakeBoldText = true

        val colDateX = margin + 8f
        val colCategoryX = margin + 80f
        val colAccountX = margin + 175f
        val colNoteX = margin + 270f
        val colAmountX = pageWidth - margin - 8f

        canvas.drawText("DATE", colDateX, currentY + 13f, paint)
        canvas.drawText("CATEGORY", colCategoryX, currentY + 13f, paint)
        canvas.drawText("ACCOUNT", colAccountX, currentY + 13f, paint)
        canvas.drawText("NOTE", colNoteX, currentY + 13f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("AMOUNT", colAmountX, currentY + 13f, paint)
        paint.textAlign = Paint.Align.LEFT

        currentY += 20f

        val rowHeight = 22f
        val txDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

        fun checkNewPage() {
            if (currentY + rowHeight > pageHeight - 50f) {
                // Finish page
                drawFooter(pageNumber)
                pdfDocument.finishPage(page)

                // Start new page
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin

                // Draw Header for Page 2+
                paint.color = Color.parseColor("#1B2A27")
                paint.textSize = 11f
                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("VELLUM - Detailed Transactions (Continued)", margin, currentY + 15f, paint)
                paint.color = Color.parseColor("#CFD8DC")
                canvas.drawLine(margin, currentY + 22f, pageWidth - margin, currentY + 22f, paint)

                currentY += 30f

                // Redraw table header
                paint.color = Color.parseColor("#F5F5F5")
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 18f, paint)

                paint.color = Color.parseColor("#37474F")
                paint.textSize = 8f
                paint.isFakeBoldText = true

                canvas.drawText("DATE", colDateX, currentY + 12f, paint)
                canvas.drawText("CATEGORY", colCategoryX, currentY + 12f, paint)
                canvas.drawText("ACCOUNT", colAccountX, currentY + 12f, paint)
                canvas.drawText("NOTE", colNoteX, currentY + 12f, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("AMOUNT", colAmountX, currentY + 12f, paint)
                paint.textAlign = Paint.Align.LEFT

                currentY += 18f
            }
        }

        // Draw Rows
        var idx = 0
        val sortedTransactions = transactions.sortedByDescending { it.timestamp }
        for (tx in sortedTransactions) {
            checkNewPage()

            // Alternating backgrounds
            if (idx % 2 == 1) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, paint)
            }

            paint.color = Color.parseColor("#455A64")
            paint.textSize = 9f
            paint.isFakeBoldText = false

            // Date
            val dateStr = txDateFormat.format(Date(tx.timestamp))
            canvas.drawText(dateStr, colDateX, currentY + 14f, paint)

            // Category
            val catName = tx.categoryName
            val displayCat = if (catName.length > 15) catName.take(13) + ".." else catName
            canvas.drawText(displayCat, colCategoryX, currentY + 14f, paint)

            // Account
            val accName = tx.accountName
            val displayAcc = if (accName.length > 15) accName.take(13) + ".." else accName
            canvas.drawText(displayAcc, colAccountX, currentY + 14f, paint)

            // Note
            val noteText = tx.note
            val displayNote = if (noteText.length > 32) noteText.take(30) + ".." else noteText
            canvas.drawText(displayNote, colNoteX, currentY + 14f, paint)

            // Amount (Green for Income, Red for Expense)
            paint.textAlign = Paint.Align.RIGHT
            if (tx.type == "INCOME") {
                paint.color = Color.parseColor("#2E7D32")
                canvas.drawText(String.format(Locale.US, "+%s%.2f", currencySymbol, tx.amount), colAmountX, currentY + 14f, paint)
            } else {
                paint.color = Color.parseColor("#C62828")
                canvas.drawText(String.format(Locale.US, "-%s%.2f", currencySymbol, tx.amount), colAmountX, currentY + 14f, paint)
            }
            paint.textAlign = Paint.Align.LEFT

            // Border bottom line for each row
            paint.color = Color.parseColor("#ECEFF1")
            paint.strokeWidth = 0.5f
            canvas.drawLine(margin, currentY + rowHeight, pageWidth - margin, currentY + rowHeight, paint)

            currentY += rowHeight
            idx++
        }

        // Draw final page footer
        drawFooter(pageNumber)
        pdfDocument.finishPage(page)

        val stream = java.io.ByteArrayOutputStream()
        pdfDocument.writeTo(stream)
        pdfDocument.close()
        return stream.toByteArray()
    }
}
