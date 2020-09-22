package appController;



/*
 * @ Author: Yoram Melnik
 * Description: A container class for loading configuration.xml data
 * 
 */
public class LoginData {
	
	private Boolean UPDATE_SF_TO_REFLECT_BIGID;
	private Boolean USE_SSL_CERTIFICATE;
	private String BigId_url;	
	private String BigId_userName;	
	private String BigId_password;
	private String Salesforce_url;
	private String Salesforce_username;
	private String Salesforce_password;
	private String Salesforce_token;
	private Boolean Synch_categories_to_Salesforce;
	
	public String getBigId_url() {
		return BigId_url;
	}
	public void setBigId_url(String bigId_url_dev) {
		BigId_url = bigId_url_dev;
	}
	
	public String getBigId_userName() {
		return BigId_userName;
	}
	public void setBigId_userName(String bigId_userName_dev) {
		BigId_userName = bigId_userName_dev;
	}
	
	public String getBigId_password() {
		return BigId_password;
	}
	public void setBigId_password(String bigId_password) {
		BigId_password = bigId_password;
	}
	public String getSalesforce_url() {
		return Salesforce_url;
	}
	public void setSalesforce_url(String salesforce_url) {
		Salesforce_url = salesforce_url;
	}
	
	public String getSalesforce_username() {
		return Salesforce_username;
	}
	
	public void setSalesforce_username(String salesforce_username) {
		Salesforce_username = salesforce_username;
	}
	
	public String getSalesforce_password() {
		return Salesforce_password;
	}
	
	public void setSalesforce_password(String salesforce_password) {
		Salesforce_password = salesforce_password;
	}
	
	public String getSalesforce_token() {
		return Salesforce_token;
	}
	
	public void setSalesforce_token(String salesforce_token) {
		Salesforce_token = salesforce_token;
	}
	/**
	 * @return the synch_categories_to_Salesforce
	 */
	public Boolean getSynch_categories_to_Salesforce() {
		return Synch_categories_to_Salesforce;
	}
	/**
	 * @param synch_categories_to_Salesforce the synch_categories_to_Salesforce to set
	 */
	public void setSynch_categories_to_Salesforce(Boolean synch_categories_to_Salesforce) {
		Synch_categories_to_Salesforce = synch_categories_to_Salesforce;
	}
	/**
	 * @return the uSE_SSL_CERTIFICATE
	 */
	public Boolean getUSE_SSL_CERTIFICATE() {
		return USE_SSL_CERTIFICATE;
	}
	/**
	 * @param uSE_SSL_CERTIFICATE the uSE_SSL_CERTIFICATE to set
	 */
	public void setUSE_SSL_CERTIFICATE(Boolean uSE_SSL_CERTIFICATE) {
		USE_SSL_CERTIFICATE = uSE_SSL_CERTIFICATE;
	}
	/**
	 * @return the uPDATE_SF_TO_REFLECT_BIGID
	 */
	public Boolean getUPDATE_SF_TO_REFLECT_BIGID() {
		return UPDATE_SF_TO_REFLECT_BIGID;
	}
	/**
	 * @param uPDATE_SF_TO_REFLECT_BIGID the uPDATE_SF_TO_REFLECT_BIGID to set
	 */
	public void setUPDATE_SF_TO_REFLECT_BIGID(Boolean uPDATE_SF_TO_REFLECT_BIGID) {
		UPDATE_SF_TO_REFLECT_BIGID = uPDATE_SF_TO_REFLECT_BIGID;
	}	
}
