package bigIdService;

import java.util.ArrayList;

public class CategoryColumnContainer {	

	private String tableName = null;
	private ArrayList<String> categories = new ArrayList<String>();
	private ArrayList<String> columnNames = new ArrayList<String>();

	public String toString() {
		return new String( tableName + ", " + columnNames.toString() + " ," + categories.toString() );
	}

	public boolean equals(CategoryColumnContainer otherCC) {
		if (this.getTableName().equals(otherCC.getTableName()) && 
				this.getCategories().equals(otherCC.getCategories()) &&
				this.getColumnNames().equals(otherCC.getColumnNames()) ) {
			return true;
		}
		return false;
	}
	
	public CategoryColumnContainer() {
		
	}
	
	public CategoryColumnContainer(String columnName , String tableName , ArrayList<String> categories) {
		
		this.tableName = tableName;
		columnNames = new ArrayList<String>();
		columnNames.add(columnName);
		this.categories = categories;
		
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
	public void addColumnName(String name) {
		this.columnNames.add(name);
	}
	/**
	 * @return the columnNames
	 */
	public ArrayList<String> getColumnNames() {
		return columnNames;
	}
	/**
	 * @param columnNames the columnNames to set
	 */
	public void setColumnNames(ArrayList<String> columnNames) {
		this.columnNames = columnNames;
	}

	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}		

}

