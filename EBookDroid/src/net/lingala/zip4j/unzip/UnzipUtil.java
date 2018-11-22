package net.lingala.zip4j.unzip;

import java.io.File;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Zip4jUtil;

public class UnzipUtil {
	
	public static void applyFileAttributes(FileHeader fileHeader, File file) throws ZipException {
		applyFileAttributes(fileHeader, file, null);
	}
	
	public static void applyFileAttributes(FileHeader fileHeader, File file,
			UnzipParameters unzipParameters) throws ZipException{
		
		if (fileHeader == null) {
			throw new ZipException("cannot set file properties: file header is null");
		}
		
		if (file == null) {
			throw new ZipException("cannot set file properties: output file is null");
		}
		
		if (!Zip4jUtil.checkFileExists(file)) {
			throw new ZipException("cannot set file properties: file doesnot exist");
		}
		
		if (unzipParameters == null || !unzipParameters.isIgnoreDateTimeAttributes()) {
			setFileLastModifiedTime(fileHeader, file);
		}
		
		if (unzipParameters == null) {
			setFileAttributes(fileHeader, file, true, true, true, true);
		} else {
			if (unzipParameters.isIgnoreAllFileAttributes()) {
				setFileAttributes(fileHeader, file, false, false, false, false);
			} else {
				setFileAttributes(fileHeader, file, !unzipParameters.isIgnoreReadOnlyFileAttribute(),
						!unzipParameters.isIgnoreHiddenFileAttribute(), 
						!unzipParameters.isIgnoreArchiveFileAttribute(),
						!unzipParameters.isIgnoreSystemFileAttribute());
			}
		}
	}
	
	private static void setFileAttributes(FileHeader fileHeader, File file, boolean setReadOnly, 
			boolean setHidden, boolean setArchive, boolean setSystem) throws ZipException {
		if (fileHeader == null) {
			throw new ZipException("invalid file header. cannot set file attributes");
		}
		
		byte[] externalAttrbs = fileHeader.getExternalFileAttr();
		if (externalAttrbs == null) {
			return;
		}
		
		int atrrib = externalAttrbs[0];
		switch (atrrib) {
		case InternalZipConstants.FILE_MODE_READ_ONLY:
			if (setReadOnly) Zip4jUtil.setFileReadOnly(file);
			break;
		case InternalZipConstants.FILE_MODE_HIDDEN:
		case InternalZipConstants.FOLDER_MODE_HIDDEN:
			if (setHidden) Zip4jUtil.setFileHidden(file);
			break;
		case InternalZipConstants.FILE_MODE_ARCHIVE:
		case InternalZipConstants.FOLDER_MODE_ARCHIVE:
			if (setArchive) Zip4jUtil.setFileArchive(file);
			break;
		case InternalZipConstants.FILE_MODE_READ_ONLY_HIDDEN:
			if (setReadOnly) Zip4jUtil.setFileReadOnly(file);
			if (setHidden) Zip4jUtil.setFileHidden(file);
			break;
		case InternalZipConstants.FILE_MODE_READ_ONLY_ARCHIVE:
			if (setArchive) Zip4jUtil.setFileArchive(file);
			if (setReadOnly) Zip4jUtil.setFileReadOnly(file);
			break;
		case InternalZipConstants.FILE_MODE_HIDDEN_ARCHIVE:
		case InternalZipConstants.FOLDER_MODE_HIDDEN_ARCHIVE:
			if (setArchive) Zip4jUtil.setFileArchive(file);
			if (setHidden) Zip4jUtil.setFileHidden(file);
			break;
		case InternalZipConstants.FILE_MODE_READ_ONLY_HIDDEN_ARCHIVE:
			if (setArchive) Zip4jUtil.setFileArchive(file);
			if (setReadOnly) Zip4jUtil.setFileReadOnly(file);
			if (setHidden) Zip4jUtil.setFileHidden(file);
			break;
		case InternalZipConstants.FILE_MODE_SYSTEM:
			if (setReadOnly) Zip4jUtil.setFileReadOnly(file);
			if (setHidden) Zip4jUtil.setFileHidden(file);
			if (setSystem) Zip4jUtil.setFileSystemMode(file);
			break;
		default:
			//do nothing
			break;
		}
	}
	
	private static void setFileLastModifiedTime(FileHeader fileHeader, File file) throws ZipException {
		if (fileHeader.getLastModFileTime() <= 0) {
			return;
		}
		
		if (file.exists()) {
			file.setLastModified(Zip4jUtil.dosToJavaTme(fileHeader.getLastModFileTime()));
		}
	}
	
}
