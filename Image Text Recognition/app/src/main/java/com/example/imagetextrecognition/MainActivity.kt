package com.example.imagetextrecognition

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.imagetextrecognition.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var image: InputImage
    private lateinit var binding: ActivityMainBinding
    private var tempCameraFile = ""

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
        private const val EXTERNAL_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun galleryBtnClick(view: View){
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getGalleryResult.launch(galleryIntent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val getGalleryResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK){
            image = InputImage.fromFilePath(this, it.data!!.data!!)
            Log.v("GALLERY", image.toString());
            recognizeText(image)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun cameraBtnClick(view: View){
        //check if we have permissions

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), EXTERNAL_REQUEST_CODE)
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            var photo = cameraTempImage()
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", photo))
            tempCameraFile = photo.absolutePath
            getCameraResult.launch(cameraIntent)
        }
        else{
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val getCameraResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == Activity.RESULT_OK){
            val imgFile = File(tempCameraFile)
            if(imgFile.exists()){
//                val myBM : Bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
//                var x = Bitmap.createScaledBitmap(myBM, binding.root.rootView.width, binding.root.rootView.height,false)
//                x.compress(Bitmap.CompressFormat.JPEG, 100, ByteArrayOutputStream());
//                val path: String = MediaStore.Images.Media.insertImage(
//                    this.contentResolver,
//                    x,
//                    "Title",
//                    null
//                )
//                val image = InputImage.fromFilePath(this, Uri.parse(path))
                val image = InputImage.fromFilePath(this, imgFile.toUri())
                recognizeText(image)
            }
        }
    }

    private fun cameraTempImage(): File {
        val timeStamp = System.currentTimeMillis()
        val imageFileName = "NAME_$timeStamp"
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
    }

    private fun scaleImgToScreen(value: Float, scale: Float): Float = value * scale

    @RequiresApi(Build.VERSION_CODES.O)
    private fun recognizeText(image: InputImage) {

        // [START get_detector_default]
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        // [END get_detector_default]

        // [START run_detector]
        val result = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks
                val blocksRect = blocks.mapNotNull { it.boundingBox }

                val lines = blocks.flatMap { it.lines }
                val linesRect = lines.mapNotNull { it.boundingBox }

                val elements = lines.flatMap { it.elements }
                val elementsRect = elements.mapNotNull { it.boundingBox }
                val elementsText = elements.mapNotNull { it.text }

                val inputBitmap = image.bitmapInternal!!
                val mutableBitmap = inputBitmap.copy(inputBitmap.config, true)
                var blockPaint = Paint()
                blockPaint.setARGB(180,255,99,71)

                var linePaint = Paint()
                linePaint.setARGB(180,71,255,99)

                var elementPaint = Paint()
                elementPaint.setARGB(180,99,71,255)

                var _scaleY : Float
                var _scaleX : Float

                val orientation = resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // In landscape
                     _scaleY = binding.root.rootView.height.toFloat() / image.width.toFloat()
                    _scaleX = binding.root.rootView.width.toFloat() / image.height.toFloat()
                } else {
                    // In portrait
                    _scaleY = binding.root.rootView.height.toFloat() / image.height.toFloat()
                    _scaleX = binding.root.rootView.width.toFloat() / image.width.toFloat()
                }


                with(Canvas(mutableBitmap!!)) {

                    blocksRect.forEach { drawRect(it, blockPaint) }
                    linesRect.forEach { drawRect(it, linePaint) }
                    var count = 0
                    elementsRect.forEach {
                        drawRect(it, elementPaint)
                        drawText(elementsText[count],it.exactCenterX(),it.exactCenterY(),Paint(Color.BLACK))
                        val dynamicTextview = TextView(applicationContext)

                        dynamicTextview.text = elementsText[count]
//                        dynamicTextview.x = scaleImgToScreen(it.left.toFloat(), _scaleX)
//                        dynamicTextview.y = scaleImgToScreen(it.top.toFloat(), _scaleY)
//                        dynamicTextview.height = scaleImgToScreen((it.bottom - it.top).toFloat(), _scaleY).toInt()
//                        dynamicTextview.width = scaleImgToScreen((it.right - it.left).toFloat(),_scaleX).toInt()
                        dynamicTextview.x = it.left.toFloat()
                        dynamicTextview.y = it.top.toFloat()
                        dynamicTextview.height = it.bottom - it.top
                        dynamicTextview.width = it.right - it.left
                        dynamicTextview.setBackgroundColor(Color.BLUE)
                        dynamicTextview.setAutoSizeTextTypeUniformWithConfiguration(
                            1, 100, 1, TypedValue.COMPLEX_UNIT_DIP);
                        dynamicTextview.setOnClickListener {
                            Toast.makeText(this@MainActivity, "You clicked me.", Toast.LENGTH_SHORT).show()
                        }
                        binding.ImageLayout.addView(dynamicTextview)
                        count += 1
                    }
                }
                binding.imageView.setImageBitmap(mutableBitmap)
                // [END get_text]
                // [END_EXCLUDE]
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // ...
            }
        // [END run_detector]
    }
}