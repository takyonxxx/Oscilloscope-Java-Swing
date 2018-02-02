package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Toolbar extends JToolBar implements ActionListener {
	private JButton startButton;
	private JButton exitButton;
	private JSlider slider;
	private JLabel labelSample;
	private ToolbarListener listener;
	private boolean isStart;

	public Toolbar() {

		setBorder(BorderFactory.createEtchedBorder());
		isStart = false;

		startButton = new JButton();
		startButton.setIcon(Utils.createIcon("/images/start.png"));
		startButton.setToolTipText("Start");

		exitButton = new JButton();
		exitButton.setIcon(Utils.createIcon("/images/exit.png"));
		exitButton.setToolTipText("Refresh");

		startButton.addActionListener(this);
		exitButton.addActionListener(this);

		labelSample = new JLabel();

		slider = new JSlider(JSlider.HORIZONTAL, 100, 9600, 1024);
		String sliderLabel = String.format("Sample Rate : %.2f kHz", (float) (slider.getValue() / 1000.0));
		labelSample.setText(sliderLabel);

		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (listener != null) {
					listener.sliderChangedEventOccured(slider.getValue());
					String sliderLabel = String.format("Sample Rate : %.2f kHz", (float) (slider.getValue() / 1000.0));
					labelSample.setText(sliderLabel);
				}
			}
		});

		slider.setMinorTickSpacing(2);
		slider.setMajorTickSpacing(10);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);

		// We'll just use the standard numeric labels for now...
		slider.setLabelTable(slider.createStandardLabels(1000));

		addSeparator();
		add(startButton);
		addSeparator();
		add(exitButton);
		addSeparator();
		add(slider);
		addSeparator();
		add(labelSample);
		addSeparator();
	}

	public void setToolbarListener(ToolbarListener listener) {
		this.listener = listener;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JButton clicked = (JButton) e.getSource();

		if (clicked == startButton) {
			if (listener != null) {
				if (isStart) {
					listener.stopEventOccured();
					startButton.setIcon(Utils.createIcon("/images/start.png"));
					startButton.setToolTipText("Start");
					isStart = false;
				} else {
					listener.startEventOccured();
					startButton.setIcon(Utils.createIcon("/images/stop.png"));
					startButton.setToolTipText("Stop");
					isStart = true;
				}

			}
		} else if (clicked == exitButton) {
			if (listener != null) {
				listener.exitEventOccured();
			}
		}

	}
}
