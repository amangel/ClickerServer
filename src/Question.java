
public class Question {
	
	private String ID;
	private String flags;
	private String widgets;
	private String followUp;
	private String color;
	private String originalString;
	
	public Question(String questionString) {
		this.originalString = questionString;
		String[] parts = questionString.split("`/;");
		ID = parts[0];
		if (parts.length > 1) {
			flags = parts[1];
			widgets = parts[2];
			try {
				color = parts[3];
			} catch (ArrayIndexOutOfBoundsException e) {
				color = "";
			}
		} else {
			flags = "";
			widgets = "";
			color = "";
		}
		followUp = "Close";
	}
	
	public String getID() {
		return ID;
	}
	
	public String getFlags() {
		return flags;
	}
	
	public String getWidgets() {
		return widgets;
	}
	
	public String getColor() {
		return color;
	}
	
	public String getFollowUp() {
		return followUp;
	}
	
	public boolean hasFollowUp() {
		return followUp.equals("Close");
	}
	
	public void setFollowUp(String followID) {
		followUp = followID;
	}
	
	public String getQuestionString() {
		return originalString;
	}
	
}
