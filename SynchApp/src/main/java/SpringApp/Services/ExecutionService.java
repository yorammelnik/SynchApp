package SpringApp.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.bigid.appinfra.appinfrastructure.DTO.ExecutionContext;
import com.bigid.appinfra.appinfrastructure.ExternalConnections.BigIDProxy;
import com.bigid.appinfra.appinfrastructure.Services.AbstractExecutionService;

import appController.BigIdSalesforceAppController;
import appController.ReturnFalseIndicationExceptionToBigId;

@Service
public class ExecutionService extends AbstractExecutionService {

	@Autowired
	public ExecutionService(BigIDProxy bigIDProxy) {
		super(bigIDProxy);
	}

	public boolean Synch(@RequestBody ExecutionContext executionContext){
		try {

			// start synch app with the params sent from BigId
			BigIdSalesforceAppController bigController = new BigIdSalesforceAppController();
			bigController.setContextActionParams(executionContext);
			bigController.appController();

		} catch (ReturnFalseIndicationExceptionToBigId e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}   

}
