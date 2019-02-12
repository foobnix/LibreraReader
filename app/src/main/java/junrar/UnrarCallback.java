package junrar;


/**
 *
 * @author alban
 */
public interface UnrarCallback {

    /**
     * Return <tt>true</tt> if the next volume is ready to be processed,
     * <tt>false</tt> otherwise.
     */
    boolean isNextVolumeReady(Volume nextVolume);

    /**
     * This method is invoked each time the progress of the current
     * volume changes.
     */
    void volumeProgressChanged(long current, long total);
}
