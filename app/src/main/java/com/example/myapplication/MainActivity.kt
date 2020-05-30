package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.permission_alertdialog.view.*

class MainActivity : AppCompatActivity() {
    var image_uri: Uri? = null

    companion object {
        private val IMAGE_PICK_CODE = 1000;
        private val PERMISSION_CODE_GALLERY = 1001;
        private val OPEN_CAMERA_CODE = 2000;
        private val PERMISSION_CODE_CAMERA = 2001
        const val image_path = "image_path"
        var allow = false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imgPickBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    requestPermissions(permission, PERMISSION_CODE_GALLERY)
                } else
                    pickImageFromGallery()
            } else
                pickImageFromGallery()
        }
        captureBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                ) {
                    val permission = arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    requestPermissions(permission, PERMISSION_CODE_CAMERA)
                } else
                    openCamera()
            } else
                openCamera()
        }
        redactPhoto.setOnClickListener {
            if (allow == true) {
                val dialogView =
                    LayoutInflater.from(this).inflate(R.layout.permission_alertdialog, null)
                val builder = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setTitle("Attention!")
                val alertDialog = builder.show()
                alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.GRAY))
                dialogView.dialogYesBtnPerm.setOnClickListener {
                    alertDialog.dismiss()
                    val intent = Intent(this, RedactImageActivity::class.java)
                    intent.putExtra("image_path", image_uri.toString())
                    startActivity(intent)
                }
                dialogView.dialogNoBtnPerm.setOnClickListener {
                    alertDialog.dismiss()
                }
            }
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, OPEN_CAMERA_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                PERMISSION_CODE_GALLERY ->
                    pickImageFromGallery()
                PERMISSION_CODE_CAMERA ->
                    openCamera()
            }
        } else
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK)
            when (requestCode) {
                OPEN_CAMERA_CODE -> {
                    imageView.setImageURI(image_uri)
                    allow = true
                }
                IMAGE_PICK_CODE -> {
                    imageView.setImageURI(data?.data)
                    image_uri = data?.data
                    allow = true
                }
            }
    }
}
