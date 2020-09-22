package bigIdService;

import java.util.ArrayList;
import java.util.Iterator;

public class ColumnToSynch {
	private String source;
	private String tableFullyQualifiedName;
	private ArrayList<String> categories = new ArrayList<String>();
	private String columnName = null;
	private ArrayList<String> attributes = new ArrayList<String>();
	
	public ColumnToSynch(String source, String name) {
		this.source = source;
		this.tableFullyQualifiedName = name;		
	}
	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}
	public String toString() {
		return new String(tableFullyQualifiedName + " ," + columnName + " ," + attributes.toString()  +  " ," + categories.toString() );
	}
	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}
	/**
	 * @return the tableFullyQualifiedName
	 */
	public String getTableFullyQualifiedName() {
		return tableFullyQualifiedName;
	}
	/**
	 * @param tableFullyQualifiedName the tableFullyQualifiedName to set
	 */
	public void setTableFullyQualifiedName(String tableFullyQualifiedName) {
		this.tableFullyQualifiedName = tableFullyQualifiedName;
	}
	/**
	 * @return the categories
	 */
	public ArrayList<String> getCategories() {
		return categories;
	}
	/**
	 * @param categories the categories to set
	 */
	public void setCategories(ArrayList<String> categories) {
		this.categories = categories;
	}
	
	public void addCategory(String newCategory) {
		this.categories.add(newCategory);
	}
	
	public void addCategoriesNoDuplicates(ArrayList<String> newCategories) {
		for (Iterator iterator = newCategories.iterator(); iterator.hasNext();) {
			String currCategory = (String) iterator.next();
			if (! this.categories.contains(currCategory)) {
				this.categories.add(currCategory);
			}
			
		}
	}
	
	public void addAttributesNoDuplicates(ArrayList<String> newAttributes) {
		for (Iterator iterator = newAttributes.iterator(); iterator.hasNext();) {
			String currCategory = (String) iterator.next();
			if (! this.attributes.contains(currCategory)) {
				this.attributes.add(currCategory);
			}
			
		}
	}
	
	public void addAttribute(String att) {
		this.attributes.add(att);
	}	
	
	/**
	 * @return the attributes
	 */
	public ArrayList<String> getAttributes() {
		return attributes;
	}
	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(ArrayList<String> attributes) {
		this.attributes = attributes;
	}
	/**
	 * @return the columnName
	 */
	public String getColumnName() {
		return columnName;
	}
	/**
	 * @param columnName the columnName to set
	 */
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	

}
