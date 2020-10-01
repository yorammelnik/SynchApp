package appController;

/*
 * @ Author: Yoram Melnik
 * Description: An Exception class for throwing when a response from BigId is not OK
 *  
 */@SuppressWarnings("serial")
public class ResponseNotOKException extends Exception {
	public ResponseNotOKException(String errorMessage) {
        super(errorMessage);
    }

}