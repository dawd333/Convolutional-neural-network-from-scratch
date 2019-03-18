class Gradients {
    private double[][][][] df1;
    private double[][][][] df2;
    private double[][] dw3;
    private double[][] dw4;
    private double[] db1;
    private double[] db2;
    private double[] db3;
    private double[] db4;
    private double loss;

    Gradients(double[][][][] df1, double[][][][] df2, double[][] dw3, double[][] dw4, double[] db1, double[] db2, double[] db3, double[] db4, double loss){
        this.df1 = df1;
        this.df2 = df2;
        this.dw3 = dw3;
        this.dw4 = dw4;
        this.db1 = db1;
        this.db2 = db2;
        this.db3 = db3;
        this.db4 = db4;
        this.loss = loss;
    }

    double[][][][] getDf1() {
        return df1;
    }

    double[][][][] getDf2() {
        return df2;
    }

    double[][] getDw3() {
        return dw3;
    }

    double[][] getDw4() {
        return dw4;
    }

    double[] getDb1() {
        return db1;
    }

    double[] getDb2() {
        return db2;
    }

    double[] getDb3() {
        return db3;
    }

    double[] getDb4() {
        return db4;
    }

    double getLoss() {
        return loss;
    }
}
