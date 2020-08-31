package com.example.biophonie.viewmodels

import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.biophonie.database.NewSoundDatabase
import com.example.biophonie.domain.GeoPoint
import com.example.biophonie.domain.Sound
import com.example.biophonie.domain.dateAsCalendar
import com.example.biophonie.repositories.GeoPointRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.mapboxsdk.geometry.LatLng
import fr.haran.soundwave.controller.DefaultPlayerController
import fr.haran.soundwave.ui.PlayerView
import kotlinx.coroutines.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class BottomPlayerViewModel(private val repository: GeoPointRepository) : ViewModel() {

    private var amplitudes = arrayOf(-0.009, 0.005, 0.004, 0.0, 0.001, -0.004, 0.002, -0.004, -0.002, -0.002, 0.006, 0.008, 0.019, -0.039, 0.121, 0.126, 0.046, -0.026, -0.027, -0.095, -0.0, -0.017, 0.006, -0.067, 0.026, -0.08, 0.009, 0.018, -0.018, -0.005, -0.002, -0.003, 0.005, 0.011, 0.002, -0.004, 0.003, 0.01, 0.011, -0.011, -0.003, 0.002, -0.001, -0.008, 0.001, 0.001, -0.011, -0.009, 0.004, 0.012, -0.05, 0.235, 0.152, -0.073, -0.152, -0.006, 0.091, 0.001, -0.068, -0.063, -0.021, -0.018, 0.05, 0.001, -0.018, 0.048, 0.001, -0.029, -0.004, 0.009, -0.014, 0.006, -0.001, 0.001, 0.001, -0.798, -0.37, 0.749, -0.496, -0.14, -0.036, -0.014, 0.014, 0.038, -0.014, -0.026, -0.004, -0.01, 0.003, -0.011, -0.001, -0.01, -0.015, 0.001, -0.002, -0.002, 0.015, -0.019, -0.019, 0.028, 0.014, 0.003, -0.054, -0.047, -0.013, 0.005, 0.007, -0.005, 0.006, -0.03, 0.147, 0.056, -0.051, 0.067, 0.048, 0.347, 0.039, -0.319, 0.386, 0.339, 0.036, 0.152, -0.016, -0.013, 0.016, -0.011, -0.0, -0.002, 0.006, 0.0, -0.002, -0.011, -0.008, -0.009, -0.001, -0.004, 0.016, 0.013, 0.004, 0.004, 0.004, 0.002, -0.002, 0.004, 0.001, 0.008, 0.001, -0.007, -0.001, -0.001, -0.009, -0.003, 0.011, 0.028, 0.074, 0.042, -0.006, 0.023, 0.051, -0.003, 0.001, 0.167, 0.235, -0.191, -0.016, -0.043, 0.434, -0.266, 0.045, -0.128, -0.08, 0.068, -0.075, 0.042, 0.076, 0.068, 0.012, -0.009, 0.006, 0.025, 0.021, 0.024, 0.023, -0.005, -0.017, 0.003, -0.01, 0.001, -0.022, -0.006, 0.032, 0.009, 0.016, -0.002, -0.017, 0.001, 0.006, -0.002, 0.587, -0.614, -0.108, -0.274, -0.06, -0.01, -0.005, -0.01, 0.01, -0.001, -0.003, -0.007, 0.008, 0.009, -0.005, 0.004, 0.014, -0.003, 0.004, -0.007, 0.007, 0.01, 0.002, -0.007, 0.019, -0.001, -0.008, 0.002, -0.021, 0.006, -0.051, 0.033, -0.016, 0.025, -0.029, -0.024, 0.021, -0.014, -0.002, -0.001, -0.016, 0.022, 0.011, -0.002, 0.011, -0.001, -0.01, 0.008, -0.001, 0.005, 0.001, -0.009, 0.006, -0.007, 0.037, -0.133, -0.021, 0.024, -0.009, -0.035, -0.049, 0.041, 0.012, -0.016, 0.041, -0.014, 0.038, -0.055, -0.012, 0.028, 0.022, -0.011, 0.0, 0.004, 0.003, -0.006, 0.006, -0.002, 0.001, -0.007, -0.004, -0.008, 0.005, -0.004, -0.004, -0.011, 0.009, 0.0, 0.004, -0.01, 0.007, -0.021, 0.115, -0.198, 0.044, 0.028, -0.006, -0.068, -0.036, -0.005, 0.014, -0.015, 0.016, 0.006, -0.043, -0.009, 0.0, -0.017, -0.008, -0.011, -0.005, -0.002, -0.011, 0.011, -0.012, 0.008, 0.0, 0.674, 0.06, -0.032, -0.353, 0.186, -0.021, 0.08, -0.028, -0.021, 0.081, -0.027, -0.006, -0.009, 0.004, -0.002, 0.003, 0.008, 0.007, 0.006, -0.01, -0.016, 0.017, 0.001, 0.004, 0.036, 0.031, -0.028, -0.079, 0.031, -0.017, -0.014, 0.007, 0.001, 0.009, 0.014, 0.066, 0.008, -0.025, -0.021, -0.066, -0.245, 0.419, 0.41, -0.157, -0.103, 0.128, 0.139, -0.058, 0.029, -0.015, 0.003, 0.001, -0.006, 0.005, -0.006, 0.004, -0.005, -0.005, -0.01, 0.004, -0.001, 0.017, -0.001, -0.002, -0.001, -0.002, -0.014, 0.001, -0.001, -0.001, 0.004, 0.013, 0.012, -0.01, -0.01, 0.0, -0.004, -0.004, 0.068, 0.155, -0.033, -0.029, -0.063, 0.011, 0.052, 0.052, -0.071, -0.001, 0.193, -0.101, -0.041, -0.103, -0.085, -0.041, -0.177, -0.099, 0.054, 0.041, -0.017, 0.079, -0.025, -0.026, 0.02, -0.008, -0.009, 0.017, 0.007, -0.012, 0.007, 0.031, -0.018, 0.011, 0.002, 0.002, -0.001, -0.01, 0.019, -0.013, 0.002, -0.005, 0.003, 0.004, 0.004, -0.186, -0.759, 0.197, 0.172, 0.186, -0.094, 0.006, 0.012, 0.0, -0.008, 0.006, 0.007, 0.006, 0.009, 0.004, 0.006, 0.004, 0.004, 0.004, -0.007, 0.001, -0.007, -0.006, 0.014, -0.019, -0.021, 0.005, 0.045, 0.004, 0.001, 0.028, -0.007, 0.028, -0.014, 0.022, 0.036, 0.027, 0.013, 0.004, 0.002, 0.015, -0.009, -0.0, 0.014, 0.006, -0.005, 0.003, 0.002, -0.004, -0.002, 0.002, -0.006, -0.008, -0.014, 0.072, -0.182, 0.022, -0.047, 0.043, 0.025, -0.011, 0.006, -0.04, 0.038, -0.08, -0.01, -0.014, 0.034, 0.008, -0.001, 0.019, -0.013, 0.001, 0.007, 0.016, -0.003, -0.008, 0.002, 0.009, 0.004, -0.007, -0.003, -0.001, 0.006, -0.005, 0.01, -0.005, 0.005, 0.005, -0.003, 0.005, -0.011, 0.084, -0.236, -0.112, 0.034, 0.014, 0.01, -0.048, 0.001, 0.009, 0.009, 0.014, 0.014, 0.03, 0.001, -0.017, -0.014, -0.009, 0.009, -0.004, -0.006, -0.001, 0.006, 0.005, -0.009, 0.0, -0.04, 0.865, 0.59, 0.472, 0.04, 0.077, -0.013, -0.011, -0.043, -0.055, 0.037, 0.004, -0.01, 0.003, 0.006, -0.008, 0.016, -0.008, 0.009, 0.009, -0.017, 0.001, -0.012, 0.018, -0.036, -0.026, 0.008, 0.05, -0.048, 0.004, 0.009, 0.017, 0.001, 0.009, 0.004, 0.002, 0.026, -0.01, 0.075, 0.008, 0.194, -0.388, -0.164, 0.148, 0.015, 0.056, 0.191, 0.107, -0.016, -0.021, 0.004, 0.0, -0.004, -0.006, -0.006, -0.002, 0.007, -0.003, 0.0, -0.001, -0.007, -0.007, -0.005, 0.005, 0.003, 0.002, -0.01, -0.003, 0.007, -0.008, -0.009, -0.001, 0.019, -0.0, -0.002, -0.004, 0.007, -0.004, 0.054, 0.022, -0.033, -0.051, -0.017, 0.17, -0.063, 0.068, 0.009, 0.01, -0.118, -0.222, -0.03, -0.004, 0.121, -0.034, -0.119, -0.174, -0.013, 0.021, -0.071, 0.022, -0.046, -0.007, 0.011, -0.001, -0.007, -0.034, -0.018, 0.001, -0.021, 0.024, 0.019, 0.002, 0.002, -0.014, -0.006, -0.015, 0.006, 0.009, -0.009, -0.01, -0.002, 0.015, -0.002, -0.819, 1.0, 0.217, -0.031, 0.098, 0.021, -0.025, 0.011, -0.019, -0.005, -0.006, -0.009, -0.003, 0.01, -0.006, 0.0, 0.006, 0.003, -0.003, 0.004, 0.006, 0.012, -0.02, 0.005, 0.022, -0.002, 0.012, -0.042, 0.004, -0.02, 0.002, 0.011, 0.045, -0.0, 0.011, 0.024, -0.048, 0.009, -0.001, -0.003, 0.006, 0.036, -0.001, 0.001, 0.0, 0.002, -0.005, -0.008, 0.001, -0.006, 0.015, -0.002, -0.005, -0.01, -0.098, -0.095, 0.07, -0.149, -0.03, 0.028, 0.004, 0.063, 0.006, 0.026, 0.093, -0.011, 0.006, -0.021, -0.056, 0.046, -0.004, 0.0, 0.008, -0.004, -0.001, -0.001, 0.005, -0.003, -0.008, -0.005, 0.001, 0.016, 0.009, 0.011, -0.004, -0.001, -0.012, 0.001, 0.006, 0.005, 0.002, -0.053, 0.043, 0.12, -0.031, 0.066, 0.036, -0.005, 0.031, 0.014, 0.04, -0.01, -0.003, -0.019, 0.021, -0.02, 0.051, -0.03, -0.022, 0.007, -0.005, -0.003, -0.009, 0.008, 0.001, -0.004, -0.05, 0.651, -0.385, 0.104, 0.486, 0.183, -0.032, 0.013, 0.031, 0.05, 0.018, -0.038, -0.005, 0.004, 0.004, 0.0, 0.003, -0.003, 0.001, 0.013, 0.003, -0.007, -0.002, -0.009, -0.006, -0.009, -0.006, -0.004, 0.043, 0.01, -0.022, 0.013, -0.015, -0.016, 0.014, -0.121, -0.095, 0.033, 0.019, -0.014, -0.287, 0.362, 0.491, 0.239, -0.105, 0.153, 0.153, -0.017, -0.055, -0.007, 0.002, -0.01, 0.001, 0.003, 0.009, 0.009, -0.015, -0.01, 0.001, 0.012, 0.003, -0.001, -0.003, -0.011, -0.006, 0.006, -0.001, -0.009, 0.001, -0.0, -0.014, -0.006, 0.004, -0.021, -0.004, 0.007, 0.0, -0.007, 0.011, 0.057, 0.008, -0.02, 0.009, -0.031, 0.013, 0.142, -0.299, -0.011, 0.078, 0.072, 0.109, 0.1, -0.055, -0.065, 0.0, -0.093, 0.036, 0.01, -0.008, -0.027, -0.076, -0.057, -0.009, 0.025, 0.014, -0.0, 0.019, -0.005, -0.023, 0.009, 0.022, 0.061, 0.007, -0.002, 0.008, -0.003, -0.002, -0.015, -0.01, 0.001, 0.001, -0.015, -0.001, 0.009, 0.101, -0.691, 0.313, 0.52, -0.063, 0.0, 0.009, -0.013, -0.002, -0.001, -0.002, -0.004, 0.001, 0.003, -0.004, 0.0, 0.011, -0.013, -0.013, -0.001, 0.005, -0.006, -0.006, 0.001, 0.005, -0.006, 0.003, -0.059, -0.036, -0.052, -0.019, 0.026, -0.003, -0.061, 0.008, -0.005, -0.0, -0.005, -0.005, -0.004, 0.003, -0.007)
    private lateinit var soundsIterator: ListIterator<Sound>
    lateinit var playerController: DefaultPlayerController
    var sound: Sound? = null

    private val _bottomSheetState = MutableLiveData<Int>()
    val bottomSheetState: LiveData<Int>
        get() = _bottomSheetState

    private val _visibility = MutableLiveData<Boolean>()
    val visibility: LiveData<Boolean>
        get() = _visibility

    private val _isNetworkErrorShown = MutableLiveData<Boolean>()
    val isNetworkErrorShown: LiveData<Boolean>
        get() = _isNetworkErrorShown

    private val _eventNetworkError = MutableLiveData<Boolean>()
    val eventNetworkError: LiveData<Boolean>
        get() = _eventNetworkError

    private val _leftClickable = MutableLiveData<Boolean>()
    val leftClickable: LiveData<Boolean>
        get() = _leftClickable

    private val _rightClickable = MutableLiveData<Boolean>()
    val rightClickable: LiveData<Boolean>
        get() = _rightClickable

    private val _title: MutableLiveData<String> = MutableLiveData()
    val title: LiveData<String>
        get() = _title

    private val _date: MutableLiveData<String> = MutableLiveData()
    val date: LiveData<String>
        get() = _date

    private val _datePicker: MutableLiveData<String> = MutableLiveData()
    val datePicker: LiveData<String>
        get() = _datePicker

    private val viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(viewModelJob + Dispatchers.Main)
    val geoPoint: LiveData<GeoPoint> = repository.geoPoint

    fun setPlayerController(context: Context, view: PlayerView){
        playerController = DefaultPlayerController(view).apply { setPlayerListener() }
        //TODO(find the right sound)
        val uri = Uri.parse("android.resource://${context.packageName}/raw/france")
        try {
            playerController.addAudioFileUri(context, uri, amplitudes)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun onNetworkErrorShown() {
        _isNetworkErrorShown.value = true
    }

    fun onLeftClick(){
        soundsIterator.previous()
        sound = soundsIterator.previous()
        displaySound(sound!!)
    }

    fun onRightClick(){
        sound = soundsIterator.next()
        displaySound(sound!!)
    }

    //TODO something's wrong, need to click twice to take into account the changes
    private fun checkClickability(sounds: List<Sound>){
        // A bit of a hack due to ListIterators' behavior.
        // The index is between two elements.
        try {
            Log.d(TAG, "checkClickability: previous URL " + sounds[soundsIterator.previousIndex()-1].soundPath)
            _leftClickable.value = true
        } catch (e: IndexOutOfBoundsException){
            _leftClickable.value = false
        }
        try {
            Log.d(TAG, "checkClickability: next URL "+ sounds[soundsIterator.nextIndex()].soundPath)
            _rightClickable.value = true
        } catch (e: IndexOutOfBoundsException){
            _rightClickable.value = false
        }
    }

    private fun displaySound(sound: Sound) {
        checkClickability(geoPoint.value?.sounds!!)
        val calendar: Calendar = sound.dateAsCalendar()
        _date.value = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(calendar.time)
        _datePicker.value = SimpleDateFormat("MMM yyyy", Locale.FRANCE).format(calendar.time)
        _title.value = sound.title
        _visibility.value = true
    }

    fun getGeoPoint(id: String, name: String, coordinates: LatLng){
        _bottomSheetState.value = BottomSheetBehavior.STATE_COLLAPSED
        if (geoPoint.value?.id == id)
            return
        _visibility.value = false
        viewModelScope.launch {
            try {
                repository.fetchGeoPoint(id, name, coordinates)
                soundsIterator = geoPoint.value!!.sounds!!.listIterator()
                sound = soundsIterator.next()
                displaySound(sound!!)
                _isNetworkErrorShown.value = true
                _eventNetworkError.value = false
            } catch (networkError: IOException) {
                // Show a Toast error message and hide the progress bar.
                if(geoPoint.value == null) {
                    _isNetworkErrorShown.value = false
                    _eventNetworkError.value = true
                }
            }
        }
    }

    class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BottomPlayerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BottomPlayerViewModel(GeoPointRepository(NewSoundDatabase.getInstance(context))) as T
            }
            throw IllegalArgumentException("Unknown class name")
        }

    }
}