import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Objects;

public class ScalePhotos {
    // File representing the folder that you select using a FileChooser
    static final private File dir = new File("validation_full_photos/doberman");

    // array of supported extensions (use a List if you prefer)
    static final private String[] EXTENSIONS = new String[]{
            "jpg", "jpeg", "png", "gif" // and other formats you need
    };
    // filter to identify images based on their extensions
    static final private FilenameFilter IMAGE_FILTER = (dir, name) -> {
        for (final String ext : EXTENSIONS) {
            if (name.endsWith("." + ext)) {
                return (true);
            }
        }
        return (false);
    };

    private static void resize(String inputImagePath, String outputImagePath, int scaledWidth, int scaledHeight) throws IOException {
        // reads input image
        File inputFile = new File(inputImagePath);
        BufferedImage inputImage = ImageIO.read(inputFile);

        // creates output image
        BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, inputImage.getType());

        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        // extracts extension of output file
        String formatName = outputImagePath.substring(outputImagePath.lastIndexOf(".") + 1);

        // writes to output file
        ImageIO.write(outputImage, formatName, new File(outputImagePath));
    }

    public static void main(String[] args){
        if (dir.isDirectory()) { // make sure it's a directory
            for (final File f : Objects.requireNonNull(dir.listFiles(IMAGE_FILTER))) {
                String inputImagePath = f.getPath();
                String outputImagePath = "/home/dawd333/Desktop/szkoła/ZSC/validation_resized_photos" + "/3" + f.getName();

                try {
                    // resize to a fixed width (not proportional)
                    int scaledWidth = 64;
                    int scaledHeight = 64;
                    ScalePhotos.resize(inputImagePath, outputImagePath, scaledWidth, scaledHeight);


                } catch (IOException ex) {
                    System.out.println("Error resizing the image.");
                    ex.printStackTrace();
                }
            }
        }
    }
}
