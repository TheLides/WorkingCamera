package com.example.myapplication


import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import kotlinx.android.synthetic.main.activity_redact_image.*
import kotlinx.android.synthetic.main.any_degree_alertdialog.view.*
import kotlinx.android.synthetic.main.any_degree_alertdialog.view.dialogCancelBtn
import kotlinx.android.synthetic.main.brightness_alertdialog.view.*
import kotlinx.android.synthetic.main.unsharp_alertdialog.view.*
import kotlinx.android.synthetic.main.return_original_alertdialog.view.*
import kotlinx.android.synthetic.main.scaling_alertdialog.view.*
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin


class RedactImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_redact_image)
        val imageString: String? = intent.getStringExtra("image_path")
        val imageUri = Uri.parse(imageString)
        val bitmapTemp = decodeSampledBitmapFromFile(imageUri, 724, 1024, this)
        imageView.setImageBitmap(bitmapTemp)

        buttonTurn.setOnClickListener {
            showPopupTurn()
        }
        buttonFilters.setOnClickListener {
            showPopFilters()
        }
        buttonSave.setOnClickListener {
            saveImage(imageView.drawable.toBitmap(), this, "Name")
        }
        buttonReturnOriginal.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.return_original_alertdialog, null)
            val builder = AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle("Attention!")
            val  alertDialog = builder.show()
            alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.GRAY))
            dialogView.dialogYesBtn.setOnClickListener {
                alertDialog.dismiss()
                imageView.setImageBitmap(bitmapTemp)
            }
            dialogView.dialogNoBtn.setOnClickListener {
                alertDialog.dismiss()
            }
        }
        buttonScaling.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.scaling_alertdialog, null)
            val builder = AlertDialog.Builder(this)
                .setView(dialogView)
            val  alertDialog = builder.show()
            alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.GRAY))
            dialogView.dialogScaleBtn.setOnClickListener {
                alertDialog.dismiss()
                val string = dialogView.dialogScaling.text.toString()
                val scaleFactor = string.toFloat()/100
                val bitmap = imageView.drawable.toBitmap()
                val bitmapNew = bilinearInterpolation(bitmap, scaleFactor)
                imageView.setImageBitmap(bitmapNew)
            }
            dialogView.dialogCancelBtn.setOnClickListener {
                alertDialog.dismiss()
            }
        }
        buttonBlur.setOnClickListener {
            showPopupUnsharpAndBlur()
        }

    }

    private fun showPopupTurn() {
        val menu = PopupMenu(this, buttonTurn)
        menu.inflate(R.menu.turn_menu)
        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.turn90 -> {
                    val degrees = 90.0
                    val bitmap = imageView.drawable.toBitmap()
                    val newBitmap = Rotation.rotateCw(bitmap,degrees)
                    imageView.setImageBitmap(newBitmap)
                    true
                }
                R.id.turnAny ->{
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.any_degree_alertdialog, null)
                    val builder = AlertDialog.Builder(this)
                        .setView(dialogView)
                    val  alertDialog = builder.show()
                    alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.GRAY))
                    dialogView.dialogEnterBtn.setOnClickListener{
                        alertDialog.dismiss()
                        val string = dialogView.dialogDegree.text.toString()
                            val degrees = string.toDouble()
                            val bitmap = imageView.drawable.toBitmap()
                            val newBitmap = Rotation.rotateCw(bitmap, degrees)
                            imageView.setImageBitmap(newBitmap)
                    }
                    dialogView.dialogCancelBtn.setOnClickListener {
                        alertDialog.dismiss()
                    }
                    true
                }
                else -> false
            }
        }
        menu.show()
    }
    object Rotation {
        fun rotateCw(img: Bitmap, degrees: Double): Bitmap? {
            val width: Int = img.width
            val height: Int = img.height
            val newImage = Bitmap.createBitmap(height, width, img.config)
            val angle = Math.toRadians(degrees)
            val sin = sin(angle)
            val cos = cos(angle)
            val x0 = 0.5 * (width - 1)
            val y0 = 0.5 * (height - 1)
            for (x in 0 until height) {
                for (y in 0 until width) {
                    val a = x - y0
                    val b = y - x0
                    val xx = (+a * cos - b * sin + x0).toInt()
                    val yy = (+a * sin + b * cos + y0).toInt()
                    if (xx in 0 until width && yy in 0 until height)  {
                        newImage.setPixel(x, y, img.getPixel(xx, yy))
                    }
                }
            }
            return newImage;
        }
    }

    private fun showPopFilters() {
        val menu = PopupMenu(this, buttonFilters)
        menu.inflate(R.menu.filters_menu)
        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.changeAllColours -> {
                    val bitmap = imageView.drawable.toBitmap()
                    val bitmap2 = processingBitmap(bitmap, Filter.RGB)
                    imageView.setImageBitmap(bitmap2)
                    true
                }
                R.id.changeOneColour ->{
                    val bitmap = imageView.drawable.toBitmap()
                    val bitmap2 = processingBitmap(bitmap, Filter.Blue)
                    imageView.setImageBitmap(bitmap2)
                    true
                }
                R.id.changeToNegative ->{
                    val bitmap = imageView.drawable.toBitmap()
                    val bitmap2 = processingBitmap(bitmap, Filter.NoRGB)
                    imageView.setImageBitmap(bitmap2)
                    true
                }
                R.id.changeToBlack ->{
                    val lightFactor: Float = 1F
                    val bitmap = imageView.drawable.toBitmap()
                    val bitmap2 = blackAndWhite(bitmap, lightFactor)
                    imageView.setImageBitmap(bitmap2)
                    true
                }
                R.id.changeToGray ->{
                    val bitmap = imageView.drawable.toBitmap()
                    val bitmap2 = grayPicture(bitmap)
                    imageView.setImageBitmap(bitmap2)
                    true
                }
                R.id.changeToSepia ->{
                    val bitmap = imageView.drawable.toBitmap()
                    val bitmap2 = sepiaPicture(bitmap)
                    imageView.setImageBitmap(bitmap2)
                    true
                }
                R.id.changeBrightness ->{
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.brightness_alertdialog, null)
                    val builder = AlertDialog.Builder(this)
                        .setView(dialogView)
                    val  alertDialog = builder.show()
                    alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.GRAY))
                    dialogView.dialogEnterBtnBr.setOnClickListener{
                        alertDialog.dismiss()
                        val string = dialogView.dialogBrightness.text.toString()
                        val lightFactor = string.toFloat()
                        val bitmap = imageView.drawable.toBitmap()
                        val bitmap2 = brightnessPicture(bitmap, lightFactor)
                        imageView.setImageBitmap(bitmap2)
                    }
                    dialogView.dialogCancelBtnBr.setOnClickListener {
                        alertDialog.dismiss()
                    }
                    true
                }
                else -> false
            }
        }
        menu.show()
    }

    internal enum class Filter{
        Blue, NoRGB, Red, Green, RGB;
    }
    private fun processingBitmap(src: Bitmap,enum: Filter): Bitmap? {

        val dest = Bitmap.createBitmap(
            src.width, src.height, src.config
        )
        for (x in 0 until src.width) {
            for (y in 0 until src.height) {
                val pixelColor = src.getPixel(x, y)
                val pixelAlpha = Color.alpha(pixelColor)
                val pixelRed = Color.red(pixelColor)
                val pixelGreen = Color.green(pixelColor)
                val pixelBlue = Color.blue(pixelColor)

                val newPixel = Color.argb(
                    pixelAlpha,
                    if (enum == Filter.Red) pixelRed else if (enum == Filter.RGB) pixelBlue else if (enum == Filter.NoRGB) (255-pixelRed) else 0,
                    if (enum == Filter.Green) pixelBlue else if (enum == Filter.RGB) pixelRed else if (enum == Filter.NoRGB) (255-pixelGreen) else 0,
                    if (enum == Filter.Blue) pixelBlue else if (enum == Filter.RGB) pixelGreen else if (enum == Filter.NoRGB) (255-pixelBlue) else 0
                )
                dest.setPixel(x, y, newPixel)
            }
        }
        return dest
    }
    private fun brightnessPicture(src: Bitmap, brightness: Float): Bitmap? {
        val dest = Bitmap.createBitmap(
            src.width, src.height, src.config
        )
        for (x in 0 until src.width) {
            for (y in 0 until src.height) {
                val pixelColor = src.getPixel(x, y)
                // получим информацию о прозрачности
                var pixelAlpha = abs(((Color.alpha(pixelColor)*brightness))).toInt()
                // получим цвет каждого пикселя
                var pixelRed = abs(((Color.red(pixelColor)*brightness))).toInt()
                var pixelGreen = abs(((Color.green(pixelColor)*brightness))).toInt()
                var pixelBlue = abs(((Color.blue(pixelColor)*brightness))).toInt()

                if(pixelAlpha > 255)
                    pixelAlpha = 255
                if(pixelRed > 255)
                    pixelRed = 255
                if(pixelGreen > 255)
                    pixelGreen = 255
                if(pixelBlue > 255)
                    pixelBlue = 255

                val newPixel = Color.argb(pixelAlpha, pixelRed, pixelGreen, pixelBlue)
                // полученный результат вернём в Bitmap
                dest.setPixel(x, y, newPixel)
            }
        }
        return dest
    }
    private fun blackAndWhite(src: Bitmap, brightness: Float): Bitmap? {
        val dest = Bitmap.createBitmap(
            src.width, src.height, src.config
        )
        val separator = 255 / brightness / 2 * 3
        for (x in 0 until src.width) {
            for (y in 0 until src.height) {
                // получим каждый пиксель
                val pixelColor = src.getPixel(x, y)
                // получим информацию о прозрачности
                val pixelAlpha = abs(((Color.alpha(pixelColor)*brightness))).toInt()
                // получим цвет каждого пикселя
                val pixelRed = abs(((Color.red(pixelColor)*brightness))).toInt()
                val pixelGreen = abs(((Color.green(pixelColor)*brightness))).toInt()
                val pixelBlue = abs(((Color.blue(pixelColor)*brightness))).toInt()
                val total = pixelRed + pixelGreen + pixelBlue
                if (total > separator) {
                    val newPixel = Color.argb(pixelAlpha, 255, 255, 255)
                    dest.setPixel(x, y, newPixel)
                }
                else {
                    val newPixel = Color.argb(pixelAlpha, 0, 0, 0)
                    dest.setPixel(x, y, newPixel)
                }
                // полученный результат вернём в Bitmap
            }
        }
        return dest
    }
    private fun grayPicture(src: Bitmap): Bitmap? {
        val dest = Bitmap.createBitmap(
            src.width, src.height, src.config
        )
        for (x in 0 until src.width) {
            for (y in 0 until src.height) {
                val pixelColor = src.getPixel(x, y)
                // получим информацию о прозрачности
                var pixelAlpha = abs(((Color.alpha(pixelColor))))
                // получим цвет каждого пикселя
                var pixelRed = abs((Color.red(pixelColor)))
                var pixelGreen = abs((Color.green(pixelColor)))
                var pixelBlue = abs((Color.blue(pixelColor)))
                val gray = (pixelRed * 0.2126 + pixelGreen * 0.7152 + pixelBlue * 0.0722).toInt()
                if(pixelAlpha > 255)
                    pixelAlpha = 255
                if(pixelRed > 255)
                    pixelRed = 255
                if(pixelGreen > 255)
                    pixelGreen = 255
                if(pixelBlue > 255)
                    pixelBlue = 255
                val newPixel = Color.argb(pixelAlpha, gray, gray, gray)
                // полученный результат вернём в Bitmap
                dest.setPixel(x, y, newPixel)
            }
        }
        return dest
    }
    private fun sepiaPicture(src: Bitmap): Bitmap? {
        val dest = Bitmap.createBitmap(
            src.width, src.height, src.config
        )
        for (x in 0 until src.width) {
            for (y in 0 until src.height) {
                val pixelColor = src.getPixel(x, y)
                // получим информацию о прозрачности
                var pixelAlpha = abs(((Color.alpha(pixelColor))))
                // получим цвет каждого пикселя
                val pixelRed = abs((Color.red(pixelColor)))
                val pixelGreen = abs((Color.green(pixelColor)))
                val pixelBlue = abs((Color.blue(pixelColor)))
                var red = (pixelRed * 0.393 + pixelGreen * 0.769 + pixelBlue * 0.189).toInt()
                var green = (pixelRed * 0.349 + pixelGreen * 0.686 + pixelBlue * 0.168).toInt()
                var blue = (pixelRed * 0.272 + pixelGreen * 0.534 + pixelBlue * 0.131).toInt()
                if(pixelAlpha > 255)
                    pixelAlpha = 255
                if(red > 255)
                    red = 255
                if(green > 255)
                    green = 255
                if(blue > 255)
                    blue = 255

                val newPixel = Color.argb(pixelAlpha, red, green, blue)
                // полученный результат вернём в Bitmap
                dest.setPixel(x, y, newPixel)
            }
        }
        return dest
    }


    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height
            val halfWidth: Int = width

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun decodeSampledBitmapFromFile(curUri: Uri, reqWidth: Int, reqHeight: Int, context: Context):Bitmap? {
        val bitmap = BitmapFactory.Options().run {
            val stream= context.contentResolver.openInputStream(curUri)
            inJustDecodeBounds = true
            BitmapFactory.decodeStream(stream,null,this)
            //считаем inSampleSize
            inSampleSize = calculateInSampleSize(this,reqWidth,reqHeight)
            //декодирование растр изображения с помощью inSampleSize
            inJustDecodeBounds = false
            val newBitmap = context.contentResolver.openInputStream(curUri)
            BitmapFactory.decodeStream(newBitmap,null,this)
        }
        return bitmap
    }

    private fun bilinearInterpolation(originBitmap: Bitmap, scaleFactor:Float ): Bitmap {
        val widthOriginal = originBitmap.width
        val heightOriginal = originBitmap.height
        val widthSecond= (originBitmap.width*scaleFactor).toInt()
        val heightSecond= (originBitmap.height*scaleFactor).toInt()
        val pixelsArray = IntArray(widthOriginal*heightOriginal)
        originBitmap.getPixels(pixelsArray,0, widthOriginal,0,0, widthOriginal, heightOriginal)
        val temp = IntArray(widthSecond * heightSecond)
        var a: Int
        var b: Int
        var c: Int
        var d: Int
        var x: Int
        var y: Int
        var index: Int
        val xRatio = (widthOriginal - 1).toFloat() / widthSecond
        val yRatio = (heightOriginal - 1).toFloat() / heightSecond
        var xDiff: Float
        var yDiff: Float
        var blue: Float
        var red: Float
        var green: Float
        var offset = 0
        for (i in 0 until heightSecond) {
            for (j in 0 until widthSecond) {
                x = (xRatio * j).toInt()
                y = (yRatio * i).toInt()
                xDiff = xRatio * j - x
                yDiff = yRatio * i - y
                index = y * widthOriginal + x
                a = pixelsArray[index]
                b = pixelsArray[index + 1]
                c = pixelsArray[index + widthOriginal]
                d = pixelsArray[index + widthOriginal + 1]

                blue = (a and 0xff) * (1 - xDiff) * (1 - yDiff) + (b and 0xff) * xDiff * (1 - yDiff) + (c and 0xff) * yDiff * (1 - xDiff) + (d and 0xff) * (xDiff * yDiff)

                green = (a shr 8 and 0xff) * (1 - xDiff) * (1 - yDiff) + (b shr 8 and 0xff) * xDiff * (1 - yDiff) + (c shr 8 and 0xff) * yDiff * (1 - xDiff) + (d shr 8 and 0xff) * (xDiff * yDiff)

                red = (a shr 16 and 0xff) * (1 - xDiff) * (1 - yDiff) + (b shr 16 and 0xff) * xDiff * (1 - yDiff) + (c shr 16 and 0xff) * yDiff * (1 - xDiff) + (d shr 16 and 0xff) * (xDiff * yDiff)
                temp[offset++] = -0x1000000 or  // hardcode alpha
                        (red.toInt() shl 16 and 0xff0000) or
                        (green.toInt() shl 8 and 0xff00) or
                        blue.toInt()

            }
        }
        return Bitmap.createBitmap(temp, widthSecond, heightSecond, Bitmap.Config.ARGB_8888)
    }

    private fun showPopupUnsharpAndBlur(){
        val menu = PopupMenu(this, buttonFilters)
        menu.inflate(R.menu.blur_and_sharp_menu)
        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.blur -> {
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.blur_alertdialog, null)
                    val builder = AlertDialog.Builder(this)
                        .setView(dialogView)
                    val  alertDialog = builder.show()
                    alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.GRAY))
                    alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.GRAY))
                    dialogView.dialogEntBtn.setOnClickListener{
                        alertDialog.dismiss()
                        val stringRad = dialogView.dialogBlurRadius.text.toString()
                        val radius = stringRad.toInt()
                        val bitmap = imageView.drawable.toBitmap()
                        val newBitmap = boxBlur(bitmap, radius)
                        imageView.setImageBitmap(newBitmap)
                    }
                    dialogView.dialogCanBtn.setOnClickListener {
                        alertDialog.dismiss()
                    }
                    true
                }
                R.id.unsharp ->{
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.unsharp_alertdialog, null)
                    val builder = AlertDialog.Builder(this)
                        .setView(dialogView)
                    val  alertDialog = builder.show()
                    alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.GRAY))
                    alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.GRAY))
                    dialogView.dialogEntBtn.setOnClickListener{
                        alertDialog.dismiss()
                        val stringRad = dialogView.dialogBlurRadius.text.toString()
                        val radius = stringRad.toInt()
                        val stringAm = dialogView.dialogBlurAmount.text.toString()
                        val am = stringAm.toFloat()
                        val stringTh = dialogView.dialogBlurThreshold.text.toString()
                        val th = stringTh.toInt()
                        val bitmap = imageView.drawable.toBitmap()
                        val newBitmap = unsharpMask(am, th, radius, bitmap)
                        imageView.setImageBitmap(newBitmap)
                    }
                    dialogView.dialogCanBtn.setOnClickListener {
                        alertDialog.dismiss()
                    }
                    true
                }
                else -> false
            }
        }
        menu.show()

    }

    private fun unsharpMask(
        amount: Float,
        threshold: Int,
        radius: Int,
        bitmapOrig: Bitmap
    ):Bitmap {
        var orgRed = 0
        var orgGreen = 0
        var orgBlue = 0
        var blurredRed = 0
        var blurredGreen = 0
        var blurredBlue = 0
        var usmPixel = 0
        val alpha = -0x1000000
        val pixelsArray = IntArray(bitmapOrig.width*bitmapOrig.height)
        bitmapOrig.getPixels(pixelsArray,0, bitmapOrig.width,0,0, bitmapOrig.width, bitmapOrig.height)
        val bitmapBlur = boxBlur(bitmapOrig, radius)
        val temp = IntArray(bitmapBlur!!.width * bitmapBlur.height)
        bitmapBlur.getPixels(temp,0, bitmapBlur.width,0,0, bitmapBlur.width, bitmapBlur.height)

        for (j in 0 until bitmapOrig.height) {
            for (i in 0 until bitmapOrig.width) {
                val origPixel = pixelsArray[j*bitmapOrig.width+i]
                val blurredPixel = temp[j*bitmapBlur.width+i]
                orgRed = origPixel shr 16 and 0xff
                orgGreen = origPixel shr 8 and 0xff
                orgBlue = origPixel and 0xff
                blurredRed = blurredPixel shr 16 and 0xff
                blurredGreen = blurredPixel shr 8 and 0xff
                blurredBlue = blurredPixel and 0xff

                if (abs(orgRed - blurredRed) >= threshold) {
                    orgRed = (amount * (orgRed - blurredRed) + orgRed).toInt()
                    orgRed = if (orgRed > 255) 255 else if (orgRed < 0) 0 else orgRed
                }
                if (abs(orgGreen - blurredGreen) >= threshold) {
                    orgGreen = (amount * (orgGreen - blurredGreen) + orgGreen).toInt()
                    orgGreen = if (orgGreen > 255) 255 else if (orgGreen < 0) 0 else orgGreen
                }
                if (abs(orgBlue - blurredBlue) >= threshold) {
                    orgBlue = (amount * (orgBlue - blurredBlue) + orgBlue).toInt()
                    orgBlue = if (orgBlue > 255) 255 else if (orgBlue < 0) 0 else orgBlue
                }
                usmPixel = alpha or (orgRed shl 16) or (orgGreen shl 8) or orgBlue
                temp[j*bitmapOrig.width+i] = usmPixel
            }
        }
        return Bitmap.createBitmap(temp, bitmapOrig.width, bitmapOrig.height, Bitmap.Config.ARGB_8888)
    }

    private fun boxBlur(bmp: Bitmap, range: Int): Bitmap? {
        assert(range and 1 == 0) { "Range must be odd." }
        val blurred = Bitmap.createBitmap(
            bmp.width, bmp.height,
            Bitmap.Config.ARGB_8888
        )
        val c = Canvas(blurred)
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(bmp.width * bmp.height)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        boxBlurHorizontal(pixels, w, h, range / 2)
        boxBlurVertical(pixels, w, h, range / 2)
        c.drawBitmap(pixels, 0, w, 0.0f, 0.0f, w, h, true, null)
        return blurred
    }
    private fun boxBlurHorizontal(
        pixels: IntArray, w: Int, h: Int,
        halfRange: Int
    ) {
        var index = 0
        val newColors = IntArray(w)
        for (y in 0 until h) {
            var hits = 0
            var r: Long = 0
            var g: Long = 0
            var b: Long = 0
            for (x in -halfRange until w) {
                val oldPixel = x - halfRange - 1
                if (oldPixel >= 0) {
                    val color = pixels[index + oldPixel]
                    if (color != 0) {
                        r -= Color.red(color).toLong()
                        g -= Color.green(color).toLong()
                        b -= Color.blue(color).toLong()
                    }
                    hits--
                }
                val newPixel = x + halfRange
                if (newPixel < w) {
                    val color = pixels[index + newPixel]
                    if (color != 0) {
                        r += Color.red(color).toLong()
                        g += Color.green(color).toLong()
                        b += Color.blue(color).toLong()
                    }
                    hits++
                }
                if (x >= 0) {
                    newColors[x] = Color.argb(0xFF, (r / hits).toInt(), (g / hits).toInt(), (b / hits).toInt())
                }
            }
            for (x in 0 until w) {
                pixels[index + x] = newColors[x]
            }
            index += w
        }
    }
    private fun boxBlurVertical(
        pixels: IntArray, w: Int, h: Int,
        halfRange: Int
    ) {
        val newColors = IntArray(h)
        val oldPixelOffset = -(halfRange + 1) * w
        val newPixelOffset = halfRange * w
        for (x in 0 until w) {
            var hits = 0
            var r: Long = 0
            var g: Long = 0
            var b: Long = 0
            var index = -halfRange * w + x
            for (y in -halfRange until h) {
                val oldPixel = y - halfRange - 1
                if (oldPixel >= 0) {
                    val color = pixels[index + oldPixelOffset]
                    if (color != 0) {
                        r -= Color.red(color).toLong()
                        g -= Color.green(color).toLong()
                        b -= Color.blue(color).toLong()
                    }
                    hits--
                }
                val newPixel = y + halfRange
                if (newPixel < h) {
                    val color = pixels[index + newPixelOffset]
                    if (color != 0) {
                        r += Color.red(color).toLong()
                        g += Color.green(color).toLong()
                        b += Color.blue(color).toLong()
                    }
                    hits++
                }
                if (y >= 0) {
                    newColors[y] = Color.argb(0xFF, (r / hits).toInt(), (g / hits).toInt(), (b / hits).toInt())
                }
                index += w
            }
            for (y in 0 until h) {
                pixels[y * w + x] = newColors[y]
            }
        }
    }


    private fun saveImage(bitmap: Bitmap, context: Context, folderName: String) {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = contentValues()
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + folderName)
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            // RELATIVE_PATH and IS_PENDING are introduced in API 29.

            val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                saveImageToStream(bitmap, context.contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                context.contentResolver.update(uri, values, null, null)
            }
        } else {
            val directory = File(Environment.getExternalStorageDirectory().toString() + separator + folderName)
            // getExternalStorageDirectory is deprecated in API 29

            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = System.currentTimeMillis().toString() + ".png"
            val file = File(directory, fileName)
            saveImageToStream(bitmap, FileOutputStream(file))
            if (file.absolutePath != null) {
                val values = contentValues()
                values.put(MediaStore.Images.Media.DATA, file.absolutePath)
                // .DATA is deprecated in API 29
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
        }
    }

    private fun contentValues() : ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        return values
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
