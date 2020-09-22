package SpringApp.Controllers;

import java.io.FileNotFoundException;

import org.springframework.stereotype.Controller;

import com.bigid.appinfra.appinfrastructure.Controllers.AbstractLoggingController;

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
			e.printStackTrace();
		}
    	return logFile;
    }
}
