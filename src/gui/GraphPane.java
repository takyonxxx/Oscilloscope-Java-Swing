package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import gui.Axis.GraphLabel;

public class GraphPane extends JPanel {

	private static final Color YELLOW = new Color(250, 250, 5);
	public static final Color LIGHT_BLUE = new Color(128, 192, 255);
	public static final Color INDIGO = new Color(75, 0, 60);
	public static final Color DARK_BLUE = new Color(25, 25, 50);
	private static final Color DIVISION_COLOR = new Color(0xbcbcbc);
	private static final Color SUB_DIVISION_COLOR = new Color(0xcdcdcd);
	private static final Color TEXT_COLOR = new Color(0x9a9a9a);
	private static final Font FONT = Font.decode(Font.MONOSPACED);
	static boolean _playing = false;

	private static final Stroke DASHED = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
			new float[] { 5 }, 0);

	private static int DEF_BUFFER_SAMPLE_SZ = 1024;

	private static float sampleRate = 96000.0F;
	private static int sampleSizeInBits = 16;
	private static int numChannels = 1;
	private static int frameRate = 2;
	private static boolean bigEndian = false;

	public enum PlayStat {
		PLAYING, STOPPED
	}

	public interface PlayerRef {
		public Object getLock();

		public PlayStat getStat();

		public void playbackEnded();

		public AudioFormat getFormat();

		public void drawDisplay(float[] samples, int svalid);
	}

	private static AudioFormat audioFormat;
	private final Object statLock = new Object();
	public volatile PlayStat playStat = PlayStat.STOPPED;
	private final Path2D.Float[] paths = { new Path2D.Float() };

	private Rectangle graphArea;
	private Axis xAxis;
	private Axis yAxis;

	public GraphPane() {

	}

	public void setCoordinateSystem(Axis xAxis, Axis yAxis) {
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2d = (Graphics2D) g;

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

		int w = getWidth();
		int h = getHeight();

		g2d.setColor(DARK_BLUE);
		g2d.fillRect(0, 0, w, h);

		if (xAxis != null && yAxis != null) {

			xAxis.complete(g2d, FONT);
			yAxis.complete(g2d, FONT);

			graphArea = new Rectangle(16, 16, w - 32, h - 32);
			Insets labelInsets = new Insets(2, 12, 0, 0);
			graphArea.width -= yAxis.getMaxBounds().width + labelInsets.left + labelInsets.right;
			graphArea.height -= xAxis.getMaxBounds().height + labelInsets.top + labelInsets.bottom;

			if (w > 0 && h > 0) {
				paintXAxis(g2d, labelInsets);
				paintYAxis(g2d, labelInsets);
			}

			g2d.setPaint(YELLOW);
			g2d.draw(paths[0]);

			g2d.dispose();
		}

	}

	private void paintXAxis(Graphics2D g, Insets labelInsets) {
		g.translate(graphArea.x, graphArea.y);
		GraphLabel[] xLabels = xAxis.graphLabels();
		for (int i = 0; i < xLabels.length; ++i) {
			GraphLabel xLabel = xLabels[i];
			int xOffset = (int) (i * graphArea.width / xAxis.getFraction());

			Stroke defaultStroke = g.getStroke();

			g.setStroke(i % (xLabels.length / 2) != 0 ? DASHED : defaultStroke);
			g.setColor(i % (xLabels.length - 1) == 0 ? DIVISION_COLOR : SUB_DIVISION_COLOR);
			g.drawLine(xOffset, 0, xOffset, graphArea.height);

			g.setStroke(defaultStroke);
			g.setColor(TEXT_COLOR);
			Rectangle bounds = xLabel.getBounds();
			int dx;
			if (i == 0) {
				dx = 0;
			} else {
				dx = -bounds.width / 2;
			}
			int dy = labelInsets.top;
			g.drawString(xLabel.getLabel(), xOffset + dx, graphArea.height + bounds.height + dy);
		}
		g.drawLine(graphArea.width, 0, graphArea.width, graphArea.height);

		g.translate(-graphArea.x, -graphArea.y);
	}

	private void paintYAxis(Graphics2D g, Insets labelInsets) {
		g.translate(graphArea.x, graphArea.y);
		GraphLabel[] yLabels = yAxis.graphLabels();
		for (int i = 0; i < yLabels.length; ++i) {
			GraphLabel yLabel = yLabels[yLabels.length - i - 1];
			int yOffset = (int) (i * graphArea.height / yAxis.getFraction());

			Stroke defaultStroke = g.getStroke();

			g.setStroke(i % (yLabels.length / 2) != 0 ? DASHED : defaultStroke);
			g.setColor(i % (yLabels.length - 1) == 0 ? DIVISION_COLOR : SUB_DIVISION_COLOR);
			g.drawLine(0, yOffset, graphArea.width, yOffset);

			g.setStroke(defaultStroke);
			g.setColor(TEXT_COLOR);
			Rectangle bounds = yLabel.getBounds();
			int dy = bounds.height / 2;
			int dx = labelInsets.left;
			g.drawString(yLabel.getLabel(), graphArea.width + dx, yOffset + dy);
		}
		g.drawLine(0, graphArea.height, graphArea.width, graphArea.height);
		g.translate(-graphArea.x, -graphArea.y);
	}

	/////////////////////////////////

	public void makePath(float[] samples, int svalid) {

		if (audioFormat == null) {
			return;
		}

		Path2D.Float current = paths[0];

		float avg = 0f;

		float hd2 = (getHeight() - 15) / 2f;

		final int channels = audioFormat.getChannels();
		int fvalid = svalid / channels;
		int i = 0;
		while (i < channels && i < svalid) {
			avg += samples[i++];
		}
		avg /= channels;
		current.reset();
		current.moveTo(0, hd2 - avg * hd2);

		for (int ch, frame = 0; i < svalid; frame++) {

			avg = 0f;
			for (ch = 0; ch < channels; ch++) {
				avg += samples[i++];
			}
			avg /= channels;

			float lx = (float) frame / fvalid * getWidth();
			float ly = (float) hd2 - avg * hd2;

			current.lineTo(lx, ly);

		}

		paths[0] = current;
	}

	private final PlayerRef micListener = new PlayerRef() {
		@Override
		public Object getLock() {
			return statLock;
		}

		@Override
		public PlayStat getStat() {
			return playStat;
		}

		@Override
		public void playbackEnded() {
			synchronized (statLock) {
				playStat = PlayStat.STOPPED;
			}
			repaint();
		}

		@Override
		public void drawDisplay(float[] samples, int svalid) {
			makePath(samples, svalid);
			repaint();
		}

		@Override
		public AudioFormat getFormat() {
			return audioFormat;
		}
	};

	public void setSampleRate(int rate) {
		DEF_BUFFER_SAMPLE_SZ = rate;
	}

	public void stop() {

		synchronized (statLock) {
			playStat = PlayStat.STOPPED;
			audioFormat = null;
		}
	}

	public void start() {

		synchronized (statLock) {

			audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, sampleSizeInBits, numChannels,
					frameRate, sampleRate, bigEndian);

			synchronized (statLock) {
				playStat = PlayStat.PLAYING;
				new PlaybackLoop(micListener).execute();
			}
		}
	}

	public static class PlaybackLoop extends SwingWorker<Void, Void> {

		private final PlayerRef playerRef;

		public PlaybackLoop(PlayerRef pr) {
			playerRef = pr;
		}

		@Override
		public Void doInBackground() {

			DataLine.Info info = null;
			TargetDataLine line = null;

			try {
				try {

					info = new DataLine.Info(TargetDataLine.class, audioFormat);
					line = (TargetDataLine) AudioSystem.getLine(info);
					line.open(audioFormat);
					line.start();

					final int normalBytes = normalBytesFromBits(audioFormat.getSampleSizeInBits());

					play_loop: do {
						while (playerRef.getStat() == PlayStat.PLAYING) {

							float[] samples = new float[DEF_BUFFER_SAMPLE_SZ * audioFormat.getChannels()];
							long[] transfer = new long[samples.length];

							int numBytesRead;
							byte[] data = new byte[DEF_BUFFER_SAMPLE_SZ * audioFormat.getChannels() * normalBytes];

							if ((numBytesRead = line.read(data, 0, data.length)) == -1) {
								break play_loop;
							}

							samples = unpack(data, transfer, samples, numBytesRead, audioFormat);
							playerRef.drawDisplay(samples, numBytesRead / normalBytes);

						}

						if (playerRef.getStat() == PlayStat.STOPPED) {
							try {
								synchronized (playerRef.getLock()) {
									playerRef.getLock().wait(500L);
								}
							} catch (InterruptedException ie) {
							}
							break;
						} else {
							break;
						}

					} while (true);

				} catch (LineUnavailableException lue) {

				}
			} finally {
				if (line != null) {
					line.close();
				}
			}

			return (Void) null;
		}

		@Override
		public void done() {
			// playerRef.playbackEnded();
		}
	}

	public static float[] unpack(byte[] bytes, long[] transfer, float[] samples, int bvalid, AudioFormat fmt) {
		if (fmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED
				&& fmt.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {

			return samples;
		}
		final int bitsPerSample = fmt.getSampleSizeInBits();
		final int normalBytes = normalBytesFromBits(bitsPerSample);

		if (fmt.isBigEndian()) {
			for (int i = 0, k = 0, b; i < bvalid; i += normalBytes, k++) {
				transfer[k] = 0L;

				int least = i + normalBytes - 1;
				for (b = 0; b < normalBytes; b++) {
					transfer[k] |= (bytes[least - b] & 0xffL) << (8 * b);
				}
			}
		} else {
			for (int i = 0, k = 0, b; i < bvalid; i += normalBytes, k++) {
				transfer[k] = 0L;

				for (b = 0; b < normalBytes; b++) {
					transfer[k] |= (bytes[i + b] & 0xffL) << (8 * b);
				}
			}
		}

		final long fullScale = (long) Math.pow(2.0, bitsPerSample - 1);

		if (fmt.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {

			final long signShift = 64L - bitsPerSample;

			for (int i = 0; i < transfer.length; i++) {
				transfer[i] = ((transfer[i] << signShift) >> signShift);
			}
		} else {

			for (int i = 0; i < transfer.length; i++) {
				transfer[i] -= fullScale;
			}
		}

		/* finally normalize to range of -1.0f to 1.0f */

		for (int i = 0; i < transfer.length; i++) {
			samples[i] = (float) transfer[i] / (float) fullScale;
		}

		return samples;
	}

	public static int normalBytesFromBits(int bitsPerSample) {

		return bitsPerSample + 7 >> 3;
	}

}
