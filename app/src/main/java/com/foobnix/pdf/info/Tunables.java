package com.foobnix.pdf.info;

/** Conservative provider concurrency; increase only after testing the selected backend. */
public final class Tunables {
    private Tunables() { }
    public static final int SAF_DISCOVERY_PARALLELISM = 2;
    public static final int METADATA_EXTRACTION_PARALLELISM = 2;
}
