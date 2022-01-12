package com.example.covertChannelReceiver

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.covertChannelReceiver.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.scheduleAtFixedRate

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var contactAdapter : ContactAdapter
    private val listOfContacts = ArrayList<Contact>()

    // defines name of used model
    var modelPath = "lite-model_yamnet_classification_tflite_1.tflite"

    // take all with probability greater than 50%
    var probabilityThreshold: Float = 0.5f

    // keeping trace of time in order to don't take same sound effect many times
    private var initialT = 0L
    private var endT = 0L
    private var deltaT = 0L
    private var elapsedT = 0F

    // indicates whether receiver is working
    private var isReceiving = false

    // ms value of checking for signal
    private val SAMPLING_EACH = 250L

    // request code for permission
    val REQUEST_RECORD_AUDIO = 1337

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)

        contactAdapter = ContactAdapter(listOfContacts)

        binding.recyclerView.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context)
            adapter = contactAdapter
        }

        // toggle transmission mode
        binding.covertChannelHeader.setOnClickListener {

            if (!isReceiving) {

                Receiver.toggleTransmissionMode()

                var transmissionMode = ""

                if (Receiver.receiverMode == Receiver.READING_ALL_MODE) {
                    transmissionMode = getString(R.string.transmitterModeAll)
                } else {
                    transmissionMode = getString(R.string.transmitterModeNumbersOnly)
                }

                Snackbar.make(view, "${getString(R.string.transmissionModeToggle)} $transmissionMode", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.startReceivingBtn.setOnClickListener {
            startTransmissionIfPermissionGranted(view)
        }
    }

    private fun startTransmissionIfPermissionGranted(view : View) {

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array(1) { Manifest.permission.READ_CONTACTS }, REQUEST_RECORD_AUDIO)
            Snackbar.make(view, getString(R.string.permissionDenied), Snackbar.LENGTH_SHORT).show()
            return
        }
        else {
            if (!isReceiving) {
                setReceivingUI()
                startReceiver(view)
            }
        }
    }

    // start receiver and listen for sound signals
    @SuppressLint("WrongConstant")
    private fun startReceiver(view: View) {

        val classifier = AudioClassifier.createFromFile(this, modelPath)
        val tensor = classifier.createInputTensorAudio()
        val record = classifier.createAudioRecord()
        record.startRecording()

        isReceiving = true

        Receiver.configureReceiver()
        Timer().scheduleAtFixedRate(1, SAMPLING_EACH) {

            tensor.load(record)
            val output = classifier.classify(tensor)

            val filteredModelOutput = output[0].categories.filter {
                it.score > probabilityThreshold
            }

            for(modelOutput in filteredModelOutput.sortedBy { -it.score }) {
                val detectedSound = modelOutput.label
                println(detectedSound)
                if (Receiver.listenFor.contains(detectedSound)) {

                    if(initialT == 0L) {
                        initialT = System.currentTimeMillis()
                    }
                    else {
                        endT = System.currentTimeMillis()
                        deltaT = endT - initialT
                        elapsedT = deltaT / 1000F

                        if(elapsedT < 2.5)
                            break

                        initialT = endT
                    }
                    Snackbar.make(view, detectedSound, Toast.LENGTH_SHORT).show()
                    Receiver.onSoundDetected(detectedSound)
                    break
                }
            }

            if (Receiver.receiverStatus != Receiver.RECEIVING_CHAR && elapsedT > 2.5) {
                runOnUiThread {
                    if (Receiver.receiverStatus == Receiver.CHAR_TRANSMISSION_END) {
                        // first received character - init contact list
                        if (listOfContacts.size == 0) {
                            listOfContacts.add(Contact())
                        }
                        // if all-mode
                        if (Receiver.receiverMode == Receiver.READING_ALL_MODE) {
                            if (Receiver.currentlyReceiving == Receiver.RECEIVING_NAME) {
                                listOfContacts[Receiver.currentTransmission].Name = Receiver.receivedSequence
                                contactAdapter.editContact(Receiver.currentTransmission, listOfContacts[Receiver.currentTransmission])
                            }
                            else {
                                listOfContacts[Receiver.currentTransmission].Number = Receiver.receivedSequence
                                contactAdapter.editContact(Receiver.currentTransmission, listOfContacts[Receiver.currentTransmission])
                            }
                        }
                        // only numbers mode
                        else {
                            listOfContacts[Receiver.currentTransmission].Number = Receiver.receivedSequence
                            contactAdapter.editContact(Receiver.currentTransmission, listOfContacts[Receiver.currentTransmission])
                        }
                    }
                    else if (Receiver.receiverStatus == Receiver.SEQUENCE_TRANSMISSION_END && Receiver.receiverSemaphore) {
                        // it means that receiver is expecting new contact so append new item to the contacts recycler view
                        if ( (Receiver.receiverMode == Receiver.READING_NUMBER_MODE) || (Receiver.receiverMode == Receiver.READING_ALL_MODE && Receiver.currentlyReceiving == Receiver.RECEIVING_NAME) ) {
                            contactAdapter.addContact(Receiver.currentTransmission, Contact())
                        }
                        Receiver.receiverSemaphore = false
                    }
                    else if (Receiver.receiverStatus == Receiver.TRANSMISSION_END){
                        // check if last inserted contact actually contains something - if not, remove
                        if (listOfContacts[Receiver.currentTransmission].Number == "") {
                            contactAdapter.removeContact(Receiver.currentTransmission)
                        }
                        Timer().cancel()
                        isReceiving = false
                        setOperativeUI()
                    }
                }
            }
        }
    }

    private fun setReceivingUI() {
        binding.startReceivingBtn.isEnabled = false
        binding.startReceivingBtn.alpha = 0.5F
    }

    private fun setOperativeUI() {
        binding.startReceivingBtn.text = getString(R.string.transmissionFinished)
        binding.startReceivingBtn.alpha = 1.0F
    }
}