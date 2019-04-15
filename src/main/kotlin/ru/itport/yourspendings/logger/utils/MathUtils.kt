package utils

object MathUtils {
    /**
     * Method rounds value with specified number of decimal digits
     * @param value  Source value
     * @param precision Number of decimal digits
     * @return Rounded value
     */
    fun round(value: Any, precision: Int): Double {
        val result = java.lang.Double.valueOf(value.toString())
        return Math.round(result * (10 * precision)).toDouble() / (10 * precision)
    }
}
