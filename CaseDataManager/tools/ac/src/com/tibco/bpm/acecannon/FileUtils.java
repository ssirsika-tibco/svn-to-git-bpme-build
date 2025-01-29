package com.tibco.bpm.acecannon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.tibco.bpm.da.dm.api.DataModel;
import com.tibco.bpm.da.dm.api.DataModelSerializationException;

/**
 * Utility methods for manipulating files and similar resources
 * @author smorgan
 */
public class FileUtils
{
	// Default max file count allowed when producing a ZIP
	private static int MAX_FILES = 30;

	private static int addFilesToZip(ZipOutputStream zos, File[] files, File baseFile, int filesLimit)
			throws IOException
	{
		int filesAdded = 0;
		byte[] buf = new byte[1024];
		for (File file : files)
		{
			AceMain.log("Adding " + file);
			if (file.isDirectory())
			{
				File[] listFiles = file.listFiles();
				filesAdded += addFilesToZip(zos, listFiles, baseFile, filesLimit);
			}
			else
			{
				filesAdded++;
				if (filesAdded > filesLimit)
				{
					// This is a pragmatic sanity check to depend against accidentally choosing the wrong folder.
					// i.e. Selecting the root path would result in an attempt to zip up the entire drive, which will
					// then run out of disk space and cause all sorts of problems.  
					throw new IOException("Folder contains more than the permitted number of files. Maximum allowed is "
							+ filesLimit);
				}
				FileInputStream fis = new FileInputStream(file.getAbsoluteFile());
				URI relative = baseFile.toURI().relativize(file.getAbsoluteFile().toURI());
				zos.putNextEntry(new ZipEntry(relative.toString()));
				int len;
				while ((len = fis.read(buf)) > 0)
				{
					zos.write(buf, 0, len);
				}
				fis.close();
			}
		}
		return filesAdded;
	}

	public static File buildZipFromFolderURI(URL url, boolean deleteOnExit) throws IOException, URISyntaxException
	{
		File tempFile = File.createTempFile("temp", ".zip");
		if (deleteOnExit)
		{
			tempFile.deleteOnExit();
		}
		FileOutputStream fos = new FileOutputStream(tempFile);
		ZipOutputStream zos = new ZipOutputStream(fos);

		File f = new File(url.toURI());
		File[] listFiles = f.listFiles();
		int count = addFilesToZip(zos, listFiles, f, MAX_FILES);
		AceMain.log("Added " + count + " files to " + tempFile + " from " + f);
		zos.close();
		return tempFile;
	}

	public static FileInputStream buildInputStreamForZipFromFolderURL(URL url) throws IOException, URISyntaxException
	{
		return buildInputStreamForZipFromFolderURL(url, true);
	}

	public static FileInputStream buildInputStreamForZipFromFolderURL(URL url, boolean deleteOnExit)
			throws IOException, URISyntaxException
	{
		File file = buildZipFromFolderURI(url, deleteOnExit);
		return new FileInputStream(file);
	}

	private static Map<String, DataModel> getDataModelsFromDirectory(Path directory)
			throws IOException, DataModelSerializationException
	{
		Map<String, DataModel> result = new HashMap<>();
		DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory);
		for (Iterator<Path> iter = directoryStream.iterator(); iter.hasNext();)
		{
			Path next = iter.next();
			if (Files.isDirectory(next))
			{
				result.putAll(getDataModelsFromDirectory(next));
			}
			else if (next.getFileName().toString().endsWith(".dm"))
			{
				DataModel dm = DataModel.deserialize(Files.newInputStream(next));
				result.put(next.toString(), dm);
			}
		}
		return result;
	}

	public static Map<String, DataModel> getDataModelsFromRASC(Path zipPath)
			throws IOException, DataModelSerializationException
	{
		Map<String, DataModel> result = new HashMap<>();
		FileSystem zfs = FileSystems.newFileSystem(zipPath, null);
		for (Path rootD : zfs.getRootDirectories())
		{
			result.putAll(getDataModelsFromDirectory(rootD));
		}
		return result;
	}
}
