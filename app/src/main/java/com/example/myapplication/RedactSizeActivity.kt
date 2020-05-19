package com.example.myapplication

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_turn_picture.*

class RedactSizeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_redact_size)
        val imageString: String? = intent.getStringExtra("image_path")
        val imageUri = Uri.parse(imageString)
        imageView.setImageURI(imageUri)


    }
}
