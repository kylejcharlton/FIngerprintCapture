
package scanner;

import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.event.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

public class CaptureForm extends JDialog {
    private DPFPCapture capturer = DPFPGlobal.getCaptureFactory().createCapture();
    private JLabel picture = new JLabel();
    private JTextField prompt = new JTextField();
    private JTextArea log = new JTextArea();
    private Image image;

	/**
	 * Creates a JDialog for capturing fingerprints.
	 *
	 * @param owner - the Frame to set as the owner of the CaptureForm
	 */
	public CaptureForm(Frame owner) {
        super(owner, true);
        setTitle("Fingerprint Capture");

        setLayout(new BorderLayout());
        rootPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        picture.setPreferredSize(new Dimension(240, 280));
        picture.setBorder(BorderFactory.createLoweredBevelBorder());
        prompt.setFont(UIManager.getFont("Panel.font"));
        prompt.setEditable(false);
        prompt.setColumns(40);
        prompt.setMaximumSize(prompt.getPreferredSize());
        prompt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0), "Prompt:"),
                BorderFactory.createLoweredBevelBorder()));
        log.setColumns(40);
        log.setEditable(false);
        log.setFont(UIManager.getFont("Panel.font"));
        JScrollPane logpane = new JScrollPane(log);
        logpane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0), "Status:"),
                BorderFactory.createLoweredBevelBorder()));

        JButton save = new JButton("Save");
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });

        JButton quit = new JButton("Close");
        quit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Color.getColor("control"));
        right.add(prompt, BorderLayout.PAGE_START);
        right.add(logpane, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Color.getColor("control"));
        center.add(right, BorderLayout.CENTER);
        center.add(picture, BorderLayout.LINE_START);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        bottom.setBackground(Color.getColor("control"));
        bottom.add(save);
        bottom.add(quit);

        setLayout(new BorderLayout());
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.PAGE_END);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                init();
                start();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                stop();
            }

        });

        pack();
        setLocationRelativeTo(null);
    }

	/**
	 * Saves the scanned fingerprint to a png in a location of the user's choosing.
	 *
	 * @author Kyle Charlton
	 */
	protected void save() {
        Preferences prefs = Preferences.userRoot().node(getClass().getName());
        JFileChooser chooser = new JFileChooser(prefs.get("LAST_USED_FOLDER",
                new File(".").getAbsolutePath()));
        while (true) {
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = chooser.getSelectedFile();
                    if (!file.toString().toLowerCase().endsWith(".png"))
                        file = new File(file.toString() + ".png");
                    if (file.exists()) {
                        int choice = JOptionPane.showConfirmDialog(this,
                                String.format("File \"%1$s\" already exists.\nDo you want to replace it?", file.toString()),
                                "Fingerprint saving",
                                JOptionPane.YES_NO_CANCEL_OPTION);
                        if (choice == JOptionPane.NO_OPTION)
                            continue;
                        else if (choice == JOptionPane.CANCEL_OPTION)
                            break;
                    }
                    prefs.put("LAST_USED_FOLDER", chooser.getSelectedFile().getParent());
                    BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);

                    Graphics2D bGr = bufferedImage.createGraphics();
                    bGr.drawImage(image, 0, 0, null);
                    bGr.dispose();

                    try {
                        ImageIO.write(bufferedImage, "png", file);
                    } catch (IOException e) {
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getLocalizedMessage(), "Fingerprint saving", JOptionPane.ERROR_MESSAGE);
                }
            }
            break;
        }
    }

    protected void init() {
        capturer.addDataListener(new DPFPDataAdapter() {
            @Override
            public void dataAcquired(final DPFPDataEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        makeReport("The fingerprint sample was captured.");
                        process(e.getSample());
                    }
                });
            }
        });
        capturer.addReaderStatusListener(new DPFPReaderStatusAdapter() {
            @Override
            public void readerConnected(final DPFPReaderStatusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        makeReport("The fingerprint reader was connected.");
                    }
                });
            }

            @Override
            public void readerDisconnected(final DPFPReaderStatusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        makeReport("The fingerprint reader was disconnected.");
                    }
                });
            }
        });
        capturer.addSensorListener(new DPFPSensorAdapter() {
            @Override
            public void fingerTouched(final DPFPSensorEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        makeReport("The fingerprint reader was touched.");
                    }
                });
            }

            @Override
            public void fingerGone(final DPFPSensorEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        makeReport("The finger was removed from the fingerprint reader.");
                    }
                });
            }
        });
    }

    protected void process(DPFPSample sample) {
        // Draw fingerprint sample image.
        drawPicture(convertSampleToBitmap(sample));
    }

    protected void start() {
        capturer.startCapture();
        setPrompt("Using the fingerprint reader, scan your fingerprint.");
    }

    protected void stop() {
        capturer.stopCapture();
    }

    public void setPrompt(String string) {
        prompt.setText(string);
    }

    public void makeReport(String string) {
        log.append(string + "\n");
    }

    public void drawPicture(Image image) {
        this.image = image; // Set the image so it can be saved.
        picture.setIcon(
                new ImageIcon(image.getScaledInstance(picture.getWidth(), picture.getHeight(), Image.SCALE_DEFAULT)));
    }

    protected Image convertSampleToBitmap(DPFPSample sample) {
        return DPFPGlobal.getSampleConversionFactory().createImage(sample);
    }
}
