package com.teasupplier.app.utils

object NumberToWordsConverter {
    private val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    )
    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    fun convert(n: Long): String {
        if (n == 0L) return "Zero"
        var number = n
        var words = ""

        if (number / 10000000 > 0) {
            words += convert(number / 10000000) + " Crore "
            number %= 10000000
        }
        if (number / 100000 > 0) {
            words += convert(number / 100000) + " Lakh "
            number %= 100000
        }
        if (number / 1000 > 0) {
            words += convert(number / 1000) + " Thousand "
            number %= 1000
        }
        if (number / 100 > 0) {
            words += convert(number / 100) + " Hundred "
            number %= 100
        }
        if (number > 0) {
            if (number < 20) {
                words += units[number.toInt()]
            } else {
                words += tens[(number / 10).toInt()]
                if (number % 10 > 0) {
                    words += "-" + units[(number % 10).toInt()]
                }
            }
        }
        return words.trim()
    }
}
