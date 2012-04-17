// TODO: Auto-generated Javadoc
/**
 * The Class Question.
 */
public class Question {
    
    
    /** The question ID. */
    private final String questionId;
    
    /** The flags that are set on the question. */
    private String questionFlags;
    
    /** The set of widgets in the question. */
    private String widgets;
    
    /** The follow up question id if it is set for a question. */
    private String followUp;
    
    /** The background color string for the question if it is different than the default. */
    private String backgroundColor;
    
    /** The original string. */
    private final String originalQuestionString;
    
    /**
     * Instantiates a new question.
     * 
     * @param questionString
     *            the question string
     */
    public Question(final String questionString) {
        originalQuestionString = questionString;
        final String[] questionParts = questionString.split(PushServer.SEMI_COLON_SEPARATOR);
        questionId = questionParts[0];
        if (questionParts.length > 1) {
            questionFlags = questionParts[1];
            widgets = questionParts[2];
            try {
                backgroundColor = questionParts[3];
            } catch (final ArrayIndexOutOfBoundsException e) {
                backgroundColor = "";
            }
        } else {
            questionFlags = "";
            widgets = "";
            backgroundColor = "";
        }
        followUp = Constants.CLOSE;
    }
    
    /**
     * Gets the question ID.
     * 
     * @return the ID
     */
    public String getQuestionId() {
        return questionId;
    }
    
    /**
     * Gets the question flags.
     * 
     * @return the flags
     */
    public String getQuestionFlags() {
        return questionFlags;
    }
    
    /**
     * Gets the widgets.
     * 
     * @return the widgets
     */
    public String getWidgets() {
        return widgets;
    }
    
    /**
     * Gets the color.
     * 
     * @return the color
     */
    public String getBackgroundColor() {
        return backgroundColor;
    }
    
    /**
     * Gets the follow up.
     * 
     * @return the follow up
     */
    public String getFollowUp() {
        return followUp;
    }
    
    /**
     * Checks to see if the question has a follow up.
     * 
     * @return true, if successful
     */
    public boolean hasFollowUp() {
        return followUp.equals(Constants.CLOSE);
    }
    
    /**
     * Sets the follow up.
     * 
     * @param followID
     *            the new follow up
     */
    public void setFollowUp(final String followID) {
        followUp = followID;
    }
    
    /**
     * Gets the original question string.
     * 
     * @return the question string
     */
    public String getQuestionString() {
        return originalQuestionString;
    }
    
}
