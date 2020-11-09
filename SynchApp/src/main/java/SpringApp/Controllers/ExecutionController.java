package SpringApp.Controllers;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import com.bigid.appinfra.appinfrastructure.Controllers.AbstractExecutionController;
import com.bigid.appinfra.appinfrastructure.DTO.ActionResponseDetails;
import com.bigid.appinfra.appinfrastructure.DTO.ExecutionContext;
import com.bigid.appinfra.appinfrastructure.DTO.StatusEnum;

import SpringApp.Services.ExecutionService;

@Controller
public class ExecutionController extends AbstractExecutionController{


	@Autowired
	public ExecutionController(ExecutionService executionService) throws SecurityException, IOException {
		this.executionService = executionService;
	}

	@Autowired
	private TaskExecutor taskExecutor;

	@Override
	public ResponseEntity<ActionResponseDetails> executeAction(@RequestBody ExecutionContext executionContext) {
		AppLogger.getLogger().info("Begining of executeAction()");
		String action = executionContext.getActionName();
		String executionId = executionContext.getExecutionId();
		executionService.setValuesForBigIDProxy(executionContext);

		switch (action) {            
		case ("Sync"):
			taskExecutor.execute(() -> runPeriodicAction(executionContext));
			return generateAyncSuccessMessage(executionId, "Started category synchronization. Please wait...");	
		default:
			return ResponseEntity.badRequest().body(
					new ActionResponseDetails(executionId,
							StatusEnum.ERROR,
							0d,
							"Got unresolved action = " + action));
		}
	}

	private void runPeriodicAction(ExecutionContext executionContext) {
		AppLogger.getLogger().info("Begining of ExecutionController.runPeriodicAction(), Running an async method periodically...");  		
		((ExecutionService) executionService).runPeriodicAction(executionContext);
	}
}