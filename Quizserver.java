import java.io.*;
import java.net.*;
import java.util.*;

public class Quizserver {
    private List<Question> questions; // Stores the list of quiz questions
    private int totalClients = 0; // Counter for the total number of connected clients

    // Constructor to initialize the server with a path to the question file
    public Quizserver(String questionFilePath) {
        questions = loadQuestionsFromFile(questionFilePath);
    }

    public static void main(String[] args) {
        // Create a server instance with questions loaded from "questions.csv"
        Quizserver server = new Quizserver("questions.csv");
        server.start(); // Start the server
    }

    // Loads questions from a CSV file
    private List<Question> loadQuestionsFromFile(String filePath) {
        List<Question> questions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String questionText = parts[0].trim();
                    String answer = parts[1].trim();
                    questions.add(new Question(questionText, answer)); // Add new question to the list
                }
            }
        } catch (IOException e) {
            System.out.println("ERROR while reading file: " + e.getMessage());
        }
        return questions;
    }

    // Starts the server and handles client connections
    public void start() {
        // Default IP and port settings, loaded from a configuration file
        String serverIp = "localhost"; // Default IP
        int port = 8888; // Default port
        try (BufferedReader reader = new BufferedReader(new FileReader("server_info.dat"))) {
            String line = reader.readLine();
            if (line != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    serverIp = parts[0].trim();
                    port = Integer.parseInt(parts[1].trim()); // Parse and set the custom port
                }
            }
        } catch (IOException e) {
            System.out.println("ERROR while reading server info: " + e.getMessage());
        }

        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(serverIp))) {
            System.out.println("Server start at IP: " + serverIp + " PORT: " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept a new client connection
                totalClients++;
                System.out.println("Client Connect #" + totalClients);
                new Thread(new ClientHandler(clientSocket, questions)).start(); // Start a new thread for each client
            }
        } catch (IOException e) {
            System.out.println("Server Socket error: " + e.getMessage());
        }
    }

    // Runnable class to handle client interactions
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private List<Question> questions;

        public ClientHandler(Socket socket, List<Question> questions) {
            this.socket = socket;
            this.questions = questions;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                int score = 0;

                for (Question question : questions) {
                    out.println("QUESTION:" + question.getQuestionText()); // Send the question to the client
                    String clientAnswer = in.readLine(); // Read the answer from the client
                    if (clientAnswer != null && clientAnswer.equalsIgnoreCase(question.getAnswer())) {
                        score += 10;
                        out.println("RESULT:Correct!"); // Correct answer
                    } else {
                        out.println("RESULT:Incorrect..."); // Incorrect answer
                    }
                    out.println("CLEAR_RESULT"); // Signal to clear any results
                }
                out.println("SCORE:" + score); // Send the final score to the client
                System.out.println("CLIENT TOTAL SCORE: " + score);
            } catch (IOException e) {
                System.out.println("Error in connection: " + e.getMessage());
            } finally {
                try {
                    socket.close(); // Close the socket connection
                } catch (IOException e) {
                    System.out.println("Socket closure error: " + e.getMessage());
                }
            }
        }
    }

    // Inner class to represent a quiz question
    static class Question {
        private String questionText;
        private String answer;

        public Question(String questionText, String answer) {
            this.questionText = questionText;
            this.answer = answer;
        }

        public String getQuestionText() {
            return questionText;
        }

        public String getAnswer() {
            return answer;
        }
    }
}
