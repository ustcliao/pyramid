package edu.neu.ccs.pyramid.clustering.gmm;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.stream.IntStream;

public class GMMTrainer {
    private RealMatrix data;
    private double[][] gammas;
    private GMM gmm;


    public GMMTrainer(RealMatrix data, GMM gmm) {
        this.data = data;
        this.gmm = gmm;
        this.gammas = new double[data.getRowDimension()][gmm.getNumComponents()];
    }

    void iterate(){
        eStep();
        mStep();
    }

    private void eStep(){
        IntStream.range(0,data.getRowDimension()).parallel()
                .forEach(i->gammas[i]=gmm.posteriors(data.getRowVector(i)));
    }

    private void mStep(){
        IntStream.range(0,gmm.getNumComponents()).parallel()
                .forEach(k->{
                    double sumGamma = computeSumGamma(k);
                    gmm.setMixtureCoefficient(k,sumGamma/data.getRowDimension());
                    RealVector mean = computeMean(k, sumGamma);
                    gmm.getGaussianDistributions()[k].setMean(mean);
                    RealMatrix cov = computeCov(k, mean, sumGamma);
                    gmm.getGaussianDistributions()[k].setCovariance(cov);
                });
    }

    private RealVector computeMean(int k, double sumGamma){
        RealVector res = new ArrayRealVector(data.getColumnDimension());
        for (int i=0;i<data.getRowDimension();i++){
            res = res.add(data.getRowVector(i).mapMultiply(gammas[i][k]));
        }
        return res.mapDivide(sumGamma);
    }

    private RealMatrix computeCov(int k, RealVector mean, double sumGamma){
        RealMatrix res = new Array2DRowRealMatrix(data.getColumnDimension(),data.getColumnDimension());
        for (int i=0;i<data.getRowDimension();i++){
            res = res.add(data.getRowVector(i).outerProduct(data.getRowVector(i)).scalarMultiply(gammas[i][k]));
        }
        return res.scalarMultiply(1/sumGamma).subtract(mean.outerProduct(mean));
    }

    private double computeSumGamma(int k){
        double totalGamma = 0;
        for (int i=0;i<data.getRowDimension();i++){
            totalGamma += gammas[i][k];
        }
        return totalGamma;
    }
}
