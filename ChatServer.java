
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {

    private static final int PORT = 8090;
    private static Map<String, HiloChatServer> users = new ConcurrentHashMap<>(); // Usuarios conectados (clave:nombre -> valor:manejador).

    public static void main(String[] args) {
        System.out.println("Servidor iniciado, esperando conexiones...");

        //Envía la lista de usuarios cada 30 segundos
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                //Ejecuta el método para enviar la lista de usuarios
                sendUserList();
            }
        }, 0, 30000);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket);

                HiloChatServer clientHandler = new HiloChatServer(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Envía la lista de usuarios conectados a todos los clientes
    private static void sendUserList() {

        String userList = "USER_LIST:";
        for (String username : users.keySet()){
            userList += username + ",";
        }
        for (HiloChatServer client : users.values()) {
            client.sendMessage(userList);
        }
    }

    // Registra a un nuevo usuario
    public static synchronized void registerUser(String username, HiloChatServer clientHandler) {
        users.put(username, clientHandler);
        sendUserList();
    }

    // Elimina a un usuario de la lista al desconectarse
    public static synchronized void removeUser(String username) {
        users.remove(username);
        sendUserList(); // Actualiza la lista
    }

    // Enviar mensaje privado entre dos usuarios
    public static synchronized void sendPrivateMessage(String fromUser, String toUser, String message) {
        HiloChatServer recipient = users.get(toUser);
        if (recipient != null) {
            recipient.sendMessage("PRIVATE:" + fromUser + ":" + message);
        }
    }

    //Envía un mensaje a todos los clientes conectados excepto al cliente que lo envía
    public static synchronized void broadcastMessage(String message, HiloChatServer excludeClient) {
        for (HiloChatServer client : users.values()) {
            if (client != excludeClient) { // Excluye al cliente que envía el mensaje
                client.sendMessage(message);
            }
        }
    }


    //Agregar método para obtener el ClientHandler de un usuario
    public static synchronized HiloChatServer getUserHandler(String username) {
        return users.get(username);
    }

}
