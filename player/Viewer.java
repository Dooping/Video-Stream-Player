package player;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Viewer {
	private JFrame mainFrame;
	private _JPanel panel;
	private JSlider slider;
	private boolean changedSlider;

	private QualityMetrics stats;

	public Viewer(int width, int height, int sizeBar) {
		init(sizeBar);
		mainFrame.setSize(width, height+slider.getHeight());
		stats = new QualityMetrics();
		changedSlider = true;
	}

	public BufferedImage updateFrame(byte[] imgData, int length, long timestamp) {
		ByteArrayInputStream bais = new ByteArrayInputStream(imgData, 0, length);

		try {
			BufferedImage img = ImageIO.read(bais);
			panel.setImage(img, length);
			ByteBuffer bb = ByteBuffer.wrap(imgData, length - 12, 12);
			if(!slider.getValueIsAdjusting()){
				changedSlider = true;
				slider.setValue((int)(timestamp/1000000));
			}
			stats.updateStats(img.getHeight(), bb.getInt(), bb.getLong());
			return img;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void init(int sizeBar) {
		mainFrame = new JFrame("Video Viewer");
		mainFrame.setLayout(new BorderLayout());
		mainFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent windowEvent) {
				System.exit(0);
			}
		});

		panel = new _JPanel();
		slider = createSlider("",0,sizeBar*1000,0);
		class Change implements ChangeListener {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				try {
					if (!changedSlider&&!slider.getValueIsAdjusting())
						Player.goTo(slider.getValue()/1000);
					else
						changedSlider = false;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}	
		}
		slider.addChangeListener(new Change());

		mainFrame.add(panel,BorderLayout.CENTER);
		mainFrame.add(slider,BorderLayout.PAGE_END);
		
		mainFrame.setVisible(true);
	}
	
	private JSlider createSlider(String name, int min, int max, int start){
		JSlider slider = new JSlider();
		slider.setMajorTickSpacing((max-min)/4);
		slider.setMinorTickSpacing((max-min)/20);
		slider.setPaintTicks(true);
		slider.setMaximum(max);
		slider.setMinimum(min);
		slider.setValue(start);
		slider.setBorder(new TitledBorder(new EtchedBorder(), name));
		
		return slider;
	}

	class _JPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		Image img;

		public void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			super.paintComponent(g2d);
			if (img != null) {
				g2d.drawImage(img, 0, 0, super.getWidth(), super.getHeight(), null);

				g2d.setColor(Color.GREEN);
				g2d.setFont(g2d.getFont().deriveFont(18.0f));
				g2d.drawString(String.format("(%dp, %d, %.1f)", stats.avgQuality(), stats.droppedFrames(), stats.accumJitter()), 10, super.getHeight() - 30);
			}
		}

		void setImage(Image img, int length) {
			this.img = img;
			repaint();
		}
	}
}

class QualityMetrics {
	private int totalFrames = 0;

	private int lastFrame = -1;
	private int droppedFrames = 0;

	private long t0 = -1, ts0;
	private double accumJitter = 0.0;

	private double sumVResolution = 0.0;

	void updateStats(int yres, int frame, long ts) {
		long now = System.nanoTime();

		sumVResolution += yres;

		totalFrames++;
		if (frame != lastFrame + 1)
			droppedFrames++;
		lastFrame = frame;

		double delay = Math.abs((ts - ts0) - (System.nanoTime() - t0)) / 1e9;

		if (t0 > 0 && delay > 1 / 30.0)
			accumJitter += delay;

		t0 = now;
		ts0 = ts;
	}

	int avgQuality() {
		return (int) (sumVResolution / totalFrames);
	}

	int droppedFrames() {
		return droppedFrames;
	}

	double accumJitter() {
		return accumJitter;
	}
}