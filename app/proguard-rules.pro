# Add project specific ProGuard rules here.

# Action Views
-keep class android.support.v7.widget.SearchView { public *; }
# keep setters in VectorDrawables so that animations can still work.
-keepclassmembers class android.support.graphics.drawable.VectorDrawableCompat$* {
   void set*(***);
   *** get*();
}