package org.spreadme.pdfgadgets.di

import org.koin.dsl.module
import org.spreadme.pdfgadgets.config.upgrade.AppUpgradeRepository
import org.spreadme.pdfgadgets.repository.*

val appConfigLoadModule = module {
    single<AppConfigRepository> {
        DefaultAppConfigRepository()
    }
}

val appUpgradeModule = module {
    single {
        AppUpgradeRepository()
    }
}

val fileMetadataModule = module {
    single {
        FileMetadataRepository()
    }
}

val pdfParseModule = module {

    single {
        FileMetadataParser()
    }

    single<SignatureParser> {
        DefaultSignatureParser()
    }

    single<PdfMetadataParser> {
        DefaultPdfMetadataParser()
    }

    single {
        PdfStreamParser()
    }

    single {
        CustomPdfCanvasProcessor()
    }

    single<PdfTextSearcher> {
        DefaultPdfTextSearcher()
    }
}

val asn1ParserMoudle = module {
    single {
        ASN1Parser()
    }
}