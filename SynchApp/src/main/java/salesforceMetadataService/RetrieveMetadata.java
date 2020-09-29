package salesforceMetadataService;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.PackageTypeMembers;
import com.sforce.soap.metadata.RetrieveMessage;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.RetrieveResult;
import com.sforce.soap.metadata.RetrieveStatus;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import appController.FileManipulationService;
import appController.LoggerSingelton;
import bigIdService.ColumnToSynch;


/**
 * @ Author: Yoram Melnik
 * Description: A class that handles the retrieval of metadata form Salesforce
 * 
 */
public class RetrieveMetadata {

	// Binding for the metadata WSDL used for making metadata API calls
	private MetadataConnection metadataConnection;

	static BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));

	// one second in milliseconds
	private static final long ONE_SECOND = 1000;
	// maximum number of attempts to retrieve the results
	private static final int MAX_NUM_POLL_REQUESTS = 50; 

	// manifest file that controls which components get retrieved
	private static final String MANIFEST_FILE = "D:\\BigId\\eclipse-workspace\\BigId2SalesforceApp\\package.xml"; 

	private static final Double API_VERSION = 47.0; 
	private static final String API_VERSION_STRING = "47.0";
	private ArrayList<RetrieveMessage> retrieveWarnings = new ArrayList<RetrieveMessage>();
	
	public RetrieveMetadata(MetadataConnection metadataConnection) 
			throws ConnectionException {
		this.metadataConnection = metadataConnection;
	}


	/**
	 * Set the zip file to be sent to Salesforce to retrieve the metadata fields.
	 */
	public File retrieveZip(ArrayList<ColumnToSynch> bigIdColumnsToSynch) throws RemoteException, Exception
	{
		LoggerSingelton.getInstance().getLogger().info("Beginning of retrieveZipFile()");

		RetrieveRequest retrieveRequest = new RetrieveRequest();
		// The version in package.xml overrides the version in RetrieveRequest
		retrieveRequest.setApiVersion(API_VERSION);
		//setUnpackaged(retrieveRequest);
		setUnpackaged(retrieveRequest, bigIdColumnsToSynch);

		// Start the retrieve operation
		AsyncResult asyncResult = metadataConnection.retrieve(retrieveRequest);
		String asyncResultId = asyncResult.getId();

		// Wait for the retrieve to complete
		int poll = 0;
		long waitTimeMilliSecs = ONE_SECOND;
		RetrieveResult result = null;
		do {
			Thread.sleep(waitTimeMilliSecs);
			// Double the wait time for the next iteration
			waitTimeMilliSecs *= 2;
			if (poll++ > MAX_NUM_POLL_REQUESTS) {
				throw new Exception("Request timed out.  If this is a large set " +
						"of metadata components, check that the time allowed " +
						"by MAX_NUM_POLL_REQUESTS is sufficient.");
			}
			result = metadataConnection.checkRetrieveStatus(
					asyncResultId, true);
			LoggerSingelton.getInstance().getLogger().info("Retrieve Status: " + result.getStatus());
		} while (!result.isDone());

		if (result.getStatus() == RetrieveStatus.Failed) {
			throw new Exception(result.getErrorStatusCode() + " msg: " +
					result.getErrorMessage());
		} else if (result.getStatus() == RetrieveStatus.Succeeded) {      
			// Print out any warning messages
			StringBuilder buf = new StringBuilder();
			if (result.getMessages() != null) {
				for (RetrieveMessage rm : result.getMessages()) {
					buf.append(rm.getFileName() + " - " + rm.getProblem());
					// save all the warnings to use later on when updating the zip file
					retrieveWarnings.add(rm);
				}
			}
			if (buf.length() > 0) {
				LoggerSingelton.getInstance().getLogger().info("Retrieve warnings:\n" + buf);
				
			}

			// Write the zip to the file system			
			LoggerSingelton.getInstance().getLogger().info("Writing results to zip file");
			ByteArrayInputStream bais = new ByteArrayInputStream(result.getZipFile());				
			
			String path = FileManipulationService.getResourceDirectory();
			Path resultFilePath = Paths.get(path, SalesforceMetadataService.getRetrieveResultFile());			
			
			FileOutputStream os = new FileOutputStream(resultFilePath.toFile());
			try {
				ReadableByteChannel src = Channels.newChannel(bais);
				FileChannel dest = os.getChannel();
				copy(src, dest);
				LoggerSingelton.getInstance().getLogger().info("in retrieveZip(). Results written to " + resultFilePath.toFile().getAbsolutePath());
			} finally {
				os.close();
			}

			return resultFilePath.toFile();
		}
		return null;

	}

	/**
	 * Set the data to be sent to Salesforce from bigIdColumnsToSynch list.
	 */
	private void setUnpackaged(RetrieveRequest request, ArrayList<ColumnToSynch> bigIdColumnsToSynch) throws SecurityException, IOException {
		LoggerSingelton.getInstance().getLogger().info("Beginning of setUnpackaged()");		

		List<PackageTypeMembers> pd = new ArrayList<PackageTypeMembers>();

		for (Iterator iterator = bigIdColumnsToSynch.iterator(); iterator.hasNext();) {

			ColumnToSynch columnToSynch = (ColumnToSynch) iterator.next();	

			PackageTypeMembers pdi = new PackageTypeMembers();
			pdi.setName("CustomField");
			String[] field = new String[1];
			// Make sure tableName and columnName are Capitilsed
			String tableName = columnToSynch.getTableFullyQualifiedName();
			String columnName = columnToSynch.getColumnName();
			String capitaliseTableName = tableName.substring(0, 1).toUpperCase() + tableName.substring(1);
			String capitalisecolumnName = columnName.substring(0, 1).toUpperCase() + columnName.substring(1);
			field[0] = capitaliseTableName + "." + capitalisecolumnName;
			//field[0] = columnToSynch.getTableFullyQualifiedName() + "." + columnToSynch.getColumnName();			
			pdi.setMembers(field);
			pd.add(pdi);

		}    	
		com.sforce.soap.metadata.Package r = new com.sforce.soap.metadata.Package();
		r.setTypes(pd.toArray(new PackageTypeMembers[pd.size()]));		
		r.setVersion(API_VERSION_STRING);

		request.setUnpackaged(r);		
	}
	
	/**
	 * Helper method to copy from a readable channel to a writable channel,
	 * using an in-memory buffer.
	 */
	private void copy(ReadableByteChannel src, WritableByteChannel dest)
			throws IOException
	{
		LoggerSingelton.getInstance().getLogger().info("Beginning of copy()");
		// Use an in-memory byte buffer
		ByteBuffer buffer = ByteBuffer.allocate(8092);
		while (src.read(buffer) != -1) {
			buffer.flip();
			while(buffer.hasRemaining()) {
				dest.write(buffer);
			}
			buffer.clear();
		}
	}    


}