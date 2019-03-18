public class NetworkThread implements Runnable {
    private double[][][] image;
    private int[] label;
    private double[][][][] f1;
    private double[] b1;
    private double[][][][] f2;
    private double[] b2;
    private double[][] w3;
    private double[] b3;
    private double[][] w4;
    private double[] b4;
    private Utility utility = new Utility();
    private Gradients gradients;

    NetworkThread(double[][][] image, int[] label, double[][][][] f1, double[] b1, double[][][][] f2, double[] b2, double[][] w3, double[] b3, double[][] w4, double[] b4) {
        this.image = image;
        this.label = label;
        this.f1 = f1;
        this.b1 = b1;
        this.f2 = f2;
        this.b2 = b2;
        this.w3 = w3;
        this.b3 = b3;
        this.w4 = w4;
        this.b4 = b4;
    }

    public void run(){
        this.gradients = utility.network(image, label, f1, b1, f2, b2, w3, b3, w4, b4, 3, 1, 1, 2, 2);
    }

    Gradients getGradients(){
        return gradients;
    }
}
