-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

#noinspection ShrinkerUnresolvedReference
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    #noinspection ShrinkerUnresolvedReference
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    #noinspection ShrinkerUnresolvedReference
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn com.google.android.material.color.DynamicColors
-dontwarn org.slf4j.impl.StaticLoggerBinder
