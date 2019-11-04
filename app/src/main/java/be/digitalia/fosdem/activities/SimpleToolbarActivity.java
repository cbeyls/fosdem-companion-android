package be.digitalia.fosdem.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import be.digitalia.fosdem.R;

public abstract class SimpleToolbarActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);
		setSupportActionBar(findViewById(R.id.toolbar));
	}
}
