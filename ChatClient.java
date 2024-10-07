
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

public class ChatClient extends JFrame { 


    private JTextArea textArea;
    private JTextField messageField;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private PrintWriter writer;
    private BufferedReader reader;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private Map<String, PrivateChatWindow> privateChats = new HashMap<>();

    public ChatClient(String serverAddress, int serverPort) {

        try {
    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
            UIManager.setLookAndFeel(info.getClassName());
            break;
        }
    }
} catch (Exception e) {
    e.printStackTrace();
}

        // Configuración de la interfaz gráfica
        setTitle("Chat");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setBackground(new Color(240, 240, 240));  // Color de fondo
        textArea.setForeground(Color.BLACK);  // Color del texto
        textArea.setFont(new Font("Arial", Font.PLAIN, 14));  // Fuente del texto
        JScrollPane scrollPane = new JScrollPane(textArea);

        messageField = new JTextField(30);
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));

        JButton sendButton = new JButton("Enviar");
        sendButton.setBackground(new Color(59, 89, 182));  // Fondo del botón
        sendButton.setForeground(Color.WHITE);  // Color del texto del botón
        sendButton.setFont(new Font("Arial", Font.BOLD, 12));  // Fuente del texto del botón


        JButton sendFileButton = new JButton("Enviar archivo");
        sendFileButton.setBackground(new Color(76, 175, 80));  // Fondo del botón
        sendFileButton.setForeground(Color.WHITE);
        sendFileButton.setFont(new Font("Arial", Font.BOLD, 12));


        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));  // Layout mejorado

        panel.add(messageField);
        panel.add(sendButton);
        panel.add(sendFileButton);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setBackground(new Color(220, 220, 220));  // Fondo de la lista de usuarios
        userList.setFont(new Font("Arial", Font.PLAIN, 14));
        userList.setForeground(Color.BLACK);


        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(userList), scrollPane);
        splitPane.setDividerLocation(150);
        add(splitPane, BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendFile();
            }
        });

        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    startPrivateChat();
                }
            }
        });

        // Conectar al servidor
        try {
            Socket socket = new Socket(serverAddress, serverPort);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            // Solicitar nombre de usuario
            String username = null;
            while (username == null || username.trim().isEmpty()) {
                username = JOptionPane.showInputDialog(this, "Ingrese su nombre de usuario:");

                // Verifica si el usuario cerró la ventana o presionó cancelar
                if (username == null) {
                    socket.close(); //Cierra la conexión
                    System.exit(0);
                } else if (username.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "El campo no puede estar vacío, ingrese un nombre de usuario");
                }
            }
            writer.println(username);  // Enviar el nombre al servidor

            // Hilo para escuchar mensajes del servidor
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String message;
                        while ((message = reader.readLine()) != null) {
                            if (message.startsWith("USER_LIST:")) {
                                updateUserList(message);
                            } else if (message.startsWith("FILE_RECEIVE")) {
                                handleFileReceive(message);
                            } else if (message.startsWith("PRIVATE")) {
                                handlePrivateMessage(message);
                            } else {
                                // Mostrar mensajes públicos
                                textArea.append(message + "\n");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para enviar mensajes
    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            writer.println(message);  // Enviar mensaje al servidor
            textArea.append("Tú: " + message + "\n");  // Mostrar el mensaje en la pantalla del cliente
            messageField.setText("");  // Limpiar el campo de texto
        }
    }

    // Método para enviar mensajes privados
    private void handlePrivateMessage(String message) {
        String[] parts = message.split(":", 3);
        String sender = parts[1];
        String privateMessage = parts[2];

        // Abre una nueva ventana si aún no está abierta
        if (!privateChats.containsKey(sender)) {
            PrivateChatWindow privateChat = new PrivateChatWindow(sender, writer, dataOutputStream);
            privateChats.put(sender, privateChat);
            privateChat.setVisible(true);
        }

        // Mostrar el mensaje en la ventana de chat privado
        privateChats.get(sender).appendMessage(sender + ": " + privateMessage);
    }

    // Método para enviar archivos
    private void sendFile() {
        String recipient = userList.getSelectedValue();
        if (recipient == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario para enviar el archivo.");
            return;
        }

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

                textArea.append("Archivo enviado a " + recipient + ": " + file.getName() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Método para manejar la recepción de archivos
    private void handleFileReceive(String message) {
        String[] parts = message.split(":");
        String sender = parts[1];
        String fileName = parts[2];
        long fileSize = Long.parseLong(parts[3]);

        int option = JOptionPane.showConfirmDialog(this, "Recibir archivo de " + sender + ": " + fileName + " (" + fileSize + " bytes)?");
        if (option == JOptionPane.YES_OPTION) {
            try {
                // Seleccionar ubicación para guardar el archivo
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(fileName));
                int saveOption = fileChooser.showSaveDialog(this);
                if (saveOption == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();

                    // Recibir archivo en bloques de 4096 bytes
                    FileOutputStream fileOutputStream = new FileOutputStream(fileToSave);
                    byte[] buffer = new byte[4096];
                    long bytesRead = 0;
                    while (bytesRead < fileSize) {
                        int read = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize - bytesRead));
                        fileOutputStream.write(buffer, 0, read);
                        bytesRead += read;
                    }
                    fileOutputStream.close();
                    textArea.append("Archivo recibido de " + sender + ": " + fileName + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Método para actualizar la lista de usuarios conectados
    private void updateUserList(String message) {
        String[] users = message.substring("USER_LIST:".length()).split(",");
        userListModel.clear();
        for (String user : users) {
            userListModel.addElement(user);
        }
    }

    // Método para iniciar un chat privado
    private void startPrivateChat() {
        String recipient = userList.getSelectedValue();
        if (recipient != null && !privateChats.containsKey(recipient)) {
            PrivateChatWindow privateChat = new PrivateChatWindow(recipient, writer, dataOutputStream);
            privateChats.put(recipient, privateChat);
            privateChat.setVisible(true);
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient("localhost", 8090);
        client.setVisible(true);
    }
}
