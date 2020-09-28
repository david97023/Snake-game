package game;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import com.google.gson.*;
public class sserver {
    public static void main(String[] args) throws IOException {
        System.out.println("Server Start! Port : 5566");
        GameServer server = new GameServer(5566);
        server.start();
    }
}
class GameServer extends Server {
    public static int MAX_PLAYER_IN_A_ROOM = 2;
    private GameEngine currentGameEngine = null;
    private LinkedHashMap<Socket, GameEngine> socketList = new LinkedHashMap<>();
    private MessageListener onMessage = ((source, message) -> {
        ClientManager mgr = getList().get(source);
        mgr.sendMessage(socketList.get(source).toJson());
    });
    public GameServer(int port) throws IOException {
        super(port);
        this.setMessageListener(((source, message) -> {
            JsonElement jElement = new JsonParser().parse(message);
            JsonObject jObject = jElement.getAsJsonObject();
            if(jObject.has("handShake")) {
                String word = jObject.get("handShake").getAsString();
                if(word.equals("hello")) {
                    if(currentGameEngine == null) {
                        currentGameEngine = new GameEngine(onMessage);
                        currentGameEngine.setWaiting();
                    }
                    socketList.put(source, currentGameEngine);
                    int id = currentGameEngine.addPlayer(source);
                    getList().get(source).sendMessage(String.format("{\"handShake\":%d}", id));
                    if(currentGameEngine.playerCount() == MAX_PLAYER_IN_A_ROOM) {
                        currentGameEngine.start();
                        currentGameEngine = null;
                    }
                }
            } else {
                    int key = jObject.get("keyCode").getAsInt();
                    socketList.get(source).processKey(source, key);
            }
        }));
        this.setConnectionListener((socket)->{
            socketList.get(socket).interrupt();
        });
    }
}
class Server {
    private LinkedHashMap<Socket, ClientManager> list = new LinkedHashMap<>();
    private boolean run = true;
    private MessageListener listener;
    private ConnectionListener connectionListener;
    private Thread thread;
    public Server(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        this.thread = new Thread(()->{
            while(run) {
                try {
                    Socket socket = serverSocket.accept();
                    ClientManager clientManager = new ClientManager(socket);
                    list.put(socket, clientManager);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public Server(int port, MessageListener listener) throws IOException {
        this(port);
        this.listener = listener;
    }
    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }
    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }
    public void start() {
        thread.start();
    }
    public void stop() {
        this.run = false;
        for(ClientManager mgr : list.values()) {
            try {
                mgr.close();
            } catch (IOException e) {
                System.out.println("Error close server!");
            }
        }
    }
    public void broadcast(String message) {
        for(ClientManager mgr : list.values()) {
            mgr.sendMessage(message);
        }
    }
    public int clientCount() {
        return list.keySet().size();
    }
    protected LinkedHashMap<Socket, ClientManager> getList() {
        return list;
    }
    public class ClientManager extends socket {
        public ClientManager(Socket socket) throws IOException {
            super(socket, (Socket source, String message) -> {
                Server.this.listener.onMessage(source, message);
            });
            this.setConnectionListener(Server.this.connectionListener);
            this.start();
        }
        public String getDisplayName() {
            return getSocket().getInetAddress().getHostAddress()+"/"+getSocket().getPort();
        }
    }
}
class GameEngine extends GameInfo implements Runnable {
    private Thread thread = new Thread(this);
    private LinkedHashMap<Socket, Snake> players = new LinkedHashMap<>();
    private boolean running = true;
    private MessageListener messageListener;
    public GameEngine(MessageListener messageListener) {
        this.messageListener = messageListener;
    }
    public synchronized int playerCount() {
        return players.size();
    }
    public synchronized int addPlayer(Socket socket) {
        Snake snake = new Snake();
        players.put(socket, snake);
        snakes.add(snake);
        return playerCount() - 1;
    }
    public int getSpeed() {
        return Snake.SPEED_1;
    }
    public void run() {
        while (running) {
            update();
            syncClient();
            try {
                Thread.sleep(getSpeed());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void start() {
        information = INFORMATION_NONE;
        running = true;
        thread.start();
    }
    public void stop() {
        running = false;
    }
    public void interrupt() {
        information = INFORMATION_INTERRUPTED;
    }
    public void setWaiting() {
        information = INFORMATION_WAIT;
    }
    public void update() {
        showMessage(null, Color.BLACK);
        if (isFirstRun) {
            isFirstRun = false;
            initSnake();
            genDessert();
        } else if (isStarted) {
            if(isPaused) {
                showMessage("Pause", Color.BLACK);
            } else if(isCrushed()) {
                showMessage("Game Over", Color.BLACK);
            } else{
                changeSnakeLocation();
            }
        }
    }
    private void syncClient() {
        for(Socket socket : players.keySet()) {
            messageListener.onMessage(socket, null);
        }
    }
    public void initSnake() {
        for(Snake snake : snakes) {
            snake.getHead().setX(Util.random(0, COORD_X_MAX));
            snake.getHead().setY(Util.random(0, COORD_Y_MAX));
            snake.setWinner(true);
            snake.setSpeed(Snake.SPEED_3);
            if(snake.getHead().getX() >= COORD_X_MAX /2) {
                snake.setDirection(Snake.DIRECTION_LEFT);
            } else {
                snake.setDirection(Snake.DIRECTION_RIGHT);
            }
            snake.setBody(new LinkedList<>());
        }
    }
    public void genDessert() {
        int xRandom, yRandom;
        do {
            xRandom = Util.random(0, COORD_X_MAX);
            yRandom = Util.random(0, COORD_Y_MAX);
        } while (isDotOverlapWithHeads(xRandom, yRandom));
        dessert.setX(xRandom);
        dessert.setY(yRandom);
    }
    private boolean isDotOverlapWithHeads(int x, int y) {
        for(Snake snake : snakes) {
            if(x == snake.getHead().getX() && y == snake.getHead().getY()) {
                return true;
            }
        }
        return false;
    }
    public synchronized void changeSnakeLocation() {
        for (Snake snake : snakes) {
            int xPrevious = snake.getHead().getX();
            int yPrevious = snake.getHead().getY();
            switch (snake.getDirection()) {
                case Snake.DIRECTION_UP:
                    snake.getHead().setY(yPrevious - 1);
                    break;
                case Snake.DIRECTION_DOWN:
                    snake.getHead().setY(yPrevious + 1);
                    break;
                case Snake.DIRECTION_LEFT:
                    snake.getHead().setX(xPrevious - 1);
                    break;
                case Snake.DIRECTION_RIGHT:
                    snake.getHead().setX(xPrevious + 1);
                    break;
                default:
            }
            if (isEncountered(snake)) {
                genDessert();
                snake.increaseScore();
                snake.getBody().addFirst(new Dot(xPrevious, yPrevious));
            } else {
                snake.getBody().addFirst(new Dot(xPrevious, yPrevious));
                snake.getBody().removeLast();
            }
        }
    }
    public boolean isEncountered(Snake snake) {
        if (snake.getHead().getX() == dessert.getX()
                && snake.getHead().getY() == dessert.getY()) {
            return true;
        } else {
            return false;
        }
    }
    public boolean isCrushed() {
        for (Snake snake : snakes) {
            boolean isCrushedByBorder = snake.getHead().getX() > COORD_X_MAX
                    || snake.getHead().getX() < 0
                    || snake.getHead().getY() > COORD_Y_MAX
                    || snake.getHead().getY() < 0;
            if (isCrushedByBorder) {
                information = GameInfo.INFORMATION_HIT_WALL;
                snake.setWinner(false);
                isCrushed = true;
                return true;
            }
            boolean isCrushedByItself = false;
            for (int i = 0; i < snake.getBody().size(); i++) {
                if (snake.getHead().getX() == snake.getBody().get(i).getX()
                        && snake.getHead().getY() == snake.getBody().get(i).getY() && !isCrushedByItself) {
                    isCrushedByItself = true;
                }
            }
            if (isCrushedByItself) {
                information = GameInfo.INFORMATION_KILL_ITSELF;
                snake.setWinner(false);
                isCrushed = true;
                return true;
            }
        }
        isCrushed = false;
        return false;
    }
    public synchronized void processKey(Socket socket, int keyCode) {
        Snake snake = players.get(socket);
        int direction = snake.getDirection();
        switch (keyCode) {
            case KeyEvent.VK_UP:
                if (isStarted && !isPaused && !isCrushed()) {
                    if (direction != Snake.DIRECTION_UP && direction != Snake.DIRECTION_DOWN) {
                        snake.setDirection(Snake.DIRECTION_UP);
                        changeSnakeLocation();
                    }
                }
                break;
            case KeyEvent.VK_DOWN:
                if (isStarted && !isPaused && !isCrushed()) {
                    if (direction != Snake.DIRECTION_UP && direction != Snake.DIRECTION_DOWN) {
                        snake.setDirection(Snake.DIRECTION_DOWN);
                        changeSnakeLocation();
                    }
                }
                break;
            case KeyEvent.VK_LEFT:
                if (isStarted && !isPaused && !isCrushed()) {
                    if (direction != Snake.DIRECTION_LEFT && direction != Snake.DIRECTION_RIGHT) {
                        snake.setDirection(Snake.DIRECTION_LEFT);
                        changeSnakeLocation();
                    }
                }
                break;
            case KeyEvent.VK_RIGHT:
                if (isStarted && !isPaused && !isCrushed()) {
                    if (direction != Snake.DIRECTION_LEFT && direction != Snake.DIRECTION_RIGHT) {
                        snake.setDirection(Snake.DIRECTION_RIGHT);
                        changeSnakeLocation();
                    }
                }
                break;
            case KeyEvent.VK_ENTER:
                if (isCrushed()) {
                    initSnake();
                    isFirstRun = true;
                    isStarted = false;
                    isPaused = false;
                    isCrushed = false;
                    information = GameInfo.INFORMATION_NONE;

                } else {
                    isStarted = true;
                }
                break;
            case KeyEvent.VK_SPACE:
                if (isStarted && !isCrushed()) {
                    isPaused = !isPaused;
                }
                break;
            case KeyEvent.VK_F1:
                snake.setSpeed(Snake.SPEED_1);
                break;
            case KeyEvent.VK_F2:
                snake.setSpeed(Snake.SPEED_2);
                break;
            case KeyEvent.VK_F3:
                snake.setSpeed(Snake.SPEED_3);
                break;
            case KeyEvent.VK_F4:
                snake.setSpeed(Snake.SPEED_4);
                break;
            case KeyEvent.VK_F5:
                snake.setSpeed(Snake.SPEED_5);
                break;
            default:
        }
    }
}
class Util {
    public static int random(int from, int to) {
        return (int) (from + Math.random() * (to+1));
    }
}