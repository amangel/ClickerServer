import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//import java.sql.*;

import javax.swing.*;

public class ClickerServer {
    
    private static PushServer pushServer;
    private static PullServer pullServer;
    
    public static void main(String[] args) {
        /*
		JFrame clickerFrame = new JFrame("Clicker Server");
		JPanel controlPanel = new JPanel(new FlowLayout());
		JButton closeButton = new JButton("Shut Down");
		//closeButton.addActionListener(new ActionListener() {
		//	public void actionPerformed (ActionEvent e) {
		//		shutDown();
		//	}
		//});
	    controlPanel.add(closeButton);
	    clickerFrame.add(controlPanel);
         */
        pushServer = new PushServer(4321, 7700, 7171);
        //pullServer = new PullServer(6543, 8800);
        //clickerFrame.setVisible(true);
    }
    
    //public static void shutDown() {
    //	pullServer.shutDown();
    //	System.exit(1);
    //}
}
