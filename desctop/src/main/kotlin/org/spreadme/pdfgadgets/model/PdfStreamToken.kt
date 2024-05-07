package org.spreadme.pdfgadgets.model

data class PdfStreamToken(
    val tokenType: PdfStreamTokenType,
    val token: String
)

data class PdfStreamTokenSequence(
    val tokens: ArrayList<PdfStreamToken> = arrayListOf()
) {

    fun append(tokenType: PdfStreamTokenType, vararg tokens: String): PdfStreamTokenSequence {
        val builder = StringBuilder()
        for (token in tokens) {
            builder.append(token)
        }
        this.tokens.add(PdfStreamToken(tokenType, builder.toString()))
        return this
    }

    override fun toString(): String = tokens.joinToString("") { it.token }
}

enum class PdfStreamTokenType{
    operator,
    number,
    string,
    escape,
    tname,
    indent,
}