package appController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import SpringApp.Controllers.AppLogger;
import bigIdService.CategoryColumnContainer;
import bigIdService.ColumnToSynch;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

/*
 * @ Author: Yoram Melnik
 * Description: A service class for working with xml and zip files.
 * 
 */

public class FileManipulationService {

	private final String OVERWRITE_SF_CATEGORIES_TO_REFLECT_BIGID = "OVERWRITE_SF_CATEGORIES_TO_REFLECT_BIGID";
	private final String BYPASS_SSL_CERTIFICATE = "BYPASS_SSL_CERTIFICATE";
	private final String BigId_url = "BigId_url";	
	private final String BigId_userName = "BigId_userName";	
	private final String BigId_password = "BigId_password";
	private final String Salesforce_url = "Salesforce_url";
	private final String Salesforce_username = "Salesforce_username";
	private final String Salesforce_password = "Salesforce_password";
	private final String Salesforce_token = "Salesforce_token";
	private final String Synch_categories_to_Salesforce = "Synch_categories_to_Salesforce";


	/**
	 * Description: Read configuration.xml
	 * @param 
	 * @return LoginData object with configuration.xml data
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws xception 
	 * 
	 */

	public LoginData readConfig(InputStream in2) throws XMLStreamException, SecurityException, IOException {
		AppLogger.getLogger().info("Beginning of readConfig() {}");

		LoginData item = new LoginData();

		// First, create a new XMLInputFactory
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		// Setup a new eventReader
		//InputStream in = new FileInputStream(in2);
		XMLEventReader eventReader = inputFactory.createXMLEventReader(in2);			

		// read the XML document
		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();

			if (event.isStartElement()) {
				StartElement startElement = event.asStartElement();
				// If we have an item element, we create a new item
				String elementName = startElement.getName().getLocalPart();

				switch (elementName) {
				case OVERWRITE_SF_CATEGORIES_TO_REFLECT_BIGID:
					event = eventReader.nextEvent();						
					item.setOVERWRITE_SF_CATEGORIES_TO_REFLECT_BIGID(Boolean.valueOf(event.asCharacters().toString()));
					break;
				case BYPASS_SSL_CERTIFICATE:
					event = eventReader.nextEvent();						
					item.setBYPASS_SSL_CERTIFICATE(Boolean.valueOf(event.asCharacters().toString()));
					break;
				case BigId_url:
					event = eventReader.nextEvent();						
					item.setBigId_url(event.asCharacters().toString());
					break;						
				case BigId_userName:
					event = eventReader.nextEvent();						
					item.setBigId_userName(event.asCharacters().toString());
					break;	
				case BigId_password:
					event = eventReader.nextEvent();						
					item.setBigId_password(event.asCharacters().toString());
					break;
				case Salesforce_url:
					event = eventReader.nextEvent();						
					item.setSalesforce_url(event.asCharacters().toString());
					break;
				case Salesforce_username:
					event = eventReader.nextEvent();						
					item.setSalesforce_username(event.asCharacters().toString());
					break;
				case Salesforce_password:
					event = eventReader.nextEvent();						
					item.setSalesforce_password(event.asCharacters().toString());
					break;
				case Salesforce_token:
					event = eventReader.nextEvent();						
					item.setSalesforce_token(event.asCharacters().toString());
					break;

				case Synch_categories_to_Salesforce:
					event = eventReader.nextEvent();						
					item.setSynch_categories_to_Salesforce(Boolean.valueOf(event.asCharacters().toString()));
					break;
				}

			}
		}
		return item;
	}

	/**
	 * Description: Extract zip file retrieved from Salesfgorce
	 * @param 
	 * @return outputDirectory String 
	 * @throws Exception 
	 *
	 */
	public static String extractZipFile(String zipFilePath, String outputDirectory) throws XMLStreamException, IOException {
		AppLogger.getLogger().info("Beginning of extractZipFile()");		

		byte[] buffer = new byte[1024];

		/** create output directory is not exists */
		File folder = new File(outputDirectory);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
		ZipEntry ze = zis.getNextEntry();

		while (ze != null) {

			String fileName = ze.getName();
			File newFile = new File(outputDirectory + File.separator + fileName);

			/** create all non exists parent folders */
			newFile.getParentFile().mkdirs();

			FileOutputStream fos = new FileOutputStream(newFile);

			int len;
			while ((len = zis.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}

			fos.close();

			/** get the next zip file entry */
			ze = zis.getNextEntry();
		}

		zis.closeEntry();
		zis.close();		

		AppLogger.getLogger().fine("In extractZipFile(). outputDirectory: " + outputDirectory);
		return outputDirectory;
	}



	/**
	 * @param salesforce_token the salesforce_token to set
	 * @return the salesforce_token
	 * @throws Exception 
	 *  
	 */
	public static void zip(String zipFileString, String directoryToZip) throws IOException, ZipException {
		AppLogger.getLogger().info("Beginning of zip().");

		ArrayList<File> fileList = new ArrayList<File>();
		getAllFiles(new File(directoryToZip), fileList);			

		ZipParameters parameters = new ZipParameters();
		parameters.setCompressionMethod(CompressionMethod.DEFLATE);   //(Zip4jConstants.COMP_DEFLATE);
		parameters.setEncryptionMethod(EncryptionMethod.AES); //(Zip4jConstants.ENC_METHOD_AES);
		parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);//(Zip4jConstants.AES_STRENGTH_256);
		parameters.setCompressionLevel(CompressionLevel.NORMAL);//(Zip4jConstants.DEFLATE_LEVEL_NORMAL);	
		parameters.setEncryptFiles(false);

		File zipFile = new File(zipFileString);
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
		out.close();

		ZipFile zip = new ZipFile(zipFile);		

		zip.addFolder(new File(directoryToZip), parameters);		

		AppLogger.getLogger().fine("In zip(). New zip file:");

	}

	/**
	 * Traverse a directory and get all files, and add the file into fileList
	 * 
	 * @param file
	 *            file or directory
	 * @return the list with all files to be added
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	public static ArrayList<String> getAllFileList(File file) throws SecurityException, IOException {
		AppLogger.getLogger().fine("Beginning of getAllFileList(). File is " + file);
		ArrayList<String> result = new ArrayList<String>();
		/** add the file only */
		if (file.isFile()) {
			result.add(file.getAbsolutePath());
			/** add all files from the folder */
		} else if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (File f : files) {
				result.addAll(getAllFileList(f));
			}
		}
		return result;
	}


	/**
	 * A method that add all the files and directories from a specific input directory to the input fileList 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws Exception 
	 * 
	 */
	public static void getAllFiles(File dir, List<File> fileList) throws SecurityException, IOException {
		AppLogger.getLogger().fine("Beginning of getAllFiles");
		try {
			File[] files = dir.listFiles();
			for (File file : files) {
				fileList.add(file);
				if (file.isDirectory()) {

					AppLogger.getLogger().fine("directory:" + file.getCanonicalPath());
					getAllFiles(file, fileList);
				} else {

					AppLogger.getLogger().fine("file:" + file.getCanonicalPath());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * A method that updates all the categories that need to be updated to Salesforce
	 * @param OVERWRITE_COMPLIANCE_TAGS
	 * @param baseDirectory
	 * @param newAttributes
	 * @return void
	 */
	@SuppressWarnings("rawtypes")
	public static void addComplianceGroupToZipFile(Boolean OVERWRITE_COMPLIANCE_TAGS, String baseDirectory, ArrayList<ColumnToSynch> bigIdColumnsToSynch) throws XMLStreamException, ParserConfigurationException, SAXException, IOException, TransformerException {
		AppLogger.getLogger().info("Start of addComplianceGroupToZip()");

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

		String packageXmlFile = null;
		ArrayList<String> retrievedFieldNamesList = new ArrayList<String>();

		// get the files that need to add a complianceGroup tag to them
		File directoryToZip = new File(baseDirectory);
		ArrayList<String> filesToManipulate = FileManipulationService.getAllFileList(directoryToZip);

		for (String file : filesToManipulate) {
			AppLogger.getLogger().info("In addComplianceGroupToZipFile() filesToManipulate loop. Current file is: " + file);

			// -Ignore package.xml file
			if (file.contains("package.xml")) {	
				// save the package.xml file for later processing
				packageXmlFile = file;
				continue;
			}

			Document doc = dBuilder.parse(file);
			// Get the root element
			Node costumObject = doc.getFirstChild();

			// loop the costumObject child node
			NodeList list = costumObject.getChildNodes();

			for (int i = 0; i < list.getLength(); i++) {

				Node field = list.item(i);
				if (field.getNodeName().equals("fields")) {
					NodeList currFieldTags = field.getChildNodes();
					String fullName ="";

					// Add comment here 
					ArrayList<String> listForHoldingComplianceValues = new ArrayList<String>();

					// if OVERWRITE_COMPLIANCE_TAGS is true than delete all compliance group tags and later insert the new ones
					for (int j = 0; j < currFieldTags.getLength(); j++) {
						Node innerTag = currFieldTags.item(j);
						if ("fullName".equals(innerTag.getNodeName())) {							
							fullName = innerTag.getTextContent();
						}
						// Save the content of the current complianceGroup value. 
						// If the OVERWRITE_COMPLIANCE_TAGS flag is false than the value will be added to
						// categories list below to be added again to the new complianceGroup tag. The current tag will be removed.
						if (! OVERWRITE_COMPLIANCE_TAGS && "complianceGroup".equals(innerTag.getNodeName())) {
							listForHoldingComplianceValues = addComplianceGroupSeperately(innerTag.getTextContent());
							field.removeChild(innerTag);
						}
						if (OVERWRITE_COMPLIANCE_TAGS && "complianceGroup".equals(innerTag.getNodeName())) {
							field.removeChild(innerTag);
						}							
					}
					// Add the current field name to the list. later on this list will be reference for deleting fields that
					// were not retrieved form package.xml					
					retrievedFieldNamesList.add(extractTableNameFromPath(file) + "." + fullName);

					// add the new complianceGroup tags from newAttributes												
					ArrayList<String> categories = findCategoriesForField( directoryToZip ,file, fullName, bigIdColumnsToSynch);
					// remove all to be sure there are no duplicates and after that add them
					if (categories != null) {
						categories.removeAll(listForHoldingComplianceValues);
						categories.addAll(listForHoldingComplianceValues);
					}
					// empty listForHoldingComplianceValues list for the next 
					listForHoldingComplianceValues = null;

					// add the element only if childs were added - i.e, there are categories to add
					if (categories!= null && ! categories.isEmpty()) {
						Element newComplianceGroup = doc.createElement("complianceGroup");
						for (Iterator iterator = categories.iterator(); iterator.hasNext();) {
							String category = (String) iterator.next();
							if (iterator.hasNext())	{
								newComplianceGroup.appendChild(doc.createTextNode(category +";"));
							} 
							// don't add a ";" after the last category
							else { 
								newComplianceGroup.appendChild(doc.createTextNode(category));
							}
						}
						field.appendChild(newComplianceGroup);
					}
				}

			}

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(file));
			transformer.transform(source, result);			
		}

		// delete fields in package.xml that were not retrieved from Salesforce
		deleteFieldsFromPackageXml(packageXmlFile, retrievedFieldNamesList);
	}

	// A method that extracts from the string the complianceGroup values without the ';' char between them.
	private static ArrayList<String> addComplianceGroupSeperately( String textContent) {
		AppLogger.getLogger().fine("Begining of addComplianceGroupSeperately()");
		ArrayList<String> seperatedComplianceValues = new ArrayList<String>();

		if (! (textContent == null) ) {
			int index = 0;
			for (int i = 0; i < textContent.length(); i++) {				
				if (textContent.charAt(i) == ';') {					
					seperatedComplianceValues.add(textContent.substring(index, i));
					index = i+1;
				}
			}
			// add the last element that has no ';' after it
			seperatedComplianceValues.add(textContent.substring(index, textContent.length()));
		}
		return seperatedComplianceValues;
	}

	/**
	 * @param extract the table name from the path
	 * @return String - fileName
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws Exception 
	 * 
	 */
	private static String extractTableNameFromPath(String path) throws SecurityException, IOException {	
		AppLogger.getLogger().fine("extractTableNameFromPath");

		Path p = Paths.get(path);
		String file = p.getFileName().toString();
		String filename = com.google.common.io.Files.getNameWithoutExtension(file);
		return filename;
	}

	/**
	 * Update the package.xml to reflect the exact fields that are sent in the *.object files.
	 * the package.xml has all the requested fields and therefore the fields that cannot be updated should be deleted from the file.
	 * @return void
	 * @throws ParserConfigurationException, SAXException, IOException, TransformerException 
	 * 
	 */
	private static void deleteFieldsFromPackageXml(String packageXmlFile, ArrayList<String> retrievedFieldNamesList) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		AppLogger.getLogger().info("Beginning of deleteFieldsFromPackageXml");

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

		Document doc = dBuilder.parse(packageXmlFile);
		// Get the root element
		Node costumObject = doc.getFirstChild();

		// loop the costumObject child node
		NodeList list = costumObject.getChildNodes();

		for (int i = 0; i < list.getLength(); i++) {

			Node field = list.item(i);
			if (field.getNodeName().equals("types")) {
				NodeList currFieldTags = field.getChildNodes();
				String member ="";

				for (int j = 0; j < currFieldTags.getLength(); j++) {
					Node innerTag = currFieldTags.item(j);						
					if ("members".equals(innerTag.getNodeName())) {							
						member = innerTag.getTextContent();
						// delete all the "type" tages that are not in the list
						if (!retrievedFieldNamesList.contains(member)) {
							costumObject.removeChild(field);
						}
						break;
					}
				}
			}
		}

		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(packageXmlFile));
		transformer.transform(source, result);


	}

	/**
	 * Get the categories for the param field in the specific table
	 * @return Array list with the categoris for the input field
	 * @throws IOException 
	 * 
	 */
	@SuppressWarnings("rawtypes")
	private static ArrayList<String> findCategoriesForField(File baseDirectoy, String file, String field, ArrayList<ColumnToSynch> bigIdColumnsToSynch) throws IOException{
		AppLogger.getLogger().fine("Beginning of findCategoriesForField");
		for (Iterator iterator = bigIdColumnsToSynch.iterator(); iterator.hasNext();) {
			ColumnToSynch currAttribue = (ColumnToSynch) iterator.next();	
			String TableFullyQualifiedName = currAttribue.getTableFullyQualifiedName();
			String fileName = extractFullPathChars( baseDirectoy, file);
			if (TableFullyQualifiedName.equals(fileName) ){
				if (currAttribue.getColumnName().equals(field)) {
					return currAttribue.getCategories();
				}
			}
		}
		return null;
	}

	/**
	 * A method for extracting the full path from the file
	 * @return String 
	 * @throws IOException 
	 * 
	 */
	private static String extractFullPathChars(File baseDirectoy, String file) throws IOException {
		AppLogger.getLogger().fine("Beginning of extractFullPathChars");
		File f = new File(file);
		File directoryToZip = f.getParentFile();
		// we want the zipEntry's path to be a relative path that is relative
		// to the directory being zipped, so chop off the rest of the path
		String zipFilePath = (f.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,
				f.getCanonicalPath().length()));
		// remove the .object from the end
		String ret = zipFilePath.substring(0, zipFilePath.length()-7);
		return ret;
	}

	/**
	 * A method for getting files from a zip file
	 * @param zipfile
	 * @return String arrayList with fileName 
	 * @throws IOException 
	 * 
	 */
	public static ArrayList<String> getFilesFromZipfile(File retrieveResult) throws IOException {
		File parentDir = new File(retrieveResult.getAbsolutePath()).getParentFile();
		ZipFile zipFile = new ZipFile(retrieveResult.getAbsolutePath());

		zipFile.extractAll(parentDir.getAbsolutePath());
		return FileManipulationService.getAllFileList(new File(parentDir.getAbsoluteFile() + "/unpackaged") );		
	}

	/**
	 * A method for reading data form xml files and returning it as an arrayList
	 * @return String 
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * 
	 */
	public static ArrayList<CategoryColumnContainer> readComplianceGroupfieldAndValues(ArrayList<String> files) throws SAXException, IOException, ParserConfigurationException {

		ArrayList<CategoryColumnContainer> columnsAndComplianceGroups = new ArrayList<CategoryColumnContainer>();

		for (String file : files) {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

			// -Ignore package.xml file
			if (file.contains("package.xml")) {					
				continue;
			}

			Document doc = dBuilder.parse(file);
			// Get the root element
			Node costumObject = doc.getFirstChild();

			// loop the costumObject child node
			NodeList list = costumObject.getChildNodes();

			for (int i = 0; i < list.getLength(); i++) {

				Node field = list.item(i);
				if (field.getNodeName().equals("fields")) {
					NodeList currFieldTags = field.getChildNodes();
					String fullName ="";
					String complianceGroupValue = null;

					for (int j = 0; j < currFieldTags.getLength(); j++) {
						Node innerTag = currFieldTags.item(j);
						if ("fullName".equals(innerTag.getNodeName())) {							
							fullName = innerTag.getTextContent();							
						}
						if ("complianceGroup".equals(innerTag.getNodeName())) {
							complianceGroupValue = innerTag.getTextContent();													
						}
					}
					CategoryColumnContainer currValues = new CategoryColumnContainer();
					currValues.setCategories(addComplianceGroupSeperately(complianceGroupValue));
					currValues.addColumnName(fullName);
					currValues.setTableName(extractTableNameFromPath(file));
					columnsAndComplianceGroups.add(currValues);
				}
			}
		}

		return columnsAndComplianceGroups;
	}

}

