package be.ap.edu.mapsaver

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.beust.klaxon.*
import org.osmdroid.config.Configuration

import org.osmdroid.util.GeoPoint
import org.osmdroid.views.*

import java.util.*
import org.osmdroid.views.overlay.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


class MainActivity : Activity() {


    val parser: Parser = Parser.default()


    var mMapView: MapView? = null
    var mMyLocationOverlay: ItemizedOverlay<OverlayItem>? = null
    var searchField: EditText? = null
    var searchButton: Button? = null
    var clearButton: Button? = null
    val urlSearch = "https://nominatim.openstreetmap.org/search?q="

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Problem with SQLite db, solution :
        // https://stackoverflow.com/questions/40100080/osmdroid-maps-not-loading-on-my-device
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        val basePath = File(cacheDir.absolutePath, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        val tileCache = File(osmConfig.osmdroidBasePath, "tile")
        osmConfig.osmdroidTileCache = tileCache

        setContentView(R.layout.activity_main)
        mMapView = findViewById(R.id.mapview) as MapView

        searchField = findViewById(R.id.search_txtview)
        searchButton = findViewById(R.id.search_button)
        searchButton!!.setOnClickListener {
            val url = URL(urlSearch + URLEncoder.encode(searchField?.text.toString(), "UTF-8") + "&format=json")
            it.hideKeyboard()

//            MyAsyncTask().execute(url)
        }

        clearButton = findViewById(R.id.clear_button)
        clearButton!!.setOnClickListener {
            mMapView!!.overlays.clear()
            // Redraw map
            mMapView!!.invalidate()
        }

        if (hasPermissions()) {
            initMap()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (hasPermissions()) {
                initMap()
            } else {
                finish()
            }
        }
    }

    fun initMap() {
        mMapView!!.setTileSource(TileSourceFactory.MAPNIK)

        run {
            val json = application.assets.open("geoLocations.json")
            val array = parser.parse(json) as JsonArray<JsonObject>
            val items = ArrayList<OverlayItem>()

            for (item in array){
                val naam = item.string("naam")
                val lat =  item.double("point_lat")
                val long = item.double("point_lng")
                val nrFietskes = item.int("aantal_loc")

                items.add(OverlayItem(naam,nrFietskes.toString(),
                        GeoPoint(lat!!.toDouble(),long!!.toDouble())))
            }


            // OnTapListener for the Markers, shows a simple Toast
            this.mMyLocationOverlay = ItemizedIconOverlay(items,
                    object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
                        override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                            Toast.makeText(
                                    applicationContext,item.title+": "+item.snippet+" fietskes", Toast.LENGTH_LONG).show()
                            return true // We 'handled' this event.
                        }

                        override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                            Toast.makeText(
                                    applicationContext, "Item '" + item.title + "' (index=" + index
                                    + ") got long pressed", Toast.LENGTH_LONG).show()
                            return true
                        }
                    }, applicationContext)
            this.mMapView!!.overlays.add(this.mMyLocationOverlay)
        }

        // MiniMap
        run {
            val miniMapOverlay = MinimapOverlay(this, mMapView!!.tileRequestCompleteHandler)
            this.mMapView!!.overlays.add(miniMapOverlay)
        }

        val mapController = mMapView!!.controller
        mapController.setZoom(20.0)
        // Default = Ellermanstraat 33
        mapController.setCenter(GeoPoint(51.23020595, 4.41655480828479))
    }

    fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onPause() {
        super.onPause()
        mMapView!!.onPause()
    }

    override fun onResume() {
        super.onResume()
        mMapView!!.onResume()
    }

}