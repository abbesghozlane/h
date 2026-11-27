package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.local.CustomerEntity
import com.example.data.local.LoyaltyModes
import com.example.data.local.ShopSettingsEntity
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * Renders the Haider Parfums Black & Gold Digital Loyalty Card to a crisp 1200x760 PNG Bitmap,
 * saves via zero-permission MediaStore, and shares via FileProvider directly to WhatsApp or system sheet.
 */
object LoyaltyCardExporter {

    fun renderLoyaltyCardBitmap(
        context: Context,
        customer: CustomerEntity,
        settings: ShopSettingsEntity
    ): Bitmap {
        val width = 1200
        val height = 760
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cairoTypeface = ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT_BOLD
        val tajawalTypeface = ResourcesCompat.getFont(context, R.font.tajawal) ?: Typeface.DEFAULT

        // 1. Deep Obsidian Card Background
        val cardRect = RectF(16f, 16f, width - 16f, height - 16f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#08080C"),
                    Color.parseColor("#141210"),
                    Color.parseColor("#0B0B10")
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, bgPaint)

        // Subtle warm golden glow behind card
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.78f, height * 0.22f, 460f,
                intArrayOf(Color.parseColor("#38D4AF37"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, glowPaint)

        // 2. Double Metallic Champagne Gold Outer & Inner Borders
        val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#F3D06B"),
                    Color.parseColor("#AA821C"),
                    Color.parseColor("#F3D06B")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, outerBorderPaint)

        val innerRect = RectF(32f, 32f, width - 32f, height - 32f)
        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#55FFD782")
        }
        canvas.drawRoundRect(innerRect, 38f, 38f, innerBorderPaint)

        // 3. Left Brand Emblem (Haider Parfums Crest + Brand Title)
        val crestBitmap = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.haider_app_icon)
        }.getOrNull()
        if (crestBitmap != null) {
            val crestRect = RectF(68f, 64f, 224f, 220f)
            val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.save()
            val clipPath = android.graphics.Path().apply {
                addRoundRect(crestRect, 28f, 28f, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(crestBitmap, null, crestRect, clipPaint)
            canvas.restore()
            canvas.drawRoundRect(crestRect, 28f, 28f, innerBorderPaint)
        }

        val brandGoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = cairoTypeface
            textSize = 36f
            textAlign = Paint.Align.CENTER
            shader = LinearGradient(
                60f, 230f, 230f, 280f,
                Color.parseColor("#F3D06B"),
                Color.parseColor("#D4AF37"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawText("HAIDER", 146f, 266f, brandGoldPaint)

        val brandSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tajawalTypeface
            textSize = 20f
            color = Color.parseColor("#D4AF37")
            letterSpacing = 0.25f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PARFUMS", 146f, 294f, brandSubPaint)

        // 4. Right Side QR Code Box on Cream Card
        val qrSize = 224
        val qrBitmap = generateQrBitmap(customer.loyaltyCode, qrSize)
        val qrCardRect = RectF(width - 294f, 64f, width - 64f, 294f)
        val creamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAF6EE")
        }
        canvas.drawRoundRect(qrCardRect, 24f, 24f, creamPaint)
        val qrDest = Rect(
            (qrCardRect.left + 14).toInt(),
            (qrCardRect.top + 14).toInt(),
            (qrCardRect.right - 14).toInt(),
            (qrCardRect.bottom - 14).toInt()
        )
        canvas.drawBitmap(qrBitmap, null, qrDest, null)
        canvas.drawRoundRect(qrCardRect, 24f, 24f, outerBorderPaint.apply { strokeWidth = 3f })

        val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = cairoTypeface
            textSize = 26f
            color = Color.parseColor("#F3D06B")
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(customer.loyaltyCode, qrCardRect.centerX(), 332f, codePaint)

        // 5. Center Header Copy ("شكراً لك" + Customer Name + Tagline)
        val thankYouPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = cairoTypeface
            textSize = 62f
            textAlign = Paint.Align.CENTER
            shader = LinearGradient(
                400f, 80f, 760f, 150f,
                Color.parseColor("#F3D06B"),
                Color.parseColor("#D4AF37"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawText("شكراً لك", width / 2f, 128f, thankYouPaint)

        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tajawalTypeface
            textSize = 28f
            color = Color.parseColor("#FAF6EE")
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("لثقتك بنا واختيارك لعطورنا الفاخرة", width / 2f, 178f, taglinePaint)

        // Customer Name Golden Pill
        val custPill = RectF(320f, 206f, 840f, 266f)
        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#24D4AF37")
        }
        canvas.drawRoundRect(custPill, 30f, 30f, pillBg)
        canvas.drawRoundRect(custPill, 30f, 30f, innerBorderPaint)
        val custNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = cairoTypeface
            textSize = 30f
            color = Color.parseColor("#FAF6EE")
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${customer.name}  •  ${settings.shopName}", custPill.centerX(), 248f, custNamePaint)

        // 6. Stamp Tracker Grid (10 Gold/Cream HP Circles) OR Points Hero Counter
        val target = settings.stampTarget.coerceIn(1, 20)
        if (customer.loyaltyMode == LoyaltyModes.STAMPS) {
            val cols = min(5, target)
            val rows = (target + cols - 1) / cols
            val circleRadius = if (rows <= 2) 44f else 34f
            val hSpacing = 124f
            val vSpacing = if (rows <= 2) 110f else 84f
            val gridWidth = (cols - 1) * hSpacing
            val startX = (width - gridWidth) / 2f
            val startY = 418f

            val earnedFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, startY - 40f, 0f, startY + 180f,
                    Color.parseColor("#F3D06B"),
                    Color.parseColor("#AA821C"),
                    Shader.TileMode.CLAMP
                )
            }
            val emptyFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F4ECDC")
            }
            val coinRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 5f
                color = Color.parseColor("#D4AF37")
            }
            val earnedText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = cairoTypeface
                textSize = 30f
                color = Color.parseColor("#1A1300")
                textAlign = Paint.Align.CENTER
            }
            val emptyText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = cairoTypeface
                textSize = 26f
                color = Color.parseColor("#9E9583")
                textAlign = Paint.Align.CENTER
            }

            for (i in 0 until target) {
                val r = i / cols
                val c = i % cols
                val cx = startX + c * hSpacing
                val cy = startY + r * vSpacing
                val earned = i < customer.stamps
                canvas.drawCircle(cx, cy, circleRadius, if (earned) earnedFill else emptyFill)
                canvas.drawCircle(cx, cy, circleRadius, coinRing)
                val label = if (earned) "HP" else "${i + 1}"
                canvas.drawText(label, cx, cy + 10f, if (earned) earnedText else emptyText)
            }
        } else {
            val ptsBox = RectF(260f, 380f, 940f, 560f)
            canvas.drawRoundRect(ptsBox, 32f, 32f, pillBg)
            canvas.drawRoundRect(ptsBox, 32f, 32f, outerBorderPaint)
            val ptsBig = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = cairoTypeface
                textSize = 84f
                color = Color.parseColor("#F3D06B")
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("${customer.points}", ptsBox.centerX(), 484f, ptsBig)
            val ptsSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = tajawalTypeface
                textSize = 30f
                color = Color.parseColor("#FAF6EE")
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("نقطة ولاء ذهبية • VIP", ptsBox.centerX(), 536f, ptsSub)
        }

        // 7. Bottom Reward Ribbon Banner
        val bannerRect = RectF(140f, 624f, width - 140f, 704f)
        val bannerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CC141210")
        }
        canvas.drawRoundRect(bannerRect, 40f, 40f, bannerBg)
        canvas.drawRoundRect(bannerRect, 40f, 40f, outerBorderPaint)

        val rewardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = cairoTypeface
            textSize = 30f
            color = Color.parseColor("#F3D06B")
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(settings.rewardText, bannerRect.centerX(), 676f, rewardPaint)

        return bitmap
    }

    fun saveLoyaltyCardToGallery(
        context: Context,
        bitmap: Bitmap,
        loyaltyCode: String
    ): Result<String> {
        return runCatching {
            val fileName = "Haider_Parfums_${loyaltyCode}_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/HaiderParfums"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("تعذر إنشاء ملف الصورة في المعرض")
                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                } ?: error("تعذر كتابة الصورة")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "تم حفظ البطاقة في معرض الصور"
            } else {
                val dir = File(context.cacheDir, "loyalty_cards").apply { mkdirs() }
                val file = File(dir, fileName)
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                "تم حفظ البطاقة بنجاح"
            }
        }
    }

    fun shareLoyaltyCard(
        context: Context,
        bitmap: Bitmap,
        customerName: String,
        loyaltyCode: String,
        preferWhatsApp: Boolean
    ): Result<Unit> {
        return runCatching {
            val dir = File(context.cacheDir, "loyalty_cards").apply { mkdirs() }
            val file = File(dir, "card_${loyaltyCode}.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val caption = "بطاقة الولاء الذهبية من حيدر للعطور ✨\nالزبون: $customerName\nالكود: $loyaltyCode"
            val baseIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (preferWhatsApp) {
                val waIntent = Intent(baseIntent).setPackage("com.whatsapp")
                if (waIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(waIntent)
                    return@runCatching
                }
            }
            val chooser = Intent.createChooser(baseIntent, "مشاركة بطاقة الولاء")
            context.startActivity(chooser)
        }
    }
}
