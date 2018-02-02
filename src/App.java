
import java.awt.EventQueue;

import javax.swing.SwingUtilities;

import gui.MainFrame;

public class App {

	public static void main(String[] args) {
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new MainFrame();
			}
		});	
	}
	
	/*EventQueue.invokeLater(new Runnable() {
		public void run() {
			try {	

			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	});*/

}
