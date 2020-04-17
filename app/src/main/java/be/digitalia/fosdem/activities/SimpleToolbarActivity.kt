package be.digitalia.fosdem.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import be.digitalia.fosdem.R

abstract class SimpleToolbarActivity : AppCompatActivity(R.layout.content) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.toolbar))
    }
}