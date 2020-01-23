package be.digitalia.fosdem.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import be.digitalia.fosdem.R

abstract class SimpleToolbarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content)
        setSupportActionBar(findViewById(R.id.toolbar))
    }
}