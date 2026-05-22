package wile.redstonepen.net

internal fun setField(obj: Any, fieldName: String, value: Any?) {
    var clazz: Class<*>? = obj.javaClass
    while (clazz != null) {
        try {
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(obj, value)
            return
        } catch (_: NoSuchFieldException) {
            clazz = clazz.superclass
        }
    }
    throw NoSuchFieldException("$fieldName not found in ${obj.javaClass} hierarchy")
}
