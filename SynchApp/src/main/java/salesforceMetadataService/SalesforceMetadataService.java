package salesforceMetadataService;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import com.sforce.soap.metadata.CustomField;
import com.sforce.soap.metadata.FieldType;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.ReadResult;
import com.sforce.soap.metadata.StandardValue;
import com.sforce.soap.metadata.StandardValueSet;
import com.sforce.soap.metadata.UpsertResult;
import com.sforce.ws.ConnectionException;

import SpringApp.Controllers.AppLogger;
import appController.FileManipulationService;
import bigIdService.CategoryColumnContainer;
import bigIdService.ColumnToSynch;
import net.lingala.zip4j.exception.ZipException;
//import salesforceRestService.SalesforceRestService;

/*
 * @ Author: Yoram Melnik
 * Description: A class that handles the connection and data management with Salesforce through metaData API
 * 
 */
public class SalesforceMetadataService {

	private final String TYPE = "StandardValueSet";
	@SuppressWarnings("unused")
	private final String SECURITY_CLASSIFICATION = "SecurityClassification";
	private final String COMPLIANCE_GROUP = "ComplianceGroup";

	private MetadataConnection metadataConnection = null;

	// Directories and files that are used for deployment of the complianceGroup metadata
	private final String UNZIPPED_DIRECTORY = "unzipped";
	private final String UNPACKAGED_DIRECTORY = "unpackaged";
	private final String ZIP_TO_UPLOAD_FILE = "zipToUpload.zip";
	private final static String RETRIEVE_RESULT = "retrieveResult.zip";
	
	private static String zipTempDirectoryPath;

	// This file is used by RetrieveMetada class.
	// For clarity of all the files used with Salesforce it is declared in this class.
	public static String getRetrieveResultFile() {
		return RETRIEVE_RESULT;
	}


	public SalesforceMetadataService() throws IOException {
		String customDirPrefix = "ZipFiles_BidId_Salesforce_App_";
		Path tempDirectory = Paths.get( Files.createTempDirectory(customDirPrefix).toAbsolutePath().toString() );
		zipTempDirectoryPath = tempDirectory.toAbsolutePath().toString();
	}


	// Use MetadateUtil class to connect to Salesforce and return a metadata connection
	public void connect(String url, String userName, String password, String token) throws ConnectionException, SecurityException, IOException  {
		AppLogger.getLogger().fine("Beginning of connect() {}");
		metadataConnection = MetadataLoginUtil.login( url,  userName,  password,  token);	

		// If the connection fails an exception will porpagate all the way back to main method
	}

	/**	 
	 * Get all the complianceGroupValues from Salesforce
	 * @throws IOException 
	 * @throws SecurityException 
	 */	
	public ArrayList<String> getComplianceGroupValues() throws ConnectionException, SecurityException, IOException{
		AppLogger.getLogger().info("Beginning of getComplianceGroupValues()");

		MetadataValuesContainer valuesContainer = getMedatadataValue(TYPE, COMPLIANCE_GROUP);

		//  If the getMedatadataValue fails an exception will porpagate all the way back to main method

		return valuesContainer.getMetaDataValues();		
	}

	/**	 
	 * Use metadata api to read metadate from Salesforce
	 * @throws IOException 
	 * @throws SecurityException 
	 */ 

	private MetadataValuesContainer getMedatadataValue(String type, String typeName) throws ConnectionException, SecurityException, IOException {
		AppLogger.getLogger().info("Beginning of getMedatadataValue()");

		MetadataValuesContainer container = new MetadataValuesContainer();

		// Read data
		ReadResult readResult = metadataConnection.readMetadata(type, new String[] {typeName});

		//  If the readMetaData fails an exception will porpagate all the way back to main method

		Metadata[] mdInfo = readResult.getRecords();

		// Extract data to a set in the container
		if (mdInfo[0] != null) {	
			StandardValueSet currSet = (StandardValueSet) mdInfo[0];
			container.setMetaDataType(currSet.getFullName());

			for (int j = 0; j < currSet.getStandardValue().length; j++ ) {
				container.addItemToList(currSet.getStandardValue()[j].getFullName());
			}
			// save the StandardValueSet in the container
			container.setCurrentSet(currSet);
		} 
		else { // Empty metadata
			AppLogger.getLogger().warning("In getMedatadataValue(). Metadata type");
		}

		return container;
	}

	/**	 
	 * Add new category values from BigId to Salesforce as ComplianceGroup
	 * @throws IOException 
	 * @throws SecurityException 
	 */ 
	public void addNewMetadataValue(ArrayList<String> newCategories) throws ConnectionException, SecurityException, IOException  {
		AppLogger.getLogger().info("Beginning of addNewMetadataValue()");

		// Get current compliance group values from Salesforce and add them to the new items - metadata api requirement.
		// Inserting only new items to the metada will disactive the old values. Therefore we insert all of them again
		// with the active flag set to true;
		ArrayList<String> currentSalesforceComplianceValues = getMedatadataValue(TYPE, COMPLIANCE_GROUP).getMetaDataValues();
		currentSalesforceComplianceValues.addAll(newCategories);

		StandardValueSet newSet = new StandardValueSet();
		newSet.setFullName(COMPLIANCE_GROUP);
		newSet.setStandardValue(new StandardValue[currentSalesforceComplianceValues.size()]);

		for (int i = 0; i < currentSalesforceComplianceValues.size(); i++) {

			StandardValue currValue = new StandardValue();
			currValue.setFullName(currentSalesforceComplianceValues.get(i));
			currValue.setDefault(false);
			currValue.setDescription("Synched from BigId - " + LocalDateTime.now());
			currValue.setIsActive(true);
			currValue.setLabel(currentSalesforceComplianceValues.get(i));
			currValue.setClosed(false);
			newSet.getStandardValue()[i] = currValue;

			// Name field with a type and label is required
			CustomField cf = new CustomField();
			cf.setType(FieldType.Picklist);
			cf.setLabel(newSet.getFullName() + " Name");
			//co.setNameField(cf);			

		}

		@SuppressWarnings("unused")
		UpsertResult[] results = metadataConnection.upsertMetadata(new Metadata[] { newSet });
		AppLogger.getLogger().info("Inserting {} new metadata complianceGroup values to Salesforce");

		//  If the updateMetadata fails an exception will porpagate all the way back to main method		
	}

	/**	 
	 * A method that deploys all new attributes from BigId to Salesforce
	 */
	public void deployAttributes(Boolean OVERWRITE_SF, ArrayList<ColumnToSynch> bigIdColumnsToSynch) throws RemoteException, Exception {
		AppLogger.getLogger().info("Beginning of deployAttributes(). newAttributes lists:");

		RetrieveMetadata ret = new RetrieveMetadata(metadataConnection);
		File retrieveResult = ret.retrieveZip(bigIdColumnsToSynch);	

		// unzip the file, update the complianceGroup tags, and zip the files back
		String newZipFileToUpload = updateRetrievedData(OVERWRITE_SF, retrieveResult.getAbsolutePath(), bigIdColumnsToSynch);

		// Deploy the new zip back to SF
		DeployMetada deploy = new DeployMetada(newZipFileToUpload, metadataConnection);
		deploy.deployZip();
	}

	/**	 
	 * A method that updates BigId categories for all the fields that were retrieved from Salesforce
	 * 1. Unzip the retrieved.zip from Salesforce
	 * 2. add new categories
	 * 3. create a new zip ready to deploy to Salesforce
	 */
	private String updateRetrievedData(Boolean OVERWRITE_SF, String retrievedZipFilePath, ArrayList<ColumnToSynch> bigIdColumnsToSynch) throws URISyntaxException, XMLStreamException, IOException, ParserConfigurationException, SAXException, TransformerException, ZipException {
		AppLogger.getLogger().info("Beginning of updateRetrievedData()");

		Path unzippedDirectoryPath = Paths.get(getTempDirectory(), UNZIPPED_DIRECTORY);

		AppLogger.getLogger().fine("In updateRetrievedData(), Line 193, UNZIPPED_DIRECTORY: " +UNZIPPED_DIRECTORY);
		AppLogger.getLogger().fine("In updateRetrievedData(), Line 143, FileManipulationService.getResourceDirectory(): " + getTempDirectory() );
		AppLogger.getLogger().fine("In updateRetrievedData(), Line 195, path: " + unzippedDirectoryPath.toAbsolutePath().toString());
		AppLogger.getLogger().fine("In updateRetrievedData(), Line 196, retrievedZipFilePath: " + retrievedZipFilePath);

		String unzippedDirectory = FileManipulationService.extractZipFile(retrievedZipFilePath, unzippedDirectoryPath.toString() );		

		FileManipulationService.addComplianceGroupToZipFile(OVERWRITE_SF, unzippedDirectory, bigIdColumnsToSynch);

		Path zipToUploadPath = Paths.get(getTempDirectory() , ZIP_TO_UPLOAD_FILE);

		Path directoryToZipPath = Paths.get(getTempDirectory() , UNZIPPED_DIRECTORY, UNPACKAGED_DIRECTORY);

		FileManipulationService.zip(zipToUploadPath.toString(), directoryToZipPath.toString());	

		return zipToUploadPath.toString();		

	}


	/**	 
	 * A method that uses SalesforceMetadataService (through RetrieveMetadata) to retrieve the specific fields with their 
	 * complianceGroup value from Salesforce
	 * @param ArrayList<ColumnToSynch> columns
	 * @throws Exception 
	 * 
	 */
	public ArrayList<CategoryColumnContainer> retrieveCorrelationSetColumnsFromSalesforce(ArrayList<ColumnToSynch> columns) throws Exception {
		AppLogger.getLogger().info("Beginning of retrieveCorrelationSetColumns()");

		RetrieveMetadata ret = new RetrieveMetadata(metadataConnection);
		File retrieveResult = ret.retrieveZip(columns);

		ArrayList<String> files = FileManipulationService.getFilesFromZipfile(retrieveResult);

		return FileManipulationService.readComplianceGroupfieldAndValues(files);		

	}

	// Get temp directory from everywhere in the project
	public static String getTempDirectory() throws IOException {
		//Path resourceDirectory = Paths.get("src","main","resources");		
		return zipTempDirectoryPath;
	}

}
