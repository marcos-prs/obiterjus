import java.util.regex.Pattern

fun clean(raw: String): String {
    val tagRegex = Regex("<[^>]+>")
    val multilineWhitespaceRegex = Regex("[ \\t\\x0B\\f\\r]+")
    val repeatedBlankLinesRegex = Regex("\\n{3,}")
    
    val hasHtml = tagRegex.containsMatchIn(raw)
    val withoutTags = if (hasHtml) {
        raw.replace(Regex("(?i)<br\\s*/?>"), "\n")
           .replace(Regex("(?i)</p\\s*>"), "\n")
           .replace(Regex("(?i)</div\\s*>"), "\n")
           .replace(tagRegex, "")
    } else raw

    val clean = withoutTags
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .lineSequence()
        .map { line -> line.replace(multilineWhitespaceRegex, " ").trim() }
        .joinToString("\n")
        .replace(repeatedBlankLinesRegex, "\n\n")
        .trim()
    return clean
}

val input = \"\"\"<p>... The class 'java.lang.String' does not have the property 'processoOriginario'.</p> <p>CLASSE: [CÍVEL]</p>\"\"\"
println(clean(input))
