package SpringApp.Controllers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.stereotype.Controller;

import com.bigid.appinfra.appinfrastructure.Controllers.AbstractLoggingController;

import appController.LoggerSingelton;
import appController.ReturnFalseIndicationExceptionToBigId;

@Controller
public class LogsController implements AbstractLoggingController {

    @Override
    public String getLogs(){   	
    	
    	// Get synch app's log file
    	// Get it from logback-test.xml file under resources of Bigid2SalesforceApp
    	String logFile = null;
    	try {
			logFile = appController.BigIdSalesforceAppController.getLogfile();
		} catch (FileNotFoundException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			try {
				LoggerSingelton.getInstance().getLogger().severe("error log Stack trace:"+ sw.toString());
			} catch (SecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}			
		}
    	return logFile;
    }
}
