package candybar.lib.services

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import candybar.lib.databases.Database
import candybar.lib.preferences.Preferences
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract

@SuppressLint("NewApi")
class CandyBarArtWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val workerTag = applicationContext.packageName + ".ArtProvider"
    private val mContext = applicationContext

    override fun doWork(): Result {
        val wallpapers = Database.get(mContext).getWallpapers(null)
        val providerClient = ProviderContract.getProviderClient(applicationContext, workerTag)

        if (Preferences.get(applicationContext).isConnectedAsPreferred) {
            val artworks = ArrayList<Artwork>()

            for (wallpaper in wallpapers) {
                if (wallpaper != null) {
                    val uri = Uri.parse(wallpaper.url)

                    val artwork = Artwork.Builder()
                        .title(wallpaper.name)
                        .byline(wallpaper.author)
                        .persistentUri(uri)
                        .build()

                    if (!artworks.contains(artwork)) {
                        artworks.add(artwork)
                    } else {
                        LogUtil.d("Already Contains Artwork" + wallpaper.name)
                    }
                } else {
                    LogUtil.d("Wallpaper is Null")
                }
            }

            LogUtil.d("Closing Database - Muzei")
            Database.get(mContext).closeDatabase()

            providerClient.setArtwork(artworks)
            return Result.success()
        }

        return Result.failure()
    }

    companion object {
        @JvmStatic
        fun enqueueLoad(context: Context) {
            val manager = WorkManager.getInstance(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequest.Builder(CandyBarArtWorker::class.java)
                .setConstraints(constraints)
                .build()
            manager.enqueue(request)
        }
    }
}
