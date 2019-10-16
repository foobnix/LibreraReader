package at.stefl.commons.cli;

import java.io.IOException;

public interface CommandLineExecutor {
    
    public CommandLineInterface execute(String command) throws IOException;
    
}