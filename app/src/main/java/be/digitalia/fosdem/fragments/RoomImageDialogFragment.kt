package be.digitalia.fosdem.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import be.digitalia.fosdem.R
import be.digitalia.fosdem.activities.configureRoomImageDialogToolbar
import be.digitalia.fosdem.utils.invertImageColors
import be.digitalia.fosdem.utils.isLightTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RoomImageDialogFragment : DialogFragment() {

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()

        val dialogBuilder: AlertDialog.Builder = MaterialAlertDialogBuilder(requireContext())

        val contentView = LayoutInflater.from(dialogBuilder.context).inflate(R.layout.dialog_room_image, null)
        contentView.findViewById<ImageView>(R.id.room_image).apply {
            if (!context.isLightTheme) {
                invertImageColors()
            }
            setImageResource(args.getInt(ARG_ROOM_IMAGE_RESOURCE_ID))
        }
        configureRoomImageDialogToolbar(this, contentView.findViewById(R.id.toolbar), args.getString(ARG_ROOM_NAME)!!)

        return dialogBuilder
                .setView(contentView)
                .create()
                .apply {
                    window?.attributes?.windowAnimations = R.style.RoomImageDialogAnimations
                }
    }

    fun show(manager: FragmentManager) {
        show(manager, TAG)
    }

    companion object {
        const val TAG = "room"
        private const val ARG_ROOM_NAME = "roomName"
        private const val ARG_ROOM_IMAGE_RESOURCE_ID = "imageResId"

        fun newInstance(roomName: String, @DrawableRes imageResId: Int) = RoomImageDialogFragment().apply {
            arguments = Bundle(2).apply {
                putString(ARG_ROOM_NAME, roomName)
                putInt(ARG_ROOM_IMAGE_RESOURCE_ID, imageResId)
            }
        }
    }
}