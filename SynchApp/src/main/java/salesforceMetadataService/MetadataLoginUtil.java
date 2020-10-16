package salesforceMetadataService;
import java.io.IOException;

import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import SpringApp.Controllers.AppLogger;

/**
 * Salesforce Metadata Api Login utility.
 */
public class MetadataLoginUtil {


	public static MetadataConnection login(String url, String userName, String password, String token) throws ConnectionException, SecurityException, IOException {
		AppLogger.getLogger().info("Beginning of login()");

		String URL = url; 
		String USERNAME = userName;        
		
		String TOKEN = token; // Salesforce token is retrieved from setup in Salesforce website. See help in Salesforce
		String PASSWORD = password + TOKEN; 
		
		final LoginResult loginResult = loginToSalesforce(USERNAME, PASSWORD, URL);
		return createMetadataConnection(loginResult);
	}

	private static MetadataConnection createMetadataConnection(
			final LoginResult loginResult) throws ConnectionException, SecurityException, IOException {
		AppLogger.getLogger().info("Beginning of createMetadataConnection()");

		final ConnectorConfig config = new ConnectorConfig();
		config.setServiceEndpoint(loginResult.getMetadataServerUrl());
		config.setSessionId(loginResult.getSessionId());
		MetadataConnection meta = new MetadataConnection(config);

		return meta;
	}

	private static LoginResult loginToSalesforce(
			final String username,
			final String password,
			final String loginUrl) throws ConnectionException, SecurityException, IOException {
		AppLogger.getLogger().info("Beginning of loginToSalesforce()");

		final ConnectorConfig config = new ConnectorConfig();
		config.setAuthEndpoint(loginUrl);
		config.setServiceEndpoint(loginUrl);
		config.setManualLogin(true);
		return (new EnterpriseConnection(config)).login(username, password);
	}
}