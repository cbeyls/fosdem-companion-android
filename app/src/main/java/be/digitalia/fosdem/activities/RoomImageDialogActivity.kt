package be.digitalia.fosdem.activities

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.text.set
import androidx.lifecycle.LifecycleOwner
import be.digitalia.fosdem.R
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.api.FosdemUrls
import be.digitalia.fosdem.utils.configureToolbarColors
import be.digitalia.fosdem.utils.invertImageColors
import be.digitalia.fosdem.utils.isLightTheme
import be.digitalia.fosdem.utils.toSlug

/**
 * A special Activity which is displayed like a dialog and shows a room image.
 * Specify the room name and the room image id as Intent extras.
 *
 * @author Christophe Beyls
 */
class RoomImageDialogActivity : AppCompatActivity(R.layout.dialog_room_image) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val roomName = intent.getStringExtra(EXTRA_ROOM_NAME)!!
        title = roomName

        findViewById<ImageView>(R.id.room_image).apply {
            if (!context.isLightTheme) {
                invertImageColors()
            }
            setImageResource(intent.getIntExtra(EXTRA_ROOM_IMAGE_RESOURCE_ID, 0))
        }
        configureToolbar(this, findViewById(R.id.toolbar), roomName)
    }

    companion object {
        const val EXTRA_ROOM_NAME = "roomName"
        const val EXTRA_ROOM_IMAGE_RESOURCE_ID = "imageResId"

        fun configureToolbar(owner: LifecycleOwner, toolbar: Toolbar, roomName: String) {
            toolbar.title = roomName
            if (roomName.isNotEmpty()) {
                val context = toolbar.context

                toolbar.inflateMenu(R.menu.room_image_dialog)
                toolbar.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.navigation -> {
                            val localNavigationUrl = FosdemUrls.getLocalNavigationToLocation(roomName.toSlug())
                            try {
                                CustomTabsIntent.Builder()
                                        .configureToolbarColors(context, R.color.light_color_primary)
                                        .setShowTitle(true)
                                        .build()
                                        .launchUrl(context, Uri.parse(localNavigationUrl))
                            } catch (ignore: ActivityNotFoundException) {
                            }
                            true
                        }
                        else -> false
                    }
                }

                // Display the room status as subtitle
                FosdemApi.getRoomStatuses(toolbar.context).observe(owner) { roomStatuses ->
                    val roomStatus = roomStatuses[roomName]
                    toolbar.subtitle = if (roomStatus != null) {
                        SpannableString(context.getString(roomStatus.nameResId)).apply {
                            this[0, length] = ForegroundColorSpan(ContextCompat.getColor(context, roomStatus.colorResId))
                        }
                    } else null
                }
            }
        }
    }
}