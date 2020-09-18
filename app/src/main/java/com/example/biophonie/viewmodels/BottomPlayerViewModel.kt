package com.example.biophonie.viewmodels

import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.biophonie.database.NewSoundDatabase
import com.example.biophonie.domain.GeoPoint
import com.example.biophonie.domain.Sound
import com.example.biophonie.domain.dateAsCalendar
import com.example.biophonie.repositories.GeoPointRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.mapboxsdk.geometry.LatLng
import fr.haran.soundwave.controller.DefaultPlayerController
import fr.haran.soundwave.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class BottomPlayerViewModel(private val repository: GeoPointRepository) : ViewModel() {

    private var amplitudes = arrayOf(-291.0, 162.0, 129.0, 0.0, 32.0, -129.0, 65.0, -129.0, -65.0, -65.0, 194.0, 259.0, 615.0, -1262.0, 3916.0, 4078.0, 1489.0, -841.0, -874.0, -3075.0, -0.0, -550.0, 194.0, -2168.0, 841.0, -2589.0, 291.0, 583.0, -583.0, -162.0, -65.0, -97.0, 162.0, 356.0, 65.0, -129.0, 97.0, 324.0, 356.0, -356.0, -97.0, 65.0, -32.0, -259.0, 32.0, 32.0, -356.0, -291.0, 129.0, 388.0, -1618.0, 7606.0, 4919.0, -2363.0, -4919.0, -194.0, 2945.0, 32.0, -2201.0, -2039.0, -680.0, -583.0, 1618.0, 32.0, -583.0, 1553.0, 32.0, -939.0, -129.0, 291.0, -453.0, 194.0, -32.0, 32.0, 32.0, -25826.0, -11975.0, 24241.0, -16053.0, -4531.0, -1165.0, -453.0, 453.0, 1230.0, -453.0, -841.0, -129.0, -324.0, 97.0, -356.0, -32.0, -324.0, -485.0, 32.0, -65.0, -65.0, 485.0, -615.0, -615.0, 906.0, 453.0, 97.0, -1748.0, -1521.0, -421.0, 162.0, 227.0, -162.0, 194.0, -971.0, 4758.0, 1812.0, -1651.0, 2168.0, 1553.0, 11230.0, 1262.0, -10324.0, 12493.0, 10971.0, 1165.0, 4919.0, -518.0, -421.0, 518.0, -356.0, -0.0, -65.0, 194.0, 0.0, -65.0, -356.0, -259.0, -291.0, -32.0, -129.0, 518.0, 421.0, 129.0, 129.0, 129.0, 65.0, -65.0, 129.0, 32.0, 259.0, 32.0, -227.0, -32.0, -32.0, -291.0, -97.0, 356.0, 906.0, 2395.0, 1359.0, -194.0, 744.0, 1651.0, -97.0, 32.0, 5405.0, 7606.0, -6182.0, -518.0, -1392.0, 14046.0, -8609.0, 1456.0, -4143.0, -2589.0, 2201.0, -2427.0, 1359.0, 2460.0, 2201.0, 388.0, -291.0, 194.0, 809.0, 680.0, 777.0, 744.0, -162.0, -550.0, 97.0, -324.0, 32.0, -712.0, -194.0, 1036.0, 291.0, 518.0, -65.0, -550.0, 32.0, 194.0, -65.0, 18998.0, -19871.0, -3495.0, -8868.0, -1942.0, -324.0, -162.0, -324.0, 324.0, -32.0, -97.0, -227.0, 259.0, 291.0, -162.0, 129.0, 453.0, -97.0, 129.0, -227.0, 227.0, 324.0, 65.0, -227.0, 615.0, -32.0, -259.0, 65.0, -680.0, 194.0, -1651.0, 1068.0, -518.0, 809.0, -939.0, -777.0, 680.0, -453.0, -65.0, -32.0, -518.0, 712.0, 356.0, -65.0, 356.0, -32.0, -324.0, 259.0, -32.0, 162.0, 32.0, -291.0, 194.0, -227.0, 1197.0, -4304.0, -680.0, 777.0, -291.0, -1133.0, -1586.0, 1327.0, 388.0, -518.0, 1327.0, -453.0, 1230.0, -1780.0, -388.0, 906.0, 712.0, -356.0, 0.0, 129.0, 97.0, -194.0, 194.0, -65.0, 32.0, -227.0, -129.0, -259.0, 162.0, -129.0, -129.0, -356.0, 291.0, 0.0, 129.0, -324.0, 227.0, -680.0, 3722.0, -6408.0, 1424.0, 906.0, -194.0, -2201.0, -1165.0, -162.0, 453.0, -485.0, 518.0, 194.0, -1392.0, -291.0, 0.0, -550.0, -259.0, -356.0, -162.0, -65.0, -356.0, 356.0, -388.0, 259.0, 0.0, 21813.0, 1942.0, -1036.0, -11424.0, 6020.0, -680.0, 2589.0, -906.0, -680.0, 2621.0, -874.0, -194.0, -291.0, 129.0, -65.0, 97.0, 259.0, 227.0, 194.0, -324.0, -518.0, 550.0, 32.0, 129.0, 1165.0, 1003.0, -906.0, -2557.0, 1003.0, -550.0, -453.0, 227.0, 32.0, 291.0, 453.0, 2136.0, 259.0, -809.0, -680.0, -2136.0, -7929.0, 13561.0, 13269.0, -5081.0, -3333.0, 4143.0, 4499.0, -1877.0, 939.0, -485.0, 97.0, 32.0, -194.0, 162.0, -194.0, 129.0, -162.0, -162.0, -324.0, 129.0, -32.0, 550.0, -32.0, -65.0, -32.0, -65.0, -453.0, 32.0, -32.0, -32.0, 129.0, 421.0, 388.0, -324.0, -324.0, 0.0, -129.0, -129.0, 2201.0, 5016.0, -1068.0, -939.0, -2039.0, 356.0, 1683.0, 1683.0, -2298.0, -32.0, 6246.0, -3269.0, -1327.0, -3333.0, -2751.0, -1327.0, -5728.0, -3204.0, 1748.0, 1327.0, -550.0, 2557.0, -809.0, -841.0, 647.0, -259.0, -291.0, 550.0, 227.0, -388.0, 227.0, 1003.0, -583.0, 356.0, 65.0, 65.0, -32.0, -324.0, 615.0, -421.0, 65.0, -162.0, 97.0, 129.0, 129.0, -6020.0, -24564.0, 6376.0, 5567.0, 6020.0, -3042.0, 194.0, 388.0, 0.0, -259.0, 194.0, 227.0, 194.0, 291.0, 129.0, 194.0, 129.0, 129.0, 129.0, -227.0, 32.0, -227.0, -194.0, 453.0, -615.0, -680.0, 162.0, 1456.0, 129.0, 32.0, 906.0, -227.0, 906.0, -453.0, 712.0, 1165.0, 874.0, 421.0, 129.0, 65.0, 485.0, -291.0, -0.0, 453.0, 194.0, -162.0, 97.0, 65.0, -129.0, -65.0, 65.0, -194.0, -259.0, -453.0, 2330.0, -5890.0, 712.0, -1521.0, 1392.0, 809.0, -356.0, 194.0, -1295.0, 1230.0, -2589.0, -324.0, -453.0, 1100.0, 259.0, -32.0, 615.0, -421.0, 32.0, 227.0, 518.0, -97.0, -259.0, 65.0, 291.0, 129.0, -227.0, -97.0, -32.0, 194.0, -162.0, 324.0, -162.0, 162.0, 162.0, -97.0, 162.0, -356.0, 2719.0, -7638.0, -3625.0, 1100.0, 453.0, 324.0, -1553.0, 32.0, 291.0, 291.0, 453.0, 453.0, 971.0, 32.0, -550.0, -453.0, -291.0, 291.0, -129.0, -194.0, -32.0, 194.0, 162.0, -291.0, 0.0, -1295.0, 27995.0, 19095.0, 15276.0, 1295.0, 2492.0, -421.0, -356.0, -1392.0, -1780.0, 1197.0, 129.0, -324.0, 97.0, 194.0, -259.0, 518.0, -259.0, 291.0, 291.0, -550.0, 32.0, -388.0, 583.0, -1165.0, -841.0, 259.0, 1618.0, -1553.0, 129.0, 291.0, 550.0, 32.0, 291.0, 129.0, 65.0, 841.0, -324.0, 2427.0, 259.0, 6279.0, -12557.0, -5308.0, 4790.0, 485.0, 1812.0, 6182.0, 3463.0, -518.0, -680.0, 129.0, 0.0, -129.0, -194.0, -194.0, -65.0, 227.0, -97.0, 0.0, -32.0, -227.0, -227.0, -162.0, 162.0, 97.0, 65.0, -324.0, -97.0, 227.0, -259.0, -291.0, -32.0, 615.0, -0.0, -65.0, -129.0, 227.0, -129.0, 1748.0, 712.0, -1068.0, -1651.0, -550.0, 5502.0, -2039.0, 2201.0, 291.0, 324.0, -3819.0, -7185.0, -971.0, -129.0, 3916.0, -1100.0, -3851.0, -5631.0, -421.0, 680.0, -2298.0, 712.0, -1489.0, -227.0, 356.0, -32.0, -227.0, -1100.0, -583.0, 32.0, -680.0, 777.0, 615.0, 65.0, 65.0, -453.0, -194.0, -485.0, 194.0, 291.0, -291.0, -324.0, -65.0, 485.0, -65.0, -26506.0, 32364.0, 7023.0, -1003.0, 3172.0, 680.0, -809.0, 356.0, -615.0, -162.0, -194.0, -291.0, -97.0, 324.0, -194.0, 0.0, 194.0, 97.0, -97.0, 129.0, 194.0, 388.0, -647.0, 162.0, 712.0, -65.0, 388.0, -1359.0, 129.0, -647.0, 65.0, 356.0, 1456.0, -0.0, 356.0, 777.0, -1553.0, 291.0, -32.0, -97.0, 194.0, 1165.0, -32.0, 32.0, 0.0, 65.0, -162.0, -259.0, 32.0, -194.0, 485.0, -65.0, -162.0, -324.0, -3172.0, -3075.0, 2265.0, -4822.0, -971.0, 906.0, 129.0, 2039.0, 194.0, 841.0, 3010.0, -356.0, 194.0, -680.0, -1812.0, 1489.0, -129.0, 0.0, 259.0, -129.0, -32.0, -32.0, 162.0, -97.0, -259.0, -162.0, 32.0, 518.0, 291.0, 356.0, -129.0, -32.0, -388.0, 32.0, 194.0, 162.0, 65.0, -1715.0, 1392.0, 3884.0, -1003.0, 2136.0, 1165.0, -162.0, 1003.0, 453.0, 1295.0, -324.0, -97.0, -615.0, 680.0, -647.0, 1651.0, -971.0, -712.0, 227.0, -162.0, -97.0, -291.0, 259.0, 32.0, -129.0, -1618.0, 21069.0, -12460.0, 3366.0, 15729.0, 5923.0, -1036.0, 421.0, 1003.0, 1618.0, 583.0, -1230.0, -162.0, 129.0, 129.0, 0.0, 97.0, -97.0, 32.0, 421.0, 97.0, -227.0, -65.0, -291.0, -194.0, -291.0, -194.0, -129.0, 1392.0, 324.0, -712.0, 421.0, -485.0, -518.0, 453.0, -3916.0, -3075.0, 1068.0, 615.0, -453.0, -9288.0, 11716.0, 15891.0, 7735.0, -3398.0, 4952.0, 4952.0, -550.0, -1780.0, -227.0, 65.0, -324.0, 32.0, 97.0, 291.0, 291.0, -485.0, -324.0, 32.0, 388.0, 97.0, -32.0, -97.0, -356.0, -194.0, 194.0, -32.0, -291.0, 32.0, -0.0, -453.0, -194.0, 129.0, -680.0, -129.0, 227.0, 0.0, -227.0, 356.0, 1845.0, 259.0, -647.0, 291.0, -1003.0, 421.0, 4596.0, -9677.0, -356.0, 2524.0, 2330.0, 3528.0, 3236.0, -1780.0, -2104.0, 0.0, -3010.0, 1165.0, 324.0, -259.0, -874.0, -2460.0, -1845.0, -291.0, 809.0, 453.0, -0.0, 615.0, -162.0, -744.0, 291.0, 712.0, 1974.0, 227.0, -65.0, 259.0, -97.0, -65.0, -485.0, -324.0, 32.0, 32.0, -485.0, -32.0, 291.0, 3269.0, -22364.0, 10130.0, 16829.0, -2039.0, 0.0, 291.0, -421.0, -65.0, -32.0, -65.0, -129.0, 32.0, 97.0, -129.0, 0.0, 356.0, -421.0, -421.0, -32.0, 162.0, -194.0, -194.0, 32.0, 162.0, -194.0, 97.0, -1909.0, -1165.0, -1683.0, -615.0, 841.0, -97.0, -1974.0, 259.0, -162.0, -0.0, -162.0, -162.0, -129.0, 97.0, -227.0)
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