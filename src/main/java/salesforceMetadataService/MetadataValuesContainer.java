package salesforceMetadataService;
import java.util.ArrayList;
import java.util.Iterator;

import com.sforce.soap.metadata.StandardValueSet;

/*
 * @ Author: Yoram Melnik
 * Description: A container class to pass metadata type and values
 * 
 */
public class MetadataValuesContainer {
	
	private String metaDataType = new String();
	private ArrayList<String> metaDataValues = new ArrayList<String>();
	private StandardValueSet currentSet;
	
	public String getMetaDataType() {
		return metaDataType;
	}
	public void setMetaDataType(String metaDataType) {
		this.metaDataType = metaDataType;
	}
	public ArrayList<String> getMetaDataValues() {
		return metaDataValues;
	}
	public void setMetaDataValues(ArrayList<String> metaDataValues) {
		this.metaDataValues = metaDataValues;
	}
	
	public void addItemToList(String item) {
		metaDataValues.add(item);
	}
	
	public StandardValueSet getCurrentSet() {
		return currentSet;
	}
	public void setCurrentSet(StandardValueSet currentSet) {
		this.currentSet = currentSet;
	}
	
	@SuppressWarnings("rawtypes")
	public boolean itemExists(String newItem) {
		for (Iterator iterator = metaDataValues.iterator(); iterator.hasNext();) {
			String currItem = (String) iterator.next();
			if (newItem.equals(currItem)) {
				return true;
			}			
		}		
		return false;
	}
	
}
