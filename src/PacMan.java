import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;

public class PacMan extends JPanel implements ActionListener, KeyListener {
    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;

        int startX;
        int startY;
        char direction = 'U'; // U, D, L, R
        int velocityX = 0;
        int velocityY = 0;

        boolean isScared = false;
        long scaredStartTimer = 0; // Timer since the scared Ghost have been active

        boolean released = false;
        long releaseTime = 0;

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                }
            }
        }

        void updateVelocity() {
            if (this.direction == 'U') {
                this.velocityX = 0;
                this.velocityY = -TILE_SIZE/4;
            }
            else if (this.direction == 'D') {
                this.velocityX = 0;
                this.velocityY = TILE_SIZE/4;
            }
            else if (this.direction == 'L') {
                this.velocityX = -TILE_SIZE/4;
                this.velocityY = 0;
            }
            else if (this.direction == 'R') {
                this.velocityX = TILE_SIZE/4;
                this.velocityY = 0;
            }

            if (isScared) {
                this.velocityX /= 2;
                this.velocityY /= 2;
            }
        }

        char nextDirection = 'U';
        void tryUpdateDirection(HashSet<Block> walls) {
        // only allow turns if aligned on tile center
            if (x % TILE_SIZE == 0 && y % TILE_SIZE == 0) {
            // save current position
                int oldX = x;
                int oldY = y;
                char prevDirection = direction;

                // simulating the next move
                direction = nextDirection;
                updateVelocity();
                x += velocityX;
                y += velocityY;

                boolean canTurn = true;
                for (Block wall : walls) {
                    if (collision(this, wall)) {
                        canTurn = false;
                        break;
                    }
                }

                if (!canTurn) {
                // revert if blocked
                    direction = prevDirection;
                    x = oldX;
                    y = oldY;
                    updateVelocity();
                }
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    private int ROW_COUNT = 21;
    private int COLUMN_COUNT = 19;
    private int TILE_SIZE = 32;
    private int boardWidth = COLUMN_COUNT * TILE_SIZE;
    private int boardHeight = ROW_COUNT * TILE_SIZE;

    private Image wallImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;
    private Image pacManDownImage;
    private Image pacManLeftImage;
    private Image pacManRightImage;
    private Image pacManUpImage;

    private Image scaredGhostImage;

    private Image powerPelletsImage;


    //X = wall, O = skip, P = pac man, ' ' = food
    //Ghosts: b = blue, o = orange, p = pink, r = red
    //Power Pellets: T = Power Pellet
    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "X        X        X",
        "XTXX XXX X XXX XXTX",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXrXX X XXXX",
        "O      XbpoX       O",
        "XXXX X XXXXX X XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "XT X     P     X TX",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "X                 X",
        "XXXXXXXXXXXXXXXXXXX" 
    };

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    HashSet<Block> powerPellets;
    Block pacman;
    

    //To make the game work with the repaint
    Timer gameLoop;
    char[] directions = {'U', 'D', 'L', 'R'}; //up, down, left, right
    Random random = new Random();

    //Lives and score
    int score = 0;
    int lives = 3;
    long highScore;
    Image pacmanLivesImage;
    final int LIFE_ICON_SIZE = 20;
    final int LIFE_ICON_SPACE = 30;

    boolean gameOver = false;

    final int SCARED_DURATION = 7000; // 7 seconds

    PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        //loading images
        wallImage = new ImageIcon(getClass().getResource("./wall.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();

        pacManDownImage = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();
        pacManLeftImage = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacManRightImage = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();
        pacManUpImage = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();

        pacmanLivesImage = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();

        scaredGhostImage = new ImageIcon(getClass().getResource("./scaredGhost.png")).getImage();

        powerPelletsImage = new ImageIcon(getClass().getResource("./powerFood.png")).getImage();
    
        loadMap();
        long gameStartTime = System.currentTimeMillis();
        int delayInterval = 5000; // 5 seconds delay between ghosts
        int index = 0;
        for (Block ghost : ghosts) {
            ghost.releaseTime = gameStartTime + index * delayInterval;
            ghost.released = false;
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
            index++;
        }

        //30 milliseconds in between frames
        gameLoop = new Timer(30, this); // 33fps (1000/30)
        gameLoop.start();

    }

    //loading the map components
    public void loadMap() {
        walls = new HashSet<Block>();
        foods = new HashSet<Block>();
        ghosts = new HashSet<Block>();
        powerPellets = new HashSet<Block>();

        for (int r = 0; r < ROW_COUNT; r++) {
            for (int c = 0; c < COLUMN_COUNT; c++) {
                String row = tileMap[r];
                char tileMapChar = row.charAt(c);

                int x = c * TILE_SIZE;
                int y = r * TILE_SIZE;

                if (tileMapChar == 'X') { //Means a wall
                    Block wall = new Block(wallImage, x, y, TILE_SIZE, TILE_SIZE);
                    walls.add(wall);
                }
                else if (tileMapChar == 'b') { //Blue Ghost
                    Block ghost = new Block(blueGhostImage, x, y, TILE_SIZE, TILE_SIZE);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'o') { //Orange Ghost
                    Block ghost = new Block(orangeGhostImage, x, y, TILE_SIZE, TILE_SIZE);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'p') { //Pink Ghost
                    Block ghost = new Block(pinkGhostImage, x, y, TILE_SIZE, TILE_SIZE);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'r') { //Red Ghost
                    Block ghost = new Block(redGhostImage, x, y, TILE_SIZE, TILE_SIZE);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'P') { //PacMan
                    pacman = new Block(pacManRightImage, x, y, TILE_SIZE, TILE_SIZE);
                }
                else if (tileMapChar == 'T') { //Power Pellet
                    Block powerPellet = new Block(powerPelletsImage, x, y, TILE_SIZE, TILE_SIZE);
                    powerPellets.add(powerPellet);
                }
                else if (tileMapChar == ' ') { //Food
                    Block food = new Block(null, x + 14, y + 14, 4, 4);
                    foods.add(food);
                }
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        //Drawing the lives
        int topMargin = 20;
        int leftMargin = TILE_SIZE/2;
        int spacingBetweenLivesAndText = 10;
        for (int i = 0; i < lives; i++) {
            g.drawImage(pacmanLivesImage, 10 + i * LIFE_ICON_SPACE, boardHeight - LIFE_ICON_SIZE - 10
                , LIFE_ICON_SIZE, LIFE_ICON_SIZE, null);
        }

        for (Block ghost : ghosts) {
            g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
        }

        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }

        for (Block powerPellet : powerPellets) {
            g.drawImage(powerPelletsImage, powerPellet.x + 12, powerPellet.y + 14, 8, 8, null);
        }

        g.setColor(Color.WHITE);
        for (Block food : foods) {
            g.fillRect(food.x, food.y, food.width, food.height);
        }
        //Score updater
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        if (gameOver) {
            g.drawString("Game Over - Score: " + String.valueOf(score) + " | High Score: " + String.valueOf(highScore), leftMargin, topMargin);
        }
        else {
            int lifeStartX = leftMargin;
            for (int i = 0; i < lives; i++) {
                g.drawImage(pacmanLivesImage, lifeStartX + i * (LIFE_ICON_SIZE + 5), topMargin - LIFE_ICON_SIZE + 4, LIFE_ICON_SIZE, LIFE_ICON_SIZE, null);
            }
            int textStartX = lifeStartX + lives * (LIFE_ICON_SIZE + 5) + spacingBetweenLivesAndText;
            g.drawString(" Score: " + String.valueOf(score) + " | High Score: " + String.valueOf(highScore), textStartX, topMargin);
        }
    }

    public void move() {
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        //Checking for collision on walls
        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
            }
        }

        //checking power pellet collisions with pacman
        Block eatenPellet = null;
        for (Block powerPellet : powerPellets) {
            if (collision(pacman, powerPellet)) {
                eatenPellet = powerPellet;

                for (Block ghost : ghosts) {
                    ghost.isScared = true;
                    ghost.scaredStartTimer = System.currentTimeMillis();
                    ghost.image = scaredGhostImage;
                }
            }
        }
        powerPellets.remove(eatenPellet);

        //"Teleporters" on the sides of the game to teleport pacman to the other side
        if (pacman.y == TILE_SIZE*9) {
            if (pacman.x <= 0) {
                pacman.updateDirection('L');
                pacman.x = boardWidth - pacman.width;
            }
            else if (pacman.x >= pacman.width + boardWidth) {
                pacman.updateDirection('R');
                pacman.x = 0;
            }
        }

        //check ghost collisions
        long currentTimeRelease = System.currentTimeMillis();
        for (Block ghost : ghosts) {
            if (!ghost.released) {
                if (currentTimeRelease >= ghost.releaseTime) {
                    ghost.released = true;
                }
                else {
                    continue;
                }
            }

            if (ghost.y == TILE_SIZE*9 && ghost.direction != 'U' && ghost.direction != 'D') {
                char ghostStuck[] = {'U', 'D'};
                char ghostMove = ghostStuck[random.nextInt(2)];
                ghost.updateDirection(ghostMove);
            }
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;
            for (Block wall : walls) {
                if (collision(wall, ghost)) {
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    char newDirection = directions[random.nextInt(4)];
                    ghost.updateDirection(newDirection);
                }
            }

            //Checking pacman and ghost collision
            if (ghost.released && collision(pacman, ghost)) {
                if (ghost.isScared) {
                    //Eating the ghost
                    score += 200;
                    //Updating highScore
                    if (score > highScore) {
                        highScore = score;
                    }
                    //Resetting the eaten ghost back to orignal location
                    ghost.x = ghost.startX;
                    ghost.y = ghost.startY;
                    ghost.isScared = false;
                    //Reset to original image
                    if (tileMap[ghost.startY / TILE_SIZE].charAt(ghost.startX / TILE_SIZE) == 'b') {
                        ghost.image = blueGhostImage;
                    } 
                    else if (tileMap[ghost.startY / TILE_SIZE].charAt(ghost.startX / TILE_SIZE) == 'o') {
                        ghost.image = orangeGhostImage;
                    } 
                    else if (tileMap[ghost.startY / TILE_SIZE].charAt(ghost.startX / TILE_SIZE) == 'p') {
                        ghost.image = pinkGhostImage;
                    } 
                    else if (tileMap[ghost.startY / TILE_SIZE].charAt(ghost.startX / TILE_SIZE) == 'r') {
                        ghost.image = redGhostImage;
                    }
                    break;
                }
                if (!gameOver && lives > 0) {
                    lives--;
                    resetPositions();
                    resetGhostImage(ghosts);

                    if (lives <= 0) {
                        gameOver = true;
                        return;
                    }
                }
                break;
            }
        }

        //Resetting ghost after the timer reaches 7 seconds
        long currentTime = System.currentTimeMillis();
        for (Block ghost : ghosts) {
            if (ghost.isScared && currentTime - ghost.scaredStartTimer > SCARED_DURATION) {
                ghost.isScared = false;

                //Resetting ghost images after 7 seconds
                resetGhostImage(ghosts);
            }
        }

        //check food collision
        Block foodEaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                foodEaten = food;
                score += 10;
                if (score > highScore) {
                    highScore = score;
                }
            }
        }
        foods.remove(foodEaten);

        if (foods.isEmpty()) {
            loadMap();
            resetPositions();
            resetGhostImage(ghosts);
        }
    }

    public void resetGhostImage(HashSet<Block> ghosts) {
        for (Block ghost : ghosts) {
        //Reset to original image
            if (tileMap[ghost.startY / TILE_SIZE].charAt(ghost.startX / TILE_SIZE) == 'b') {
                ghost.image = blueGhostImage;
            } 
            else if (tileMap[ghost.startY / TILE_SIZE].charAt(ghost.startX / TILE_SIZE) == 'o') {
                ghost.image = orangeGhostImage;
            } 
            else if (tileMap[ghost.startY / TILE_SIZE].charAt(ghost.startX / TILE_SIZE) == 'p') {
                ghost.image = pinkGhostImage;
            } 
            else if (tileMap[ghost.startY / TILE_SIZE].charAt(ghost.startX / TILE_SIZE) == 'r') {
                ghost.image = redGhostImage;
            }
        }
    }

    //Resetting positions after death
    public void resetPositions() {
        pacman.reset();
        pacman.velocityX = 0;
        pacman.velocityY = 0;

        long gameStartTime = System.currentTimeMillis();
        int delayInterval = 5000; //5 seconds delay for each ghost
        int index = 0;

        for (Block ghost : ghosts) {
            ghost.reset();
            ghost.released = false;
            ghost.releaseTime = gameStartTime + index * delayInterval;
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
            index++;
        }
    }

    //Checks collision with two blocks
    public boolean collision(Block a, Block b) {
        return  a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        pacman.tryUpdateDirection(walls);
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            loadMap();
            resetPositions();
            lives = 3;
            score = 0;
            gameOver = false;
            gameLoop.start();
        }
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            pacman.image = pacManUpImage;
            pacman.nextDirection = 'U';
        } 
        else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            pacman.image = pacManDownImage;
            pacman.nextDirection = 'D';
        } 
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            pacman.image = pacManLeftImage;
            pacman.nextDirection = 'L';
        } 
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            pacman.image = pacManRightImage;
            pacman.nextDirection = 'R';
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) {
            loadMap();
            resetPositions();
            lives = 3;
            score = 0;
            gameOver = false;
            gameLoop.start();
        }
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            pacman.image = pacManUpImage;
            pacman.nextDirection = 'U';
        } 
        else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            pacman.image = pacManDownImage;
            pacman.nextDirection = 'D';
        } 
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            pacman.image = pacManLeftImage;
            pacman.nextDirection = 'L';
        } 
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            pacman.image = pacManRightImage;
            pacman.nextDirection = 'R';
        }
    }
}
