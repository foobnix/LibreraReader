package junrar;

import java.io.File;
import java.io.IOException;

import junrar.exception.RarException;
import junrar.extract.RarExtractor;

/**
 * extract an archive to the given location
 * 
 * @author edmund wagner
 * 
 */
public class ExtractArchive {

	private ExtractArchive() {
		throw new IllegalAccessError();
	}

	public static void main(final String[] args) throws IOException, RarException {
		if (args.length == 3) {
			extractArchive(args[0], args[1], args[2]);
		}
		else if (args.length == 2) {
			extractArchive(args[0], args[1], null);
		}
		else {
			System.out.println("Usage: java -jar junrar.jar <archive.rar> <destination directory> [<password>]");
		}
	}

	public static void extractArchive(final String archive, final String destination, final String password) throws IOException, RarException {
		if (archive == null || destination == null) {
			throw new RuntimeException("Archive and destination must be set");
		}
		final File arch = new File(archive);
		if (!arch.exists()) {
			throw new RuntimeException("The archive does not exist: " + archive);
		}
		final File dest = new File(destination);
		if (!dest.exists() || !dest.isDirectory()) {
			throw new RuntimeException("The destination must exist and point to a directory: " + destination);
		}
		ExtractArchive.extractArchive(arch, dest, password);
	}

	public static void extractArchive(final File archive, final File destination, final String password) throws IOException, RarException {
		new RarExtractor().extractArchive(archive, destination);
	}

}
