package be.digitalia.fosdem.activities

import android.content.ActivityNotFoundException
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ImageView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.text.set
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import be.digitalia.fosdem.R
import be.digitalia.fosdem.api.FosdemApi
import be.digitalia.fosdem.api.FosdemUrls
import be.digitalia.fosdem.utils.configureColorSchemes
import be.digitalia.fosdem.utils.consumeHorizontalWindowInsetsAsPadding
import be.digitalia.fosdem.utils.invertImageColors
import be.digitalia.fosdem.utils.isLightTheme
import be.digitalia.fosdem.utils.launchAndRepeatOnLifecycle
import be.digitalia.fosdem.utils.rootView
import be.digitalia.fosdem.utils.toRoomSlug
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A special Activity which is displayed like a dialog and shows a room image.
 * Specify the room name and the room image id as Intent extras.
 *
 * @author Christophe Beyls
 */
@AndroidEntryPoint
class RoomImageDialogActivity : AppCompatActivity(R.layout.dialog_room_image) {

    @Inject
    lateinit var api: FosdemApi

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        rootView.consumeHorizontalWindowInsetsAsPadding()
        val intent = intent
        val roomName = intent.getStringExtra(EXTRA_ROOM_NAME)!!
        val imageResId = intent.getIntExtra(EXTRA_ROOM_IMAGE_RESOURCE_ID, 0)

        title = roomName
        val roomImageView: ImageView = findViewById(R.id.room_image)

        if (imageResId != 0) {
            roomImageView.apply {
                if (!context.isLightTheme) {
                    invertImageColors()
                }
                setImageResource(imageResId)
            }
        } else {
            roomImageView.isVisible = false
            findViewById<View>(R.id.room_image_placeholder).isVisible = true
        }

        configureToolbar(api, this, findViewById(R.id.toolbar), roomName)
    }

    companion object {
        const val EXTRA_ROOM_NAME = "roomName"
        const val EXTRA_ROOM_IMAGE_RESOURCE_ID = "imageResId"

        fun configureToolbar(api: FosdemApi, owner: LifecycleOwner, toolbar: Toolbar, roomName: String) {
            toolbar.title = roomName
            if (roomName.isNotEmpty()) {
                val context = toolbar.context

                toolbar.inflateMenu(R.menu.room_image_dialog)
                toolbar.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.navigation -> {
                            val localNavigationUrl = FosdemUrls.getLocalNavigationToLocation(roomName.toRoomSlug())
                            try {
                                CustomTabsIntent.Builder()
                                        .configureColorSchemes(context, R.color.light_color_primary)
                                        .setShowTitle(true)
                                        .build()
                                        .launchUrl(context, localNavigationUrl.toUri())
                            } catch (_: ActivityNotFoundException) {
                            }
                            true
                        }
                        else -> false
                    }
                }

                owner.launchAndRepeatOnLifecycle {
                    // Display the room status as subtitle
                    api.roomStatuses.collect { statuses ->
                        toolbar.subtitle = statuses[roomName]?.let { roomStatus ->
                            SpannableString(context.getString(roomStatus.nameResId)).apply {
                                this[0, length] = ForegroundColorSpan(context.getColor(roomStatus.colorResId))
                            }
                        }
                    }
                }
            }
        }
    }
}