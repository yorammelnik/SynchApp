package SpringApp.Controllers;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.stereotype.Controller;

import com.bigid.appinfra.appinfrastructure.Controllers.AbstractLoggingController;

import appController.LoggerSingeltonnnnn;

@Controller
public class LogsController implements AbstractLoggingController {

    @Override
    public String getLogs(){   	
    	
    	// Get synch app's log file    	
    	String logFile = null;
    	try {
			logFile = AppLogger.getLogFile();
		} catch (FileNotFoundException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			try {
				LoggerSingeltonnnnn.getInstance().severe("error log Stack trace:"+ sw.toString());
			} catch (Exception e1) {				
				e1.printStackTrace();
			}			
		}
    	return logFile;
    }
}
