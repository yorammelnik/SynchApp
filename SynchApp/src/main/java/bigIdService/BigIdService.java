package bigIdService;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import SpringApp.Controllers.AppLogger;
import appController.ResponseNotOKException;

/*
 * @ Author: Yoram Melnik
 * Description: Micro service for handling the connection and rest api calls to BigId
 * 
 */
@SuppressWarnings("rawtypes")
public class BigIdService {

	private static String URL;
	private static final String API = "/api";
	private final static String VERSION = "/v1";
	private final static String SESSIONS = "/sessions";
	private final static String DATA_CATEGORIES = "/data_categories";
	private final static String ATTRIBUTES = "/lineage/attributes";
	private final static String ID_CONNECTIOS = "/id_connections";
	private final static String DS_CONNECTIOS = "/ds_connections";


	private static final int CONNECTION_TIMEOUT = 3000;

	private String USER_NAME;
	private String PASSWORD; 
	private String TOKEN = "";

	private Boolean USE_SSL_CERTIFICATE = false;

	private HttpClient client;			

	/**	 
	 * Param: useSSLCertificate - a boolean to decide if httpClient should use SSL certificate
	 * Param: url, userName,password - taken from configuration file
	 * @param: A list of the categories that need to be added to BigId
	 * @return - A list of the categories in BigId
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	public BigIdService(Boolean useSSLCertificate, String url, String userName, String password, String bigIdToken) throws SecurityException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		AppLogger.getLogger().fine("Beginning of BigIdService()");
		// set fields from configuration.xml
		URL = url;
		USER_NAME = userName;
		PASSWORD = password;
		TOKEN = bigIdToken;
		USE_SSL_CERTIFICATE = useSSLCertificate;		
	}

	/**
	 * A method for creating an httpClient.
	 * 
	 * Use sslcontext library to deal with BigId instance in Development without SSL certificate.
	 * Use this only when there isn't a certificate in the BigId environment.
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	private HttpClient createClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, SecurityException, IOException {
		AppLogger.getLogger().fine("Beginning of createClient())");
		if (USE_SSL_CERTIFICATE) {	
			javax.net.ssl.SSLContext sslcontext = SSLContexts
					.custom()
					.loadTrustMaterial(TrustAllStrategy.INSTANCE)
					.build();

			SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
					sslcontext, NoopHostnameVerifier.INSTANCE);

			client = HttpClients
					.custom()
					.setSSLSocketFactory(sslSocketFactory)
					.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
					.build();    

		}
		else {
			client = HttpClientBuilder.create().build();
		}

		return client;
	}

	/**	 
	 * A method that initiated the first connection to BigId and stores the session auth_tokn in TOKEN data member
	 * 
	 * @param: A list of the categories that need to be added to BigId
	 * @return - A list of the categories in BigId
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	public void postConnect() throws ClientProtocolException, IOException, ResponseNotOKException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {				
		AppLogger.getLogger().info("Beginning of postConnect()");


		// Set user name and password 		
		JSONObject jsonPostObject = new JSONObject();
		jsonPostObject.put("password", PASSWORD);
		jsonPostObject.put("username", USER_NAME);

		String loginUrl = URL + API + VERSION + SESSIONS;

		HttpPost httpPostRequest = new HttpPost(loginUrl);		
		httpPostRequest.setHeader("Accept", "application/json");
		httpPostRequest.setHeader("Content-type", "application/json");
		StringEntity params = new StringEntity(jsonPostObject.toString());
		httpPostRequest.setEntity(params);	

		// Set CONNECTION_TIMEOUT secondes timeout for the http call
		final RequestConfig timeoutParams = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(CONNECTION_TIMEOUT).build();
		httpPostRequest.setConfig(timeoutParams);		

		createClient();
		// execute and convert response to json object
		HttpResponse response = client.execute(httpPostRequest);
		proccessHttpResponse(response, loginUrl);

		String result = EntityUtils.toString(response.getEntity());		
		JSONObject responseJObject = new JSONObject(result);

		// set TOKEN = auth_token from response
		TOKEN = responseJObject.getString("auth_token");			
		AppLogger.getLogger().fine("postConnect(). Token received is " + TOKEN);
	}

	/**	 
	 * Get all the categories from BigId
	 * 
	 * @param: A list of the categories that need to be added to Salesforce
	 * @return - A list of the categories in BigId
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	public ArrayList<String> getCategories() throws ClientProtocolException, IOException, ResponseNotOKException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		AppLogger.getLogger().fine("Beginning of getCategories() {}");

		String uri = URL + API + VERSION + DATA_CATEGORIES;

		HttpGet getRequest = new HttpGet(uri);
		getRequest.setHeader("Authorization", TOKEN);
		getRequest.setHeader("Accept", "application/json");
		getRequest.setHeader("Content-type", "application/json");

		ArrayList<String> categoryList = new ArrayList<String>();

		// Set CONNECTION_TIMEOUT secondes timeout for the http call
		final RequestConfig timeoutParams = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(CONNECTION_TIMEOUT).build();
		getRequest.setConfig(timeoutParams);

		createClient();
		HttpResponse response = client.execute(getRequest);
		proccessHttpResponse(response, uri);

		String result = EntityUtils.toString(response.getEntity());

		AppLogger.getLogger().finer("result string for uri: " + uri + ":" + result);

		// Set the results into a JSONArray. An array is used because there may be more that on category 
		JSONArray categories  = new JSONArray(result);

		// Iterate through the JSONArray and extract all the categories into a categoryList
		JSONObject currentJson;
		for (int i = 0; i < categories.length(); i++) {
			currentJson = categories.getJSONObject(i);
			categoryList.add(currentJson.getString("name"));
		}

		return categoryList;
	}


	/**	 
	 * Add new categories into BigId
	 * 
	 * @param: A list of the categories that need to be added to BigId
	 * @return
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	public void postNewCategories(ArrayList<String> newCategories) throws ClientProtocolException, IOException, ResponseNotOKException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {				
		AppLogger.getLogger().info("Beginning of postNewCategories()");

		String uri = URL + API + VERSION + DATA_CATEGORIES;
		JSONArray newCategoriesJsonArray = new JSONArray(newCategories);

		// Loop through the array and execute each category individually
		for (int i = 0; i < newCategoriesJsonArray.length(); i++) {

			HttpPost httpPostRequest = new HttpPost(uri);	
			httpPostRequest.setHeader("Authorization", TOKEN);
			httpPostRequest.setHeader("Accept", "application/json");
			httpPostRequest.setHeader("Content-type", "application/json");

			String currCategory = newCategoriesJsonArray.getString(i);
			AppLogger.getLogger().fine("postNewCategories. currCategory = " + currCategory);

			LocalDateTime today = LocalDateTime.now();
			//JSONObject jsonPostObject = new JSONObject( "{\"unique_name\":" + "\"" + currCategory + "\"," + "\"description\":" + "\"Imported from Salesforce on - " + today +  "\"," + "\"display_name\":" + "\"" + currCategory + "\"," + "\"color\":" + "\""  + "\"}" );
			JSONObject jsonPostObject = new JSONObject();
			jsonPostObject.put("unique_name", currCategory);
			jsonPostObject.put("description", "Imported from Salesforce on" + ": " + today);
			jsonPostObject.put("display_name", currCategory);
			jsonPostObject.put("color", "");

			StringEntity params = new StringEntity(jsonPostObject.toString());
			httpPostRequest.setEntity(params);

			// Set CONNECTION_TIMEOUT secondes timeout for the http call
			final RequestConfig timeoutParams = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(CONNECTION_TIMEOUT).build();
			httpPostRequest.setConfig(timeoutParams);			

			// create a new client for each iteration
			createClient();
			AppLogger.getLogger().fine("postNewCategories. Before client.execute(httpPostRequest). " + currCategory);
			HttpResponse response = client.execute(httpPostRequest);
			AppLogger.getLogger().fine("postNewCategories. After client.execute(httpPostRequest). " + currCategory);

			proccessHttpResponse(response, uri);			

			AppLogger.getLogger().fine("JsonObject when posting a new category " + jsonPostObject.toString());

		}		
	}

	/**	A method that returns a list containing all the columns to synch to Salesforce.
	 *  Each columnsToSynch may have attributes and the attributes may have categories.
	 *  
	 * @param 
	 * @return ArrayList<ColumnToSynch> columnsToSynch
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */

	public ArrayList<ColumnToSynch> getObjectsToSynch() throws ClientProtocolException, IOException, ResponseNotOKException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		AppLogger.getLogger().info("Beginning of getObjectsToSynch");

		// get all the "Objects" from bigId that were retrieved from Salesforce
		ArrayList<JSONObject> salesforceRelatedObjects = getSalesforceObjectsFromDb();

		// A list of all the categories and columns that have a certain attribute from all the Salesforce object in BigId
		ArrayList<CategoryColumnContainer> categoryAndColumnList = getCategoriesAndColumns(salesforceRelatedObjects);

		// The return list containing all the columns to synch to Salesforce
		ArrayList<ColumnToSynch> columnsToSynch = new ArrayList<ColumnToSynch>();		

		for (Iterator iterator = salesforceRelatedObjects.iterator(); iterator.hasNext();) {	

			JSONObject objectToSynch = (JSONObject) iterator.next();
			String currObject = objectToSynch.getString("fullyQualifiedName");
			AppLogger.getLogger().info("getObjectsToSynch, Current object is: " + currObject);

			// set a query to retrieve the columns of the current object
			String uri = URL + API + VERSION +"/data-catalog/object-details/columns?object_name=" + currObject;
			AppLogger.getLogger().fine("IN getObjectsToSynch, uri is " + uri);

			HttpGet getRequest = new HttpGet(uri);
			getRequest.setHeader("Authorization", TOKEN);
			getRequest.setHeader("Accept", "application/json");
			getRequest.setHeader("Content-type", "application/json");


			// Set CONNECTION_TIMEOUT secondes timeout for the http call
			final RequestConfig timeoutParams = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(CONNECTION_TIMEOUT).build();
			getRequest.setConfig(timeoutParams);

			createClient();
			HttpResponse response = client.execute(getRequest);
			proccessHttpResponse(response, uri);

			JSONObject currentSFOjbect = new JSONObject(EntityUtils.toString(response.getEntity()));
			AppLogger.getLogger().fine("IN getObjectsToSynch, currentSFOjbect is: " + currentSFOjbect.toString());

			JSONArray columns = currentSFOjbect.getJSONArray("data");

			for (int k = 0; k < columns.length(); k++) {

				JSONObject detailedColumnInfo = columns.getJSONObject(k);	

				// create container to contain the current column
				ColumnToSynch currentColumn =  new ColumnToSynch("Salesforce", objectToSynch.getString("objectName"));
				currentColumn.setColumnName(detailedColumnInfo.getString("column_name"));

				// if current column does not have an attribute list skip the if statement and just add the new currentColumn to columnsToSynch
				// if an attribute list exists for this column set the attributes in currentColumn and the categoris that may be an empty list 
				if (! detailedColumnInfo.isNull("attribute_list")) {

					// for the specific column retrieve the relevant categories from categoryAndColumnList - there may be no categories
					ArrayList<String> categoryList = getCategoriesForColumn(currObject, categoryAndColumnList, detailedColumnInfo.getString("column_name"));

					// Set attribues (classifiers) -  There can be more than 1 attribute per column
					JSONArray attributes = detailedColumnInfo.getJSONArray("attribute_list");	
					for (int i = 0; i < attributes.length(); i++) {
						String attribue =  attributes.getJSONObject(i).getString("attribute_original_name");
						currentColumn.addAttribute(attribue);
					}					
					currentColumn.setCategories(categoryList);					
				}
				columnsToSynch.add(currentColumn);
			}
		}

		AppLogger.getLogger().fine("IN getObjectsToSynch, Columns to synch" + columnsToSynch.toString());

		dealWithSalesforceComplexFields(columnsToSynch);

		return columnsToSynch;
	}

	/**	 
	 * Salesforce has complex fields that are split into a few sub fields. Address is split to City, Country, PostalCode, State, Street, etc.... 
	 * Name in Salesforce is also complex and is splitted to FirstName and LastName.
	 * In BigId we use the splitted fields but when sending the data to Salesforce we need to combine them into the complex field. We
	 * gather all the categories from the sub fields and append them to the complex field.
	 * 
	 * @param ArrayList<ColumnToSynch> columnsToSynch
	 * @return void
	 * @throws IOException 
	 * @throws SecurityException 
	 *  City, Country, PostalCode, State, Street --> Address
	 *  FirstName, LastName --> Name
	 */
	private void dealWithSalesforceComplexFields(ArrayList<ColumnToSynch> columnsToSynch) throws SecurityException, IOException {

		AppLogger.getLogger().info("Begining of dealWithSalesforceComplexFields()");

		ArrayList<ColumnToSynch> addressColumnsToDealWith = new ArrayList<ColumnToSynch>();
		ArrayList<ColumnToSynch> addressColumnsToRemove = new ArrayList<ColumnToSynch>();

		ArrayList<ColumnToSynch> nameColumnsToDealWith = new ArrayList<ColumnToSynch>();
		ArrayList<ColumnToSynch> nameColumnsToRemove = new ArrayList<ColumnToSynch>();


		// Iterate over the entire columnsToSynch list and find the First field that to be dealt with
		for (Iterator iterator = columnsToSynch.iterator(); iterator.hasNext();) {
			ColumnToSynch columnToSynch = (ColumnToSynch) iterator.next();
			if (columnToSynch.getColumnName().contains("City")) {
				addressColumnsToDealWith.add(columnToSynch);
			}
			if (columnToSynch.getColumnName().equals("FirstName")) {
				nameColumnsToDealWith.add(columnToSynch);
			}
		}

		// Deal with Address fields
		for (Iterator iterator = addressColumnsToDealWith.iterator(); iterator.hasNext();) {
			ColumnToSynch columnToDeal = (ColumnToSynch) iterator.next();

			String columnName = columnToDeal.getColumnName();
			String addressPrefix = columnName.substring(0, columnName.length() - 4);
			columnToDeal.setColumnName(addressPrefix + "Address");			

			for (Iterator iterator2 = columnsToSynch.iterator(); iterator2.hasNext();) {
				ColumnToSynch columnToSynch = (ColumnToSynch) iterator2.next();
				if (columnToDeal.getTableFullyQualifiedName().contentEquals(columnToSynch.getTableFullyQualifiedName()) && 
						( columnToSynch.getColumnName().contentEquals(addressPrefix + "Country") ||
								columnToSynch.getColumnName().contentEquals(addressPrefix + "PostalCode") ||
								columnToSynch.getColumnName().contentEquals(addressPrefix + "State") || 
								columnToSynch.getColumnName().contentEquals(addressPrefix + "Street") ) ) {
					columnToDeal.addCategoriesNoDuplicates(columnToSynch.getCategories());
					columnToDeal.addAttributesNoDuplicates(columnToSynch.getAttributes());
					addressColumnsToRemove.add(columnToSynch);
				}				
			}
		}

		// Deal with Name fields			
		for (Iterator iterator1 = nameColumnsToDealWith.iterator(); iterator1.hasNext();) {
			ColumnToSynch nameColumnToDeal = (ColumnToSynch) iterator1.next();

			String columnName1 = nameColumnToDeal.getColumnName().substring (5, nameColumnToDeal.getColumnName().length() );
			nameColumnToDeal.setColumnName(columnName1);			

			for (Iterator iterator2 = columnsToSynch.iterator(); iterator2.hasNext();) {
				ColumnToSynch columnToSynch = (ColumnToSynch) iterator2.next();
				if (nameColumnToDeal.getTableFullyQualifiedName().contentEquals(columnToSynch.getTableFullyQualifiedName()) && 
						( columnToSynch.getColumnName().contentEquals("LastName") ) ) {
					nameColumnToDeal.addCategoriesNoDuplicates(columnToSynch.getCategories());
					nameColumnToDeal.addAttributesNoDuplicates(columnToSynch.getAttributes());
					nameColumnsToRemove.add(columnToSynch);
				}				
			}
		}

		// Remove the extra fields from columnsToSync after the their categories and attributes were copied to the first field found
		columnsToSynch.removeAll(nameColumnsToRemove);			
		columnsToSynch.removeAll(addressColumnsToRemove);			
	}

	/**	 
	 * A method called from getObjectsToSynch() and returns the categories of the specific column parameter.
	 * 
	 * @param String currObject, ArrayList categoryAndColumnList, String column
	 * @return ArrayList<String> categoriesFound
	 * @throws IOException 
	 * @throws SecurityException 
	 * 
	 */
	private ArrayList<String> getCategoriesForColumn(String currObject, ArrayList categoryAndColumnList, String column) throws SecurityException, IOException {
		AppLogger.getLogger().fine("Begining of getCategoriesForColumn()");

		ArrayList<String> categoriesFound = new ArrayList<String>();
		for (Iterator iterator = categoryAndColumnList.iterator(); iterator.hasNext();) {
			CategoryColumnContainer currContainer = (CategoryColumnContainer) iterator.next();
			if (currContainer.getTableName().equals(currObject) && (currContainer.getColumnNames().contains(column)) ) {
				for (Iterator iterator2 = currContainer.getCategories().iterator(); iterator2.hasNext(); ) {
					String currCategory = (String) iterator2.next();
					categoriesFound.add(currCategory);					
				}
			}
		}

		return categoriesFound;
	}

	/**	 
	 * A method called from getObjectsToSynch() and returns A list of all the categories and columns that have 
	 * a certain attribute from all the Salesforce object in BigId 
	 * 
	 * @param  ArrayList<JSONObject> salesforceRelatedObjects
	 * @return ArrayList<CategoryColumnContainer> categoriesAndColumsFound
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	private ArrayList<CategoryColumnContainer> getCategoriesAndColumns(ArrayList<JSONObject> salesforceRelatedObjects) throws ParseException, ResponseNotOKException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		AppLogger.getLogger().fine("Begining of getCategoriesAndColumns()");

		ArrayList<CategoryColumnContainer> categoriesAndColumsFound = new ArrayList<CategoryColumnContainer>();

		for (Iterator iterator = salesforceRelatedObjects.iterator(); iterator.hasNext();) {			
			JSONObject objectToSynch = (JSONObject) iterator.next();

			// set a query to retrieve the attributes of the object			
			String uri = URL + API + VERSION +"/data-catalog/object-details/attributes?object_name=" + objectToSynch.getString("fullyQualifiedName");
			AppLogger.getLogger().fine("IN getCategoriesAndColumns(), uri is: " + uri);

			HttpGet getRequest = new HttpGet(uri);
			getRequest.setHeader("Authorization", TOKEN);
			getRequest.setHeader("Accept", "application/json");
			getRequest.setHeader("Content-type", "application/json");

			// Set CONNECTION_TIMEOUT secondes timeout for the http call
			final RequestConfig timeoutParams = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(CONNECTION_TIMEOUT).build();
			getRequest.setConfig(timeoutParams);

			createClient();
			HttpResponse response = client.execute(getRequest);
			proccessHttpResponse(response, uri);

			JSONObject currentSFOjbect = new JSONObject(EntityUtils.toString(response.getEntity()));
			AppLogger.getLogger().fine("IN getCategoriesAndColumns(), currentSFOjbect is " + currentSFOjbect.toString());

			JSONArray data = (JSONArray) currentSFOjbect.get("data");

			for (int k = 0; k < data.length(); k++) {

				JSONObject detailedInfo = (JSONObject) data.getJSONObject(k);			

				// Add to synch ALL the objects - even if they don't have categories. 
				JSONArray columnList = detailedInfo.getJSONArray("column_list");
				CategoryColumnContainer currCC =  new CategoryColumnContainer();
				currCC.setTableName(objectToSynch.getString("fullyQualifiedName"));

				for (int j = 0; j < columnList.length(); j++) {	
					// in this api only high and medium confidence results categories are displayed
					currCC.addColumnName((( JSONObject) columnList.get(j)).getString("column_name"));
				}
				// Add categories
				if ( !detailedInfo.isNull("categories") ){  // && ! detailedInfo.getJSONArray("categories").isEmpty()) {
					JSONArray categories = detailedInfo.getJSONArray("categories");
					for (int i = 0; i < categories.length(); i++) {
						currCC.addCategory((( JSONObject) categories.get(i)).getString("display_name"));
					}
				}

				// There could be to attributes that have the SAME columns and the SAME categories.
				// If this happens than we do not need to add theis currCC
				if (! duplicateCategoryColumnContainerExits(categoriesAndColumsFound, currCC )) {
					categoriesAndColumsFound.add(currCC);	
				}
			}
		}
		return categoriesAndColumsFound;
	}

	/**	 
	 * A method that is called from getCategoriesAndColumns() and checks if their is a duplicate in categoriesAndColumsFound list
	 * for the currCC parameter because we do not want duplicate items in the list.
	 * 
	 * @param 
	 * @return
	 * @throws IOException 
	 * @throws SecurityException 
	 * 
	 */
	private boolean duplicateCategoryColumnContainerExits(ArrayList<CategoryColumnContainer> categoriesAndColumsFound,
			CategoryColumnContainer currCC) throws SecurityException, IOException {
		AppLogger.getLogger().fine("Begining of duplicateCategoryColumnContainerExits");

		for (Iterator iterator = categoriesAndColumsFound.iterator(); iterator.hasNext();) {
			CategoryColumnContainer categoryColumnContainer = (CategoryColumnContainer) iterator.next();
			if (categoryColumnContainer.equals(currCC)) {
				return true;
			}			
		}
		return false;
	}

	/**	 
	 * A method that retrieves all the Salesforce related objects from BigId database.
	 * 
	 * @return ArrayList<JSONObject> salesforceObjects
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	private ArrayList<JSONObject> getSalesforceObjectsFromDb() throws ResponseNotOKException, ParseException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		AppLogger.getLogger().fine("Beginning of getSalesforceObjectsFromDb()");

		String uri =  URL + API + VERSION + "/data-catalog?format=json&&sort=&filter=system=Salesforce";

		ArrayList<JSONObject> salesforceObjects = new ArrayList<JSONObject>();

		HttpGet getRequest = new HttpGet(uri);
		getRequest.setHeader("Authorization", TOKEN);
		getRequest.setHeader("Accept", "application/json");
		getRequest.setHeader("Content-type", "application/json");

		// Set CONNECTION_TIMEOUT secondes timeout for the http call
		final RequestConfig timeoutParams = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(CONNECTION_TIMEOUT).build();
		getRequest.setConfig(timeoutParams);

		createClient();
		HttpResponse response = client.execute(getRequest);
		AppLogger.getLogger().fine("In getSalesforceObjectsFromDb, Status code is " + response.getStatusLine().getStatusCode() + " response is " + response.toString());

		proccessHttpResponse(response, uri);

		// Set the results into a JSONArray. An array is used because there may be more that on category
		String result = EntityUtils.toString(response.getEntity());			 
		JSONObject jo = new JSONObject(result);
		JSONArray jArray = jo.getJSONArray("results");

		//Iterate through the JSONAraay and extract all the categories into a categoryList
		JSONObject currentJson;
		for (int j = 0; j < jArray.length(); j++) {
			currentJson = jArray.getJSONObject(j);
			Object a = currentJson.get("attribute_original_name");
			// if currentJason has attributes than add it to the ArrayList 
			if (! ((JSONArray) a).isEmpty() ) {
				salesforceObjects.add(currentJson);						
			}
		}
		AppLogger.getLogger().fine("In getSalesforceObjectsFromDb, salesforceObjects to write: " + salesforceObjects.toString() );
		return salesforceObjects;
	}

	/**	 
	 * A method that processes an http response and throws an exception if the response is not OK
	 * 
	 * @param HttpResponse response, String uri
	 * @return void
	 * 
	 * Throws ResponseNotOKException if response is not OK
	 */
	public void proccessHttpResponse(HttpResponse response, String uri) throws ResponseNotOKException, ParseException, IOException {
		// Process the result
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == 200) {
		}
		else {
			AppLogger.getLogger().severe("In proccessHttpResponse(), Response code for uri" + uri + "is " + statusCode + " response, reasonPhrase:" + response.getStatusLine().getReasonPhrase());
			throw new ResponseNotOKException("Response code for uri " + uri + " is" +  statusCode);
		}
	}


	/**	 
	 * Get all the relevant columns from Attributes list in the correlation page that correspond to the correlationsets that are set for Salesforce.
	 * These are the attributes that need to be sent to Salesforce to retrieve their complianceGroup values. 
	 * 
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws ResponseNotOKException 
	 * @throws ParseException 
	 * 
	 */
	public ArrayList<ColumnToSynch> getAttributesForRetrieval() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, SecurityException, IOException, ParseException, ResponseNotOKException{
		AppLogger.getLogger().info("Beginning of getAttributesForRetrieval()");

		// Get all the fields from the correlation sets that are of data source type "Salesforce"
		ArrayList<ColumnToSynch> relevantColumsFromCorrelation = getColumnsFromCorrelationsets();

		// Get all the attributes from correlation page
		JSONArray idsor_attributes = getCurrentAttributes();

		ArrayList<ColumnToSynch> columnsToRetrieve = new ArrayList<ColumnToSynch>();

		// loop through the attribute list and for each attribute that exists in relevantColumsFromCorrelation create a ColumnToSynch 
		for (int i = 0; i < idsor_attributes.length(); i++) {
			String attribute = idsor_attributes.getJSONObject(i).getString("friendly_name");
			
			ArrayList<ColumnToSynch> attributeFound = isAttributeExistInCorrelation(attribute, relevantColumsFromCorrelation);

			if (! attributeFound.isEmpty()) {
				
				// For each table that the attribute is in it create a ColumnToSynch object
				for(int j = 0; j < attributeFound.size(); j++) {
					ColumnToSynch currColumn = new ColumnToSynch("Salesforce" , attributeFound.get(j).getTableFullyQualifiedName());
					currColumn.setColumnName(attribute);
					columnsToRetrieve.add(currColumn);
				}	
			}
		}

		// Convert primitive fields into their complex types because the retrieval from Salesforce is from the Complex field
		// The method updates columnsToRetrieve list that is sent as a parameter.
		convertPrimitve2Complex(columnsToRetrieve);
		return columnsToRetrieve;
	}

	/**	 
	 * Find if the attribure parameter exists in the input list. if exists return it. Otherwise, return null
	 * @throws KeyStoreException  
	 * 
	 */
	private ArrayList<ColumnToSynch> isAttributeExistInCorrelation(String attribute,
			ArrayList<ColumnToSynch> relevantColumsFromCorrelation) {
		AppLogger.getLogger().fine("Beginning of isAttributeExistInCorrelation()");

		ArrayList<ColumnToSynch> columnsFound = new ArrayList<ColumnToSynch>();

		for (int i = 0; i < relevantColumsFromCorrelation.size(); i++) {
			if (attribute.equalsIgnoreCase(relevantColumsFromCorrelation.get(i).getColumnName())) {
				columnsFound.add(relevantColumsFromCorrelation.get(i));
			}
		}
		return columnsFound;
	}

	/**	 
	 * Get all the relevant Attributes from the correlation page. 
	 * 
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws ResponseNotOKException 
	 * @throws ParseException 
	 * 
	 */
	public JSONArray getCurrentAttributes() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, SecurityException, IOException, ParseException, ResponseNotOKException {
		AppLogger.getLogger().info("Beginning of getCurrentAttributes()");

		String uri = URL + API + VERSION + ATTRIBUTES;

		HttpGet getRequest = new HttpGet(uri);
		getRequest.setHeader("Authorization", TOKEN);
		getRequest.setHeader("Accept", "application/json");
		getRequest.setHeader("Content-type", "application/json");

		// Set CONNECTION_TIMEOUT seconds timeout for the http call
		final RequestConfig timeoutParams = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(CONNECTION_TIMEOUT).build();
		getRequest.setConfig(timeoutParams);

		createClient();
		HttpResponse response = client.execute(getRequest);
		proccessHttpResponse(response, uri);

		String result = EntityUtils.toString(response.getEntity());

		AppLogger.getLogger().fine("In getAttributes(), result string for uri: " + uri + ":" + result);

		// Go down the Jsonobject hierarchy until reaching the list of attributes.
		JSONObject Jresult  = new JSONObject(result);
		JSONArray data = Jresult.getJSONArray("data");
		JSONObject jArrayData = data.getJSONObject(0);
		JSONObject attributes = jArrayData.getJSONObject("attributes");
		JSONArray idsor_attributes = attributes.getJSONArray("idsor_attributes");

		return idsor_attributes;
	}


	/**	 
	 * Get all the tables that are linked to the input attribute parameter in the correlation page.
	 * 
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws ResponseNotOKException 
	 * @throws ParseException 
	 * 
	 */
	private ArrayList<String> getTablesForAttribute(String attribute) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, SecurityException, IOException, ParseException, ResponseNotOKException {
		AppLogger.getLogger().fine("Beginning of getTablesForAttribute()");

		ArrayList<String> tables = new ArrayList<String>();
		String uri = URL + API + VERSION + "/lineage/collections?filter=attribute=" + attribute;


		HttpGet getRequest = new HttpGet(uri);
		getRequest.setHeader("Authorization", TOKEN);
		getRequest.setHeader("Accept", "application/json");
		getRequest.setHeader("Content-type", "application/json");

		// Set CONNECTION_TIMEOUT seconds timeout for the http call
		final RequestConfig timeoutParams = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(CONNECTION_TIMEOUT).build();
		getRequest.setConfig(timeoutParams);

		createClient();
		HttpResponse response = client.execute(getRequest);
		proccessHttpResponse(response, uri);

		String result = EntityUtils.toString(response.getEntity());

		AppLogger.getLogger().fine("In getTablesForAttribute(), result string for uri: " + uri + ":" + result);

		// Set the results into a JSONArray.  
		JSONObject Jresult  = new JSONObject(result);
		JSONArray data = Jresult.getJSONArray("data");
		JSONObject collectionsInfo = data.getJSONObject(0);
		JSONArray collections = collectionsInfo.getJSONArray("collectionsInfo");	

		for (int i = 0; i <  collections.length(); i++) {			
			JSONObject jo = collections.getJSONObject(i);			
			JSONArray ja = jo.getJSONArray("collections");
			for (int k = 0; k < ja.length(); k++){
				// remove "Salesforce" from the table name. Salesforce.contact --> contact
				tables.add(ja.getString(k).substring(11));
			}
		}

		return tables;
	}

	/**	 
	 * Get all the relevant columns from the relevant correlationSets 
	 * 
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws ResponseNotOKException 
	 * @throws ParseException 
	 * 
	 */
	public ArrayList<ColumnToSynch> getColumnsFromCorrelationsets() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, SecurityException, IOException, ParseException, ResponseNotOKException {
		AppLogger.getLogger().info("Beginning of getColumnsFromCorrelationsets()");

		// https://yoram4.westeurope.cloudapp.azure.com/api/v1/id_connections
		String uri = URL + API + VERSION + ID_CONNECTIOS;

		HttpGet getRequest = new HttpGet(uri);
		getRequest.setHeader("Authorization", TOKEN);
		getRequest.setHeader("Accept", "application/json");
		getRequest.setHeader("Content-type", "application/json");

		// Set CONNECTION_TIMEOUT seconds timeout for the http call
		final RequestConfig timeoutParams = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(CONNECTION_TIMEOUT).build();
		getRequest.setConfig(timeoutParams);

		createClient();
		HttpResponse response = client.execute(getRequest);
		proccessHttpResponse(response, uri);

		String result = EntityUtils.toString(response.getEntity());

		AppLogger.getLogger().fine("In getColumnsFromCorrelationsets(), result string for uri: " + uri + ":" + result);

		// Set the results into a JSONArray.  
		JSONArray correlationSets  = new JSONObject(result).getJSONArray("id_connections");


		// Get all the data sources in Bigid in order to be able to find all the correlation sets that their
		// data source type is Saleforce.
		JSONArray dataSources = getCurrentDataSourceFromDB();

		ArrayList<JSONObject> salesForceCorrelationSets = new ArrayList<JSONObject>();

		// Iterate through the JSONAraay "correlationSets" and find the correlationSets that their data sourse is of type "Salesforce"
		for (int i = 0; i < correlationSets.length(); i++) {
			JSONObject currentCorrelation = correlationSets.getJSONObject(i);
			// only work with correlation sets that are enabled
			if (currentCorrelation.getString("enabled").equalsIgnoreCase("yes")) {
				String correlationDsConncection = currentCorrelation.getString("dsConnection");
				// loop through the data source list and add to salesForceCorrelationSets the correlation sets that
				// are of type "Salesforce"
				for (int j = 0; j < dataSources.length(); j++) {
					JSONObject currentDatasource = dataSources.getJSONObject(j);
					String datasourceName = currentDatasource.getString("name");
					if (datasourceName.equalsIgnoreCase(correlationDsConncection)  && currentDatasource.getString("type").equalsIgnoreCase("Salesforce") ) {
						salesForceCorrelationSets.add(currentCorrelation);
					}
				}	
			}
		}

		// Iterate over each correlation set and extract all the columns into columnsToRetrieve list
		ArrayList<ColumnToSynch> columnsToRetrieve = new ArrayList<ColumnToSynch>();		
		for (Iterator iterator = salesForceCorrelationSets.iterator(); iterator.hasNext();) {
			JSONObject jsonObject = (JSONObject) iterator.next();
			JSONArray attributes = jsonObject.getJSONArray("attributes");
			for (int i = 0; i < attributes.length(); i++) {
				JSONObject currentJson = (JSONObject) attributes.get(i);				
				String tableName = jsonObject.getString("db_table");
				ColumnToSynch currColumn = new ColumnToSynch("Salesforce" , tableName );
				currColumn.setColumnName(currentJson.getString("columnName"));
				columnsToRetrieve.add(currColumn);				
			}						
		}

		// Convert primitive fields into their complex types because the retrieval from Salesforce is from the Complex field
		// The method updates columnsToRetrieve list that is sent as a parameter.
		//convertPrimitve2Complex(columnsToRetrieve);
		
		
		return columnsToRetrieve;
	}	

	/**	 
	 * Get all the data sources that are currently in BigId data base. 
	 * 
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws ResponseNotOKException 
	 * @throws ParseException 
	 * 
	 */
	private JSONArray getCurrentDataSourceFromDB() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, SecurityException, IOException, ParseException, ResponseNotOKException {
		String uri = URL + API + VERSION + DS_CONNECTIOS;

		HttpGet getRequest = new HttpGet(uri);
		getRequest.setHeader("Authorization", TOKEN);
		getRequest.setHeader("Accept", "application/json");
		getRequest.setHeader("Content-type", "application/json");

		// Set CONNECTION_TIMEOUT seconds timeout for the http call
		final RequestConfig timeoutParams = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(CONNECTION_TIMEOUT).build();
		getRequest.setConfig(timeoutParams);

		createClient();
		HttpResponse response = client.execute(getRequest);
		proccessHttpResponse(response, uri);

		String result = EntityUtils.toString(response.getEntity());

		AppLogger.getLogger().fine("In getColumnsFromCorrelationsets(), result string for uri: " + uri + ":" + result);

		// Set the results into a JSONArray.  
		JSONArray dataSources  = new JSONObject(result).getJSONArray("ds_connections");
		return dataSources;
	}

	/**	 
	 * Convert primitive fields into their complex types because the retrieval from Salesforce is from the Complex field and the 
	 * presentatin in correlation page if of the subtypes of the complex field. 
	 * The method updates columnsToRetrieve list that is sent as a parameter.
	 * 
	 * @param ArrayList<CategoryColumnContainer> columnsToRetrieve
	 * @return void	 * 
	 * @throws SecurityException 	 * 
	 * @throws IOException 
	 *  City, Country, PostalCode, State, Street --> Address
	 *  FirstName, LastName --> Name
	 */
	private void convertPrimitve2Complex(ArrayList<ColumnToSynch> columnsToRetrieve) throws SecurityException, IOException {
		AppLogger.getLogger().fine("Beginning of BigIdService.convertPrimitve2Complex(). columnsToRetrieve: " + columnsToRetrieve);

		ArrayList<ColumnToSynch> primitiveAddressColumns = new ArrayList<ColumnToSynch>();
		ArrayList<ColumnToSynch> primitiveNameColumns = new ArrayList<ColumnToSynch>();

		// Loop through the list and find the all the primitive columns that are of the type Address or Name
		for (int i = 0; i < columnsToRetrieve.size(); i++) {
			if ( columnsToRetrieve.get(i).getColumnName().contains("City") ||
					columnsToRetrieve.get(i).getColumnName().contains("Country") ||
					columnsToRetrieve.get(i).getColumnName().contains("PostalCode") ||
					columnsToRetrieve.get(i).getColumnName().contains("State") ||
					columnsToRetrieve.get(i).getColumnName().contains("Street")) {
				primitiveAddressColumns.add(columnsToRetrieve.get(i));
			}
			if (columnsToRetrieve.get(i).getColumnName().contains("FirstName") ||
					columnsToRetrieve.get(i).getColumnName().contains("LastName")) {
				primitiveNameColumns.add(columnsToRetrieve.get(i));				
			}			
		}

		ArrayList<ColumnToSynch> addressPostfix = extractAddressPostfix(primitiveAddressColumns);
		ArrayList<ColumnToSynch> namePostfix = extractNamePrefix(primitiveNameColumns);

		// loop through the list of the primitive Address columns to find if there is the complex field in the list
		for (int j = 0; j < addressPostfix.size(); j++) {
			Boolean ComlexFieldFound = false;
			ColumnToSynch currentPostfix = addressPostfix.get(j);
			ColumnToSynch temp = currentPostfix.deepCopy();
			temp.setColumnName(currentPostfix.getColumnName()+ "Address");
			for (int k = 0; k < columnsToRetrieve.size(); k++) {
				ColumnToSynch currColumn = columnsToRetrieve.get(k);
				if ( temp.equals(currColumn) ) {
					ComlexFieldFound = true;
				}
			}
			if (! ComlexFieldFound) {
				currentPostfix.setColumnName(currentPostfix.getColumnName()+ "Address");
				columnsToRetrieve.add(currentPostfix);
			}
		}
		// remove the primitve types from columnsToRetrieve
		columnsToRetrieve.removeAll(primitiveAddressColumns);

		// loop through the list of the primitive Name columns to find if there is the complex field in the list
		for (int j = 0; j < namePostfix.size(); j++) {
			Boolean ComlexFieldFound = false;
			ColumnToSynch currentPostfix = namePostfix.get(j);
			ColumnToSynch temp = currentPostfix.deepCopy();
			temp.setColumnName(currentPostfix.getColumnName());
			for (int k = 0; k < columnsToRetrieve.size(); k++) {
				ColumnToSynch currColumn = columnsToRetrieve.get(k);
				if ( temp.equals(currColumn) ) {
					ComlexFieldFound = true;
				}
			}
			if (! ComlexFieldFound) {
				currentPostfix.setColumnName(currentPostfix.getColumnName());
				columnsToRetrieve.add(currentPostfix);
			}
		}		
		// remove the primitve types from columnsToRetrieve
		columnsToRetrieve.removeAll(primitiveNameColumns);		

		AppLogger.getLogger().fine("End of BigIdService.convertPrimitve2Complex(). columnsToRetrieve: " + columnsToRetrieve);	
	}


	/**	 
	 * A method that extracts the postfix of the Name primitive columns
	 * 
	 * @param String column
	 * @return void
	 * @throws SecurityException 	 
	 * @throws IOException 
	 *  City, Country, PostalCode, State, Street --> Address
	 *  FirstName, LastName --> Name
	 */
	private ArrayList<ColumnToSynch> extractNamePrefix(ArrayList<ColumnToSynch> nameColumns) {
		ArrayList<ColumnToSynch> primitveColumnsPrefix = new ArrayList<ColumnToSynch>();

		for (int i = 0; i < nameColumns.size(); i++) {
			String currColumn = nameColumns.get(i).getColumnName();;
			String currTableName = nameColumns.get(i).getTableFullyQualifiedName();
			String currSource = nameColumns.get(i).getSource();
			String currPrefix = "";
			if ( nameColumns.get(i).getColumnName().contains("First") ){				
				currPrefix = currColumn.substring(5, currColumn.length() );				
			}
			else if ( nameColumns.get(i).getColumnName().contains("Last") ) {
				currPrefix = currColumn.substring(4, currColumn.length() );
			}			
			else {

				// TODO raise an exception

			}

			ColumnToSynch currColumnToSynch = new ColumnToSynch(currSource, currTableName );
			currColumnToSynch.setColumnName(currPrefix);
			primitveColumnsPrefix.add(currColumnToSynch);
		}

		return primitveColumnsPrefix;
	}

	/**	 
	 * A method that extracts the postfix of the Address primitive columns
	 * 
	 * @param String column
	 * @return void
	 * @throws SecurityException 	 
	 * @throws IOException 
	 *  City, Country, PostalCode, State, Street --> Address
	 *  FirstName, LastName --> Name
	 */
	private ArrayList<ColumnToSynch> extractAddressPostfix(ArrayList<ColumnToSynch> addressColumns) {

		ArrayList<ColumnToSynch> primitveColumnsPostfix = new ArrayList<ColumnToSynch>();

		for (int i = 0; i < addressColumns.size(); i++) {
			String currColumn = addressColumns.get(i).getColumnName();;
			String currTableName = addressColumns.get(i).getTableFullyQualifiedName();
			String currSource = addressColumns.get(i).getSource();
			String currPostfix = "";
			if ( addressColumns.get(i).getColumnName().contains("City") ){				
				currPostfix = currColumn.substring(0, currColumn.length() - 4 );				
			}
			else if (addressColumns.get(i).getColumnName().contains("Country") ) {
				currPostfix = currColumn.substring(0, currColumn.length() - 7 );
			}
			else if (addressColumns.get(i).getColumnName().contains("PostalCode") ) {
				currPostfix = currColumn.substring(0, currColumn.length() - 10 );
			}
			else if (addressColumns.get(i).getColumnName().contains("State") ) {
				currPostfix = currColumn.substring(0, currColumn.length() - 5 );
			}
			else if ( addressColumns.get(i).getColumnName().contains("Street") ) {
				currPostfix = currColumn.substring(0, currColumn.length() - 6 );
			}
			else {

				// TODO raise an exception

			}

			ColumnToSynch currColumnToSynch = new ColumnToSynch(currSource, currTableName );
			currColumnToSynch.setColumnName(currPostfix);
			primitveColumnsPostfix.add(currColumnToSynch);
		}

		return primitveColumnsPostfix;
	}

	/**	 
	 * A method that converts a Salesforce complexField into its subfields becasue they are the fields that 
	 * BigId refers to
	 * 
	 * @param String column
	 * @return void
	 * @throws SecurityException 	 
	 * @throws IOException 
	 *  City, Country, PostalCode, State, Street --> Address
	 *  FirstName, LastName --> Name
	 */
	private void convertComplexField2Primitive(ArrayList<CategoryColumnContainer> complianceGroupToUpdate) throws SecurityException, IOException {
		AppLogger.getLogger().fine("Beginning of BigIdService.convertComplexField2Primitive()");

		ArrayList<CategoryColumnContainer> primitiveFields = new ArrayList<CategoryColumnContainer>();
		ArrayList<CategoryColumnContainer> complexFieldsToDelete = new ArrayList<CategoryColumnContainer>();

		for (int i = 0; i < complianceGroupToUpdate.size(); i++) {

			// Each CategoryColumnContainer in the list has only 1 column in it.
			CategoryColumnContainer currContainer = complianceGroupToUpdate.get(i);
			String columnName = currContainer.getColumnNames().get(0);

			if ( columnName.contains("Address")){				
				String primitiveName = columnName.substring(0, columnName.length() - 7);
				primitiveFields.add(new CategoryColumnContainer(primitiveName + "City", currContainer.getTableName() , new ArrayList<String>(currContainer.getCategories())));
				primitiveFields.add(new CategoryColumnContainer(primitiveName + "Country", currContainer.getTableName() , new ArrayList<String>(currContainer.getCategories())));
				primitiveFields.add(new CategoryColumnContainer(primitiveName + "PostalCode", currContainer.getTableName() , new ArrayList<String>(currContainer.getCategories())));
				primitiveFields.add(new CategoryColumnContainer(primitiveName + "State", currContainer.getTableName() , new ArrayList<String>(currContainer.getCategories())));
				primitiveFields.add(new CategoryColumnContainer(primitiveName + "Street", currContainer.getTableName() , new ArrayList<String>(currContainer.getCategories())));
				complexFieldsToDelete.add(currContainer);

			}
			if (columnName.contains("Name") ) {				
				primitiveFields.add(new CategoryColumnContainer("First" + columnName, currContainer.getTableName() , new ArrayList<String>(currContainer.getCategories())));
				primitiveFields.add(new CategoryColumnContainer("Last" + columnName, currContainer.getTableName() , new ArrayList<String>(currContainer.getCategories())));
				
				// Name complex field is displayed in BigId's correlation page and Salefore with both its subfields firstName and lastName.
				// Therefore, I am not deleting this complex field so it will display the category in the correlation page.
				
				//complexFieldsToDelete.add(currContainer);
			}			
		}

		// Delete the complex fields from complianceGroupToUpdate list 
		complianceGroupToUpdate.removeAll(complexFieldsToDelete);

		// Add the primitive fields to complianceGroupToUpdate list ONLY IF they do not exist
		for (int i = 0; i < primitiveFields.size(); i++) {
			CategoryColumnContainer currPrimitiveContainer = primitiveFields.get(i);
			boolean contains = false;
			for (int j = 0; j < complianceGroupToUpdate.size(); j++) {
				CategoryColumnContainer currComplexContainer = complianceGroupToUpdate.get(j);				
				if (currPrimitiveContainer.equals(currComplexContainer)) {
					contains = true;					
				}				
			}
			if (! contains) {
				complianceGroupToUpdate.add(currPrimitiveContainer);
			}
		}

		AppLogger.getLogger().fine("End of BigIdService.convertComplexField2Primitive()");

	}




	/**	 
	 * A method that updates the correlation page with new complianceGroup values retrieved from Salesforce
	 * 
	 * @param ArrayList<CategoryColumnContainer> complianceGroupToUpdate
	 * @return void
	 * 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * @throws ResponseNotOKException 
	 * @throws ParseException 
	 */
	public void updateCorrelationSetWithComplianceGroup(ArrayList<CategoryColumnContainer> complianceGroupToUpdate) throws SecurityException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ParseException, ResponseNotOKException {
		AppLogger.getLogger().info("Beginning of BigIdService.updateCorrelationSetWithComplianceGroup()");

		// Deal with Salesforce complex fields: Address and Name
		convertComplexField2Primitive(complianceGroupToUpdate);

		// Loop through the array and execute each complianceGroup individually only if there are complianceGroupValues for the current column
		for (int i = 0; i < complianceGroupToUpdate.size(); i++) {

			// first check if there are any complianceGroupValues to update
			ArrayList<String> complianceGroupValues = complianceGroupToUpdate.get(i).getCategories();

			if (! complianceGroupValues.isEmpty()) {

				// Remove from complianceGroupValues list all the categories that already exist
				// There is only 1 column in a CategoryColumnContainer.columnNames list so just get the first and only one.
				ArrayList<JSONObject> previousCategories = getPreviousCategories(complianceGroupToUpdate.get(i).getColumnNames().get(0));
				for(int k = 0; k < previousCategories.size(); k++) {
					String currC = previousCategories.get(k).getString("display_name");
					complianceGroupValues.remove(currC);
				}				

				// Iterate through the complianceGroupValues list that is holding new complianceGroup values only
				for (int j = 0; j < complianceGroupValues.size(); j++) {

					// There is only 1 column in a CategoryColumnContainer so just get the first and only one.
					String column = complianceGroupToUpdate.get(i).getColumnNames().get(0);						

					// Get the category, the category's unique_name (glossay_id), and previous categories
					String newCategory = complianceGroupValues.get(j);
					String new_glossary_id = getGlossary_id(newCategory);
					//previousCategories = getPreviousCategories(column);

					String uri = URL + "/api/v1/attributes";

					HttpPost httpPostRequest = new HttpPost(uri);
					httpPostRequest.setHeader("Authorization", TOKEN);
					httpPostRequest.setHeader("Accept", "application/json");
					httpPostRequest.setHeader("Content-type", "application/json");					

					AppLogger.getLogger().fine("In updateCorrelationSetWithComplianceGroup(), postNewCategories. currCategory = " + newCategory);						

					JSONObject jsonPostObject = new JSONObject();
					jsonPostObject.put("original_name", column);
					jsonPostObject.putOpt("glossary_id", null);
					jsonPostObject.put("friendly_name", column);
					jsonPostObject.put("isShow", true);
					jsonPostObject.put("type", "idsor_attributes");
					jsonPostObject.put("showImage", "true");
					jsonPostObject.put("attributeSelected", "true");
					JSONObject newCat = new JSONObject();
					newCat.put("display_name", newCategory);
					newCat.put("unique_name", new_glossary_id);	
					previousCategories.add(newCat);					
					jsonPostObject.put("categories", previousCategories);	
					JSONArray jsonPostArray = new JSONArray().put(jsonPostObject);

					AppLogger.getLogger().fine("In updateCorrelationSetWithComplianceGroup(), JsonObject when posting a new complianceGroup " + jsonPostObject.toString());

					StringEntity params = new StringEntity(jsonPostArray.toString());
					httpPostRequest.setEntity(params);

					// Set CONNECTION_TIMEOUT secondes timeout for the http call
					final RequestConfig timeoutParams = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(CONNECTION_TIMEOUT).build();
					httpPostRequest.setConfig(timeoutParams);			

					// create a new client for each iteration
					createClient();			
					HttpResponse response = client.execute(httpPostRequest);
					AppLogger.getLogger().info("In updateCorrelationSetWithComplianceGroup(), updateCorrelationSetWithComplianceGroup. After execute(). field: " + column + ", value: " + newCategory);

					proccessHttpResponse(response, uri);
				}
			}
		}	
	}


	/**	 
	 * A helper method to get the current categories that are assigned to a specific column
	 * 
	 * @param String unique_name, String currentComplianceGroup
	 * @return void
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * @throws ParseException 
	 * @throws ResponseNotOKException 	 
	 */
	private ArrayList<JSONObject> getPreviousCategories(String column) throws SecurityException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, ParseException, ResponseNotOKException {
		AppLogger.getLogger().fine("Beginning of getPreviousCategories()");

		ArrayList<JSONObject> previousCategories = new ArrayList<JSONObject>();

		JSONArray idsor_attributes = getCurrentAttributes();

		for (int i = 0; i < idsor_attributes.length(); i++) {
			String friendly_name = idsor_attributes.getJSONObject(i).getString("friendly_name");
			if (friendly_name.equals(column)) {

				// There could be attributes that do not have a category or categories
				if (! idsor_attributes.getJSONObject(i).isNull("categories")) {
					JSONArray categories = idsor_attributes.getJSONObject(i).getJSONArray("categories");					
					for (int k = 0; k < categories.length(); k++) {									
						// Only if there is a display_name than copy them to categoriesString						
						if (! categories.getJSONObject(k).isNull("display_name") ) {
							JSONObject currCategory = new JSONObject();
							currCategory.put("_id", categories.getJSONObject(k).getString("unique_name"));
							currCategory.put("color", categories.getJSONObject(k).getString("color"));
							currCategory.put("display_name", categories.getJSONObject(k).getString("display_name"));
							currCategory.put("glossary_id", categories.getJSONObject(k).getString("unique_name"));
							currCategory.put("unique_name", categories.getJSONObject(k).getString("unique_name"));
							//currCategory.put("description", categories.getJSONObject(k).getString("description"));
							previousCategories.add(currCategory);
						}						
					}
				}
			}

		}
		return previousCategories;
	}			


	/**	 
	 * A helper method to get the unique_name of a specific category
	 * 
	 * @param String unique_name, String currentComplianceGroup
	 * @return void
	 * @throws ResponseNotOKException 
	 * @throws IOException 
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws ClientProtocolException 
	 * @throws KeyManagementException 
	 * @throws SecurityException 	 
	 */
	private String getGlossary_id(String category) throws KeyManagementException, ClientProtocolException, NoSuchAlgorithmException, KeyStoreException, IOException, ResponseNotOKException {

		AppLogger.getLogger().fine("Beginning of getGlossary_id()");
		String uri = URL + API + VERSION + DATA_CATEGORIES;

		HttpGet getRequest = new HttpGet(uri);
		getRequest.setHeader("Authorization", TOKEN);
		getRequest.setHeader("Accept", "application/json");
		getRequest.setHeader("Content-type", "application/json");

		// Set CONNECTION_TIMEOUT secondes timeout for the http call
		final RequestConfig timeoutParams = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(CONNECTION_TIMEOUT).build();
		getRequest.setConfig(timeoutParams);

		createClient();
		HttpResponse response = client.execute(getRequest);
		proccessHttpResponse(response, uri);

		String result = EntityUtils.toString(response.getEntity());

		AppLogger.getLogger().fine("In getGlossary_id(), result string for uri: " + uri + ":" + result);

		// Set the results into a JSONArray. An array is used because there may be more than one category 
		JSONArray categories  = new JSONArray(result);

		for (int i = 0; i < categories.length(); i++) {
			JSONObject currentJson = categories.getJSONObject(i);			
			if (category.equals(currentJson.getString("name"))) {
				//JSONArray jArray = currentJson.getJSONArray("dc");
				String uniqu_name = currentJson.getString("glossary_id");
				//String uniqu_name = ((JSONObject) jArray.get(0)).getString("_id");
				return uniqu_name;
			}

		}
		return null;
	}


}
