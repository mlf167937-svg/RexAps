# GeckoView ships consumer ProGuard rules with its AAR. Do not keep the full GeckoView
# package here: that would inhibit R8 without evidence that those classes need it.

# Preserve generic signatures and runtime annotations used by Compose, Kotlin and libraries.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Retain enum names used by persisted settings / string-based enum lookup.
-keepclassmembers enum com.rexaps.rexfox.SearchEngine {
    *;
}
