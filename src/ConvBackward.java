class ConvBackward {
    private double[][][] dconv;
    private double[][][][] dfilter;
    private double[] db;

    ConvBackward(double[][][] dconv, double[][][][] dfilter, double[] db){
        this.dconv = dconv;
        this.dfilter = dfilter;
        this.db = db;
    }

    double[][][] getDconv() {
        return dconv;
    }

    double[][][][] getDfilter() {
        return dfilter;
    }

    double[] getDb() {
        return db;
    }
}
