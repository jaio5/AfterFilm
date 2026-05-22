import java.io.*;
import java.nio.file.*;

public class MakeIco {
    public static void main(String[] args) throws Exception {
        String base = "src/main/resources/static";
        byte[][] images = new byte[2][];
        String[] names = {"favicon-16.png", "favicon-32.png"};
        for (int i = 0; i < names.length; i++) {
            images[i] = Files.readAllBytes(Paths.get(base, names[i]));
            System.out.println("Read: " + names[i] + " (" + images[i].length + " bytes)");
        }
        try (FileOutputStream fos = new FileOutputStream(base + "/favicon.ico")) {
            // ICONDIR: 2 bytes reserved, 2 bytes type, 2 bytes count
            fos.write(new byte[]{0,0}); // reserved
            fos.write(new byte[]{1,0}); // type 1
            fos.write(new byte[]{(byte)images.length,0}); // count

            int dirSize = 6 + images.length * 16;
            int offset = dirSize;
            // write dir entries
            for (int i = 0; i < images.length; i++) {
                byte[] img = images[i];
                // width and height (1..255) — 0 means 256
                int size = (i==0) ? 16 : 32;
                fos.write((byte)size); // width
                fos.write((byte)size); // height
                fos.write((byte)0); // color count
                fos.write((byte)0); // reserved
                // planes (2 bytes)
                fos.write((byte)1);
                fos.write((byte)0);
                // bit count (2 bytes)
                fos.write((byte)32);
                fos.write((byte)0);
                // bytes in resource (4 bytes)
                int len = img.length;
                fos.write(new byte[]{(byte)(len & 0xFF), (byte)((len>>8)&0xFF), (byte)((len>>16)&0xFF), (byte)((len>>24)&0xFF)});
                // image offset (4 bytes)
                fos.write(new byte[]{(byte)(offset & 0xFF), (byte)((offset>>8)&0xFF), (byte)((offset>>16)&0xFF), (byte)((offset>>24)&0xFF)});
                offset += len;
            }
            // write image data
            for (byte[] img : images) {
                fos.write(img);
            }
            System.out.println("Wrote favicon.ico with " + images.length + " PNG entries.");
        }
    }
}
