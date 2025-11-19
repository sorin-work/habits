package app.habits.common

/**
 * Collects field errors so a request reports every problem at once instead of the first one.
 */
class Validator {
    private val errors = linkedMapOf<String, String>()

    fun check(
        field: String,
        ok: Boolean,
        message: () -> String,
    ) {
        if (!ok && field !in errors) errors[field] = message()
    }

    fun error(
        field: String,
        message: String,
    ) {
        if (field !in errors) errors[field] = message
    }

    fun hasError(field: String) = field in errors

    fun throwIfInvalid() {
        if (errors.isNotEmpty()) throw ValidationException(errors.toMap())
    }
}

inline fun validate(block: Validator.() -> Unit) {
    Validator().apply(block).throwIfInvalid()
}

private val EMAIL = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

fun isEmail(value: String) = value.length <= 254 && EMAIL.matches(value)

fun normalizeEmail(value: String) = value.trim().lowercase()
