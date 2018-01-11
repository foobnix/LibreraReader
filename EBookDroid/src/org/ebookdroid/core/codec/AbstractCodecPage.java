package org.ebookdroid.core.codec;

import java.util.Collections;
import java.util.List;

public abstract class AbstractCodecPage implements CodecPage {

    @Override
    public List<PageLink> getPageLinks() {
        return Collections.emptyList();
    }

}
