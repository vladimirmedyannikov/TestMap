package ru.medyannikov.testmap

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.anko.longToast
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.medyannikov.testmap.pojo.DirectionResponse
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

class MapsActivity : FragmentActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

  private lateinit var mMap: GoogleMap
  private lateinit var api: GoogleApi
  val objectMapper by lazy {
    ObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maps)
    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)
    val client = OkHttpClient.Builder()
        .connectTimeout(60000, TimeUnit.MILLISECONDS)
        .readTimeout(60000, TimeUnit.MILLISECONDS)
        .writeTimeout(60000, TimeUnit.MILLISECONDS)
        .addInterceptor(HttpLoggingInterceptor().setLevel(
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE))
        .build()

    api = Retrofit.Builder().baseUrl("https://maps.googleapis.com")
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .client(client)
        .build()
        .create(GoogleApi::class.java)
  }

  private var mStart: LatLng? = null

  private var mEnd: LatLng? = null

  override fun onMapClick(latLng: LatLng) {
    mMap.addMarker(MarkerOptions().position(latLng).title("Marker"))
    mMarkers.add(latLng)
    if (mMarkers.size == 1) {
      mStart = latLng
    } else if (mMarkers.size == 2) {
      mEnd = latLng
    }
    //onRefreshPooints()

    getResponseFromServer()
  }

  private fun getResponseFromServer() {
    val sb = StringBuilder()
    mMarkers.forEachIndexed { index, latLng ->
      if (index > 0) {
        sb.append("|")
      }
      sb.append("${latLng.latitude},${latLng.longitude}")
    }

    if (mMarkers.size > 2) {
      doInBackground(api.getDirectionApi(mapOf("origin" to "${mStart?.latitude},${mStart?.longitude}",
          "destination" to "${mEnd?.latitude},${mEnd?.longitude}",
          "waypoints" to "optimize:true|${sb.toString()}",
          "key" to "AIzaSyDPd1XUXQ99XkVXfU4avf4fJeoVQ2eNj-k")))
          .subscribe({ onLoadResponse(it) }, { longToast(it.message.toString()) })
    }
  }

  private fun onRefreshPooints() {
    mMap.clear()
    val line = PolylineOptions()
    line.width(4f).color(resources.getColor(android.R.color.holo_red_light))
    val latLngBuilder = LatLngBounds.Builder()
    for (i in mPoints.indices) {

      val startMarkerOptions = MarkerOptions()
          .position(mPoints[i])
          .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_round))
      //mMap !!.addMarker(startMarkerOptions)

      line.add(mPoints[i])
      latLngBuilder.include(mPoints[i])
    }
    mMap !!.addPolyline(line)
    val size = resources.displayMetrics.widthPixels
    val latLngBounds = latLngBuilder.build()
    val track = CameraUpdateFactory.newLatLngBounds(latLngBounds, size, size, 25)
    mMap !!.moveCamera(CameraUpdateFactory.newLatLng(mPoints[mPoints.size - 1]))
    mMarkers.forEach { mMap.addMarker(MarkerOptions().position(it).title("Marker")) }
  }

  fun <T> doInBackground(func: Observable<T>): Observable<T> =
      Observable.defer<T> { func.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()) }

  private fun onLoadResponse(response: DirectionResponse?) {
    response?.apply {
      val points = PolyUtil.decode(this.routes?.get(0)?.overviewPolyline?.points)
      mPoints.clear()
      mPoints.addAll(points)
      onRefreshPooints()
    }

    longToast("state ${response?.status.toString()}")
  }

  internal var mPoints: MutableList<LatLng> = ArrayList()
  internal var mMarkers: MutableList<LatLng> = ArrayList()
  /**
   * Manipulates the map once available.
   * This callback is triggered when the map is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera. In this case,
   * we just add a marker near Sydney, Australia.
   * If Google Play services is not installed on the device, the user will be prompted to install
   * it inside the SupportMapFragment. This method will only be triggered once the user has
   * installed Google Play services and returned to the app.
   */
  override fun onMapReady(googleMap: GoogleMap) {
    mMap = googleMap
    mMap.isMyLocationEnabled = true
    mMap.isBuildingsEnabled = true
    mMap.setOnMyLocationButtonClickListener(this)
    mMap.setOnMarkerClickListener(this)
    mMap.setOnMapClickListener(this)

  }

  override fun onMarkerClick(marker: Marker): Boolean {
    val intex = mMarkers.indexOf(marker.position)
    mMarkers.removeAt(intex)
    marker.remove()
    getResponseFromServer()
    return false
  }

  override fun onMyLocationButtonClick(): Boolean {
    return false
  }
}
