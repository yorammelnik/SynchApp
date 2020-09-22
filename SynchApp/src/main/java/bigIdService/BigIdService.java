package bigIdService;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.tomcat.jni.SSLContext;
import org.json.JSONArray;
import org.json.JSONObject;

import appController.LoggerSingelton;
import appController.ResponseNotOKException;
import nl.altindag.sslcontext.SSLFactory;

/*
 * @ Author: Yoram Melnik
 * Description: Micro service for handling the connection and rest api calls to BigId
 * 
 */

public class BigIdService {

	private static String URL;
	private static final String API = "/api";
	private final static String VERSION = "/v1";
	private final static String SESSIONS = "/sessions";
	private final static String DATA_CATEGORIES = "/data_categories";
	private static final String ATTRIBUTES = "/attributes";
	private static final String FORMAT_JSON = "/?format=json";

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
		LoggerSingelton.getInstance().getLogger().info("Beginning of BigIdService()");
		// set fields from configuration.xml
		URL = url;
		USER_NAME = userName;
		PASSWORD = password;
		TOKEN = bigIdToken;
		USE_SSL_CERTIFICATE = useSSLCertificate;		
	}

	private HttpClient createClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		// Use sslcontext library to deal with BigId instance in Development without SSL certificate
		// use this only when there isn't a certificate in the BigId environment.
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
	 * A method that initiated the first connection to BigId and stores the seession auth_tokn in TOKEN data member
	 * @param: A list of the categories that need to be added to BigId
	 * @return - A list of the categories in BigId
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	public void postConnect() throws ClientProtocolException, IOException, ResponseNotOKException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {				
		LoggerSingelton.getInstance().getLogger().info("Beginning of postConnect()");


		// Set user name and password 
		JSONObject jsonPostObject = new JSONObject( "{\"password\":" + "\"" + PASSWORD + "\"," + "\"username\":" + "\"" + USER_NAME + "\"}" );
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
		LoggerSingelton.getInstance().getLogger().info("postConnect(). Token received is " + TOKEN);
	}

	/**	 
	 * Get all the categories from BigId
	 * @param: A list of the categories that need to be added to Salesforce
	 * @return - A list of the categories in BigId
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	public ArrayList<String> getCategories() throws ClientProtocolException, IOException, ResponseNotOKException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		LoggerSingelton.getInstance().getLogger().info("Beginning of getCategories() {}");



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

		LoggerSingelton.getInstance().getLogger().info("result string for uri: " + uri + ":" + result);

		// Set the results into a JSONArray. An array is used because there may be more that on category 
		JSONArray categories  = new JSONArray(result);

		// Iterate through the JSONAraay and extract all the categories into a categoryList
		JSONObject currentJson;
		for (int i = 0; i < categories.length(); i++) {
			currentJson = categories.getJSONObject(i);
			categoryList.add(currentJson.getString("name"));
		}
		return categoryList;
	}


	/**	 
	 * Add new categories into BigId
	 * @param: A list of the categories that need to be added to BigId
	 * @return
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	public void postNewCategories(ArrayList<String> newCategories) throws ClientProtocolException, IOException, ResponseNotOKException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {				
		LoggerSingelton.getInstance().getLogger().info("Beginning of postNewCategories()");

		String uri = URL + API + VERSION + DATA_CATEGORIES;
		JSONArray newCategoriesJsonArray = new JSONArray(newCategories);

		// Loop through the array and execute each category individually
		for (int i = 0; i < newCategoriesJsonArray.length(); i++) {



			HttpPost httpPostRequest = new HttpPost(uri);	
			httpPostRequest.setHeader("Authorization", TOKEN);
			httpPostRequest.setHeader("Accept", "application/json");
			httpPostRequest.setHeader("Content-type", "application/json");

			String currCategory = newCategoriesJsonArray.getString(i);
			LoggerSingelton.getInstance().getLogger().info("postNewCategories. currCategory = " + currCategory);

			LocalDateTime today = LocalDateTime.now();
			JSONObject jsonPostObject = new JSONObject( "{\"unique_name\":" + "\"" + currCategory + "\"," + "\"description\":" + "\"Imported from Salesforce on - " + today +  "\"," + "\"display_name\":" + "\"" + currCategory + "\"," + "\"color\":" + "\""  + "\"}" );

			StringEntity params = new StringEntity(jsonPostObject.toString());
			httpPostRequest.setEntity(params);

			// Set CONNECTION_TIMEOUT secondes timeout for the http call
			final RequestConfig timeoutParams = RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).setSocketTimeout(CONNECTION_TIMEOUT).build();
			httpPostRequest.setConfig(timeoutParams);			

			// create a new client for each iteration
			createClient();
			LoggerSingelton.getInstance().getLogger().info("postNewCategories. Before client.execute(httpPostRequest). " + currCategory);
			HttpResponse response = client.execute(httpPostRequest);
			LoggerSingelton.getInstance().getLogger().info("postNewCategories. After client.execute(httpPostRequest). " + currCategory);

			proccessHttpResponse(response, uri);			

			LoggerSingelton.getInstance().getLogger().info("JsonObject when posting a new category " + jsonPostObject.toString());

		}		
	}

	/**	 
	 * @param 
	 * @return
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	public ArrayList<ColumnToSynch> getObjectsToSynch() throws ClientProtocolException, IOException, ResponseNotOKException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		LoggerSingelton.getInstance().getLogger().info("Beginning of getObjectsToSynch");

		// get all the "Objects" from bigId that were retrieved from Salesforce
		ArrayList<JSONObject> salesforceRelatedObjects = getSalesforceObjectsFromDb();

		// A list of all the categories and columns that have a certain attribute from all the Salesforce object in BigId
		ArrayList<CategoryColumnContainer> categoryAndColumnList = getCategoriesAndColumns(salesforceRelatedObjects);

		// The return list containing all the columns to synch to Salesforce
		ArrayList<ColumnToSynch> columnsToSynch = new ArrayList<ColumnToSynch>();		

		for (Iterator iterator = salesforceRelatedObjects.iterator(); iterator.hasNext();) {	



			JSONObject objectToSynch = (JSONObject) iterator.next();
			String currObject = objectToSynch.getString("fullyQualifiedName");
			LoggerSingelton.getInstance().getLogger().info("getObjectsToSynch, Current object is: " + currObject);

			/* TODO - for debug purposes 
			if (  ! currObject.equals("Salesforce.Account")  ) {
				continue;
			}
			 */

			// set a query to retrieve the columns of the current object
			String uri = URL + API + VERSION +"/data-catalog/object-details/columns?object_name=" + currObject;
			LoggerSingelton.getInstance().getLogger().info("IN getObjectsToSynch, uri is " + uri);

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
			LoggerSingelton.getInstance().getLogger().info("IN getObjectsToSynch, currentSFOjbect is: " + currentSFOjbect.toString());

			JSONObject data = currentSFOjbect.getJSONObject("data");
			JSONArray columns = data.getJSONArray("results");
			//JSONArray columns = currentSFOjbect.getJSONArray("data");

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

		LoggerSingelton.getInstance().getLogger().info("IN getObjectsToSynch, Columns to synch" + columnsToSynch.toString());

		dealWithSalesforceComplexFields(columnsToSynch);

		return columnsToSynch;
	}

	/**	 
	 * @param 
	 * @return
	 * @throws IOException 
	 * @throws SecurityException 
	 *  City, Country, PostalCode, State, Street --> Address
	 *  FirstName, LastName --> Name
	 */
	private void dealWithSalesforceComplexFields(ArrayList<ColumnToSynch> columnsToSynch) throws SecurityException, IOException {

		LoggerSingelton.getInstance().getLogger().info("Begining of dealWithSalesforceComplexFields");

		ArrayList<ColumnToSynch> addressColumnsToDealWith = new ArrayList<ColumnToSynch>();
		ArrayList<ColumnToSynch> addressColumnsToRemove = new ArrayList<ColumnToSynch>();

		ArrayList<ColumnToSynch> nameColumnsToDealWith = new ArrayList<ColumnToSynch>();
		ArrayList<ColumnToSynch> nameColumnsToRemove = new ArrayList<ColumnToSynch>();


		// Iterae over the entrie columnsToSynch list and find the First field that to be dealt with
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
			String table = columnToDeal.getTableFullyQualifiedName();

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
			String table1 = nameColumnToDeal.getTableFullyQualifiedName();

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
	 * @param 
	 * @return
	 * @throws IOException 
	 * @throws SecurityException 
	 * 
	 */
	private ArrayList<String> getCategoriesForColumn(String currObject, ArrayList categoryAndColumnList, String column) throws SecurityException, IOException {
		LoggerSingelton.getInstance().getLogger().info("Begining of getCategoriesForColumn");

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
	 * @param 
	 * @return
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	private ArrayList<CategoryColumnContainer> getCategoriesAndColumns(ArrayList<JSONObject> salesforceRelatedObjects) throws ParseException, ResponseNotOKException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		LoggerSingelton.getInstance().getLogger().info("Begining of getCategoriesAndColumns");

		ArrayList<CategoryColumnContainer> categoriesAndColumsFound = new ArrayList<CategoryColumnContainer>();

		for (Iterator iterator = salesforceRelatedObjects.iterator(); iterator.hasNext();) {			
			JSONObject objectToSynch = (JSONObject) iterator.next();

			// set a query to retrieve the attributes of the object			
			String uri = URL + API + VERSION +"/data-catalog/object-details/attributes?object_name=" + objectToSynch.getString("fullyQualifiedName");
			LoggerSingelton.getInstance().getLogger().info("IN getObjectsToSynch, uri is: " + uri);

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
			LoggerSingelton.getInstance().getLogger().info("IN getObjectsToSynch, currentSFOjbect is " + currentSFOjbect.toString());

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
	 * @param 
	 * @return
	 * @throws IOException 
	 * @throws SecurityException 
	 * 
	 */
	private boolean duplicateCategoryColumnContainerExits(ArrayList<CategoryColumnContainer> categoriesAndColumsFound,
			CategoryColumnContainer currCC) throws SecurityException, IOException {
		LoggerSingelton.getInstance().getLogger().info("Begining of duplicateCategoryColumnContainerExits");

		for (Iterator iterator = categoriesAndColumsFound.iterator(); iterator.hasNext();) {
			CategoryColumnContainer categoryColumnContainer = (CategoryColumnContainer) iterator.next();
			if (categoryColumnContainer.equals(currCC)) {
				return true;
			}			
		}
		return false;
	}

	/**	 
	 * @param 
	 * @return
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * 
	 */
	private ArrayList<JSONObject> getSalesforceObjectsFromDb() throws ResponseNotOKException, ParseException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		LoggerSingelton.getInstance().getLogger().fine("Beginning of getSalesforceObjectsFromDb()");

		// TODO - delete requireTotalCount=true		

		String uri =  URL + API + VERSION + "/data-catalog?format=json&&sort=&filter=system=Salesforce";
		//String uri =  URL + API + VERSION + "/data-catalog?format=json&requireTotalCount=true&&sort=&filter=system=Salesforce";		

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
		LoggerSingelton.getInstance().getLogger().info("IN getSalesforceObjectsFromDb, Status code is " + response.getStatusLine().getStatusCode() + " response is " + response.toString());

		proccessHttpResponse(response, uri);

		// Set the results into a JSONArray. An array is used because there may be more that on category
		String result = EntityUtils.toString(response.getEntity());			 
		JSONObject jo = new JSONObject(result);
		JSONArray jArray = jo.getJSONArray("results");

		//Iterate through the JSONAraay and extract all the categories into a categoryList
		// TODO - make sure that I am adding to the list only objects that have PI - personal information
		JSONObject currentJson;
		for (int j = 0; j < jArray.length(); j++) {
			currentJson = jArray.getJSONObject(j);
			Object a = currentJson.get("attribute_original_name");
			// if currentJason has attributes than add it to the ArrayList 
			if (! ((JSONArray) a).isEmpty() ) {
				salesforceObjects.add(currentJson);						
			}
		}
		LoggerSingelton.getInstance().getLogger().info("salesforceObjects to write: " + salesforceObjects.toString() );
		return salesforceObjects;
	}

	/**	 
	 * A method that processes an http response and throws an exception if the response is not OK
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
			LoggerSingelton.getInstance().getLogger().severe("Response code for uri" + uri + "is " + statusCode + " response, reasonPhrase:" + response.getStatusLine().getReasonPhrase());
			throw new ResponseNotOKException("Response code for uri" + uri + "is" +  statusCode);
		}
	}	
}
