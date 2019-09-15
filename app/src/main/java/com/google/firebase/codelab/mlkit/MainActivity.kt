package com.google.firebase.codelab.mlkit

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel
import com.google.firebase.ml.custom.*
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.experimental.and

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private val TAG = MainActivity::class.java.name
    private var mSelectedImage: Bitmap? = null
    private var mImageMaxWidth: Int? = null
    private var mImageMaxHeight: Int? = null
    private var mLableList = mutableListOf<String>()
    private lateinit var mInterpreter: FirebaseModelInterpreter
    private lateinit var mDataOptions: FirebaseModelInputOutputOptions

    companion object {
        const val HOSTED_MODEL_NAME = "cloud_model_image1"
        const val LOCAL_MODEL_ASSET = "mobilenet_v1_1.0_224_quant.tflite"
        const val LABEL_PATH = "labels.txt"
        const val RESULTS_TO_SHOW = 3
        const val DIM_BATCH_SIZE = 1
        const val DIM_PIXEL_SIZE = 3
        const val DIM_IMG_SIZE_X = 224
        const val DIM_IMG_SIZE_Y = 224
    }

    private val sortedLabels: PriorityQueue<Map.Entry<String,Float>> =
        PriorityQueue(RESULTS_TO_SHOW,
            compareBy<Map.Entry<String,Float>>{it.value})

    private var intValues: IntArray = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button_text.setOnClickListener { view ->
            runTextRecognition()
        }
        button_face.setOnClickListener { view ->
            runFaceContourDetection()
        }
        button_cloud_text.setOnClickListener { view ->
            runCloudTextRecognition()
        }
        button_run_custom_model.setOnClickListener { view ->
            runModelInference()
        }

        val items = arrayOf("Test Image 1(Text)","Test Image 2 (Text)","Test Image 3 (Face)",
            "Test Image 4 (Object)","Test Image 5 (Object)")
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = this
        initCustomModel()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        graphic_overlay.clear()
        when(position) {
            0 -> mSelectedImage = getBitmapFromAsset(this, "Please_walk_on_the_grass.jpg")
            1 -> mSelectedImage = getBitmapFromAsset(this, "nl2.jpg")
            2 -> mSelectedImage = getBitmapFromAsset(this, "grace_hopper.jpg")
            3 -> mSelectedImage = getBitmapFromAsset(this, "tennis.jpg")
            4 -> mSelectedImage = getBitmapFromAsset(this, "mountain.jpg")
        }
        if(mSelectedImage != null) {
            val (targetWidth,targetHeight) = getTargetWidthHeight()
            val scaleFactor = Math.max(
                mSelectedImage!!.width.toFloat() / targetWidth.toFloat(),
                mSelectedImage!!.height / targetHeight.toFloat()
            )
            val resizedBitmap = Bitmap.createScaledBitmap(
                mSelectedImage!!,
                (mSelectedImage!!.width/scaleFactor).toInt(),
                (mSelectedImage!!.height/scaleFactor).toInt(),
                true
            )
            image_view.setImageBitmap(resizedBitmap)
            mSelectedImage = resizedBitmap
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }

    fun getBitmapFromAsset(context: Context, filepath: String): Bitmap? {
        val assetManager = context.assets
        var inputStream: InputStream
        var bitmap: Bitmap? = null
        try {
            inputStream = assetManager.open(filepath)
            bitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmap
    }

    private fun getTargetWidthHeight(): Pair<Int,Int> {
        return Pair(getImageMaxWidth()!!,getImageMaxHeight()!!)
    }
    private fun getImageMaxWidth(): Int? {
        if(mImageMaxWidth == null) {
            mImageMaxWidth = image_view.width
        }
        return mImageMaxWidth
    }
    private fun getImageMaxHeight(): Int? {
        if(mImageMaxHeight == null) {
            mImageMaxHeight = image_view.height
        }
        return mImageMaxHeight
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    @Synchronized private fun getTopLabels(labelProbArray: Array<ByteArray>): List<String> {
        for(i in mLableList.indices) {
            sortedLabels.add(
                AbstractMap.SimpleEntry<String,Float>(mLableList.get(i),
                    ((labelProbArray[0][i].toInt() and 0xff) / 255.0f)
            ))
            if(sortedLabels.size > RESULTS_TO_SHOW) {
                sortedLabels.poll()
            }
        }
        var result = ArrayList<String>()
        for(i in sortedLabels.indices) {
            val label = sortedLabels.poll()
            result.add(label.key+": "+label.value)
        }
        Log.d(TAG,"labels: "+result.toString())
        return result
    }

    private fun loadLabelList(activity: Activity): MutableList<String> {
        val labelList = mutableListOf<String>()
        BufferedReader(InputStreamReader(activity.assets.open(LABEL_PATH))).use { reader ->
            var line: String? = null
            while({line = reader.readLine(); line}() != null) {
                labelList.add(line!!)
            }
        }
        return labelList
    }

    @Synchronized private fun convertBitmapToByteBuffer(bitmap:Bitmap, width:Int, height:Int): ByteBuffer {
        val imgData = ByteBuffer
            .allocateDirect(DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
        imgData.order(ByteOrder.nativeOrder())
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true)
        imgData.rewind()
//        Log.i(TAG,"length: "+intValues.size)
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.width,
            0,0,scaledBitmap.width,scaledBitmap.height)
        var pixel = 0
        for(i in 0 until DIM_IMG_SIZE_X) {
            for(j in 0 until DIM_IMG_SIZE_Y) {
                val intVal = intValues[pixel++]
                imgData.put(((intVal shr  16) and  0xFF).toByte())
                imgData.put(((intVal shr  8) and  0xFF).toByte())
                imgData.put((intVal and  0xFF).toByte())
            }
        }
        return imgData
    }

    private fun initCustomModel() {
        mLableList = loadLabelList(this)
        val inputDims = intArrayOf(DIM_BATCH_SIZE, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, DIM_PIXEL_SIZE)
        val outputDims = intArrayOf(DIM_BATCH_SIZE, mLableList.size)
        try {
            mDataOptions = FirebaseModelInputOutputOptions.Builder()
                .setInputFormat(0,FirebaseModelDataType.BYTE,inputDims)
                .setOutputFormat(0,FirebaseModelDataType.BYTE,outputDims)
                .build()
            val conditions = FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build()
            val remoteModel = FirebaseRemoteModel.Builder(HOSTED_MODEL_NAME)
                .enableModelUpdates(true)
                .setInitialDownloadConditions(conditions)
                .setUpdatesDownloadConditions(conditions)
                .build()
            val localModel = FirebaseLocalModel.Builder("asset")
                .setAssetFilePath(LOCAL_MODEL_ASSET)
                .build()
            val manager = FirebaseModelManager.getInstance()
            manager.registerRemoteModel(remoteModel)
            manager.registerLocalModel(localModel)
            val modelOptions = FirebaseModelOptions.Builder()
                .setRemoteModelName(HOSTED_MODEL_NAME)
                .setLocalModelName("asset")
                .build()
            mInterpreter = FirebaseModelInterpreter.getInstance(modelOptions)!!
        } catch (e: java.lang.Exception) {
            showToast("Error while setting up the model")
            e.printStackTrace()
        }
    }

    private fun runTextRecognition() {
        val image = FirebaseVisionImage.fromBitmap(mSelectedImage!!)
        val recognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
        button_text.isEnabled = false
        recognizer.processImage(image)
            .addOnSuccessListener { texts ->
                button_text.isEnabled = true
                processTextRecognitionResult(texts)
            }
            .addOnFailureListener { e ->
                button_text.isEnabled = true
                e.printStackTrace()
            }
    }
    private fun processTextRecognitionResult(texts: FirebaseVisionText) {
        val blocks = texts.textBlocks
        if(blocks.isEmpty()) {
            showToast("No text found")
            return
        }
        graphic_overlay.clear()
        for(i in blocks.indices) {
            val lines = blocks.get(i).lines
            for(j in lines.indices) {
                val elements = lines.get(j).elements
                for(k in elements.indices) {
                    val textGraphic = TextGraphic(graphic_overlay,elements.get(k))
                    graphic_overlay.add(textGraphic)
                }
            }
        }
    }

    private fun runFaceContourDetection() {
        val image = FirebaseVisionImage.fromBitmap(mSelectedImage!!)
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()
        button_face.isEnabled = false
        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
        detector.detectInImage(image)
            .addOnSuccessListener { faces ->
                button_face.isEnabled = true
                processFaceContourDetectionResult(faces)
            }
            .addOnFailureListener { e ->
                button_face.isEnabled = true
                e.printStackTrace()
            }

    }
    private fun processFaceContourDetectionResult(faces: List<FirebaseVisionFace>) {
        if(faces.isEmpty()) {
            showToast("No face found")
            return
        }
        graphic_overlay.clear()
        for(i in faces.indices) {
            val face = faces.get(i)
            val faceGraphic = FaceContourGraphic(graphic_overlay)
            graphic_overlay.add(faceGraphic)
            faceGraphic.updateFace(face)
        }
    }

    private fun runCloudTextRecognition() {
        button_cloud_text.isEnabled = false
        val image = FirebaseVisionImage.fromBitmap(mSelectedImage!!)
        val recognizer = FirebaseVision.getInstance().getCloudDocumentTextRecognizer()
        recognizer.processImage(image)
            .addOnSuccessListener { texts ->
                button_cloud_text.isEnabled = true
                processCloudTextRecognitionResult(texts)
            }
            .addOnFailureListener { e ->
                button_cloud_text.isEnabled = true
                e.printStackTrace()
            }
    }
    private fun processCloudTextRecognitionResult(text: FirebaseVisionDocumentText) {
        if(text == null) {
            showToast("No text found")
            return
        }
        graphic_overlay.clear()
        val blocks = text.blocks
        for(i in blocks.indices) {
            val paragraphs = blocks[i].paragraphs
            for(j in paragraphs.indices) {
                val words = paragraphs[j].words
                for(m in words.indices) {
                    val cloudDocumentTextGraphic = CloudTextGraphic(graphic_overlay,words[m])
                    graphic_overlay.add(cloudDocumentTextGraphic)
                }
            }
        }
    }

    private fun runModelInference() {
        if(mInterpreter == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped")
            return
        }
        val imgData = convertBitmapToByteBuffer(mSelectedImage!!,
            mSelectedImage!!.width,mSelectedImage!!.height)
        try {
            val inputs = FirebaseModelInputs.Builder()
                .add(imgData)
                .build()
            mInterpreter.run(inputs, mDataOptions)
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    showToast("Error running model inferance")
                }
//                .continueWith(object :Continuation<FirebaseModelOutputs,List<String>> {
//                    override fun then(task: Task<FirebaseModelOutputs>): List<String> {
//                        Log.i(TAG,"continue task")
//                        val labelProbArray = task.result!!.getOutput<Array<ByteArray>>(0)
//                        Log.i(TAG,"labelProbArray: "+labelProbArray)
//                        val topLabels = getTopLabels(labelProbArray)
//                        graphic_overlay.clear()
//                        val labelGraphic = LabelGraphic(graphic_overlay, topLabels);
//                        graphic_overlay.add(labelGraphic)
//                        return topLabels
//                    }
//                })
                .continueWith{ task ->
                    val labelProbArray = task.result!!.getOutput<Array<ByteArray>>(0)
                    val topLabels = getTopLabels(labelProbArray)
                    graphic_overlay.clear()
                    val labelGraphic = LabelGraphic(graphic_overlay, topLabels)
                    graphic_overlay.add(labelGraphic)
                }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showToast("Error running model inference")
        }
    }
}
