package org.spreadme.pdfgadgets

import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.test.KoinTest
import org.koin.test.get
import org.spreadme.pdfgadgets.di.asn1ParserMoudle
import org.spreadme.pdfgadgets.repository.ASN1Parser
import java.util.*
import kotlin.test.assertNotNull

class ASN1ParserTest  : KoinTest {

    @Test
    fun parse() {
        startKoin{
            modules(
                asn1ParserMoudle
            )
        }

        val asn1Parser = get<ASN1Parser>()
        assertNotNull(asn1Parser)

        val base64String = "MIHqMIGVAgEAMDAxFjAUBgNVBAMMDWV4YW1wbGUubG9jYWwxFjAUBgNVBAoMDURlbW9uc3RyYXRpb24wXDANBgkqhkiG9w0BAQEFAANLADBIAkEAqu7qhOa63jTfT3KdAxp53ep7HHiJ9F6n6SIqBOeIqIStHK2wKT6PCk8qjRyHIz0nBiNT8gfYumzcAa+V8nX11QIDAQABoAAwDQYJKoZIhvcNAQELBQADQQCVwaST6W+IYTR5OPPSTUif+kjL3q0PgPEMg8pOLCW099+IU53PjsMxveFl+PzmNOq+VoXA/BEy9sv4EEaDkvtY"
        val byteArray = Base64.getDecoder().decode(base64String)

        val asN1Node = asn1Parser.parse(byteArray)
        println(asN1Node)
    }
}