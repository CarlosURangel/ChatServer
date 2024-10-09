
import java.io.*;
import java.net.*;

class HiloChatServer implements Runnable {

    private Socket socket;
    private String username;
    private PrintWriter writer;
    private BufferedReader reader;
    //DataInputStream y DataOutputStream para manejar la transferencia de archivos y mensajes de texto
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public HiloChatServer(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            //Solicitar nombre de usuario
            this.username = reader.readLine();
            ChatServer.registerUser(username, this);  //Registra al usuario en el servidor
            System.out.println(username + " se ha conectado.");

            String message;
            while ((message = reader.readLine()) != null) {
                if (message.startsWith("PRIVATE")) {
                    handlePrivateMessage(message);
                } else if (message.startsWith("FILE")) {
                    handleFileTransfer(message);
                } else {
                    // Enviar mensaje público a todos los clientes
                    ChatServer.broadcastMessage(username + ": " + message, this);
                }
            }
        } catch(SocketException e){
            System.out.println(username +" se ha desconectado");
        }catch (IOException e) {
            e.printStackTrace();
        } finally {
            ChatServer.removeUser(username);
            closeConnections();
        }
    }

    //Método para manejar mensajes privados
    private void handlePrivateMessage(String message) {
        //Formato para los mensajes privados
        //PRIVATE:destinatario:mensaje
        String[] parts = message.split(":", 3);
        String recipient = parts[1];
        String privateMessage = parts[2];
        ChatServer.sendPrivateMessage(username, recipient, privateMessage);
    }

    //Método para manejar la transferencia de archivos
    private void handleFileTransfer(String message) {
        try {
            String[] parts = message.split(":", 3);
            String recipient = parts[1];
            String fileName = parts[2];

            //Leer el tamaño del archivo
            long fileSize = dataInputStream.readLong();

            //Verificar que el archivo no exceda los 50MB
            if (fileSize > 50 * 1024 * 1024) {
                writer.println("ERROR: El archivo excede el tamaño máximo permitido de 50MB.");
                return;
            }

            //Enviar notificación al destinatario de que está recibiendo un archivo y obtener el handler
            HiloChatServer recipientHandler = ChatServer.getUserHandler(recipient);
            if (recipientHandler != null) {
                //Mandar notificación al usuario de que va a recibir un archivo
                recipientHandler.sendMessage("FILE_RECEIVE:" + username + ":" + fileName + ":" + fileSize);

                // Transferir el archivo
                byte[] buffer = new byte[4096];
                long bytesRead = 0;
                while (bytesRead < fileSize) {
                    int read = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize - bytesRead));
                    if (read == -1) {
                        break;
                    }
                    recipientHandler.dataOutputStream.write(buffer, 0, read);
                    bytesRead += read;
                }
                //Asegura de que todos los datos se envíen correctamente
                recipientHandler.dataOutputStream.flush();

                //Confirmar la transferencia al destinatario
                recipientHandler.sendMessage("FILE_TRANSFER_COMPLETE:" + fileName);

                //Confirmar al remitente que el archivo fue enviado correctamente
                writer.println("Archivo enviado correctamente a " + recipient);
            } else {
                writer.println("ERROR: El usuario " + recipient + " no está conectado.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Método para enviar mensajes al cliente
    public void sendMessage(String message) {
        writer.println(message);
    }

    // Cerrar conexiones
    private void closeConnections() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
