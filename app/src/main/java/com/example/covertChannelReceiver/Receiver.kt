package com.example.covertChannelReceiver

object Receiver {

    // CONFIGURATION START (same configuration is required for Transmitter and Receiver, also manually set same operative modes)
    private const val digitsString = "0123456789"
    private const val alphabetString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val charIfUnknown = '■'
    private var specialCharsString = " $charIfUnknown!@#\$%^&*()_+-"

    // signal values
    private var dogSignal = 1
    private var cowSignal = 2
    private var catSignal = 3
    private var fluteSignal = 5
    private var birdSignal = 7
    private var sheepSignal = 9
    private var breathingSignal = 10
    private var fartSignal = 20
    private var horseSignal = 40
    private var turkeySignal = 60

    // CONFIGURATION END

    // sound labels detected by yamnet model
    private const val dogLabel = "Dog"
    private const val cowLabel = "Moo"
    private const val catLabel = "Meow"
    private const val fluteLabel = "Flute"
    private const val birdLabel = "Bird"
    private const val sheepLabel = "Sheep"
    private const val breathingLabel = "Breathing"
    private const val fartLabel = "Fart"
    private const val horseLabel = "Horse"
    private const val turkeyLabel = "Turkey"
    private const val buzzerLabel = "Buzzer"
    val listenFor = arrayListOf(dogLabel, cowLabel, catLabel, fluteLabel, birdLabel, sheepLabel, breathingLabel, fartLabel, horseLabel, turkeyLabel, buzzerLabel)

    // counters for received specified signals
    private var dogNum = 0
    private var cowNum = 0
    private var catNum = 0
    private var fluteNum = 0
    private var birdNum = 0
    private var sheepNum = 0
    private var breathingNum = 0
    private var fartNum = 0
    private var horseNum = 0
    private var turkeyNum = 0

    // possible status of Receiver based on actual transmission state
    const val RECEIVING_CHAR = 0
    const val CHAR_TRANSMISSION_END = 1
    const val SEQUENCE_TRANSMISSION_END = 2
    const val TRANSMISSION_END = 3
    // initial status for Receiver
    var receiverStatus = RECEIVING_CHAR

    // possible modes of Receiver set by user (must be the same for Transmitter and Receiver)
    const val READING_ALL_MODE = 1
    const val READING_NUMBER_MODE = 2
    // initial mode for Receiver
    var receiverMode = READING_ALL_MODE

    // possible data expected by Receiver based on current mode
    const val RECEIVING_NAME = 0
    const val RECEIVING_NUMBER = 1
    // initial expected data for Receiver
    var currentlyReceiving = RECEIVING_NAME

    // trace number of transmissions (for ALL_MODE transmission is Name and Number, for NUMBER_MODE transmission is only number)
    var currentTransmission = 0

    // store last three signals which indicates current state of Receiver
    private val lastThreeSignals = arrayListOf<String>()

    // store data received in current transmission
    var receivedSequence = ""

    //
    var receiverSemaphore = false

    private var transmissionDictionary = linkedMapOf<Int , Char>()

    fun configureReceiver() {

        // populate transmission dictionary
        transmissionDictionary = linkedMapOf()
        var i = 1

        for(singleChar in digitsString.toCharArray()) {
            transmissionDictionary[i] = singleChar
            i++
        }

        for(singleChar in specialCharsString.toCharArray()) {
            transmissionDictionary[i] = singleChar
            i++
        }

        for(singleChar in alphabetString.toCharArray()) {
            transmissionDictionary[i] = singleChar
            i++
        }
    }

    private fun onCharTransmitted() {

        receiverStatus = CHAR_TRANSMISSION_END

        val transmissionValue = getTransmissionValue()
        receivedSequence += transmissionDictionary[transmissionValue]

        dogNum = 0
        cowNum = 0
        catNum = 0
        fluteNum = 0
        birdNum = 0
        sheepNum = 0
        breathingNum = 0
        fartNum = 0
        horseNum = 0
        turkeyNum = 0
    }

    private fun onSequenceTransmitted() {
        receiverStatus = SEQUENCE_TRANSMISSION_END
        receiverSemaphore = true

        if (receiverMode == READING_ALL_MODE) {

            if (currentlyReceiving == RECEIVING_NAME)
                currentlyReceiving = RECEIVING_NUMBER
            else {
                currentlyReceiving = RECEIVING_NAME
                currentTransmission += 1
            }
        }
        else
            currentTransmission += 1

        receivedSequence = ""
    }

    private fun getTransmissionValue() : Int {
        return (dogNum * dogSignal) + (cowNum * cowSignal) + (catNum * catSignal) + (fluteNum * fluteSignal) +
                    (birdNum * birdSignal) + (sheepNum * sheepSignal) + (breathingNum * breathingSignal) + (fartNum * fartSignal) +
                        (horseNum * horseSignal) + (turkeyNum * turkeySignal)
    }

    fun onSoundDetected(detectedSound : String) {

        receiverStatus = RECEIVING_CHAR

        when (detectedSound) {
            horseLabel -> horseNum++
            dogLabel -> dogNum++
            cowLabel -> cowNum++
            catLabel -> catNum++
            fluteLabel -> fluteNum++
            birdLabel -> birdNum++
            sheepLabel -> sheepNum++
            breathingLabel -> breathingNum++
            fartLabel -> fartNum++
            turkeyLabel -> turkeyNum++
        }

        // rewriting in order to maintain size 3 array with three last signals only
        if(detectedSound == buzzerLabel) {

            if(lastThreeSignals.size == 3) {
                lastThreeSignals[2] = lastThreeSignals[1]
                lastThreeSignals[1] = lastThreeSignals[0]
            }
            lastThreeSignals.add(0, detectedSound)
        }
        else {
            lastThreeSignals.clear()
        }

        when (lastThreeSignals.count { it == buzzerLabel }) {
            1 -> onCharTransmitted()
            2 -> onSequenceTransmitted()
            3 -> receiverStatus = TRANSMISSION_END
        }
    }

    fun toggleTransmissionMode() {
        if (receiverMode == READING_ALL_MODE)
            receiverMode = READING_NUMBER_MODE
        else
            receiverMode = READING_ALL_MODE
    }
}