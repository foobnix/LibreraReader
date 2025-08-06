package mobi.librera.smartreflow;

import java.util.List;

public interface SmartReflowInterface {


    void process(PlatformImage img) throws Exception;

    void drawObjects(PlatformImage output);

    void reflow(PlatformImage des);

    List<String> getStatistics();
}


