package SpringApp.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
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
    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public ResponseEntity<ActionResponseDetails> executeAction(@RequestBody ExecutionContext executionContext) {
        String action = executionContext.getActionName();
        String executionId = executionContext.getExecutionId();
                
        switch (action) {            
            case("SynchBigIdSalesforceCategories"):
                boolean sucess = ((ExecutionService)executionService).Synch(executionContext);
                return generateSyncSuccessMessage(executionId, "Synch result: " + sucess);
            default:
                return ResponseEntity.badRequest().body(
                        new ActionResponseDetails(executionId,
                                StatusEnum.ERROR,
                                0d,
                                "Got unresolved action = " + action));
        }
    }
}