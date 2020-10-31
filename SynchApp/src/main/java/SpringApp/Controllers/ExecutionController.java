package SpringApp.Controllers;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import com.bigid.appinfra.appinfrastructure.Controllers.AbstractExecutionController;
import com.bigid.appinfra.appinfrastructure.DTO.ActionResponseDetails;
import com.bigid.appinfra.appinfrastructure.DTO.ExecutionContext;
import com.bigid.appinfra.appinfrastructure.DTO.StatusEnum;

import SpringApp.Services.ActionResult;
import SpringApp.Services.ExecutionService;

@Controller
public class ExecutionController extends AbstractExecutionController{
	
	//private static AppLogger logger; 

	@Autowired
	public ExecutionController(ExecutionService executionService) throws SecurityException, IOException {
		this.executionService = executionService;
		//logger = new AppLogger();		
	}

	@Override
	public ResponseEntity<ActionResponseDetails> executeAction(@RequestBody ExecutionContext executionContext) {
		AppLogger.getLogger().info("Begining of executeAction()");
		String action = executionContext.getActionName();
		String executionId = executionContext.getExecutionId();
		

		switch (action) {            
		case("Sync"):
			ActionResult result = ((ExecutionService)executionService).Synch(executionContext);
		if (result.isSucess()) {
			return generateSyncSuccessMessage(executionId, "The action completed sucessfully");
		}
		else {
			return generateSyncSuccessMessage(executionId, "The action failed with an error message : \"" + result.getText() + "\"");
		}
		default:
			return ResponseEntity.badRequest().body(
					new ActionResponseDetails(executionId,
							StatusEnum.ERROR,
							0d,
							"Got unresolved action = " + action));
		}
	}
}