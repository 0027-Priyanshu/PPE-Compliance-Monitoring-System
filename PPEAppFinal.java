import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.filechooser.FileNameExtensionFilter;

public class PPEAppFinal {

    JFrame frame;
    CardLayout cl;
    JPanel mainPanel;

    JLabel imageLabel, resultValue;
    DefaultTableModel tableModel;

    File selectedFile;
    String selectedFileType = "";

    Timer frameTimer;
    List<String> previewFrames = new ArrayList<>();
    int currentFrameIndex = 0;

    PPEDatabase db = new PPEDatabase();

    public PPEAppFinal() {
        frame = new JFrame("PPE Compliance Monitoring System");
        frame.setSize(800, 550);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cl = new CardLayout();
        mainPanel = new JPanel(cl);

        mainPanel.add(loginPanel(), "login");
        mainPanel.add(dashboardPanel(), "dashboard");
        mainPanel.add(historyPanel(), "history");

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    JPanel loginPanel() {
        JPanel p = new JPanel(null);
        p.setBackground(new Color(30, 30, 70));

        JLabel title = new JLabel("Login", SwingConstants.CENTER);
        title.setBounds(300, 60, 200, 40);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 26));

        JLabel userLabel = new JLabel("User ID:");
        userLabel.setBounds(250, 150, 100, 30);
        userLabel.setForeground(Color.WHITE);

        JTextField user = new JTextField();
        user.setBounds(350, 150, 180, 30);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(250, 200, 100, 30);
        passLabel.setForeground(Color.WHITE);

        JPasswordField pass = new JPasswordField();
        pass.setBounds(350, 200, 180, 30);

        JButton loginBtn = new JButton("Login");
        loginBtn.setBounds(330, 260, 120, 35);

        loginBtn.addActionListener(e -> {
            String username = user.getText();
            String password = new String(pass.getPassword());

            if (db.validateUser(username, password)) {
                JOptionPane.showMessageDialog(frame, "Login Successful!");
                cl.show(mainPanel, "dashboard");
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid Credentials!");
            }
        });

        p.add(title);
        p.add(userLabel);
        p.add(user);
        p.add(passLabel);
        p.add(pass);
        p.add(loginBtn);

        return p;
    }

    JPanel dashboardPanel() {
        JPanel p = new JPanel() {
            Image bg;

            {
                try {
                    java.net.URL url = getClass().getResource("/background.jpeg");

                    if (url != null) {
                        bg = new ImageIcon(url).getImage();
                    } else {
                        System.out.println("Background image NOT FOUND");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (bg != null) {
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                    g.setColor(new Color(0, 0, 0, 100));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        p.setLayout(null);

        JPanel left = new JPanel(null);
        left.setBounds(30, 100, 200, 250);
        left.setBackground(new Color(255, 255, 255, 40));

        JButton uploadImg = new JButton("Upload Image");
        uploadImg.setBounds(20, 20, 160, 35);

        JButton uploadVid = new JButton("Upload Video");
        uploadVid.setBounds(20, 70, 160, 35);

        JButton check = new JButton("Analyze");
        check.setBounds(20, 120, 160, 35);

        JButton history = new JButton("History");
        history.setBounds(20, 170, 160, 35);

        left.add(uploadImg);
        left.add(uploadVid);
        left.add(check);
        left.add(history);

        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBounds(300, 120, 300, 180);
        previewPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        previewPanel.setOpaque(false);

        imageLabel = new JLabel("", SwingConstants.CENTER);
        imageLabel.setForeground(Color.WHITE);
        previewPanel.add(imageLabel, BorderLayout.CENTER);

        JLabel resultText = new JLabel("Result:");
        resultText.setBounds(300, 90, 200, 30);
        resultText.setForeground(Color.WHITE);

        resultValue = new JLabel("");
        resultValue.setBounds(300, 320, 420, 30);
        resultValue.setForeground(Color.WHITE);

        uploadImg.addActionListener(this::uploadImage);
        uploadVid.addActionListener(e -> uploadVideo());

        check.addActionListener(e -> {
            if (selectedFile == null) {
                JOptionPane.showMessageDialog(frame, "Upload file first!");
                return;
            }

            resultValue.setText("Analyzing...");
            resultValue.setForeground(Color.WHITE);

            new Thread(() -> {
                try {
                    if ("image".equals(selectedFileType)) {
                        analyzeImage();
                    } else if ("video".equals(selectedFileType)) {
                        analyzeVideo();
                    } else {
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(frame, "Unknown file type selected!")
                        );
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();

                    SwingUtilities.invokeLater(() -> {
                        resultValue.setText("Result: ERROR");
                        resultValue.setForeground(Color.RED);
                        JOptionPane.showMessageDialog(frame, "Error running Python");
                    });
                }
            }).start();
        });

        history.addActionListener(e -> cl.show(mainPanel, "history"));

        p.add(left);
        p.add(previewPanel);
        p.add(resultText);
        p.add(resultValue);

        return p;
    }

    JPanel historyPanel() {
        JPanel p = new JPanel(new BorderLayout());

        String[] cols = {"File", "Result"};
        tableModel = new DefaultTableModel(cols, 0);

        JTable table = new JTable(tableModel);
        JScrollPane sp = new JScrollPane(table);

        JButton back = new JButton("Back");
        back.addActionListener(e -> cl.show(mainPanel, "dashboard"));

        p.add(sp, BorderLayout.CENTER);
        p.add(back, BorderLayout.SOUTH);

        return p;
    }

    void uploadImage(ActionEvent e) {
        stopFrameSlideshow();

        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter(
                "Image Files", "jpg", "jpeg", "png"
        ));

        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            selectedFileType = "image";

            showPreviewImage(selectedFile.getAbsolutePath());

            resultValue.setText("Selected Image: " + selectedFile.getName());
            resultValue.setForeground(Color.WHITE);
        }
    }

    void uploadVideo() {
        stopFrameSlideshow();

        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter(
                "Video Files", "mp4", "m4v", "mov"
        ));

        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            selectedFileType = "video";

            try {
                List<String> frames = extractVideoFrames(selectedFile.getAbsolutePath(), "selected");
                startFrameSlideshow(frames);

                resultValue.setText("Selected Video: " + selectedFile.getName());
                resultValue.setForeground(Color.WHITE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Could not generate video preview.");
            }
        }
    }

    void analyzeImage() throws Exception {
        stopFrameSlideshow();

        ProcessBuilder pb = new ProcessBuilder(
                "/Users/priyanshu/python_ai/ppe_env/bin/python",
                "/Users/priyanshu/Desktop/ppe_system/python_ai/detection/main_image.py",
                selectedFile.getAbsolutePath()
        );

        pb.redirectErrorStream(true);

        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        String line;
        String finalStatus = null;
        String outputImagePath = null;

        while ((line = reader.readLine()) != null) {
            System.out.println(line);

            if (line.startsWith("RESULT:")) {
                finalStatus = line.substring("RESULT:".length()).trim();
            }

            if (line.startsWith("OUTPUT:")) {
                outputImagePath = line.substring("OUTPUT:".length()).trim();
            }
        }

        int exitCode = process.waitFor();
        System.out.println("Python image process exit code: " + exitCode);

        if (finalStatus == null || finalStatus.isEmpty()) {
            finalStatus = "ERROR";
        }

        String result = finalStatus;
        String finalOutputImagePath = outputImagePath;

        System.out.println("JAVA FINAL IMAGE RESULT BEFORE DB INSERT = " + result);

        SwingUtilities.invokeLater(() -> {
            resultValue.setText("Result: " + result);

            if ("SAFE".equalsIgnoreCase(result)) {
                resultValue.setForeground(Color.GREEN);
            } else if ("WARNING".equalsIgnoreCase(result)) {
                resultValue.setForeground(Color.ORANGE);
            } else if ("UNSAFE".equalsIgnoreCase(result)) {
                resultValue.setForeground(Color.RED);
            } else if ("NO PERSON".equalsIgnoreCase(result)) {
                resultValue.setForeground(Color.WHITE);
            } else {
                resultValue.setForeground(Color.RED);
            }

            if (finalOutputImagePath != null && !finalOutputImagePath.isEmpty()) {
                showPreviewImage(finalOutputImagePath);
            }

            tableModel.addRow(new Object[]{selectedFile.getName(), result});
        });

        db.insertRecord(
                selectedFile.getName(),
                result,
                selectedFile.getAbsolutePath()
        );
    }

    void analyzeVideo() throws Exception {
        stopFrameSlideshow();

        ProcessBuilder pb = new ProcessBuilder(
                "/Users/priyanshu/python_ai/ppe_env/bin/python",
                "/Users/priyanshu/Desktop/ppe_system/python_ai/detection/main_video.py",
                selectedFile.getAbsolutePath()
        );

        pb.redirectErrorStream(true);

        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        String line;
        String finalStatus = null;
        String outputVideoPath = null;

        while ((line = reader.readLine()) != null) {
            System.out.println(line);

            if (line.startsWith("RESULT:")) {
                finalStatus = line.substring("RESULT:".length()).trim();
            }

            if (line.startsWith("OUTPUT_VIDEO:")) {
                outputVideoPath = line.substring("OUTPUT_VIDEO:".length()).trim();
            }
        }

        int exitCode = process.waitFor();
        System.out.println("Python video process exit code: " + exitCode);
        System.out.println("Final output video path: " + outputVideoPath);

        if (finalStatus == null || finalStatus.isEmpty()) {
            finalStatus = "VIDEO PROCESSED";
        }

        String result = finalStatus;
        String finalOutputVideoPath = outputVideoPath;

        System.out.println("JAVA FINAL VIDEO RESULT BEFORE DB INSERT = " + result);

        SwingUtilities.invokeLater(() -> {
            resultValue.setText("Result: " + result);

            if ("SAFE".equalsIgnoreCase(result)) {
                resultValue.setForeground(Color.GREEN);
            } else if ("WARNING".equalsIgnoreCase(result)) {
                resultValue.setForeground(Color.ORANGE);
            } else if ("UNSAFE".equalsIgnoreCase(result)) {
                resultValue.setForeground(Color.RED);
            } else if ("NO PERSON".equalsIgnoreCase(result)) {
                resultValue.setForeground(Color.WHITE);
            } else {
                resultValue.setForeground(Color.CYAN);
            }

            tableModel.addRow(new Object[]{selectedFile.getName(), result});

            if (finalOutputVideoPath != null && !finalOutputVideoPath.isEmpty()) {
                try {
                    List<String> outputFrames = extractVideoFrames(finalOutputVideoPath, "output");
                    startFrameSlideshow(outputFrames);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Video processed, but preview frames could not be generated.");
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Video processed, but output path not received from Python.");
            }
        });

        db.insertRecord(
                selectedFile.getName(),
                result,
                selectedFile.getAbsolutePath()
        );
    }

    void showPreviewImage(String imagePath) {
        ImageIcon img = new ImageIcon(
                new ImageIcon(imagePath)
                        .getImage()
                        .getScaledInstance(300, 180, Image.SCALE_SMOOTH)
        );

        imageLabel.setText("");
        imageLabel.setIcon(img);
    }

    void startFrameSlideshow(List<String> framePaths) {
        stopFrameSlideshow();

        if (framePaths == null || framePaths.isEmpty()) {
            imageLabel.setIcon(null);
            imageLabel.setText("No Preview");
            return;
        }

        previewFrames = framePaths;
        currentFrameIndex = 0;

        showPreviewImage(previewFrames.get(0));

        frameTimer = new Timer(500, e -> {
            if (!previewFrames.isEmpty()) {
                currentFrameIndex = (currentFrameIndex + 1) % previewFrames.size();
                showPreviewImage(previewFrames.get(currentFrameIndex));
            }
        });

        frameTimer.start();
    }

    void stopFrameSlideshow() {
        if (frameTimer != null && frameTimer.isRunning()) {
            frameTimer.stop();
        }
    }

    List<String> extractVideoFrames(String videoPath, String prefix) throws Exception {
        File framesRootDir = new File("outputs/preview_frames");
        if (!framesRootDir.exists()) {
            framesRootDir.mkdirs();
        }

        String videoName = new File(videoPath).getName();
        String baseName = videoName.contains(".")
                ? videoName.substring(0, videoName.lastIndexOf('.'))
                : videoName;

        File outputDir = new File(framesRootDir, prefix + "_" + baseName);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        ProcessBuilder pb = new ProcessBuilder(
                "/Users/priyanshu/python_ai/ppe_env/bin/python",
                "/Users/priyanshu/Desktop/ppe_system/python_ai/detection/extract_frames.py",
                videoPath,
                outputDir.getAbsolutePath()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        String line;
        List<String> framePaths = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            System.out.println(line);

            if (line.startsWith("FRAME:")) {
                framePaths.add(line.substring("FRAME:".length()).trim());
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0 || framePaths.isEmpty()) {
            throw new RuntimeException("Frame extraction failed");
        }

        return framePaths;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PPEAppFinal::new);
    }
}