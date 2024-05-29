package app.banafsh.android.utils

import android.os.Bundle
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Marker interface that marks a class as Bundle accessor
 */
interface BundleAccessor

private inline fun <T> Bundle.bundleDelegate(
    name: String? = null,
    crossinline get: Bundle.(String) -> T,
    crossinline set: Bundle.(k: String, v: T) -> Unit
) = PropertyDelegateProvider<BundleAccessor, ReadWriteProperty<BundleAccessor, T>> { _, property ->
    val actualName = name ?: property.name

    object : ReadWriteProperty<BundleAccessor, T> {
        override fun getValue(thisRef: BundleAccessor, property: KProperty<*>) =
            get(this@Bundle, actualName)

        override fun setValue(thisRef: BundleAccessor, property: KProperty<*>, value: T) =
            set(this@Bundle, actualName, value)
    }
}

context(BundleAccessor)
val Bundle.boolean
    get() = bundleDelegate(
        get = { getBoolean(it) },
        set = { k, v -> putBoolean(k, v) }
    )

context(BundleAccessor)
val Bundle.byte
    get() = bundleDelegate(
        get = { getByte(it) },
        set = { k, v -> putByte(k, v) }
    )

context(BundleAccessor)
val Bundle.char
    get() = bundleDelegate(
        get = { getChar(it) },
        set = { k, v -> putChar(k, v) }
    )

context(BundleAccessor)
val Bundle.short
    get() = bundleDelegate(
        get = { getShort(it) },
        set = { k, v -> putShort(k, v) }
    )

context(BundleAccessor)
val Bundle.int
    get() = bundleDelegate(
        get = { getInt(it) },
        set = { k, v -> putInt(k, v) }
    )

context(BundleAccessor)
val Bundle.long
    get() = bundleDelegate(
        get = { getLong(it) },
        set = { k, v -> putLong(k, v) }
    )

context(BundleAccessor)
val Bundle.float
    get() = bundleDelegate(
        get = { getFloat(it) },
        set = { k, v -> putFloat(k, v) }
    )

context(BundleAccessor)
val Bundle.double
    get() = bundleDelegate(
        get = { getDouble(it) },
        set = { k, v -> putDouble(k, v) }
    )

context(BundleAccessor)
val Bundle.string
    get() = bundleDelegate(
        get = { getString(it) },
        set = { k, v -> putString(k, v) }
    )

context(BundleAccessor)
val Bundle.intList
    get() = bundleDelegate(
        get = { getIntegerArrayList(it) },
        set = { k, v -> putIntegerArrayList(k, v) }
    )

context(BundleAccessor)
val Bundle.stringList
    get() = bundleDelegate<List<String>?>(
        get = { getStringArrayList(it) },
        set = { k, v -> putStringArrayList(k, v?.let { ArrayList(it) }) }
    )

context(BundleAccessor)
val Bundle.booleanArray
    get() = bundleDelegate(
        get = { getBooleanArray(it) },
        set = { k, v -> putBooleanArray(k, v) }
    )

context(BundleAccessor)
val Bundle.byteArray
    get() = bundleDelegate(
        get = { getByteArray(it) },
        set = { k, v -> putByteArray(k, v) }
    )

context(BundleAccessor)
val Bundle.shortArray
    get() = bundleDelegate(
        get = { getShortArray(it) },
        set = { k, v -> putShortArray(k, v) }
    )

context(BundleAccessor)
val Bundle.charArray
    get() = bundleDelegate(
        get = { getCharArray(it) },
        set = { k, v -> putCharArray(k, v) }
    )

context(BundleAccessor)
val Bundle.intArray
    get() = bundleDelegate(
        get = { getIntArray(it) },
        set = { k, v -> putIntArray(k, v) }
    )

context(BundleAccessor)
val Bundle.floatArray
    get() = bundleDelegate(
        get = { getFloatArray(it) },
        set = { k, v -> putFloatArray(k, v) }
    )

context(BundleAccessor)
val Bundle.doubleArray
    get() = bundleDelegate(
        get = { getDoubleArray(it) },
        set = { k, v -> putDoubleArray(k, v) }
    )

context(BundleAccessor)
val Bundle.stringArray
    get() = bundleDelegate(
        get = { getStringArray(it) },
        set = { k, v -> putStringArray(k, v) }
    )

class SongBundleAccessor(val extras: Bundle = Bundle()) : BundleAccessor {
    companion object {
        fun bundle(block: SongBundleAccessor.() -> Unit) = SongBundleAccessor().apply(block).extras
    }

    var albumId by extras.string
    var durationText by extras.string
    var artistNames by extras.stringList
    var artistIds by extras.stringList
    var explicit by extras.boolean
    var isFromPersistentQueue by extras.boolean
}

inline val Bundle.songBundle get() = SongBundleAccessor(this)
