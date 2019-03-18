import java.io.Serializable;

class Parameters implements Serializable {
    private double[][][][] f1;
    private double[][][][] f2;
    private double[][] w3;
    private double[][] w4;
    private double[] b1;
    private double[] b2;
    private double[] b3;
    private double[] b4;
    private double cost;

    Parameters(double[][][][] f1, double[][][][] f2, double[][] w3, double[][] w4, double[] b1, double[] b2, double[] b3, double[] b4, double cost){
        this.f1 = f1;
        this.f2 = f2;
        this.w3 = w3;
        this.w4 = w4;
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
        this.b4 = b4;
        this.cost = cost;
    }

    double[][][][] getF1() {
        return f1;
    }

    double[][][][] getF2() {
        return f2;
    }

    double[][] getW3() {
        return w3;
    }

    double[][] getW4() {
        return w4;
    }

    double[] getB1() {
        return b1;
    }

    double[] getB2() {
        return b2;
    }

    double[] getB3() {
        return b3;
    }

    double[] getB4() {
        return b4;
    }

    double getCost() {
        return cost;
    }

    void setF1(double[][][][] f1) {
        this.f1 = f1;
    }

    void setF2(double[][][][] f2) {
        this.f2 = f2;
    }

    void setW3(double[][] w3) {
        this.w3 = w3;
    }

    void setW4(double[][] w4) {
        this.w4 = w4;
    }

    void setB1(double[] b1) {
        this.b1 = b1;
    }

    void setB2(double[] b2) {
        this.b2 = b2;
    }

    void setB3(double[] b3) {
        this.b3 = b3;
    }

    void setB4(double[] b4) {
        this.b4 = b4;
    }

    void setCost(double cost) {
        this.cost = cost;
    }

}
