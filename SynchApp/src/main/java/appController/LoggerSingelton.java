package appController;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


/*
 * A singelton class for logging 
 */
public class LoggerSingelton {	

	// static variable single_instance of type LoggerSingelton 
	private static LoggerSingelton logger_instance = null;
	FileHandler fHandler;

	//TRACE	DEBUG	INFO	WARN	ERROR	OFF
	//private static final Logger logger = (Logger) LoggerFactory.getLogger(BigIdSalesforceAppController.class);
	private static final Logger logger = Logger.getLogger(BigIdSalesforceAppController.class.getName());

	// private constructor restricted to this class itself 
	// Default level of logging is DEBUG
	private LoggerSingelton() throws SecurityException, IOException { 
		logger.setLevel(Level.FINE);
		fHandler = new FileHandler("C:\\BigIdSynchAppLog\\synchAppLogFile.log");
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

		File logFile = new File("C:\\BigIdSynchAppLog\\synchAppLogFile.log");
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
