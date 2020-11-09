package SpringApp.Services;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import SpringApp.Controllers.AppLogger;

@Service
@Slf4j
public class DsConnectionsParserService {

    public int getAmountOfConnections(JSONObject dsConnectionsObject) {
        try {
            JSONArray httpResultBody = (JSONArray)(dsConnectionsObject.get("ds_connections"));
            return httpResultBody.length();
        } catch (JSONException ex){            
            AppLogger.getLogger().info("In DsConnectionsParserService.getAmountOfConnections() - unable to parse dsConnections field");
            return -1;
        }
    }
}
