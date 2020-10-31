package SpringApp.Services;

public class ActionResult {
	private boolean sucess;
	private String text;
	
	public ActionResult(boolean s, String t) {
		setSucess(s);
		setText(t);
	}

	public boolean isSucess() {
		return sucess;
	}

	public void setSucess(boolean sucess) {
		this.sucess = sucess;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
