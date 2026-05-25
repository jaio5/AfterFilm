import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateFavicons {
    public static void main(String[] args) throws Exception {
        String src = "src/main/resources/static/images/logo.png";
        if (args.length > 0) src = args[0];
        String outDir = "src/main/resources/static";

        int[] sizes = {16,32,48,64,128,256};
        BufferedImage srcImg = ImageIO.read(new File(src));
        for (int s : sizes) {
            BufferedImage resized = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(srcImg, 0, 0, s, s, null);
            g.dispose();
            File out = new File(outDir + "/favicon-" + s + ".png");
            ImageIO.write(resized, "PNG", out);
            System.out.println("Wrote: " + out.getPath());
        }

        // Also write favicon-32.png and favicon-16.png specifically
        System.out.println("Done generating favicons.");
    }
}
