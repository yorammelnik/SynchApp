package SpringApp.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bigid.appinfra.appinfrastructure.DTO.ActionResponseDetails;
import com.bigid.appinfra.appinfrastructure.DTO.ExecutionContext;
import com.bigid.appinfra.appinfrastructure.DTO.ParamDetails;
import com.bigid.appinfra.appinfrastructure.DTO.StatusEnum;
import com.bigid.appinfra.appinfrastructure.ExternalConnections.BigIDProxy;
import com.bigid.appinfra.appinfrastructure.Services.AbstractExecutionService;

import SpringApp.Controllers.AppLogger;
import appController.BigIdSalesforceAppController;

@Service
public class ExecutionService extends AbstractExecutionService {
	
	
	
	@Autowired
	public ExecutionService(BigIDProxy bigIDProxy, DsConnectionsParserService dsConnectionsParserService) {
		super(bigIDProxy);					
	}

	public void runPeriodicAction(ExecutionContext executionContext) {
		AppLogger.getLogger().info("Begining of ExecutionService.runPeriodicAction()");
					
		ActionResponseDetails actionResponseDetails;
		try {			
			// if one of the boolean parameres is invalid an exeption will be thrown
			validateBooleanActionParams(executionContext.getActionParams());

			// start synch app with the params sent from BigId
			BigIdSalesforceAppController bigController = new BigIdSalesforceAppController();
			bigController.setContextActionParams(executionContext);
			bigController.start();					

			// loop while bigController thread is running and send bigId progress info
			int j = 1;
			while (bigController.isAlive()) {	
				TimeUnit.SECONDS.sleep(6);
				double progress = j / 10.0;
				// limit to 9 so the user will not see percentage that is 100% and the action is not completed....
				if (j < 9) {
					j++;
				}
				String message = "Execution is in porgress...";
				actionResponseDetails = initializeResponseToBigID(executionContext, StatusEnum.IN_PROGRESS, progress, message);
				AppLogger.getLogger().info("In ExecutionService.runPeriodicAction()" + actionResponseDetails);
				bigIDProxy.updateActionStatusToBigID(actionResponseDetails);				
			}
			
			// check if an exception is raised in the thread to decide if the action was completed successfully or failed
			
			// Therad raised missing Salesforce url exception
			if (bigController.isIncorrectSalesforceURL()) {
				AppLogger.getLogger().severe("In ExecutionService.runPeriodicAction()- the action was not completed successfully");	
				actionResponseDetails = initializeResponseToBigID(executionContext, StatusEnum.ERROR, 0, "Category syncronization Failed Due to missing Salesforce URL.");
				bigIDProxy.updateActionStatusToBigID(actionResponseDetails);
			}
			// Therad raised incorrect Salesforce login exception
			else if (bigController.isIncorrectSalesforceLogin()) {
				AppLogger.getLogger().severe("In ExecutionService.runPeriodicAction()- the action was not completed successfully");	
				actionResponseDetails = initializeResponseToBigID(executionContext, StatusEnum.ERROR, 0, "Category syncronization Failed Due to incorrect login parameters to Salesforce.");
				bigIDProxy.updateActionStatusToBigID(actionResponseDetails);
			}		
			// General exception in the thread
			else if (bigController.isGeneralExceptionRaised()) {
				AppLogger.getLogger().severe("In ExecutionService.runPeriodicAction()- the action was not completed successfully");	
				actionResponseDetails = initializeResponseToBigID(executionContext, StatusEnum.ERROR, 0, "Category syncronization Failed. Please Check the log file.");
				bigIDProxy.updateActionStatusToBigID(actionResponseDetails);
								
			}
			// No exceptions were raised in the Thread. Action completed sucessfully
			else {
				actionResponseDetails = initializeResponseToBigID(executionContext, StatusEnum.COMPLETED, 1, "Category syncronization completed!");
				bigIDProxy.updateActionStatusToBigID(actionResponseDetails);			
			}

		}		
				
		catch (ActoionParamValueException e) {
			AppLogger.getLogger().severe("In ExecutionService.runPeriodicAction() " + e.getMessage());	
			actionResponseDetails = initializeResponseToBigID(executionContext, StatusEnum.ERROR, 0, e.getMessage());
			bigIDProxy.updateActionStatusToBigID(actionResponseDetails);
		}		
		catch (Exception e) {
			AppLogger.getLogger().severe("In ExecutionService.runPeriodicAction() " + e.getMessage());	
			actionResponseDetails = initializeResponseToBigID(executionContext, StatusEnum.ERROR, 0, "Category syncronization Failed. Please Check the log file.");
			bigIDProxy.updateActionStatusToBigID(actionResponseDetails);
		}
	

	}

	// check that all the flag strings have a boolean value
	private void validateBooleanActionParams(List<ParamDetails> actionParams) throws Exception {				
		
		ArrayList<String> booleanParameters = new ArrayList<String>();
		booleanParameters.add("Sync BigId category list to Salesforce");
		booleanParameters.add("Sync Salesforce categories to BigId Correlation set fields");
		booleanParameters.add("Overwrite Salesforce with BigId categories");
		booleanParameters.add("Apply BigId categories to Salesforce");
		booleanParameters.add("Bypass SSL certificate");
		
		for (int i = 0; i < actionParams.size(); i++) {
			ParamDetails currParam = actionParams.get(i);
			if (booleanParameters.contains(currParam.getParamName()) && ! ( "true".equals(currParam.getParamValue() ) || "false".equals(currParam.getParamValue()) )) {				
				throw new ActoionParamValueException("One of the flag action parametes is incorrect. Flag Name:  " + currParam.getParamName() + ", Flag value: " + currParam.getParamValue() );
			}
		}
	}

	private ActionResponseDetails initializeResponseToBigID(ExecutionContext executionContext,
			StatusEnum statusEnum,
			double progress,
			String message) {
		ActionResponseDetails actionResponseDetails = new ActionResponseDetails();
		actionResponseDetails.setExecutionId(executionContext.getExecutionId());
		actionResponseDetails.setStatusEnum(statusEnum);
		actionResponseDetails.setProgress(progress);
		actionResponseDetails.setMessage(message);

		return actionResponseDetails;
	}
}
