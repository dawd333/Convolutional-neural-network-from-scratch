import com.google.gson.Gson;

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

public class TrainingProbability {
    private static final Utility utility = new Utility();
    private static final int convSpatialExtent = 3;
    private static final int convStride = 1;
    private static final int convZeroPadding = 1;
    private static final int maxPoolSpatialExtent = 2;
    private static final int maxPoolStride = 2;

    static final private File dir = new File("resized_photos");

    static final private String[] EXTENSIONS = new String[]{
            "jpg", "jpeg", "png", "gif"
    };

    static final private FilenameFilter IMAGE_FILTER = (dir, name) -> {
        for (final String ext : EXTENSIONS) {
            if (name.endsWith("." + ext)) {
                return (true);
            }
        }
        return (false);
    };

    static private String readFile(String path, Charset encoding) throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static void main(String[] args) {
        int validationSize = 4096;
        double[][][][][] images = new double[64][64][64][64][3];
        int[][][] labels = new int[64][64][4];
        int[] classSize = new int[4];
        double[] classProbability = new double[4];
        double[] bestClassProbability = new double[4];
        int index = 0;
        String parametersInput = new String();
        try {
            parametersInput = readFile("weights8.txt", Charset.defaultCharset());
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
                    classSize[category-1]++;

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
        Gson gson = new Gson();
        Parameters parameters = gson.fromJson(parametersInput, Parameters.class);
        double[][][][] f1 = parameters.getF1();
        double[] b1 = parameters.getB1();
        double[][][][] f2 = parameters.getF2();
        double[] b2 = parameters.getB2();
        double[][] w3 = parameters.getW3();
        double[] b3 = parameters.getB3();
        double[][] w4 = parameters.getW4();
        double[] b4 = parameters.getB4();
        double totalAccuracy = 0;
        double totalBestAccuracy = 0;
        for (int i=0; i<validationSize; i++){
            double[][][] conv1 = utility.convolution(images[i/64][i%64], f1, b1, convSpatialExtent, convStride, convZeroPadding);
            double[][][] conv1ReLU = utility.ReLU3D(conv1);
            double[][][] conv1Reduced = utility.maxPooling(conv1ReLU, maxPoolSpatialExtent, maxPoolStride);
            double[][][] conv2 = utility.convolution(conv1Reduced, f2, b2, convSpatialExtent, convStride, convZeroPadding);
            double[][][] conv2ReLU = utility.ReLU3D(conv2);
            double[][][] conv2Reduced = utility.maxPooling(conv2ReLU, maxPoolSpatialExtent, maxPoolStride);
            double[] flatten = utility.flatten(conv2Reduced);
            double[] full = utility.fullyConnected(flatten, w3, b3);
            double[] fullReLU = utility.ReLU1D(full);
            double[] output = utility.fullyConnected(fullReLU, w4, b4);
            double[] probability = utility.softmax(output);
            int goodIndex = 0;
            for(int j=0; j<4; j++){
                if(labels[i/64][i%64][j] == 1){
                    goodIndex = j;
                    totalAccuracy += probability[j];
                    classProbability[j] += probability[j];
                }
            }
            int estimatedIndex = 0;
            double largest = 0;
            for (int k=0; k<4; k++){
                if(probability[k] > largest){
                    largest = probability[k];
                    estimatedIndex = k;
                }
            }
            if (goodIndex == estimatedIndex){
                bestClassProbability[goodIndex] += 1;
                totalBestAccuracy += 1;
            }
        }

        System.out.println("\nTraining set probability: " + totalAccuracy / validationSize * 100 + "%");
        System.out.println("Golden probability: " + classProbability[0] / classSize[0] * 100 + "%");
        System.out.println("Poodle probability: " + classProbability[1] / classSize[1] * 100 + "%");
        System.out.println("Doberman probability: " + classProbability[2] / classSize[2] * 100 + "%");
        System.out.println("Husky probability: " + classProbability[3] / classSize[3] * 100 + "%");
        System.out.println("\nTraining set 0/1 probability: " + totalBestAccuracy / validationSize * 100 + "%");
        System.out.println("Golden probability: " + bestClassProbability[0] / classSize[0] * 100 + "%");
        System.out.println("Poodle probability: " + bestClassProbability[1] / classSize[1] * 100 + "%");
        System.out.println("Doberman probability: " + bestClassProbability[2] / classSize[2] * 100 + "%");
        System.out.println("Husky probability: " + bestClassProbability[3] / classSize[3] * 100 + "%");
    }
}
