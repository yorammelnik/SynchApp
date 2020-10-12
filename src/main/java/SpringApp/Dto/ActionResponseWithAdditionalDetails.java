package SpringApp.Dto;

import java.util.HashMap;

import com.bigid.appinfra.appinfrastructure.DTO.ActionResponseDetails;
import com.bigid.appinfra.appinfrastructure.DTO.StatusEnum;

import lombok.Data;

@Data
public class ActionResponseWithAdditionalDetails extends ActionResponseDetails {
    private HashMap additionalData;

    public ActionResponseWithAdditionalDetails(String executionId, StatusEnum statusEnum, double progress, String message, HashMap additionalData) {
        super(executionId, statusEnum, progress, message);
        this.additionalData = additionalData;
    }
}
