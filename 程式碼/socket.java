package game;
import java.io.*;
import java.net.*;
public class socket extends Thread {
    private Socket socket;
    private MessageListener messageListener = null;
    private ConnectionListener connectionListener = null;
    private BufferedReader br;
    private PrintWriter pw;
    socket() {}
    public socket(Socket socket) throws IOException {
        init(socket);
    }
    public socket(Socket socket, MessageListener listener) throws IOException {
        this.messageListener = listener;
        init(socket);
    }
    protected void init(Socket socket) throws IOException {
        this.socket = socket;
        InputStreamReader isr = new InputStreamReader(this.socket.getInputStream());
        this.br = new BufferedReader(isr);
        this.pw = new PrintWriter(socket.getOutputStream(), true);
    }
    public String getDisplayName() {
        return socket.getLocalAddress().getHostAddress()+"/"+socket.getLocalPort();
    }
    public synchronized void run() {
        String line;
        try {
            while(!socket.isClosed() && (line = br.readLine()) != null) {
                if(this.messageListener != null) {
                    this.messageListener.onMessage(socket, line);
                }
            }
        } catch (IOException e) {
            System.out.println("socket interrupt");
            if(connectionListener != null) {
                connectionListener.onSocketInterrupted(this.socket);
            }
        }
    }
    public void close() throws IOException {
        socket.close();
    }
    public Socket getSocket() {
        return socket;
    }
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }
    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }
    public void sendMessage(String message) {
        if(pw != null) {
            pw.println(message);
            pw.flush();
        }
    }
}
interface ConnectionListener {
    void onSocketInterrupted(Socket socket);
}