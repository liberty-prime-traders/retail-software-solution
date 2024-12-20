package me.ezra_home.retail_software_solution.configuration.util.typeadapters

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy
import java.io.IOException

class HibernateProxyTypeAdapter private constructor(private val context: Gson) : TypeAdapter<HibernateProxy?>() {

    companion object {
        val FACTORY: TypeAdapterFactory = object : TypeAdapterFactory {
            override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
                if (HibernateProxy::class.java.isAssignableFrom(type.rawType)){
                    return HibernateProxyTypeAdapter(gson) as TypeAdapter<T>
                }
                return null
            }
        }
    }
    @Throws(IOException::class)
    override fun read(input: JsonReader): HibernateProxy? {
        throw UnsupportedOperationException("Not supported")
    }

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: HibernateProxy?) {
        if (value == null) {
            out.nullValue()
            return
        }
        // Retrieve the original (not proxy) class
        val baseType: Class<*> = Hibernate.getClass(value)
        // Get the TypeAdapter of the original class, to delegate the serialization
        val delegate: TypeAdapter<*> = context.getAdapter(TypeToken.get(baseType))
        // Get a filled instance of the original class
        val deproxiedValue = value.hibernateLazyInitializer.implementation
        // Serialize the value
        (delegate as TypeAdapter<Any>).write(out, deproxiedValue)
    }


}
