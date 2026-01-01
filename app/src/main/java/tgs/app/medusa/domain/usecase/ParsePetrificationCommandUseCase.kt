package tgs.app.medusa.domain.usecase

import tgs.app.medusa.domain.model.PetrificationCommand

class ParsePetrificationCommandUseCase {

    private val numberPattern = """(\d+|one|two|to|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|twenty|thirty|forty|fifty)"""

    private val commandRegex = Regex(
        """$numberPattern\s*meter(?:s)?\s*$numberPattern\s*second(?:s)?""",
        RegexOption.IGNORE_CASE
    )

    fun execute(result: String): PetrificationCommand? {
        val match = commandRegex.find(result) ?: return null
        val rawMeter = match.groupValues[1].lowercase()
        val rawSecond = match.groupValues[2].lowercase()

        val meterValue = wordToNumber(rawMeter)
        val secondValue = wordToNumber(rawSecond)

        val meters = meterValue.toIntOrNull() ?: return null
        val seconds = secondValue.toIntOrNull() ?: return null

        return PetrificationCommand(meters = meters, seconds = seconds)
    }

    private fun wordToNumber(value: String): String {
        return when (value) {
            "one" -> "1"
            "to", "two" -> "2"
            "three" -> "3"
            "four" -> "4"
            "five" -> "5"
            "six" -> "6"
            "seven" -> "7"
            "eight" -> "8"
            "nine" -> "9"
            "ten" -> "10"
            "eleven" -> "11"
            "twelve" -> "12"
            "thirteen" -> "13"
            "fourteen" -> "14"
            "fifteen" -> "15"
            "twenty" -> "20"
            "thirty" -> "30"
            "forty" -> "40"
            "fifty" -> "50"
            else -> value
        }
    }
}
