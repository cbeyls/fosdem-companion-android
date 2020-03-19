# Add project specific ProGuard rules here.

# Action Views
-keep class androidx.appcompat.widget.SearchView { <init>(...); }

# ViewModels
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class androidx.lifecycle.SavedStateHandleController$OnRecreation { <init>(); }