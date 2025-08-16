import javax.swing.JFrame;

public class App {
    public static void main(String[] args) throws Exception {
        int ROW_COUNT = 21;
        int COLUMNCOUNT = 19;
        int TILE_SIZE = 32;
        int boardWidth = COLUMNCOUNT * TILE_SIZE;
        int boardHeight = ROW_COUNT * TILE_SIZE;

        JFrame frame = new JFrame("Pac Man");
        // frame.setVisible(true);
        frame.setSize(boardWidth, boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        PacMan pacmanGame = new PacMan();
        frame.add(pacmanGame);
        frame.pack();
        pacmanGame.requestFocus();
        frame.setVisible(true);
    }
}
