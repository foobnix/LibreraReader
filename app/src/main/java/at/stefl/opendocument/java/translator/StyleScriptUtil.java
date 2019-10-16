package at.stefl.opendocument.java.translator;

import com.foobnix.android.utils.LOG;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import at.stefl.commons.io.CharStreamUtil;

// TODO: put under RescourceUtil into commons
public class StyleScriptUtil {
    
    public static void pipeResource(Class<?> clazz, String name, Writer out)
            {
        try {
            Reader in = new InputStreamReader(clazz.getResourceAsStream(name),
                    Charset.forName("UTF-8"));
            CharStreamUtil.writeStreamBuffered(in, out);
        }catch (Exception e){
            LOG.e(e);
        }
    }
    
    public static void pipeClassResource(Class<?> clazz, String extension,
            Writer out) throws IOException {
        String name = clazz.getSimpleName() + "." + extension;
        pipeResource(clazz, name, out);
    }
    
    public static void pipeStyleResource(Class<?> clazz, Writer out)
            throws IOException {
        pipeClassResource(clazz, "css", out);
    }
    
    public static void pipeScriptReource(Class<?> clazz, Writer out)
            throws IOException {
        pipeClassResource(clazz, "js", out);
    }
    
    private StyleScriptUtil() {}
    
}