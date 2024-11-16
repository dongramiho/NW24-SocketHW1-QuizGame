import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class Quizclient {
    // Server connection details
    private static String SERVER_IP = "localhost";
    private static int SERVER_PORT = 8888;

    // Client-side networking and user interface components
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private JFrame frame;
    private JTextField answerField;
    private JTextArea questionArea;
    private JLabel resultLabel;
    private int score = 0;

    public static void main(String[] args) {
        loadServerInfo(); // Loads server IP and port from a file
        SwingUtilities.invokeLater(Quizclient::new);
    }

    // Loads server IP and port configuration from a data file
    private static void loadServerInfo() {
        try (BufferedReader reader = new BufferedReader(new FileReader("server_info.dat"))) {
            String line = reader.readLine();
            if (line != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    SERVER_IP = parts[0].trim();
                    SERVER_PORT = Integer.parseInt(parts[1].trim());
                }
            }
        } catch (IOException e) {
            System.out.println("ERROR while reading server info: " + e.getMessage());
        }
    }

    // Constructor: Sets up the GUI and connects to the server
    public Quizclient() {
        setupGUI();
        connectToServer();
    }

    // Initializes and configures the GUI components
    private void setupGUI() {
        frame = new JFrame("Quiz Game Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);
        frame.setLayout(new BorderLayout());
    
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
    
        questionArea = new JTextArea(3, 30);
        questionArea.setFont(new Font("Arial", Font.BOLD, 24));
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setEditable(false);
        questionArea.setOpaque(false);
        questionArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        questionArea.setBorder(BorderFactory.createEmptyBorder(50, 10, 50, 10));
        
        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(questionArea);
        centerPanel.add(Box.createVerticalGlue());
    
        answerField = new JTextField(10);
        answerField.setFont(new Font("Arial", Font.PLAIN, 14));
        answerField.setMaximumSize(new Dimension(200, 30));
        answerField.setHorizontalAlignment(JTextField.CENTER);
        answerField.addActionListener(e -> sendAnswer());
        answerField.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(answerField);
    
        frame.add(centerPanel, BorderLayout.CENTER);
    
        resultLabel = new JLabel("Answer or Not:", JLabel.CENTER);
        resultLabel.setFont(new Font("Arial", Font.BOLD, 30));
        resultLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        frame.add(resultLabel, BorderLayout.SOUTH);
    
        frame.setVisible(true);
    }

    // Connects to the server using socket programming
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(this::processQuiz).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Server Connection Error!!!", "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Processes incoming messages from the server and handles quiz logic
    private void processQuiz() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("QUESTION:")) {
                    String questionText = line.substring(9);
                    questionArea.setText(questionText);
                    answerField.setText("");
                } else if (line.startsWith("RESULT:")) {
                    String result = line.substring(7);
                    resultLabel.setText(result);
                    Timer timer = new Timer(1000, e -> resultLabel.setText(""));
                    timer.setRepeats(false);
                    timer.start();
                    if (result.equals("Correct")) {
                        score += 10;
                    }
                } else if (line.startsWith("SCORE:")) {
                    int finalScore = Integer.parseInt(line.substring(6));
                    JOptionPane.showMessageDialog(frame, "End QUIZ! TOTAL SCORE: " + finalScore, "결과", JOptionPane.INFORMATION_MESSAGE);
                    socket.close();
                    frame.dispose();
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error occurred while server connection: " + e.getMessage());
        }
    }

    // Sends user's answer to the server
    private void sendAnswer() {
        String answer = answerField.getText().trim();
        out.println(answer);
    }
}
