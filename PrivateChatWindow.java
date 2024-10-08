
import java.awt.*;
import java.awt.event.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.swing.*;

public class PrivateChatWindow extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private PrintWriter writer;
    private String recipient;
    private DataOutputStream dataOutputStream;

    public PrivateChatWindow(String recipient, PrintWriter writer, DataOutputStream dataOutputStream) {
        this.recipient = recipient;
        this.writer = writer;
        this.dataOutputStream = dataOutputStream;

        setTitle("Chat Privado con " + recipient);
        setSize(400, 300);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(245, 245, 245));  // Fondo claro
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));  // Fuente

        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        inputField.setFont(new Font("Arial", Font.PLAIN, 14));  // Fuente

        JButton sendButton = new JButton("Enviar");
        sendButton.setBackground(new Color(59, 89, 182));  // Fondo azul
        sendButton.setForeground(Color.WHITE);  // Texto blanco
        sendButton.setFont(new Font("Arial", Font.BOLD, 12));

        JButton sendFileButton = new JButton("Enviar archivo"); // Botón para enviar archivo
        sendButton.setBackground(new Color(59, 89, 182));  // Fondo azul
        sendButton.setForeground(Color.WHITE);  // Texto blanco
        sendButton.setFont(new Font("Arial", Font.BOLD, 12));

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        panel.add(sendFileButton, BorderLayout.WEST); // Añadir botón de enviar archivo

        add(scrollPane, BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        sendFileButton.addActionListener(new ActionListener() { // Acción para enviar archivo
            @Override
            public void actionPerformed(ActionEvent e) {
                sendFile();
            }
        });

        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    }

    public void appendMessage(String message) {
        chatArea.append(message + "\n");
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
            writer.println("PRIVATE:" + recipient + ":" + message);  // Enviar mensaje privado al servidor
            appendMessage("Tú: " + message);  // Mostrar el mensaje enviado en la ventana del cliente
            inputField.setText("");
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            long fileSize = file.length();

            // Verificar el tamaño del archivo (máximo 50MB)
            if (fileSize > 50 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "El archivo es demasiado grande. Máximo 50MB.");
                return;
            }

            // Enviar solicitud al servidor
            writer.println("FILE:" + recipient + ":" + file.getName());

            try {
                // Enviar tamaño del archivo
                dataOutputStream.writeLong(fileSize);

                // Enviar archivo en bloques de 4096 bytes
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) > 0) {
                    dataOutputStream.write(buffer, 0, bytesRead);
                }
                dataOutputStream.flush();
                fileInputStream.close();

                appendMessage("Archivo enviado a " + recipient + ": " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
