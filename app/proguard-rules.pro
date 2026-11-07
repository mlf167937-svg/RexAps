# GeckoView ships consumer ProGuard rules with its AAR. Do not keep the full GeckoView
# package here: that would inhibit R8 without evidence that those classes need it.

# Preserve generic signatures and runtime annotations used by Compose, Kotlin and libraries.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Retain enum names used by persisted settings / string-based enum lookup.
-keepclassmembers enum com.rexaps.rexfox.SearchEngine {
    *;
}

# RexGit uses JGit's transport/protocol discovery and other runtime entry points.
# Keep JGit's class/member names stable so release shrinking cannot break those
# reflective lookups or remove constructors that are not visible to R8.
-keep class org.eclipse.jgit.** { *; }


# JGit has optional desktop-Java integrations that Android does not provide:
# JMX/management monitoring and GSSAPI (Negotiate) authentication.
# The app does not use these optional integrations; suppress only these absent APIs.
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.ietf.jgss.**

# The Node.js JNI bridge is resolved by its package/class/method symbol.
-keep class com.rexaps.rexchat.v1.RexNodeBridge { *; }
