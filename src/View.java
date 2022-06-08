import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.List;
import java.util.*;
import javax.swing.*;

/**
 * Pixel based 2D shadow mapping Test.
 *
 * @author Leonardo Ono (ono.leo80@gmail.com)
 */
public class View extends JPanel {

    private static final int W = 800, H = 600;
    private final BufferedImage offscreen;
    private final int[] offscreenData;
    
    private final int shadowMapResolution = 512;
    private final boolean[] occlusion = new boolean[shadowMapResolution];
    private final int occlusionIndicesSide = 1200;
    private final int[][] occlusionIndices 
                = new int[occlusionIndicesSide][occlusionIndicesSide];

    private final List<Rectangle> obstacles = new ArrayList<>();
    private final Ellipse2D clip = new Ellipse2D.Double();
    private int mouseX, mouseY;
    
    public View() {
        offscreen = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        offscreenData 
            = ((DataBufferInt) offscreen.getRaster().getDataBuffer()).getData();
        // precompute all occlusion indices
        for (int y = 0; y < occlusionIndicesSide; y++) {
            for (int x = 0; x < occlusionIndicesSide; x++) {
                double a = Math.PI + Math.atan2(y - occlusionIndicesSide / 2
                                              , x - occlusionIndicesSide / 2);
                
                int angleIndex = (int) ((shadowMapResolution - 1) 
                                                        * a / (Math.PI * 2));
                
                occlusionIndices[y][x] = angleIndex;
            }
        }
        // create box walls
        for (int r = 0; r < 50; r++) {
            int x = (int) (W * Math.random());
            int y = (int) (W * Math.random());
            int w = (int) (20 + 50 * Math.random());
            int h = (int) (20 + 50 * Math.random());
            obstacles.add(new Rectangle(x, y, w, h));
        }
    }

    private static final int[][] DIRECTIONS 
                                    = {{1, 0}, {0, -1}, {-1, 0}, {0, 1}};
    
    private void computeAndDrawShadows() {
        Arrays.fill(occlusion, false);

        int row = occlusionIndicesSide / 2;
        int col = occlusionIndicesSide / 2;
        for (int i = 0; i < occlusionIndicesSide; i++) {
            int s = (i / 2) + 1;
            for (int j = 0; j < s; j++) {
                int px = mouseX + col - occlusionIndicesSide / 2;
                int py = mouseY + row - occlusionIndicesSide / 2;
                if (!(px < 0 || py < 0 || px > offscreen.getWidth() - 1 
                        || py > offscreen.getHeight() - 1)) {
                    
                    if (offscreenData[px + W * py] == Color.BLACK.getRGB()) {
                        occlusion[occlusionIndices[row][col]] = true;
                    } else if (occlusion[occlusionIndices[row][col]] 
                            || occlusion[(occlusionIndices[row][col] + 1) 
                                % occlusion.length] 
                            || (occlusionIndices[row][col] > 0 
                                && occlusion[occlusionIndices[row][col] - 1])) {
                        
                        offscreenData[px + W * py] = 0xff000000;
                    } else {
                        offscreenData[px + W * py] = 0xffffffff;
                    }
                }
                row += DIRECTIONS[i % 4][0];
                col += DIRECTIONS[i % 4][1];
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setBackground(Color.BLACK);
        g.clearRect(0, 0, getWidth(), getHeight());
        Graphics2D ig = offscreen.createGraphics();
        ig.setBackground(Color.WHITE);
        ig.clearRect(0, 0, offscreen.getWidth(), offscreen.getHeight());
        ig.setColor(Color.BLACK);
        obstacles.forEach(r -> ig.fillRect(r.x, r.y, r.width, r.height));
        computeAndDrawShadows();
        clip.setFrame(mouseX - occlusionIndicesSide / 4
                    , mouseY - occlusionIndicesSide / 4
                    , occlusionIndicesSide / 2
                    , occlusionIndicesSide / 2);
        
        g.setClip(clip);
        g.drawImage(offscreen, 0, 0, null);
    }

    private class MouseHandler extends MouseAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();
            repaint();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            View view = new View();
            view.setPreferredSize(new Dimension(W, H));
            view.addMouseMotionListener(view.new MouseHandler());
            JFrame frame = new JFrame("Pixel based 2D shadow mapping Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(view);
            frame.pack();
            frame.setVisible(true);
        });
    }
    
}
