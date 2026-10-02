package com.foobnix.work;

import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CalirbeExtractor;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.SafOpfRegistry;
import com.foobnix.ui2.FileMetaCore;

import java.io.File;

/** A prior full extraction is reusable only when all known metadata inputs match. */
final class MetadataRefreshPolicy {
    private MetadataRefreshPolicy() {}

    static String settingsKey() {
        AppState state = AppState.get();
        return "v1|" + state.isShowOnlyOriginalFileNames + '|'
                + state.isUseCalibreOpf + '|'
                + state.isAuthorTitleFromMetaPDF + '|'
                + state.isFirstSurname;
    }

    static String revision(FileMeta found, SafOpfRegistry.Entry sidecar, String settings) {
        Long size;
        Long modified;
        String opf = "none";
        if (ExtUtils.isExteralSD(found.getPath())) {
            size = found.getSize();
            modified = found.getDate();
            if (sidecar != null) {
                if (sidecar.revision.contains("unknown:")) return null;
                opf = sidecar.opfUri + "|" + sidecar.revision;
            }
        } else {
            File file = new File(found.getPath());
            if (!file.isFile()) return null;
            size = file.length();
            modified = file.lastModified();
            File localOpf = CalirbeExtractor.getCalibreOPF(found.getPath());
            if (localOpf != null) {
                if (localOpf.lastModified() <= 0) return null;
                opf = localOpf.length() + ":" + localOpf.lastModified();
            }
        }
        if (size == null || size <= 0 || modified == null || modified <= 0) return null;
        String revision = settings + "|" + size + ":" + modified + "|" + opf;
        return revision;
    }

    static boolean needsExtraction(FileMeta before, String previousRevision, String currentRevision) {
        return before == null || before.getState() == null
                || before.getState() != FileMetaCore.STATE_FULL || currentRevision == null
                || !currentRevision.equals(previousRevision);
    }
    private static final long FIRST_RETRY = 10 * 60 * 1000L;
    private static final long MAX_RETRY = 24 * 60 * 60 * 1000L;

    static String failedRevision(String revision, long now) {
        return failedRevision(null, revision, now);
    }

    /** Exponential, capped retries also apply when the provider has no revision information. */
    static String failedRevision(String previous, String revision, long now) {
        String[] parts = failureParts(previous, revision);
        int attempts = 1;
        if (parts != null) {
            try { attempts = Math.min(9, Integer.parseInt(parts[1]) + 1); }
            catch (NumberFormatException corrupt) { /* restart the delay */ }
        }
        return now + "\n" + attempts + "\n" + revisionToken(revision);
    }

    private static String revisionToken(String revision) { return revision == null ? "?" : "=" + revision; }
    private static String[] failureParts(String failure, String revision) {
        if (failure == null) return null;
        String[] parts = failure.split("\n", 3);
        return parts.length == 3 && parts[2].equals(revisionToken(revision)) ? parts : null;
    }

    static boolean retryDue(String failure, String revision, long now) {
        String[] parts = failureParts(failure, revision);
        if (parts == null) return true;
        try {
            long failedAt = Long.parseLong(parts[0]);
            int attempts = Integer.parseInt(parts[1]);
            if (attempts < 1 || attempts > 9) return true;
            long delay = Math.min(MAX_RETRY, FIRST_RETRY * (1L << (attempts - 1)));
            return now < failedAt || now - failedAt >= delay;
        } catch (NumberFormatException corrupt) { return true; }
    }
}
