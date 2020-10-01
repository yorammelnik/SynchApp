package appController;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


/*
 * A singelton class for logging info and errors to log file
 */
public class LoggerSingelton {	

	// static variable single_instance of type LoggerSingelton 
	private static LoggerSingelton logger_instance = null;
	FileHandler fHandler;
	
	private static String logFilePath = "";

	private static final Logger logger = Logger.getLogger(BigIdSalesforceAppController.class.getName());

	// private constructor restricted to this class itself 
	// Default level of logging is DEBUG
	private LoggerSingelton() throws SecurityException, IOException { 
		logger.setLevel(Level.FINE);
		
		Path resourceDirectory = Paths.get("src","main","resources", "BigIdSalesforceAppLogFile.log");		
		String absolutePath = resourceDirectory.toFile().getAbsolutePath();
		logFilePath = absolutePath;
		fHandler = new FileHandler(absolutePath);
		logger.addHandler(fHandler);
		SimpleFormatter formatter = new SimpleFormatter();
		fHandler.setFormatter(formatter);
	} 

	// static method to create instance of Singleton class 
	public static LoggerSingelton getInstance() throws SecurityException, IOException { 
		if (logger_instance == null) {
			logger_instance = new LoggerSingelton(); 
		}

		return logger_instance; 
	} 

	// Static method to get the log file as String
	public static String getLogFile() throws FileNotFoundException {

		File logFile = new File(logFilePath);
		Scanner myReader = new Scanner(logFile);
		String data = new String();
		while (myReader.hasNextLine()) {
			data = data + "\n" + myReader.nextLine();			
		}
		myReader.close();
		return data;
	}


	public Logger getLogger() {
		return logger;
	}

	// Set Log level for the app
	public static void setLogLevel(String logLevel) {

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
