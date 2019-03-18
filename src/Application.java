import com.google.gson.Gson;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class Application {
    private static final Utility utility = new Utility();
    private static final int convSpatialExtent = 3;
    private static final int convStride = 1;
    private static final int convZeroPadding = 1;
    private static final int maxPoolSpatialExtent = 2;
    private static final int maxPoolStride = 2;

    static private String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    static private BufferedImage getFilterImage(double[][][][] f){
        int fWidth = f.length;
        int fHeight = f[0].length;
        int fOldChannels = f[0][0].length;
        int fNewChannels = f[0][0][0].length;
        int gapSize = 5;

        BufferedImage fImage = new BufferedImage(fWidth * fNewChannels + (fNewChannels-1) * gapSize, fHeight * fOldChannels + (fOldChannels-1) * gapSize, BufferedImage.TYPE_INT_RGB);
        for (int newChannel=0; newChannel<fNewChannels; newChannel++){
            for (int oldChannel=0; oldChannel<fOldChannels; oldChannel++){
                double min=0;
                double max=0;
                for (int y=0; y<fHeight; y++){
                    for (double[][][] doubles : f) {
                        if (doubles[y][oldChannel][newChannel] > max) {
                            max = doubles[y][oldChannel][newChannel];
                        } else if (doubles[y][oldChannel][newChannel] < min) {
                            min = doubles[y][oldChannel][newChannel];
                        }
                    }
                }
                max = max - min;
                for (int y=0; y<fHeight; y++){
                    for (int x=0; x<fWidth; x++){
                        int value = (int) Math.round(((f[x][y][oldChannel][newChannel] - min) / max) * 255);
                        fImage.setRGB(fWidth * newChannel + x + gapSize * newChannel, fHeight * oldChannel + y + gapSize * oldChannel, ((value << 16) | (value << 8)) | value);
                    }
                }
            }
        }
        return fImage;
    }

    static private BufferedImage getWeightImage(double[][] w){
        int wWidth = w.length;
        int wHeight = w[0].length;

        BufferedImage wImage = new BufferedImage(wWidth, wHeight, BufferedImage.TYPE_INT_RGB);
        double min=0;
        double max=0;
        for (int y=0; y<wHeight; y++){
            for (double[] doubles : w) {
                if (doubles[y] > max) {
                    max = doubles[y];
                } else if (doubles[y] < min) {
                    min = doubles[y];
                }
            }
        }
        max = max - min;
        for (int y=0; y<wHeight; y++){
            for (int x=0; x<wWidth; x++){
                int value = (int) Math.round(((w[x][y] - min) / max) * 255);
                wImage.setRGB(x, y, ((value << 16) | (value << 8)) | value);
            }
        }
        return wImage;
    }

    static private BufferedImage getImage(double[][][] conv){
        int convWidth = conv.length;
        int convHeight = conv[0].length;
        int convChannels = conv[0][0].length;
        int gapSize = 50;
        BufferedImage convImage = new BufferedImage(convWidth, convHeight * convChannels + (convChannels - 1) * gapSize, BufferedImage.TYPE_INT_RGB);

        for (int channel=0; channel<convChannels; channel++){
            double max = 0;
            double min = 0;
            for (int y=0; y<convHeight; y++){
                for (double[][] doubles : conv) {
                    if (doubles[y][channel] > max) {
                        max = doubles[y][channel];
                    } else if (doubles[y][channel] < min) {
                        min = doubles[y][channel];
                    }
                }
            }
            max = max - min;
            for (int y=0; y<convHeight; y++){
                for (int x=0; x<convWidth; x++){
                    int value = (int) Math.round(((conv[x][y][channel] - min) / max) * 255);
                    convImage.setRGB(x, convHeight * channel + y + gapSize * channel, ((value << 16) | (value << 8)) | value);
                }
            }
        }

        return convImage;
    }

    static private BufferedImage getImage(double[] fc){
        int fcLength = fc.length;

        BufferedImage wImage = new BufferedImage(fcLength, 1, BufferedImage.TYPE_INT_RGB);
        double min=0;
        double max=0;
        for (double value : fc) {
            if (value > max) {
                max = value;
            } else if (value < min) {
                min = value;
            }
        }
        max = max - min;
        for (int x=0; x<fcLength; x++){
            int value = (int) Math.round(((fc[x] - min) / max) * 255);
            wImage.setRGB(x, 0, ((value << 16) | (value << 8)) | value);
        }
        return wImage;
    }

    public static void main(String[] args) {
        try {
            File file = new File("golden.jpeg");
            BufferedImage image = ImageIO.read(file);
            int width = image.getWidth();
            int height = image.getHeight();
            int[][][] imageArray = new int[width][height][4];
            double[][][] imageToPredict = new double[width][height][3];
            double sumRed=0;
            double sumGreen=0;
            double sumBlue=0;

            for (int y=0; y<height; y++) {
                for (int x=0; x<width; x++) {
                    int pixel = image.getRGB(x, y);
                    int alpha = (pixel >> 24) & 0xff;
                    int red = (pixel >> 16) & 0xff;
                    int green = (pixel >> 8) & 0xff;
                    int blue = (pixel) & 0xff;
                    imageArray[x][y][0] = alpha;
                    imageArray[x][y][1] = red;
                    imageArray[x][y][2] = green;
                    imageArray[x][y][3] = blue;
                    imageToPredict[x][y][0] = red;
                    imageToPredict[x][y][1] = green;
                    imageToPredict[x][y][2] = blue;
                }
            }

            double meanRed = sumRed / 4096;
            double meanGreen = sumGreen / 4096;
            double meanBlue = sumBlue / 4096;

            double numRed=0;
            double numGreen=0;
            double numBlue=0;

            for(int y=0; y<height; y++){
                for(int x=0; x<width; x++){
                    numRed+= pow((imageToPredict[x][y][0] - meanRed), 2);
                    numGreen+= pow((imageToPredict[x][y][1] - meanGreen), 2);
                    numBlue+= pow((imageToPredict[x][y][2] - meanBlue), 2);
                }
            }

            double stdRed = sqrt(numRed/4096);
            double stdGreen = sqrt(numGreen/4096);
            double stdBlue = sqrt(numBlue/4096);

            for(int y=0; y<height; y++){
                for(int x=0; x<width; x++){
                    imageToPredict[x][y][0] -= meanRed;
                    imageToPredict[x][y][0] /= stdRed;
                    imageToPredict[x][y][1] -= meanGreen;
                    imageToPredict[x][y][1] /= stdGreen;
                    imageToPredict[x][y][2] -= meanBlue;
                    imageToPredict[x][y][2] /= stdBlue;
                }
            }

            BufferedImage red = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            BufferedImage green = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            BufferedImage blue = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int redPixel = (imageArray[x][y][0] << 24) | (imageArray[x][y][1] << 16) | (0);
                    int greenPixel = (imageArray[x][y][0] << 24) | (0) | (imageArray[x][y][2] << 8);
                    int bluePixel = (imageArray[x][y][0] << 24) | (0) | imageArray[x][y][2];
                    red.setRGB(x, y, redPixel);
                    green.setRGB(x, y, greenPixel);
                    blue.setRGB(x, y, bluePixel);
                }
            }

            ImageIO.write(red, "bmp", new File("result_images/red.bmp"));
            ImageIO.write(green, "bmp", new File("result_images/green.bmp"));
            ImageIO.write(blue, "bmp", new File("result_images/blue.bmp"));

            String parametersInput = readFile("weights8.txt", Charset.defaultCharset());
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


            BufferedImage f1Image = getFilterImage(f1);
            ImageIO.write(f1Image, "bmp", new File("result_images/f1.bmp"));

            BufferedImage f2Image = getFilterImage(f2);
            ImageIO.write(f2Image, "bmp", new File("result_images/f2.bmp"));

            BufferedImage w3Image = getWeightImage(w3);
            ImageIO.write(w3Image, "bmp", new File("result_images/w3.bmp"));

            BufferedImage w4Image = getWeightImage(w4);
            ImageIO.write(w4Image, "bmp", new File("result_images/w4.bmp"));

            double[][][] conv1 = utility.convolution(imageToPredict, f1, b1, convSpatialExtent, convStride, convZeroPadding);
            BufferedImage conv1Image = getImage(conv1);
            ImageIO.write(conv1Image, "bmp", new File("result_images/conv1.bmp"));

            double[][][] conv1ReLU = utility.ReLU3D(conv1);
            BufferedImage conv1ReLUImage = getImage(conv1ReLU);
            ImageIO.write(conv1ReLUImage, "bmp", new File("result_images/conv1ReLU.bmp"));

            double[][][] conv1Reduced = utility.maxPooling(conv1ReLU, maxPoolSpatialExtent, maxPoolStride);
            BufferedImage conv1ReducedImage = getImage(conv1Reduced);
            ImageIO.write(conv1ReducedImage, "bmp", new File("result_images/conv1Reduced.bmp"));

            double[][][] conv2 = utility.convolution(conv1Reduced, f2, b2, convSpatialExtent, convStride, convZeroPadding);
            BufferedImage conv2Image = getImage(conv2);
            ImageIO.write(conv2Image, "bmp", new File("result_images/conv2.bmp"));

            double[][][] conv2ReLU = utility.ReLU3D(conv2);
            BufferedImage conv2ReLUImage = getImage(conv2ReLU);
            ImageIO.write(conv2ReLUImage, "bmp", new File("result_images/conv2ReLU.bmp"));

            double[][][] conv2Reduced = utility.maxPooling(conv2ReLU, maxPoolSpatialExtent, maxPoolStride);
            BufferedImage conv2ReducedImage = getImage(conv2Reduced);
            ImageIO.write(conv2ReducedImage, "bmp", new File("result_images/conv2Reduced.bmp"));

            double[] flatten = utility.flatten(conv2Reduced);
            BufferedImage flattenImage = getImage(flatten);
            ImageIO.write(flattenImage, "bmp", new File("result_images/flatten.bmp"));

            double[] full = utility.fullyConnected(flatten, w3, b3);
            BufferedImage fullImage = getImage(full);
            ImageIO.write(fullImage, "bmp", new File("result_images/full.bmp"));

            double[] fullReLU = utility.ReLU1D(full);
            BufferedImage fullReLUImage = getImage(fullReLU);
            ImageIO.write(fullReLUImage, "bmp", new File("result_images/fullReLU.bmp"));

            double[] output = utility.fullyConnected(fullReLU, w4, b4);
            BufferedImage outputImage = getImage(output);
            ImageIO.write(outputImage, "bmp", new File("result_images/output.bmp"));

            double[] probability = utility.softmax(output);

            System.out.println("\nProbabilities:");
            System.out.println("Golden retriever: " + probability[0] * 100 + "%");
            System.out.println("Poodle: " + probability[1] * 100 + "%");
            System.out.println("Doberman: " + probability[2] * 100 + "%");
            System.out.println("Husky: " + probability[3] * 100 + "%");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
