package appController;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.http.client.ClientProtocolException;
import org.springframework.web.bind.annotation.RequestBody;

import com.bigid.appinfra.appinfrastructure.DTO.ExecutionContext;
import com.bigid.appinfra.appinfrastructure.DTO.ParamDetails;
import com.sforce.ws.ConnectionException;

import SpringApp.Controllers.AppLogger;
import bigIdService.BigIdService;
import bigIdService.CategoryColumnContainer;
import bigIdService.ColumnToSynch;
import salesforceMetadataService.SalesforceMetadataService;



/*
 * @ Author: Yoram Melnik
 * 
 * Description: A class that manages the connection and synchronization of categories between BigId and Salesforce.
 * The class contains the main method for initiating the app.
 * This class can be "launched" in 2 ways:
 * 1. Launched from ide "run configuration" and then the configuration.xml file is used to initialize all data members. Rarely used. Maninly for develop and debug.
 * 2. Launched from BigId as an application using the application framework. Data members are set via executionContext received from BigId action.
 * 
 * ** See appController() comments for explaining the main functionality of the class. **
 * 
 */

public class BigIdSalesforceAppController {	

	// This flag sets the mode of the way Salesforce is updated. 
	// When the flag is FASLE BigID categories will be appended to the existing categories of SF.
	// TURE flag means that BigID overwrites the categories in SF to be identical to BigID db.
	private Boolean OVERWRITE_SF_CATEGORIES_TO_REFLECT_BIGID = false;

	// set this flag to true in configuration.xml if working in development with no ssl certificates
	private Boolean BYPASS_SSL_CERTIFICATE = false;

	// A flag signaling if the app will update SalesForce with BigId categories
	private Boolean SYNCH_CATEGORY_TO_SALESFORCE = true;
	// If setContextActionParams() method is called than it means that BigId app framework launched the class. The flag is set 
	// to true in order to prevent initializing data members from configuration.xml and initiating a postConnect call to BigId.
	private Boolean APP_INITIATED_FROM_BIGID = false;

	// A flag that decides if to synch fields from Salesforce that have complianceGroup values added manually.
	// The values will be synched to fields that were chosen in a correlationSet in BigId
	// This flag is set only from BigId launcher and not from the configuration.xml
	private Boolean APPLY_SALESFORCE_CATEGORIES_TO_CORRELATION_SETS = true;

	// A flag that decides if to apply BidId categories to complianceGroup metadata of fields in Salesforce.
	// This flag is set only from BigId launcher and not from the configuration.xml
	private Boolean  APPLY_BIGID_CATEGORIES_TO_SALESFORCE = true;

	// Salesforce metadata connection and attributes
	private SalesforceMetadataService salesforceMetaConnectionService;// = new SalesforceMetadataService(); 
	
	private String Salesforce_url;
	private String Salesforce_username;
	private String Salesforce_password;
	private String Salesforce_token;

	// Connection fields retrieved from BigId action
	private String BigId_url;

	// Token data member is either received form BigId via executionContext or form postConnect method in bigIdServices.
	private String BigId_Token = null;
	// Username and password are for running the app from eclipse and therefore a connection to BigId should be established
	private String BigId_userName;
	private String BigId_password;	

	private BigIdService bigIdConnectionService;
	
	private ArrayList<String> SalesforceComplianceGroupValues = new ArrayList<String>();
	private ArrayList<String> BigIdCategoryValues = new ArrayList<String>();
	
	private LoginData configurationXml = null;
	
	public static void main(String[] args) throws Exception {

		// instantiate the BigIdSalesforceController and call for the appController
		BigIdSalesforceAppController controller = new BigIdSalesforceAppController();
		controller.appController();	

	}	
	
	
	public BigIdSalesforceAppController() throws IOException {	
		salesforceMetaConnectionService = new SalesforceMetadataService();
	}	

	/**
	 * A method that sets the class params according to the executionContest received from BigId
	 * @param @RequestBody ExecutionContext executionContext	 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws Exception 
	 * 
	 */

	public void setContextActionParams(@RequestBody ExecutionContext executionContext) throws SecurityException, IOException {
		AppLogger.getLogger().info("Beginning of setContextActionParams()");
		
		this.BigId_url = executionContext.getBigidBaseUrl().substring (0, (executionContext.getBigidBaseUrl().length()-8));
		this.BigId_Token = executionContext.getBigidToken();
		// BigId Username and password are for running the app from eclipse and therefore a connection to BigId should be established
		this.BigId_userName = null;
		this.BigId_password = null;
		
		String[] args = list2Array(executionContext.getActionParams());		
			
		this.SYNCH_CATEGORY_TO_SALESFORCE = Boolean.valueOf(args[0]);
		this.APPLY_SALESFORCE_CATEGORIES_TO_CORRELATION_SETS = Boolean.valueOf(args[1]);
		this.OVERWRITE_SF_CATEGORIES_TO_REFLECT_BIGID = Boolean.valueOf(args[2]);		
		this.APPLY_BIGID_CATEGORIES_TO_SALESFORCE = Boolean.valueOf(args[3]);
		this.Salesforce_url = args[4];
		this.Salesforce_username = args[5];
		this.Salesforce_password = args[6];
		this.Salesforce_token = args[7];		
		AppLogger.setLogLevel(args[8]);
		this.BYPASS_SSL_CERTIFICATE = Boolean.valueOf(args[9]);
		
		// Flag that there is no need to retrieve parameters from the configuration.xml
		APP_INITIATED_FROM_BIGID = true;

	}

	// A method that converts from List to String[] array
	@SuppressWarnings("rawtypes")
	private String[] list2Array(List<ParamDetails> actionParams) {
		String[] params = new String[actionParams.size()];
		int i = 0;
		for (Iterator iterator = actionParams.iterator(); iterator.hasNext();) {
			ParamDetails paramDetails = (ParamDetails) iterator.next();
			params[i] = paramDetails.getParamValue();
			i++;
		}
		return params;    
	}

	/**
	 * The main method of the class that handles the flow of the App. 
	 * 
	 * Main flow of the app:
	 * 
	 * 1. Initiate a connection to Salesforce. Connection to BigId is being initiated only if the class is NOT launched from BigId.
	 * 2. Get category list from BigId and ComplianceGroup list from Salesforce.
	 * 3. Set new complianceGroup values from Salesforce as categories in BigId.
	 * 4. Set BigID new categories as complianceGroup metadata in the Salesforce complianceGoup list if SYNCH_CATEGORY_TO_SALESFORCE is true.
	 * 5. Get complianceGroup values from fields in Salesforce and set them in fields of relevant correlation sets in BigId if APPLY_SALESFORCE_CATEGORIES_TO_CORRELATION_SETS is true.
	 * 6. Set categories from BigId as complianceGroup metadata into the equivalent fields in Salesforce.
	 * 
	 * @param salesforce_token the salesforce_token to set
	 * @return the salesforce_token
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws ReturnFalseIndicationExceptionToBigId 
	 * @throws Exception 
	 * 
	 */
	public void appController() throws SecurityException, IOException, ReturnFalseIndicationExceptionToBigId {				
		AppLogger.getLogger().info("Beginning of appController()");

		try {

			// Load configuration file. If the app is NOT called from BigId but from main(String[] args) than the data from
			// from the configuration.xml actually is used to initialize data members
			configurationXml = loadConfiguration();

			// Initialize data members from configuration file
			if (! APP_INITIATED_FROM_BIGID) {				

				this.SYNCH_CATEGORY_TO_SALESFORCE = configurationXml.getSynch_categories_to_Salesforce();
				this.OVERWRITE_SF_CATEGORIES_TO_REFLECT_BIGID = configurationXml.getUPDATE_SF_TO_REFLECT_BIGID();
				this.BYPASS_SSL_CERTIFICATE = configurationXml.getBYPASS_SSL_CERTIFICATE();
				this.BigId_url = configurationXml.getBigId_url();
				this.BigId_userName = configurationXml.getBigId_userName();
				this.BigId_password = configurationXml.getBigId_password();
				this.Salesforce_url = configurationXml.getSalesforce_url();
				this.Salesforce_username = configurationXml.getSalesforce_username();
				this.Salesforce_password = configurationXml.getSalesforce_password();
				this.Salesforce_token = configurationXml.getSalesforce_token();
			}


			// Create new connection for metadata services with Salesforce 
			AppLogger.getLogger().info("Trying to connect to Salesforce.");
			salesforceMetaConnectionService.connect(Salesforce_url, Salesforce_username, Salesforce_password,Salesforce_token );		
			AppLogger.getLogger().fine("Conncetion to Salesforce succeded.");

			// Create a a BigId  service
			bigIdConnectionService = new BigIdService(BYPASS_SSL_CERTIFICATE,BigId_url ,BigId_userName , BigId_password, BigId_Token);

			// Only initiate a connection to BigId if the app is NOT called from BigId. 
			// If the app is called from BigId a connection is already established and a token is provided in the executionContext	
			if (! APP_INITIATED_FROM_BIGID) {
				AppLogger.getLogger().info("Trying to connect to BigId.");			
				bigIdConnectionService.postConnect();
				AppLogger.getLogger().fine("After bigIdConnectionService.postConnect(). Conncetion to BigId succeded.");
			}				

			// Retrieve BigId categories
			BigIdCategoryValues = bigIdConnectionService.getCategories();
			AppLogger.getLogger().info("BigId category values: " + BigIdCategoryValues.toString());

			// Retrieve ComplianceGroup metadata values from Salesforce
			SalesforceComplianceGroupValues =  salesforceMetaConnectionService.getComplianceGroupValues();
			AppLogger.getLogger().info("Salesforce ComplianceGroup values: " + SalesforceComplianceGroupValues.toString());

			// Add new complianceGroup values as Categories in BigId
			AddNewSaleforceComplianceGroupToBigId();
			AppLogger.getLogger().info("After call to AddNewSaleforceComplianceGroupToBigId and adding new complianceGroup to BigId.");

			// Add new categories and ComplianceGroup in Salesforce if the SYNCH_CATEGORY_TO_SALESFORCE is true
			if (SYNCH_CATEGORY_TO_SALESFORCE) {
				addCategoriesToSalesforce();
				AppLogger.getLogger().fine("After call to addCategoriesToSalesforce() and adding new categories to to Salesforce. SYNCH_CATEGORY_TO_SALESFORCE flag is" + SYNCH_CATEGORY_TO_SALESFORCE);
			} 

			// Retrieve from BigId all the "BigId Salesforce objects" that should be synched to Salesforce
			ArrayList<ColumnToSynch> bigIdColumnsToSynch = bigIdConnectionService.getObjectsToSynch();
			AppLogger.getLogger().fine("After bigIdConnectionService.getObjectsToSynch().");

			// Get Salesforce complianceGroup values for fields chosen in correlation sets if they exist
			if (APPLY_SALESFORCE_CATEGORIES_TO_CORRELATION_SETS) {
				getComplianceGrupeValuesForCorrelationSetFields();
				AppLogger.getLogger().fine("After bigIdConnectionService.getComplianceGrupeValuesForCorrelationSetFields().");
			}			

			// Write BigId columns with their categories (is they exist) to Salesforce
			if (APPLY_BIGID_CATEGORIES_TO_SALESFORCE) {							
				salesforceMetaConnectionService.deployAttributes(OVERWRITE_SF_CATEGORIES_TO_REFLECT_BIGID, bigIdColumnsToSynch);
				AppLogger.getLogger().fine("After salesforceMetaConnectionService.deployAttributes. UPDATE_SF_TO_REFLECT_BIGID flag is " + OVERWRITE_SF_CATEGORIES_TO_REFLECT_BIGID);	
			}
			AppLogger.getLogger().info("End of appController(). Action completed.");
			// Close the logger handler.
			//LoggerSingelton.closeHandler();

		}		
		// All exception are caught and written to log file and than a new exception is thrown to 
		// indicate to BigId that the action did not complete correctly
		catch (Exception e) {	
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			AppLogger.getLogger().severe("error log Stack trace:"+ sw.toString());
			throw new ReturnFalseIndicationExceptionToBigId("false");
		}		
		finally {
			// Close the log handler.
			//LoggerSingelton.closeHandler();
		}
	}

	/**	 
	 * A method that retrieves complianceGroup values from Salesforce for fields that are chosen in a correlation 
	 * set  in BigId that the dataSource is "Salesforce"	 * 
	 * @throws Exception 
	 * 
	 */
	private void getComplianceGrupeValuesForCorrelationSetFields() throws Exception {
		AppLogger.getLogger().info("Beginning of getComplianceGrupeValuesForCorrelationSetFields()");

		// Get columns from all the "Salesforce" correlation Sets
		ArrayList<ColumnToSynch> columns = bigIdConnectionService.getColumnsFromCorrelationsets();		

		// Use SalesforceMetadataService (through RetrieveMetadata) to retrieve the specific fields with their complianceGroup value 
		// from Salesforce
		ArrayList<CategoryColumnContainer> complianceGroupToUpdate = salesforceMetaConnectionService.retrieveCorrelationSetColumnsFromSalesforce(columns);		

		// Iterate through the fields that were retrieved from Salesforce and set the complianceGroup values as 
		// categories for the specific field in BigId
		bigIdConnectionService.updateCorrelationSetWithComplianceGroup(complianceGroupToUpdate);

	}

	/**	 
	 * A method that reads the configuraton.xml and returns the data to be loaded to the controller
	 * @param 
	 * @return LoginData object that contains all the data from the configuration.xml
	 * @throws IOException 
	 * @throws SecurityException 
	 * 
	 */
	private LoginData loadConfiguration() throws XMLStreamException, URISyntaxException, SecurityException, IOException   {
		AppLogger.getLogger().fine("Beginning of loadConfiguration()");

		// get the configuration.xml under resources
		InputStream in = getClass().getResourceAsStream("/configuration.xml"); 

		FileManipulationService parser = new FileManipulationService();
		LoginData configurationXml = parser.readConfig(in);

		in.close();
		return configurationXml;
	}

	/**	 
	 * A method that adds new categories to Salesforce
	 * @param 
	 * @return
	 * @throws IOException 
	 * @throws SecurityException 
	 * 
	 */
	private void addCategoriesToSalesforce() throws ConnectionException, SecurityException, IOException {		
		AppLogger.getLogger().info("Beginning of addMetadataCateoriesToSalesforce()");

		// get new BigId categories that are not in Salesforce and call addNewMetadataValue if there are new categories
		ArrayList<String> newCategories = findNewCategoryItemsFromBigId();
		if (!newCategories.isEmpty()) {
			// Set the new BigId categories in Salesforce
			salesforceMetaConnectionService.addNewMetadataValue(newCategories);
			AppLogger.getLogger().fine("New Categories added to Salesforce");
		}
	}

	/**	 
	 *  A method that lists all new category items in Salesforc
	 * @param 
	 * @return ArrayList of new categories from BigId
	 * @throws IOException 
	 * @throws SecurityException 
	 * 
	 */
	private ArrayList<String> findNewCategoryItemsFromBigId() throws SecurityException, IOException {
		AppLogger.getLogger().info("Beginning of findNewCategoryItemsFromBigId()");

		ArrayList<String> categoriesToAdd = new ArrayList<String>();

		// loop through complianceGroupValues and add them if they do not exist in BigId
		for (Iterator<String> iterator = BigIdCategoryValues.iterator(); iterator.hasNext();) {
			String currValue = (String) iterator.next();
			if (!SalesforceComplianceGroupValues.contains(currValue)) {
				categoriesToAdd.add(currValue);
			}			
		}		
		return categoriesToAdd;
	}

	/**	 
	 * A method that finds all the new Salesforce categories and adds them to BigId
	 * @param 
	 * @return
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	private void AddNewSaleforceComplianceGroupToBigId() throws ClientProtocolException, IOException, ResponseNotOKException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		AppLogger.getLogger().info("Beginning of AddNewSaleforceComplianceGroupToBigId()");

		// Get new Salesforce categories that are not in BigId
		ArrayList<String> newCategories = findNewCategoryItemsFromSalesforce();	
		if (!newCategories.isEmpty()) {
			// Set the new Salesforce categories in BigId
			bigIdConnectionService.postNewCategories(newCategories);
			AppLogger.getLogger().info("New Categories added to BigId " + newCategories.toString());
		}
	}

	/**	 
	 * A method that lists all new category items in Salesforce
	 * @param 
	 * @return - List of all the new categories in Salesforce
	 * @throws IOException 
	 * @throws SecurityException 
	 * 
	 */
	private ArrayList<String> findNewCategoryItemsFromSalesforce() throws SecurityException, IOException {
		AppLogger.getLogger().info("Beginning of findNewCategoryItemsFromSalesforce()");

		// BigIdCategoriesValue
		ArrayList<String> categoriesToAdd = new ArrayList<String>();

		// loop through complianceGroupValues and add them if they do not exist in BigId
		for (Iterator<String> iterator = SalesforceComplianceGroupValues.iterator(); iterator.hasNext();) {
			String currValue = (String) iterator.next();
			if (!BigIdCategoryValues.contains(currValue)) {
				categoriesToAdd.add(currValue);
			}			
		}		
		return categoriesToAdd;
	}
}
