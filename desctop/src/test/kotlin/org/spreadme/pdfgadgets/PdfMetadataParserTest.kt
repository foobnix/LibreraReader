package org.spreadme.pdfgadgets

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.test.KoinTest
import org.koin.test.get
import org.spreadme.pdfgadgets.di.pdfParseModule
import org.spreadme.pdfgadgets.repository.FileMetadataParser
import org.spreadme.pdfgadgets.repository.PdfMetadataParser
import java.nio.file.Paths
import kotlin.test.assertNotNull

class PdfMetadataParserTest : KoinTest {

    @Test
    fun parse() {
        startKoin{
            modules(
                pdfParseModule
            )
        }

        val pdfMetadataParser = get<PdfMetadataParser>()
        assertNotNull(pdfMetadataParser)

        val fileMetadataParser = get<FileMetadataParser>()
        assertNotNull(fileMetadataParser)

        val resourceName = "/test.pdf"
        val contextClassLoader = Thread.currentThread().contextClassLoader!!
        val resource = contextClassLoader.getResource(resourceName)
            ?: (::PdfMetadataParserTest.javaClass).getResource(resourceName)
        assertNotNull(resource)


        val pdfMetadata = runBlocking {
            val fileMetadata = fileMetadataParser.parse(Paths.get(resource.file))
            pdfMetadataParser.parse(fileMetadata)
        }
        assertNotNull(pdfMetadata)
    }
}