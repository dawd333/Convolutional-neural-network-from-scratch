import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import static java.lang.Math.*;

class Utility {
    double[][][] convolution(double[][][] image, double[][][][] filters, double[] bias, int spatialExtent, int stride, int zeroPadding){
        int imageWidth = image.length;
        int imageHeight = image[0].length;
        int imageDepth = image[0][0].length;
        int filtersDepth = filters[0][0][0].length;

        int outWidth = (imageWidth - spatialExtent + 2 * zeroPadding) / stride + 1;
        int outHeight = (imageHeight - spatialExtent + 2 * zeroPadding) / stride + 1;
        double[][][] out = new double[outWidth][outHeight][filtersDepth];

        int curY;
        int curX;
        double sum;
        int x=0;
        int y=0;

        for(int curFiltersDepth=0; curFiltersDepth<filtersDepth; curFiltersDepth++){
            curY = 0;
            for(int outY=0; outY<outHeight; outY++){
                curX = 0;
                for(int outX=0; outX<outWidth; outX++){
                    sum = 0;
                    for(int i=curY-zeroPadding; i<=curY+zeroPadding; i++, y++){
                        for(int j=curX-zeroPadding; j<=curX+zeroPadding; j++, x++){
                            for(int k=0; k<imageDepth; k++) {
                                if (i >= 0 && i < imageHeight && j >= 0 && j < imageWidth) {
                                    sum += image[j][i][k] * filters[x][y
                                            ][k][curFiltersDepth];
                                }
                            }
                        }
                        x=0;
                    }
                    y=0;
                    out[outX][outY][curFiltersDepth] = sum + bias[curFiltersDepth];
                    curX+=stride;
                }
                curY+=stride;
            }
        }
        return out;
    }

    double[][][] ReLU3D(double[][][] image){
        int imageWidth = image.length;
        int imageHeight = image[0].length;
        int imageDepth = image[0][0].length;

        for(int i=0; i<imageDepth; i++){
            for(int j=0; j<imageHeight; j++){
                for(int k=0; k<imageWidth; k++){
                    if(image[k][j][i] < 0) image[k][j][i] = 0;
                }
            }
        }
        return image;
    }

    double[] ReLU1D(double[] image){
        int imageLength = image.length;

        for(int i=0; i<imageLength; i++){
            if(image[i] < 0) image[i] = 0;
        }
        return image;
    }

    double[][][] maxPooling(double[][][] image, int spatialExtent, int stride){
        int imageWidth = image.length;
        int imageHeight = image[0].length;
        int imageDepth = image[0][0].length;

        int outWidth = (imageWidth - spatialExtent)/stride + 1;
        int outHeight = (imageHeight - spatialExtent)/stride + 1;

        double[][][] out = new double[outWidth][outHeight][imageDepth];
        double best;
        int x;
        int y;

        for(int currentDepth=0; currentDepth<imageDepth; currentDepth++){
            y=0;
            for(int currentY=0; currentY<imageHeight; currentY+=stride, y++){
                x=0;
                for(int currentX=0; currentX<imageWidth; currentX+=stride, x++){
                    best = 0;
                    for(int i=0; i<spatialExtent; i++){
                        for(int j=0; j<spatialExtent; j++){
                            if(image[currentX+j][currentY+i][currentDepth] > best){
                                best = image[currentX+j][currentY+i][currentDepth];
                            }
                        }
                    }
                    out[x][y][currentDepth] = best;
                }
            }
        }

        return out;
    }

    double[] flatten(double[][][] image){
        int imageWidth = image.length;
        int imageHeight = image[0].length;
        int imageDepth = image[0][0].length;

        int vectorLength = imageWidth * imageHeight * imageDepth;
        double[] out = new double[vectorLength];
        int index = 0;

        for(int i=0; i<imageDepth; i++){
            for(int j=0; j<imageHeight; j++){
                for (double[][] doubles : image) {
                    out[index++] = doubles[j][i];
                }
            }
        }
        return out;
    }

    private double[][][] reverseFlatten(double[] dflatten, int width, int height, int depth){
        double[][][] out = new double[width][height][depth];
        int index = dflatten.length;

        for(int i=0; i<depth; i++){
            for(int j=0; j<height; j++){
                for(int k=0; k<width; k++){
                    out[k][j][i] = dflatten[--index];
                }
            }
        }
        return out;
    }


    private double[] dot(double[] vector, double[][] weights){
        int vectorLength = vector.length;
        int weightsWidth = weights.length;

        double[] out = new double[weightsWidth];
        double sum;
        for(int i=0; i<weightsWidth; i++){
            sum = 0;
            for(int j=0; j<vectorLength; j++){
                sum+= vector[j] * weights[i][j];
            }
            out[i] = sum;
        }
        return out;
    }

    double[] fullyConnected(double[] vector, double[][] weights, double[] bias){
        double[] out = dot(vector, weights);
        int vectorLength = out.length;

        for(int i=0; i<vectorLength; i++){
            out[i] += bias[i];
        }
        return out;
    }

    double[] softmax(double[] vector){
        int vectorLength = vector.length;

        double[] exp_vector = new double[vectorLength];
        for(int i=0; i<vectorLength; i++) exp_vector[i] = Math.exp(vector[i]);

        double sum = 0;
        for(int i=0; i<vectorLength; i++) sum+=exp_vector[i];

        double[] out = new double[vectorLength];
        for(int i=0; i<vectorLength; i++) out[i] = exp_vector[i]/sum;

        return out;
    }

    private double crossEntropyLoss (double[] probability, int[] label){
        int vectorLength = probability.length;
        double out = 0;
        for(int i=0; i<vectorLength; i++){
            out -= Math.log(probability[i]) * label[i];
        }
        return out;
    }

    private double[] initializeBias(int biasLength){

        double[] bias = new double[biasLength];
        for(int i=0; i<biasLength; i++) bias[i] = 0;
        return bias;
    }

    private double[][] initializeWeight(int sizeX, int sizeY){

        Random random = new Random();
        double[][] weight = new double[sizeX][sizeY];
        for(int i=0; i<sizeY; i++){
            for(int j=0; j<sizeX; j++){
                weight[j][i] = random.nextGaussian() * 0.01;
            }
        }
        return weight;
    }

    private double[][][][] initializeFilter(int sizeX, int sizeY, int oldDepth, int newDepth){

        double stddev = 1.0 / Math.sqrt(sizeX * sizeY * oldDepth * newDepth);
        Random random = new Random();

        double[][][][] filter = new double[sizeX][sizeY][oldDepth][newDepth];
        for(int i=0; i<newDepth; i++){
            for(int j=0; j<oldDepth; j++){
                for(int k=0; k<sizeY; k++){
                    for(int l=0; l<sizeX; l++){
                        filter[l][k][j][i] = random.nextGaussian() * stddev;
                    }
                }
            }
        }
        return  filter;
    }

    private double[] doutputCalc(double[] probability, int[] label){
        int probabilityLength = probability.length;
        double[] out = new double[probabilityLength];

        for(int i=0; i<probabilityLength; i++){
            out[i] = probability[i] - label[i];
        }
        return out;
    }

    private double[][] dweightCalc(double[] upstreamGradient, double[] localGradient){
        int upstreamGradientLength = upstreamGradient.length;
        int localGradientLength = localGradient.length;

        double[][] out = new double[upstreamGradientLength][localGradientLength];
        for(int i=0; i<localGradientLength; i++){
            for(int j=0; j<upstreamGradientLength; j++){
                out[j][i] = upstreamGradient[j] * localGradient[i];
            }
        }
        return out;
    }

    private double[][] transposeMatrix(double[][] matrix){
        int matrixLength = matrix.length;
        int matrixWidth = matrix[0].length;
        double[][] out = new double[matrixWidth][matrixLength];

        for(int i=0; i<matrixWidth; i++){
            for(int j=0; j<matrixLength; j++){
                out[i][j] = matrix[j][i];
            }
        }
        return out;
    }

    private int[] argMaxIndex(double[][] array){
        int arrayWidth = array.length;
        int arrayHeight = array[0].length;
        int[] out = new int[2];
        double best = 0;
        for(int i=0; i<arrayHeight; i++){
            for(int j=0; j<arrayWidth; j++){
                if(array[j][i] > best){
                    best = array[j][i];
                    out[0] = j;
                    out[1] = i;
                }
            }
        }
        return out;
    }

    private double[][][] maxPoolingBackward(double[][][] dpool, double[][][] original, int spatialExtent, int stride){
        int originalWidth = original.length;
        int originalHeight = original[0].length;
        int originalDepth = original[0][0].length;

        double[][][] out = new double[originalWidth][originalHeight][originalDepth];
        int[] best;
        int x;
        int y;

        for(int currentDepth=0; currentDepth<originalDepth; currentDepth++){
            y=0;
            for(int currentY=0; currentY<originalHeight; currentY+=stride, y++){
                x=0;
                for(int currentX=0; currentX<originalWidth; currentX+=stride, x++){
                    double[][] tmp = new double[spatialExtent][spatialExtent];
                    for(int i=0; i<spatialExtent; i++){
                        for(int j=0; j<spatialExtent; j++){
                            tmp[j][i] = original[currentX+j][currentY+i][currentDepth];
                        }
                    }
                    best = argMaxIndex(tmp);
                    out[currentX+best[0]][currentY+best[1]][currentDepth] = dpool[x][y][currentDepth];
                }
            }
        }
        return out;
    }

    private ConvBackward convolutionBackward(double[][][] dconvPrev, double[][][] convIn, double[][][][] filters, int stride, int zeroPadding){
        int convInWidth = convIn.length;
        int convInHeight = convIn[0].length;
        int convInDepth = convIn[0][0].length;
        int filtersWidth = filters.length;
        int filtersHeight = filters[0].length;
        int filtersDepth = filters[0][0][0].length;

        double[][][] dconv = new double[convInWidth][convInHeight][convInDepth];
        double[][][][] dfilter = new double[filtersWidth][filtersHeight][convInDepth][filtersDepth];
        double[] db = new double[filtersDepth];

        int curY;
        int curX;
        int x=0;
        int y=0;

        for(int curFiltersDepth=0; curFiltersDepth<filtersDepth; curFiltersDepth++){
            curY = 0;
            for(int outY=0; outY<convInHeight; outY++){
                curX = 0;
                for(int outX=0; outX<convInWidth; outX++){
                    for(int i=curY-zeroPadding; i<=curY+zeroPadding; i++, y++){
                        for(int j=curX-zeroPadding; j<=curX+zeroPadding; j++, x++){
                            for(int k=0; k<convInDepth; k++){
                                if (i >= 0 && i < convInHeight && j >= 0 && j < convInWidth){
                                    dconv[outX][outY][k]+= filters[y][x][k][curFiltersDepth] * dconvPrev[j][i][curFiltersDepth];
                                    dfilter[x][y][k][curFiltersDepth]+= convIn[i][j][k] * dconvPrev[j][i][curFiltersDepth];
                                }
                            }
                        }
                        x = 0;
                    }
                    y = 0;
                    db[curFiltersDepth]+=dconvPrev[outX][outY][curFiltersDepth];
                    curX+=stride;
                }
                curY+=stride;
            }
        }

        return new ConvBackward(dconv, dfilter, db);
    }

    private double[] dReLU1D(double[] dReLU, double[] ReLU){
        int reluLength = dReLU.length;

        for(int i=0; i<reluLength; i++){
            if(ReLU[i] < 0) dReLU[i] = 0;
        }
        return dReLU;
    }

    private double[][][] dReLU3D(double[][][] dReLU, double[][][] ReLU){
        int reluWidth = ReLU.length;
        int reluHeight = ReLU[0].length;
        int reluDepth = ReLU[0][0].length;

        for(int i=0; i<reluDepth; i++){
            for(int j=0; j<reluHeight; j++){
                for(int k=0; k<reluWidth; k++){
                    if(ReLU[k][j][i] < 0) dReLU[k][j][i] = 0;
                }
            }
        }
        return dReLU;
    }

    private double[][][][] updateFilter(double[][][][] f, double[][][][] df, double[][][][] v, double[][][][] s, double beta1, double beta2, double learningRate, int batchSize){
        int fWidth = f.length;
        int fHeight = f[0].length;
        int fOldDepth = f[0][0].length;
        int fNewDepth = f[0][0][0].length;
        for (int i = 0; i < fNewDepth; i++) {
            for (int j = 0; j < fOldDepth; j++) {
                for (int k = 0; k < fHeight; k++) {
                    for (int l = 0; l < fWidth; l++) {
                        v[l][k][j][i] = beta1 * v[l][k][j][i] + (1 - beta1) * df[l][k][j][i] / batchSize;
                        s[l][k][j][i] = beta2 * s[l][k][j][i] + (1 - beta2) * pow((df[l][k][j][i] / batchSize), 2);
                        f[l][k][j][i] -= learningRate * v[l][k][j][i] / (sqrt(s[l][k][j][i]) + pow(10, -8));
                    }
                }
            }
        }
        return f;
    }

    private double[][] updateWeight(double[][] w, double[][] dw, double[][] v, double[][] s, double beta1, double beta2, double learningRate, int batchSize){
        int wWidth = w.length;
        int wHeight = w[0].length;
        for (int i = 0; i < wHeight; i++) {
            for (int j = 0; j < wWidth; j++) {
                v[j][i] = beta1 * v[j][i] + (1 - beta1) * dw[j][i];
                s[j][i] = beta2 * s[j][i] + (1 - beta2) * pow((dw[j][i] / batchSize), 2);
                w[j][i] -= learningRate * v[j][i] / (sqrt(s[j][i]) + pow(10, -8));
            }
        }
        return w;
    }

    private double[] updateBias(double[] b, double[] db, double[] bv, double[] bs, double beta1, double beta2, double learningRate, int batchSize){
        int bWidth = b.length;
        for (int i = 0; i < bWidth; i++) {
            bv[i] = beta1 * bv[i] + (1 - beta1) * db[i];
            bs[i] = beta2 * bs[i] + (1 - beta2) * pow((db[i] / batchSize), 2);
            b[i] -= learningRate * bv[i] / (sqrt(bs[i]) + pow(10, -8));
        }
        return b;
    }

    Gradients network(double[][][] image, int[] label, double[][][][] f1, double[] b1, double[][][][] f2, double[] b2, double[][] w3, double[] b3, double[][] w4, double[] b4, int convSpatialExtent, int convStride, int convZeroPadding, int maxPoolSpatialExtent, int maxPoolStride){
        //Forward operations
        double[][][] conv1 = convolution(image, f1, b1, convSpatialExtent, convStride, convZeroPadding);
        double[][][] conv1ReLU = ReLU3D(conv1);
        double[][][] conv1Reduced = maxPooling(conv1ReLU, maxPoolSpatialExtent, maxPoolStride);

        double[][][] conv2 = convolution(conv1Reduced,f2, b2, convSpatialExtent, convStride, convZeroPadding);
        double[][][] conv2ReLU = ReLU3D(conv2);
        double[][][] conv2Reduced = maxPooling(conv2ReLU, maxPoolSpatialExtent, maxPoolStride);

        double[] flatten = flatten(conv2Reduced);

        double[] full = fullyConnected(flatten, w3, b3);
        double[] fullReLU = ReLU1D(full);

        double[] output = fullyConnected(fullReLU, w4, b4);
        double[] probability = softmax(output);

        //Cross Entropy Loss
        double loss = crossEntropyLoss(probability, label);

        //Backward operations
        double[] doutput = doutputCalc(probability, label);
        double[][] dw4 = dweightCalc(doutput, fullReLU);
        double[] dfullRelu = dot(doutput, transposeMatrix(w4));
        double[] db4 = doutput;
        double[] dfull = dReLU1D(dfullRelu, full);
        double[][] dw3 = dweightCalc(dfull, flatten);
        double[] dflatten = dot(dfull, transposeMatrix(w3));
        double[] db3 = dfull;
        double[][][] dconv2Reduced = reverseFlatten(dflatten, conv2Reduced.length, conv2Reduced[0].length, conv2Reduced[0][0].length);
        double[][][] dconv2ReLU = maxPoolingBackward(dconv2Reduced, conv2ReLU, maxPoolSpatialExtent, maxPoolStride);
        double[][][] dconv2 = dReLU3D(dconv2ReLU, conv2);
        ConvBackward convBackward2 = convolutionBackward(dconv2, conv1Reduced, f2, convStride, convZeroPadding);
        double[][][] dconv1ReLU = maxPoolingBackward(convBackward2.getDconv(), conv1ReLU, maxPoolSpatialExtent, maxPoolStride);
        double[][][] dconv1 = dReLU3D(dconv1ReLU, conv1);
        ConvBackward convBackward = convolutionBackward(dconv1, image, f1, convStride, convZeroPadding);

        return new Gradients(convBackward.getDfilter(), convBackward2.getDfilter(), dw3, dw4, convBackward.getDb(), convBackward2.getDb(), db3, db4, loss);
    }

    private Parameters adamGD(double[][][][] images, int[][] labels, double learningRate, double beta1, double beta2, Parameters parameters) {
        double totalCost = 0;
        int batchSize = images.length;

        double[][][][] f1 = parameters.getF1();
        double[] b1 = parameters.getB1();
        double[][][][] f2 = parameters.getF2();
        double[] b2 = parameters.getB2();
        double[][] w3 = parameters.getW3();
        double[] b3 = parameters.getB3();
        double[][] w4 = parameters.getW4();
        double[] b4 = parameters.getB4();

        int f1Width = f1.length;
        int f1Height = f1[0].length;
        int f1OldDepth = f1[0][0].length;
        int f1NewDepth = f1[0][0][0].length;
        int f2Width = f2.length;
        int f2Height = f2[0].length;
        int f2OldDepth = f2[0][0].length;
        int f2NewDepth = f2[0][0][0].length;
        int w3Width = w3.length;
        int w3Height = w3[0].length;
        int w4Width = w4.length;
        int w4Height = w4[0].length;
        int b1Width = b1.length;
        int b2Width = b2.length;
        int b3Width = b3.length;
        int b4Width = b4.length;

        double[][][][] df1 = new double[f1Width][f1Height][f1OldDepth][f1NewDepth];
        double[] db1 = new double[b1Width];
        double[][][][] df2 = new double[f2Width][f2Height][f2OldDepth][f2NewDepth];
        double[] db2 = new double[b2Width];
        double[][] dw3 = new double[w3Width][w3Height];
        double[] db3 = new double[b3Width];
        double[][] dw4 = new double[w4Width][w4Height];
        double[] db4 = new double[b4Width];

        double[][][][] v1 = new double[f1Width][f1Height][f1OldDepth][f1NewDepth];
        double[] bv1 = new double[b1Width];
        double[][][][] v2 = new double[f2Width][f2Height][f2OldDepth][f2NewDepth];
        double[] bv2 = new double[b2Width];
        double[][] v3 = new double[w3Width][w3Height];
        double[] bv3 = new double[b3Width];
        double[][] v4 = new double[w4Width][w4Height];
        double[] bv4 = new double[b4Width];

        double[][][][] s1 = new double[f1Width][f1Height][f1OldDepth][f1NewDepth];
        double[] bs1 = new double[b1Width];
        double[][][][] s2 = new double[f2Width][f2Height][f2OldDepth][f2NewDepth];
        double[] bs2 = new double[b2Width];
        double[][] s3 = new double[w3Width][w3Height];
        double[] bs3 = new double[b3Width];
        double[][] s4 = new double[w4Width][w4Height];
        double[] bs4 = new double[b4Width];

        Gradients[] gradients = new Gradients[batchSize];
        ExecutorService executor = Executors.newFixedThreadPool(batchSize/4);
        NetworkThread[] networkThreads = new NetworkThread[batchSize];

        for (int i=0; i<batchSize; i++) {
            networkThreads[i] = new NetworkThread(images[i], labels[i], f1, b1, f2, b2, w3, b3, w4, b4);
            executor.execute(networkThreads[i]);
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            for(int i=0; i<batchSize; i++){
                gradients[i] = networkThreads[i].getGradients();
            }
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        for (int i=0; i<batchSize; i++){
            for (int ii = 0; ii < f1NewDepth; ii++) {
                for (int j = 0; j < f1OldDepth; j++) {
                    for (int k = 0; k < f1Height; k++) {
                        for (int l = 0; l < f1Width; l++) {
                            if(Math.random() < 0.5) df1[l][k][j][ii] += gradients[i].getDf1()[l][k][j][ii];
                        }
                    }
                }
            }
            for (int ii = 0; ii < f2NewDepth; ii++) {
                for (int j = 0; j < f2OldDepth; j++) {
                    for (int k = 0; k < f2Height; k++) {
                        for (int l = 0; l < f2Width; l++) {
                            if(Math.random() < 0.5) df2[l][k][j][ii] += gradients[i].getDf2()[l][k][j][ii];
                        }
                    }
                }
            }
            for (int j = 0; j < w3Height; j++) {
                for (int k = 0; k < w3Width; k++) {
                    if(Math.random() < 0.5) dw3[k][j] += gradients[i].getDw3()[k][j];
                }
            }
            for (int j = 0; j < w4Height; j++) {
                for (int k = 0; k < w4Width; k++) {
                    if(Math.random() < 0.5) dw4[k][j] += gradients[i].getDw4()[k][j];
                }
            }
            for (int j = 0; j < b1Width; j++) db1[j] += gradients[i].getDb1()[j];
            for (int j = 0; j < b2Width; j++) db2[j] += gradients[i].getDb2()[j];
            for (int j = 0; j < b3Width; j++) db3[j] += gradients[i].getDb3()[j];
            for (int j = 0; j < b4Width; j++) db4[j] += gradients[i].getDb4()[j];

            totalCost += gradients[i].getLoss();
        }

        parameters.setF1(updateFilter(f1, df1, v1, s1, beta1, beta2, learningRate, batchSize));
        parameters.setF2(updateFilter(f2, df2, v2, s2, beta1, beta2, learningRate, batchSize));
        parameters.setW3(updateWeight(w3, dw3, v3, s3, beta1, beta2, learningRate, batchSize));
        parameters.setW4(updateWeight(w4, dw4, v4, s4, beta1, beta2, learningRate, batchSize));
        parameters.setB1(updateBias(b1, db1, bv1, bs1, beta1, beta2, learningRate, batchSize));
        parameters.setB2(updateBias(b2, db2, bv2, bs2, beta1, beta2, learningRate, batchSize));
        parameters.setB3(updateBias(b3, db3, bv3, bs3, beta1, beta2, learningRate, batchSize));
        parameters.setB4(updateBias(b4, db4, bv4, bs4, beta1, beta2, learningRate, batchSize));
        parameters.setCost(totalCost/batchSize);

        return parameters;
    }

    Parameters train(double[][][][][] images, int[][][] labels, String parametersInput){

        double learningRate = 0.00000005;
        double beta1 = 0.9;
        double beta2 = 0.999;
        int numberOfEpochs = 10000;
        int numberOfBatches = images.length;
        Parameters parameters;
        if(parametersInput.equals("")){
            double[][][][] f1 = initializeFilter(3, 3, 3, 8);
            double[][][][] f2 = initializeFilter(3, 3, 8, 16);
            double[][] w3 = initializeWeight(1024, 4096);
            double[][] w4 = initializeWeight(4, 1024);
            double[] b1 = initializeBias(8);
            double[] b2 = initializeBias(16);
            double[] b3 = initializeBias(1024);
            double[] b4 = initializeBias(4);
            parameters = new Parameters(f1, f2, w3, w4, b1, b2, b3, b4, 0);
        } else{
            Gson gson = new Gson();
            parameters = gson.fromJson(parametersInput, Parameters.class);
        }

        ShutdownThread shutdownThread = new ShutdownThread(parameters);
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        double totalCost;
        double lastTotalCost=0;
        double[] previousCost = new double[numberOfBatches];
        double currentCost;
        for(int i=0; i<numberOfEpochs; i++){
            totalCost = 0;
            for(int j=0; j<numberOfBatches; j++){
                parameters = adamGD(images[j], labels[j], learningRate, beta1, beta2, parameters);
                currentCost = parameters.getCost();
                totalCost+= currentCost;
                System.out.format("Batch number: %d/%d, cost: %.15f, previous cost: %.15f, delta: %.15f\n", j+1, numberOfBatches, currentCost, previousCost[j], currentCost-previousCost[j]);
                previousCost[j] = currentCost;
            }
            totalCost = totalCost/numberOfBatches;
            System.out.format("Epoch: %d/%d, cost: %.15f, previous cost: %.15f, delta: %.15f\n", i+1, numberOfEpochs, totalCost, lastTotalCost, totalCost-lastTotalCost);
            lastTotalCost = totalCost;
            Runtime.getRuntime().removeShutdownHook(shutdownThread);
            shutdownThread = new ShutdownThread(parameters);
            Runtime.getRuntime().addShutdownHook(shutdownThread);
        }
        Runtime.getRuntime().removeShutdownHook(shutdownThread);
        return parameters;
    }
}