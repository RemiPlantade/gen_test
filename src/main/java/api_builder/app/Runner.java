package api_builder.app;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.cli.MavenCli;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import api_conf.conf.model.ApiBean;
import api_conf.conf.model.ApiConf;
import api_conf.conf.model.ApiGroup;
import api_conf.conf.model.ApiGroupPerm;
import api_conf.conf.model.ApiUser;
import api_conf.conf.model.ApiUserPerm;

public class Runner {

	private static final String OUTPUT_FOLDER = "maven";
	private static final int BUFFER_SIZE = 1024;

	public static void main(String[] args) {
		// Clean previously created database
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile( new File( "pom.xml" ) );
		request.setGoals( Arrays.asList("clean, install"));
		Invoker invoker = new DefaultInvoker();
		invoker.setMavenExecutable(new File("mvn.cmd"));
		invoker.setMavenHome(new File("maven"));
		try {
			InvocationResult result = invoker.execute( request );
			// Create SQLIte Database to store User preferences.
			String databaseUrl = "jdbc:sqlite:src/main/resources/sample.db";
			// create a connection source to our database
			ConnectionSource connectionSource;

			connectionSource = new JdbcConnectionSource(databaseUrl);
			TableUtils.createTable(connectionSource, ApiBean.class);
			TableUtils.createTable(connectionSource, ApiConf.class);
			TableUtils.createTable(connectionSource, ApiGroup.class);
			TableUtils.createTable(connectionSource, ApiGroupPerm.class);
			TableUtils.createTable(connectionSource, ApiUser.class);
			TableUtils.createTable(connectionSource, ApiUserPerm.class);

		} catch (MavenInvocationException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		// Invoque Retro-engineering / configuration / generation process.
		try {
			request.setGoals( Arrays.asList("antrun:run@hbm2java -X" ) );
			InvocationResult result = invoker.execute( request );
		} catch (MavenInvocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Extracts a zip file specified by the zipFilePath to a directory specified by
	 * destDirectory (will be created if does not exists)
	 * @param zipFilePath
	 * @param destDirectory
	 * @throws IOException
	 */
	public static void unzip(String zipFilePath, String destDirectory) throws IOException {
		File destDir = new File(destDirectory);
		if (!destDir.exists()) {
			destDir.mkdir();
		}
		ZipInputStream zipIn = new ZipInputStream(Runner.class.getClassLoader().getResourceAsStream(zipFilePath));
		ZipEntry entry = zipIn.getNextEntry();
		// iterates over entries in the zip file
		while (entry != null) {
			String filePath = destDirectory + File.separator + entry.getName();
			if (!entry.isDirectory()) {
				// if the entry is a file, extracts it
				extractFile(zipIn, filePath);
			} else {
				// if the entry is a directory, make the directory
				File dir = new File(filePath);
				dir.mkdir();
			}
			zipIn.closeEntry();
			entry = zipIn.getNextEntry();
		}
		zipIn.close();
	}
	/**
	 * Extracts a zip entry (file entry)
	 * @param zipIn
	 * @param filePath
	 * @throws IOException
	 */
	private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
	}
}

