package fr.labomg.biophonie.data.source

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import fr.labomg.biophonie.data.Coordinates
import fr.labomg.biophonie.data.GeoPoint
import fr.labomg.biophonie.templates
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.moveTo

class DefaultGeoPointRepository(
    private val geoPointRemoteDataSource: GeoPointDataSource,
    private val geoPointLocalDataSource: GeoPointDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : GeoPointRepository {

    override suspend fun cancelNetworkRequest() {
        geoPointRemoteDataSource.cancelCurrentJob()
    }

    override suspend fun saveAssetsInStorage(geoPoint: GeoPoint, dataPath: String) {
        val soundPath = Path(geoPoint.sound.local!!)
        val targetSound = Path(dataPath).resolve(soundPath.fileName)
        withContext(Dispatchers.IO) { soundPath.moveTo(targetSound) }
        geoPoint.sound.local = targetSound.absolutePathString()

        if (geoPoint.picture.local != null) {
            val picturePath = Path(geoPoint.picture.local!!)
            if (!templates.contains(picturePath.fileName.toString())) {
                geoPoint.picture.local = convertToWebp(picturePath,dataPath)
            }
        }
    }

    private suspend fun convertToWebp(imagePath: Path, dataPath: String): String {
        val compressedImage = File(dataPath, imagePath.fileName.toString().replaceAfter('.',"webp"))
        withContext(Dispatchers.IO) {
            try {
                compressedImage.createNewFile()
            } catch (e: IOException) {
                Timber.e("could not create file for compressed image: $e")
            }
            compressPicture(imagePath.toAbsolutePath().toString(), compressedImage)
        }
        return compressedImage.absolutePath
    }

    private suspend fun compressPicture(input: String, output: File) {
        withContext(Dispatchers.IO) {
            val picture: Bitmap
            val out: FileOutputStream
            try {
                picture = BitmapFactory.decodeFile(input)
                out = FileOutputStream(output)
            } catch (e: Exception) {
                Timber.e("file do not exist: $e")
                return@withContext
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                picture.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75,out)
            else
                picture.compress(Bitmap.CompressFormat.WEBP,75,out)
            out.close()
        }
    }

    override suspend fun fetchGeoPoint(id: Int): Result<GeoPoint> {
        cancelNetworkRequest()
        return with(geoPointLocalDataSource.getGeoPoint(id)) {
            if (isSuccess)
                return@with this
            else
                return@with geoPointRemoteDataSource.getGeoPoint(id).onSuccess {
                    geoPointLocalDataSource.addGeoPoint(it)
                }
        }
    }

    override suspend fun getClosestGeoPointId(coord: Coordinates, not: Array<Int>): Result<Int> {
        cancelNetworkRequest()
        return geoPointRemoteDataSource.getClosestGeoPointId(coord, not)
    }

    override suspend fun getUnavailableGeoPoints(): List<GeoPoint> =
        geoPointLocalDataSource.getUnavailableGeoPoints()

    override suspend fun saveNewGeoPoint(geoPoint: GeoPoint, dataPath: String): Result<GeoPoint> {
        saveAssetsInStorage(geoPoint, dataPath)
        return geoPointLocalDataSource.addGeoPoint(geoPoint, true)
    }

    override suspend fun addNewGeoPoints(): Boolean {
        var success = true
        val newGeoPoints = geoPointLocalDataSource.getNewGeoPoints()
        if (newGeoPoints.isNotEmpty()) {
            success = geoPointRemoteDataSource.pingRestricted().isSuccess
            if (success) {
                newGeoPoints.forEach { geoPoint ->
                    geoPointRemoteDataSource.addGeoPoint(geoPoint)
                        .onSuccess {
                            it.apply { id = geoPoint.id }
                            geoPointLocalDataSource.refreshGeoPoint(it)
                            Timber.i("${geoPoint.title} posted")
                        }
                        .onFailure {
                            Timber.e("could not post ${geoPoint.title}: $it")
                            success = false
                        }
                }
            }
        }
        return success
    }

    override suspend fun refreshUnavailableGeoPoints() {
        getUnavailableGeoPoints().forEach { geoPoint ->
            if (geoPoint.remoteId != 0)
                geoPointRemoteDataSource.getGeoPoint(geoPoint.remoteId)
                    .onSuccess {
                        geoPointLocalDataSource.makeAvailable(it)
                    }
                    .onFailure {
                        Timber.w("${geoPoint.title} was not enabled yet")
                    }
            else
                Timber.w("${geoPoint.title} not posted yet")
        }
    }
}