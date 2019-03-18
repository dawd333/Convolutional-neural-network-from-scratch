import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class Main {
    static final private File dir = new File("resized_photos");

    static final private String[] EXTENSIONS = new String[]{
            "jpg", "jpeg", "png"
    };

    static final private FilenameFilter IMAGE_FILTER = (dir, name) -> {
        for (final String ext : EXTENSIONS) {
            if (name.endsWith("." + ext)) {
                return (true);
            }
        }
        return (false);
    };

    static private String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static void main(String[] args) {
        double[][][][][] images = new double[64][64][64][64][3];
        int[][][] labels = new int[64][64][4];
        int index = 0;
        Utility utility = new Utility();
        String parametersInput = new String();
        try {
            parametersInput = readFile("weights7.txt", Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (dir.isDirectory()) {
            File[] files = dir.listFiles(IMAGE_FILTER);
            assert files != null;
            Collections.shuffle(Arrays.asList(files));
            for (final File f: files) {
                BufferedImage image;
                double sumRed=0;
                double sumGreen=0;
                double sumBlue=0;

                try {
                    image = ImageIO.read(f);

                    int category = Character.getNumericValue(f.getName().charAt(0));
                    labels[index/64][index%64][category-1] = 1;

                    int w = image.getWidth();
                    int h = image.getHeight();

                    for (int i = 0; i < h; i++) {
                        for (int j = 0; j < w; j++) {
                            int pixel = image.getRGB(j, i);
                            int red = (pixel >> 16) & 0xff;
                            int green = (pixel >> 8) & 0xff;
                            int blue = (pixel) & 0xff;
                            images[index/64][index%64][j][i][0] = red;
                            images[index/64][index%64][j][i][1] = green;
                            images[index/64][index%64][j][i][2] = blue;
                            sumRed+=red;
                            sumGreen+=green;
                            sumBlue+=blue;
                        }
                    }

                    double meanRed = sumRed / 4096;
                    double meanGreen = sumGreen / 4096;
                    double meanBlue = sumBlue / 4096;

                    double numRed=0;
                    double numGreen=0;
                    double numBlue=0;

                    for(int i=0; i<h; i++){
                        for(int j=0; j<w; j++){
                            numRed+= pow((images[index/64][index%64][j][i][0] - meanRed), 2);
                            numGreen+= pow((images[index/64][index%64][j][i][1] - meanGreen), 2);
                            numBlue+= pow((images[index/64][index%64][j][i][2] - meanBlue), 2);
                        }
                    }

                    double stdRed = sqrt(numRed/4096);
                    double stdGreen = sqrt(numGreen/4096);
                    double stdBlue = sqrt(numBlue/4096);

                    for(int i=0; i<h; i++){
                        for(int j=0; j<w; j++){
                            images[index/64][index%64][j][i][0] -= meanRed;
                            images[index/64][index%64][j][i][0] /= stdRed;
                            images[index/64][index%64][j][i][1] -= meanGreen;
                            images[index/64][index%64][j][i][1] /= stdGreen;
                            images[index/64][index%64][j][i][2] -= meanBlue;
                            images[index/64][index%64][j][i][2] /= stdBlue;
                        }
                    }

                } catch (IOException e){
                    e.printStackTrace();
                }
                index++;
            }
        }
        Parameters parameters = utility.train(images, labels, parametersInput);
        ShutdownThread shutdownThread = new ShutdownThread(parameters);
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }
}
