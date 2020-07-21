package com.example.biophonie.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import com.example.biophonie.R
import com.example.biophonie.databinding.ActivityMapBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.android.synthetic.main.activity_map.*

private const val TAG = "MapActivity"
private const val PROPERTY_NAME: String = "name"
private const val PROPERTY_ID: String = "id"
private const val ID_ICON: String = "biophonie.icon"
private const val ID_SOURCE: String = "biophonie"
private const val ID_LAYER: String = "biophonie.sound"
private const val FRAGMENT_TAG: String = "fragment"
private const val REQUEST_CHECK_SETTINGS = 0x1

class MapActivity : FragmentActivity(), MapboxMap.OnMapClickListener, OnMapReadyCallback,
    PermissionsListener {

    private lateinit var permissionsManager: PermissionsManager
    private lateinit var binding: ActivityMapBinding
    private lateinit var mapboxMap: MapboxMap
    private var bottomPlayer: BottomPlayerFragment =
        BottomPlayerFragment()
    private var about: AboutFragment = AboutFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        binding = DataBindingUtil.setContentView(this, R.layout.activity_map)

        addBottomSheetFragment()
        bindMap(savedInstanceState)
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding.about.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .add(R.id.containerMap, about, FRAGMENT_TAG+"about")
                .addToBackStack(null)
                .commit()
        }
        binding.locationFab.setOnClickListener {
            //askLocationSettings()
            activateLocationSettings()
        }
    }

    private fun createLocationRequest(): LocationRequest? =
        LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

    private fun activateLocationSettings(){
        val builder = createLocationRequest()?.let {
            LocationSettingsRequest.Builder()
                .addLocationRequest(it)
        }
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        client.checkLocationSettings(builder?.build()).apply {
            addOnSuccessListener {
                mapboxMap.getStyle {
                    enableLocationComponent(it)
                }
            }
            addOnFailureListener {
                if (exception is ResolvableApiException){
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        (exception as ResolvableApiException).startResolutionForResult(this@MapActivity,
                            REQUEST_CHECK_SETTINGS)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(TAG, "enableLocationComponent: truc")
            val locationComponent: LocationComponent = mapboxMap.locationComponent

            // Activate with options
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, loadedMapStyle).build()
            )

            // Enable to make component visible
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    private fun askLocationSettings(){
        AlertDialog.Builder(this).apply {
            setTitle("GPS is settings")
            setMessage("GPS is not enabled. Do you want to go to settings menu?")
            setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }

            setNegativeButton("Cancel") {dialog, _ -> dialog.cancel()}
            show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                   permissions: Array<String?>,
                                   grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String?>?) {
        Toast.makeText(this, R.string.location_permission_explanation, Toast.LENGTH_LONG)
            .show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap.getStyle { style -> enableLocationComponent(style) }
        } else {
            Toast.makeText(this, R.string.location_permission_not_granted, Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun bindMap(savedInstanceState: Bundle?) {
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        binding.scaleView.metersOnly()
    }

    private fun addBottomSheetFragment() {
        supportFragmentManager.beginTransaction()
            .add(
                R.id.containerMap, bottomPlayer,
                FRAGMENT_TAG+"bottomSheet"
            )
            .commit()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.addOnCameraMoveListener{ updateScaleBar(mapboxMap) }
        mapboxMap.addOnCameraIdleListener{ updateScaleBar(mapboxMap)}
        val symbolLayerIconFeatureList: MutableList<Feature> = ArrayList()
        symbolLayerIconFeatureList.add(
            Feature.fromGeometry(
                Point.fromLngLat(-1.803165, 47.516218)
            ).apply { addStringProperty(PROPERTY_NAME, "Point 1")
                addStringProperty(PROPERTY_ID, "1")}
        )
        symbolLayerIconFeatureList.add(
            Feature.fromGeometry(
                Point.fromLngLat(-1.814496,47.534516)
            ).apply { addStringProperty(PROPERTY_NAME, "Point 2")
                addStringProperty(PROPERTY_ID, "2")}
        )
        symbolLayerIconFeatureList.add(
            Feature.fromGeometry(
                Point.fromLngLat(-1.778381,47.512238)
            ).apply { addStringProperty(PROPERTY_NAME, "Point 3")
                addStringProperty(PROPERTY_ID, "3")}
        )
        val d = resources.getDrawable(R.drawable.ic_marker, theme)
        mapboxMap.setStyle(Style.Builder().fromUri(getString(R.string.style_url))
            .withImage(ID_ICON, d.toBitmap())
            .withSource(GeoJsonSource(ID_SOURCE, FeatureCollection.fromFeatures(symbolLayerIconFeatureList)))
            .withLayer(SymbolLayer(
                ID_LAYER,
                ID_SOURCE
            )
                .withProperties(
                    iconImage(ID_ICON),
                    iconOpacity(8f),
                    iconSize(0.7f),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                    textColor("#000000"),
                    textField("{name}"),
                    textSize(12f),
                    textOffset(arrayOf(2.2f,0f)),
                    textIgnorePlacement(false),
                    textAllowOverlap(false)
                )
            )) {
            //LoadGeoJsonDataTask(this).execute()
            mapboxMap.addOnMapClickListener(this)
        }
    }

    private fun updateScaleBar(mapboxMap: MapboxMap) {
        val cameraPosition = mapboxMap.cameraPosition
        scaleView.update(cameraPosition.zoom.toFloat(), cameraPosition.target.latitude)
    }

    override fun onMapClick(point: LatLng): Boolean {
        return handleClickIcon(mapboxMap.projection.toScreenLocation(point));
    }

    private fun handleClickIcon(screenPoint: PointF?): Boolean {
        var rectF: RectF? = null
        // This is a hack. It does not allow for a real hitbox of the size of the text
        screenPoint?.let {rectF = RectF(screenPoint.x - 10, screenPoint.y - 10, screenPoint.x + 50, screenPoint.y + 10) }

        val features: List<Feature> =
            rectF?.let { mapboxMap.queryRenderedFeatures(it,
                ID_LAYER
            ) } as List<Feature>
        return if (features.isEmpty()) false
        else {
            val clickedFeature: Feature? = features.first { it.geometry() is Point }
            val clickedPoint: Point? = clickedFeature?.geometry() as Point?
            clickedPoint?.let {
                bottomPlayer.clickOnGeoPoint(clickedFeature!!.getStringProperty(PROPERTY_ID),
                    clickedFeature.getStringProperty(PROPERTY_NAME),
                    LatLng(clickedPoint.latitude(), clickedPoint.longitude()))
                return true
            }
            return false
        }
    }

    /**
     * Convert a drawable to a bitmap
     *
     * @return bitmap
     */
    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable) {
            return this.bitmap
        }

        val bitmap = Bitmap.createBitmap(this.intrinsicWidth, this.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.setBounds(0, 0, canvas.width, canvas.height)
        this.draw(canvas)

        return bitmap
    }

    override fun onBackPressed() {
        if (bottomPlayer.bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN
            && !supportFragmentManager.fragments.contains(about))
            bottomPlayer.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        else
            super.onBackPressed()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }


    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxMap.removeOnMapClickListener(this)
        binding.mapView.onDestroy()
    }
}