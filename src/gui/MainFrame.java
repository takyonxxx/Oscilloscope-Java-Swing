package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JFrame;

public class MainFrame extends JFrame {
	private Toolbar toolbar;
	private GraphPane oscpanel;

	public MainFrame() {
		super("Oscilloscope");

		setLayout(new BorderLayout());
		setMinimumSize(new Dimension(800, 480));
		setSize(800, 480);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(null);

		setDefaultLookAndFeelDecorated(true);

		toolbar = new Toolbar();
		oscpanel = new GraphPane();

		String xFormat = "%.1f ms";
		Axis xAxis = new Axis(0, 1 * 10, xFormat);
		Axis yAxis = new Axis(-5, 5, 1, "%.1f V");
		oscpanel.setCoordinateSystem(xAxis, yAxis);

		add(toolbar, BorderLayout.PAGE_START);
		add(oscpanel, BorderLayout.CENTER);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent arg0) {
				dispose();
				System.gc();
			}

		});

		toolbar.setToolbarListener(new ToolbarListener() {
			public void startEventOccured() {
				oscpanel.start();
			}

			public void stopEventOccured() {
				oscpanel.stop();
			}

			public void exitEventOccured() {

				oscpanel.stop();

				WindowListener[] listeners = getWindowListeners();

				for (WindowListener listener : listeners) {
					listener.windowClosing(new WindowEvent(MainFrame.this, 0));
				}
			}

			public void sliderChangedEventOccured(int value) {
				oscpanel.setSampleRate(value);
				String xFormat = "%.1f ms";
				Axis xAxis = new Axis(0, 1 * value / 100, xFormat);
				Axis yAxis = new Axis(-5, 5, 1, "%.1f V");
				oscpanel.setCoordinateSystem(xAxis, yAxis);
			}
		});

		// add(oscPanel, BorderLayout.CENTER);
		pack();
		setVisible(true);
	}
}
