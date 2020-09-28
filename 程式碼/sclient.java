package game;
import com.google.gson.*;
import java.util.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;
import javax.swing.*;
public class sclient extends JFrame{
    private GamePanel p1 = new GamePanel();
    private InformationPanel p2 = new InformationPanel();
    private Client client;
    public static void main(String[] args) throws Exception {
        JFrame frame = new sclient();
    }
    public sclient() throws Exception {
        setLayout(new BorderLayout());
        add(p1.getPanelBody(),BorderLayout.CENTER);
        add(p2,BorderLayout.EAST);
        this.setTitle("Snake");
        this.setSize(1100, 800);
        this.setVisible(true);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        askIpPort(false);
        p1.setListener((int e) -> {
            client.sendMessage(String.format("{\"keyCode\":%d}", e));
        });
        client.setMessageListener((source, message) -> {
            JsonElement jElement = new JsonParser().parse(message);
            JsonObject jObject = jElement.getAsJsonObject();
            if(jObject.has("handShake")) {
                p1.setId(jObject.get("handShake").getAsInt());
                return;
            }
            System.out.println(message);
            p1.loadFromJson(message);
            p1.update();
            p2.setInformation(p1.getInformation());
            p2.setScores(p1.getScores());
            p2.update();
        });
        client.start();
        client.sendMessage("{\"handShake\":\"hello\"}");
    }
    public boolean askIpPort(boolean disableIp) {
        JTextField textFieldIp = new JTextField("127.0.0.1");
        JTextField textFieldPort = new JTextField("5566");
        Object[] inputFields = {"Server IP : ", textFieldIp,"Server Port : ", textFieldPort};
        if(disableIp) {
            inputFields = new Object[2];
            inputFields[0] = "Server Port : ";
            inputFields[1] = textFieldPort;
        }
        while(true) {
            int option = JOptionPane.showConfirmDialog(this, inputFields, "Connect", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (option == JOptionPane.OK_OPTION) {
                int port = 0;
                try {
                    port = Integer.parseInt(textFieldPort.getText());
                    onIpPortSet(disableIp, textFieldIp.getText(), port);
                    break;
                } catch (Exception e) {
                    alert("Error!!!");
                }
            } else {
                return false;
            }
        }
        return true;
    }
    public void alert(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
    public void onIpPortSet(boolean disableIp, String ip, int port) {
        try {
            client = new Client(ip, port);
        } catch (IOException e) {
            alert("Connection failed!");
            System.exit(0);
        }
    } 
}
class GamePanel extends GameInfo implements Updateable {
    private KeyPressListener listener;
    private PanelBody panelBody;
    private int id = 0;
    public void update() {
        panelBody.update();
    }
    public GamePanel() {
        panelBody = new PanelBody();
        information = INFORMATION_WAIT;
        update();
    }
    public void setId(int id) {
        this.id = id;
    }
    public void setListener(KeyPressListener listener) {
        this.listener = listener;
    }
    public void processKey(int keyCode) {
        if(information != INFORMATION_WAIT) {
            listener.onKeyPress(keyCode);
        }
    }
    public int getInformation() {
        return information;
    }
    public PanelBody getPanelBody() {
        return panelBody;
    }
    public interface KeyPressListener {
        void onKeyPress(int keyCode);
    }
    public ArrayList<Integer> getScores() {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(snakes.get(id).getScore());
        for(int i = 0; i < snakes.size(); i++) {
            if(i != id) {
                list.add(snakes.get(i).getScore());
            }
        }
        return list;
    }
    public class PanelBody extends JPanel implements Updateable {
        public PanelBody() {
            setFocusable(true);
            setFont(new Font("Californian FB", Font.BOLD, 80));
            addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    processKey(e.getKeyCode());
                }
            });
        }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int fullSide = HALF_SIDE * 2;
            int pixelX = fullSide / COORD_X_MAX;
            int pixelY = fullSide / COORD_Y_MAX;
            int xCentre = getWidth() / 2;
            int yCentre = getHeight() / 2;
            int leftTopX = xCentre - HALF_SIDE;
            int leftTopY = yCentre - HALF_SIDE;
            g.drawRect(leftTopX, leftTopY, pixelX*COORD_X_MAX+30, pixelY*COORD_Y_MAX+30);
            //for(int i = 0; i <= COORD_X_MAX; i++) {
                //for(int j = 0; j <= COORD_Y_MAX; j++) {
                    //g.drawRect(leftTopX + i * pixelX, leftTopY + j * pixelY, pixelX, pixelY);
                //}
            //}
            int n = 0;
            for(Snake snake : snakes) {
                int headX = leftTopX + snake.getHead().getX() * pixelX;
                int headY = leftTopY + snake.getHead().getY() * pixelY;
                if(n == id) {
                    g.setColor(Color.PINK);
                } else {
                    g.setColor(new Color(Color.PINK.getRed(), Color.PINK.getGreen(), Color.PINK.getBlue(), 100));
                }
                g.fillOval(headX, headY, pixelX, pixelY);
                n++;
            }
            int desX = leftTopX + dessert.getX() * pixelX;
            int desY = leftTopY + dessert.getY() * pixelY;
            g.setColor(Color.DARK_GRAY);
            g.fillOval(desX, desY, pixelX, pixelY);
            n = 0;
            for(Snake snake : snakes) {
                if(n == id) {
                    g.setColor(Color.MAGENTA);
                } else {
                    g.setColor(new Color(Color.MAGENTA.getRed(), Color.MAGENTA.getGreen(), Color.MAGENTA.getBlue(), 100));
                }
                for (int i = 0; i < snake.getBody().size(); i++) {
                    int x = leftTopX + snake.getBody().get(i).getX() * pixelX;
                    int y = leftTopY + snake.getBody().get(i).getY() * pixelY;
                    g.fillOval(x, y, pixelX, pixelY);
                }
                n++;
            }
            if(currentMessage != null) {
                g.setColor(currentMessageColor);
                FontMetrics fm = g.getFontMetrics();
                int stringWidth = fm.stringWidth(currentMessage);
                int stringAscent = fm.getAscent();
                int xCoordinate = xCentre - stringWidth / 2;
                int yCoordinate = yCentre - stringAscent / 2;
                g.drawString(currentMessage, xCoordinate, yCoordinate);
            }
        }
        public void update() {
            if(information == INFORMATION_INTERRUPTED) {
                JOptionPane.showMessageDialog(this,"The opponent exit.Please restart.");
                System.exit(0);
            } else if(information == INFORMATION_WAIT) {
                showMessage("Wait for opponent...", Color.BLACK);
                return;
            }
            if(!isPaused && isStarted && !isCrushed) {
                requestFocus();
            }
            if(isCrushed) {
                if(snakes.get(id).isWinner()) {
                    showMessage("~Win~", Color.GREEN);
                } else {
                    showMessage("!Loose!", Color.RED);
                }
            }
            repaint();
        }
    }
}
class InformationPanel extends JPanel implements Updateable {
    private Box box = Box.createVerticalBox();
    private JLabel[] help = new JLabel[5];
    private JLabel score = new JLabel("分數：");
    private JLabel show = new JLabel();
    private int scoreCount = 0;
    private int information = GameInfo.INFORMATION_NONE;
    private ArrayList<Integer> scores = new ArrayList<>();
    public InformationPanel(){
        for(int i = 0;i < help.length;i++)
            help[i] = new JLabel();
        Font font1 = new Font("DialogInput", Font.BOLD, 20);
        Font font2 = new Font("DialogInput", Font.BOLD + Font.ITALIC, 25);
        for(int i = 0;i < help.length;i++)
            help[i].setFont(font1);
        score.setFont(font2);
        score.setForeground(Color.BLUE);
        show.setFont(font2);
        show.setForeground(Color.RED);
        help[0].setText("[Enter] 開始遊戲 or 重新開始");
        help[1].setText("[方向鍵] 操控方向");
        help[2].setText("[space] 暫停遊戲");
        add(box);
        box.add(Box.createVerticalStrut(150));
        for(int i = 0;i < help.length;i++){
            box.add(help[i]);
            box.add(Box.createVerticalStrut(10));
        }
        box.add(Box.createVerticalStrut(90));
        box.add(score);
        box.add(Box.createVerticalStrut(150));
        box.add(show);
        update();
    }
    public void setInformation(int information) {
        this.information = information;
    }
    public void setScores(ArrayList<Integer> scores) {
        this.scores = scores;
    }
    public void update() {
        StringBuilder string1 = new StringBuilder("<html>分數：<br/>");
        for(int i = 0; i < scores.size(); i++) {
            if(i == 0) {
                string1.append(String.format("您：%d<br/>", scores.get(i)));
            } else {
                string1.append(String.format("對手：%d<br/>", scores.get(i)));
            }
        }
        string1.append("</html>");
        score.setText(string1.toString());
        String string2 = null;
        switch(information){
            case GameInfo.INFORMATION_NONE:
                break;
            case GameInfo.INFORMATION_HIT_WALL:
                string2 = "撞穿牆了!";
                break;
            case GameInfo.INFORMATION_KILL_ITSELF:
                string2 = "撞到自己了!";
                break;
            default:
        }
        show.setText(string2);
    }
}
class Client extends socket {
    public Client(String ip, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(ip, port));
        this.init(socket);
    }
    public Client(String ip, int port, MessageListener listener) throws IOException {
        this(ip, port);
        this.setMessageListener(listener);
    }
}
abstract class GameInfo {
    public static final int INFORMATION_NONE = 0;
    public static final int INFORMATION_HIT_WALL = 1;
    public static final int INFORMATION_KILL_ITSELF = 2;
    public static final int INFORMATION_WAIT = 3;
    public static final int INFORMATION_INTERRUPTED = 4;
    public static final int PER_UNIT_LENGTH = 20;
    public static final int MULTIPLE = 15;
    public static final int HALF_SIDE = MULTIPLE * PER_UNIT_LENGTH;
    public static final int COORD_X_MAX = PER_UNIT_LENGTH;
    public static final int COORD_Y_MAX = PER_UNIT_LENGTH;
    protected boolean isFirstRun = true;
    protected boolean isStarted = false;
    protected boolean isPaused = false;
    protected int information = GameInfo.INFORMATION_WAIT;
    protected ArrayList<Snake> snakes = new ArrayList<>();
    protected Dot dessert = new Dot();
    protected String currentMessage = null;
    protected Color currentMessageColor = Color.BLACK;
    protected boolean isCrushed = false;
    public void reset() {
        isFirstRun = true;
        isStarted = false;
        isPaused = false;
        information = GameInfo.INFORMATION_WAIT;
        snakes = new ArrayList<>();
        dessert = new Dot();
        currentMessage = null;
        currentMessageColor = Color.BLACK;
        isCrushed = false;
    }
    class DataPacket {
        protected boolean isFirstRun;
        protected boolean isStarted;
        protected boolean isPaused;
        protected int information;
        protected ArrayList<Snake> snakes;
        protected Dot dessert;
        protected String currentMessage;
        protected Color currentMessageColor;
        protected boolean isCrushed;
        DataPacket() {
            isFirstRun = GameInfo.this.isFirstRun;
            isStarted = GameInfo.this.isStarted;
            isPaused = GameInfo.this.isPaused;
            information = GameInfo.this.information;
            snakes = GameInfo.this.snakes;
            dessert = GameInfo.this.dessert;
            currentMessage = GameInfo.this.currentMessage;
            currentMessageColor = GameInfo.this.currentMessageColor;
            isCrushed = GameInfo.this.isCrushed;
        }
    }
    public String toJson() {
        Gson gson = new Gson();
        DataPacket dataPacket = new DataPacket();
        return gson.toJson(dataPacket);
    }
    public void loadFromJson(String json) {
        try {
            Gson gson = new Gson();
            JsonElement jElement = new JsonParser().parse(json);
            JsonObject jObject = jElement.getAsJsonObject();
            this.isFirstRun = jObject.get("isFirstRun").getAsBoolean();
            this.isStarted = jObject.get("isStarted").getAsBoolean();
            this.isPaused = jObject.get("isPaused").getAsBoolean();
            this.isCrushed = jObject.get("isCrushed").getAsBoolean();
            this.information = jObject.get("information").getAsInt();
            this.snakes = new ArrayList<>();
            JsonArray jArray = jObject.get("snakes").getAsJsonArray();
            for(JsonElement ele : jArray) {
                this.snakes.add(gson.fromJson(ele, Snake.class));
            }
            JsonElement desEle = jObject.get("dessert");
            this.dessert = gson.fromJson(desEle, Dot.class);
            if(jObject.has("currentMessage")){
                this.currentMessage = jObject.get("currentMessage").getAsString();
            } else {
                this.currentMessage = null;
            }
            JsonElement colEle = jObject.get("currentMessageColor");
            this.currentMessageColor = gson.fromJson(colEle, Color.class);
        } catch (Exception e) {
            System.out.println("JSON parse Error");
            e.printStackTrace();
        }
    }
    public void showMessage(String message, Color color) {
        this.currentMessage = message;
        this.currentMessageColor = color;
    }
}
class Snake{
    public static final int DIRECTION_UP = 1;
    public static final int DIRECTION_DOWN = 2;
    public static final int DIRECTION_LEFT = 3;
    public static final int DIRECTION_RIGHT = 4;
    public static final int SPEED_1 = 300;
    public static final int SPEED_2 = 200;
    public static final int SPEED_3 = 150;
    public static final int SPEED_4 = 100;
    public static final int SPEED_5 = 30;
    private int direction = DIRECTION_RIGHT;
    private int speed = SPEED_3;
    private Dot head = new Dot();
    private LinkedList<Dot> body = new LinkedList<Dot>();
    private int score;
    private boolean isWinner = true;
    public Dot getHead(){
        return head;
    }
    public LinkedList<Dot> getBody(){
        return body;
    }
    public int getDirection(){
        return direction;
    }
    public int getSpeed(){
        return speed;
    }
    public void setBody(LinkedList<Dot> body){
        this.body = body;
    }
    public void setDirection(int direction){
        this.direction = direction;
    }
    public void setSpeed(int speed){
        this.speed = speed;
    }
    public void setWinner(boolean flag) {
        this.isWinner = flag;
    }
    public boolean isWinner() {
        return isWinner;
    }
    public int getScore() {
        return score;
    }
    public void increaseScore() {
        score++;
    }
}
class Dot{
    private int x = 0;
    private int y = 0;
    public Dot(){}
    public Dot(int x, int y){
        this.x = x;
        this.y = y;
    }
    public int getX(){
        return x;
    }
    public int getY(){
        return y;
    }
    public void setX(int x){
        this.x = x;
    }
    public void setY(int y){
        this.y = y;
    }
}
interface Updateable {
    public void update();
}
interface MessageListener {
    void onMessage(Socket source, String message);
}