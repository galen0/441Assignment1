package edu.umich.icedDynamite.WeWrite;

public class TextAction implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
	public int location;
	public String text;
	public boolean backspace;
	public boolean broadcast;
	
	public TextAction() {
		backspace = false;
		broadcast = true;
		text = null;
		location = 0;
	}
}
