package com.example.myapplication


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_turn_picture.*
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_turn_picture.imageView


class TurnPictureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_turn_picture)
        seekAn.progress = 180
        val imageString: String? = intent.getStringExtra("image_path")
        val imageUri = Uri.parse(imageString)
        imageView.setImageURI(imageUri)

        redactSizeButton.setOnClickListener {
            val intent = Intent(this, RedactSizeActivity::class.java)
            intent.putExtra("image_path", imageUri.toString())
            startActivity(intent)
        }

        seekAn.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            //  val degrees = seek.progress;
            //val bitmap = imageView.drawable.toBitmap()
            // val newBitmap = Rotation.rotateCw(bitmap, degrees)
            //   imageView.setImageBitmap(newBitmap)
            override fun onProgressChanged(seek: SeekBar, progess: Int , fromUser: Boolean) {
                val bitmap = imageView.drawable.toBitmap()
                val newBitmap = Rotation.rotateCw(bitmap,seekAn.progress.toDouble()-180)
                imageView.setImageBitmap(newBitmap)
            }
            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }
            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped

                Toast.makeText(this@TurnPictureActivity,
                    "Progress is: " + seek.progress + "%",
                    Toast.LENGTH_SHORT).show()
            }
        })

        button.setOnClickListener{
            val degrees = 90.0
            val bitmap = imageView.drawable.toBitmap()
            val newBitmap = Rotation.rotateCw(bitmap,degrees)
            imageView.setImageBitmap(newBitmap)
        }

    }
    object Rotation {
        fun rotateCw(img: Bitmap, degrees: Double): Bitmap? {
            val width: Int = img.getWidth()
            val height: Int = img.getHeight()
            val newImage = Bitmap.createBitmap(height, width, img.getConfig())
            val angle = Math.toRadians(degrees)
            val sin = Math.sin(angle)
            val cos = Math.cos(angle)
            val x0 = 0.5 * (width - 1) // point to rotate about
            val y0 = 0.5 * (height - 1) // center of image
            // rotation
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
}
