package SpringApp.Controllers;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class AppLogger {	

	static FileHandler fHandler;

	private String logFileName = "BigIdSalesforceAppLogFile.log";

	private static File logFile;

	public static final Logger logger = Logger.getLogger("BigIdSalesforceAppLogger");

	// private constructor restricted to this class itself 
	// Default level of logging is DEBUG
	public AppLogger() throws SecurityException, IOException {		
		
		logger.setLevel(Level.INFO);

		String customDirPrefix = "Log_BidId_Salesforce_App_";
		Path logTempDirectory = Paths.get( Files.createTempDirectory(customDirPrefix).toAbsolutePath().toString(), logFileName ); 
		logFile = new File(logTempDirectory.toAbsolutePath().toString());		
		
		// Create (if they don't exist) the path to the resources directory where the log file and the 
		// Salesforce zip files reside.
		if (! Files.exists(logTempDirectory.toAbsolutePath())) {
			Files.createDirectories(logTempDirectory.getParent());
			Files.createFile(logTempDirectory.toAbsolutePath());
		}

		fHandler = new FileHandler(logFile.getAbsolutePath());

		logger.addHandler(fHandler);

		SimpleFormatter formatter = new SimpleFormatter();

		fHandler.setFormatter(formatter);
	} 

	// Static method to get the log file as String
	public static String getLogFile() throws FileNotFoundException {

		//File logFile = new File(logFilePath);
		Scanner myReader = new Scanner(logFile);
		String data = new String();
		while (myReader.hasNextLine()) {
			data = data + "\n" + myReader.nextLine();			
		}
		myReader.close();
		return data;
	}

	public static void closeHandler() {
		fHandler.close();
	}

	public static Logger getLogger() {
		return logger;
	}

	// Set Log level for the app
	public static void setLogLevel(String logLevel) {
		logLevel = logLevel.toUpperCase();
		switch (logLevel) {
		case "ALL":
			logger.setLevel(Level.ALL);
			break;
		case "SEVERE":
			logger.setLevel(Level.SEVERE);
			break;
		case "WARNING":
			logger.setLevel(Level.WARNING);
			break;
		case "INFO":
			logger.setLevel(Level.INFO);
			break;
		case "FINE":
			logger.setLevel(Level.FINE);
			break;
		case "FINER":
			logger.setLevel(Level.FINER);
			break;
		case "FINEST":
			logger.setLevel(Level.FINEST);
			break;
		case "CONFIG":
			logger.setLevel(Level.CONFIG);
			break;
		default:
			logger.setLevel(Level.INFO);
		}				
	}
}
