package at.stefl.commons.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class FilterCommandLineInterface implements
        CommandLineInterface {
    
    protected final CommandLineInterface src;
    protected final InputStream in;
    protected final OutputStream out;
    
    public FilterCommandLineInterface(CommandLineInterface src)
            throws IOException {
        this.src = src;
        this.in = getFilterInputStream(src.getInputStream());
        this.out = getFilterOutputStream(src.getOutputStream());
    }
    
    protected InputStream getFilterInputStream(InputStream in) {
        return in;
    }
    
    protected OutputStream getFilterOutputStream(OutputStream out) {
        return out;
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        return in;
    }
    
    @Override
    public OutputStream getOutputStream() throws IOException {
        return out;
    }
    
    @Override
    public void close() throws IOException {
        src.close();
    }
    
}