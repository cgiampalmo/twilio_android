-dontobfuscate

# Keep these classes
-keep class com.amazonaws.services.**.*Handler   # Request handlers defined in request.handlers
-keep class com.glaciersecurity.glaciermessenger.**
-keep class com.google.android.gms.**
-keep class com.soundcloud.android.crop.**
-keep class com.twilio.common.** { *; }
-keep class com.twilio.video.** { *; }
-keep class org.whispersystems.**
-keep class tvi.webrtc.** { *; }

# Keep these attributes
-keepattributes InnerClasses

# Keep these class names
-keepnames class com.amazonaws.**
-keepnames class com.amazon.**

# Don't warn for the following
-dontwarn com.amazonaws.http.**
-dontwarn com.amazonaws.metrics.**
-dontwarn com.amazonaws.mobile.**
-dontwarn com.amazonaws.mobileconnectors.**

-dontwarn com.amplifyframework.datastore.**
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn com.apollographql.apollo.**
-dontwarn com.fasterxml.jackson.**

-dontwarn com.google.common.util.concurrent.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.concurrent.LazyInit
-dontwarn com.google.errorprone.annotations.ForOverride
-dontwarn com.google.errorprone.annotations.IncompatibleModifiers
-dontwarn com.google.errorprone.annotations.RequiredModifiers
-dontwarn com.google.errorprone.annotations.Var
-dontwarn com.google.firebase.analytics.connector.AnalyticsConnector

-dontwarn javax.inject.**

-dontwarn org.apache.commons.logging.**
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl
-dontwarn org.apache.http.**   # Android 6.0 release removes support for the Apache HTTP client
-dontwarn org.bouncycastle.mail.**
-dontwarn org.bouncycastle.x509.util.LDAPStoreHelper
-dontwarn org.bouncycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.bouncycastle.cert.dane.**
-dontwarn org.codehaus.mojo.**

-dontwarn okhttp3.**
-dontwarn sun.misc.Unsafe
