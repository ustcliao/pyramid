package edu.neu.ccs.pyramid.experiment;

import edu.neu.ccs.pyramid.configuration.Config;
import edu.neu.ccs.pyramid.dataset.DataSetType;
import edu.neu.ccs.pyramid.dataset.MultiLabel;
import edu.neu.ccs.pyramid.dataset.MultiLabelClfDataSet;
import edu.neu.ccs.pyramid.dataset.TRECFormat;
import edu.neu.ccs.pyramid.eval.Accuracy;
import edu.neu.ccs.pyramid.eval.Overlap;
import edu.neu.ccs.pyramid.multilabel_classification.bmm_variant.BMMClassifier;
import edu.neu.ccs.pyramid.multilabel_classification.bmm_variant.BMMInitializer;
import edu.neu.ccs.pyramid.multilabel_classification.bmm_variant.BMMOptimizer;

import java.io.File;

/**
 * Created by Rainicy on 12/9/15.
 */
public class Exp217 {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Please specify a properties file.");
        }

        Config config = new Config(args[0]);

        System.out.println(config);

        MultiLabelClfDataSet trainSet = TRECFormat.loadMultiLabelClfDataSet(config.getString("input.trainData"),
                DataSetType.ML_CLF_SPARSE, true);
        MultiLabelClfDataSet testSet = TRECFormat.loadMultiLabelClfDataSet(config.getString("input.testData"),
                DataSetType.ML_CLF_SPARSE, true);

        int numClusters = config.getInt("numClusters");
        double softmaxVariance = config.getDouble("softmaxVariance");
        double logitVariance = config.getDouble("logitVariance");
        int numIterations = config.getInt("numIterations");
        int numSamples = config.getInt("numSamples");

        String output = config.getString("output");
        String modelName = config.getString("modelName");

        BMMClassifier bmmClassifier;
        if (config.getBoolean("train.warmStart")) {
            bmmClassifier = BMMClassifier.deserialize(new File(output, modelName));
            bmmClassifier.setAllowEmpty(config.getBoolean("allowEmpty"));
            bmmClassifier.setPredictMode(config.getString("predictMode"));
        } else {
            bmmClassifier = new BMMClassifier(trainSet.getNumClasses(),numClusters,trainSet.getNumFeatures());
            BMMOptimizer optimizer = new BMMOptimizer(bmmClassifier, trainSet,softmaxVariance,logitVariance);
            bmmClassifier.setNumSample(numSamples);
            bmmClassifier.setAllowEmpty(config.getBoolean("allowEmpty"));
            bmmClassifier.setPredictMode(config.getString("predictMode"));

            MultiLabel[] trainPredict;
            MultiLabel[] testPredict;

            if (config.getBoolean("initialize")) {
                BMMInitializer.initialize(bmmClassifier,trainSet,softmaxVariance,logitVariance, new File(config.getString("initializeBy")));
            }
            else {
                BMMInitializer.initialize(bmmClassifier,trainSet,softmaxVariance,logitVariance);
            }
            System.out.println("after initialization");
            trainPredict = bmmClassifier.predict(trainSet);
            testPredict = bmmClassifier.predict(testSet);

            System.out.print("objective: "+optimizer.getObjective()+ "\t");
            System.out.print("trainAcc : " + Accuracy.accuracy(trainSet.getMultiLabels(), trainPredict) + "\t");
            System.out.print("trainOver: " + Overlap.overlap(trainSet.getMultiLabels(), trainPredict) + "\t");
            System.out.print("testACC  : " + Accuracy.accuracy(testSet.getMultiLabels(), testPredict) + "\t");
            System.out.println("testOver : "+ Overlap.overlap(testSet.getMultiLabels(), testPredict) + "\t");

            for (int i=1;i<=numIterations;i++){
                optimizer.iterate();
                trainPredict = bmmClassifier.predict(trainSet);
                testPredict = bmmClassifier.predict(testSet);
                System.out.print("iter : "+i + "\t");
                System.out.print("objective: "+optimizer.getTerminator().getLastValue() + "\t");
                System.out.print("trainAcc : "+ Accuracy.accuracy(trainSet.getMultiLabels(),trainPredict)+ "\t");
                System.out.print("trainOver: "+ Overlap.overlap(trainSet.getMultiLabels(), trainPredict)+ "\t");
                System.out.print("testAcc  : "+ Accuracy.accuracy(testSet.getMultiLabels(),testPredict)+ "\t");
                System.out.println("testOver : "+ Overlap.overlap(testSet.getMultiLabels(), testPredict)+ "\t");
            }
            System.out.println("history = "+optimizer.getTerminator().getHistory());
        }

        System.out.println("--------------------------------Train Results-----------------------------\n");
        System.out.println();
        for (int n=0; n<trainSet.getNumDataPoints(); n++) {
            MultiLabel pred = bmmClassifier.predict(trainSet.getRow(n));
            System.out.println(trainSet.getMultiLabels()[n] + "\t" + pred);
        }
        System.out.println();
        System.out.println();

        System.out.println("--------------------------------Test Results-----------------------------\n");
        System.out.println();
        for (int n=0; n<testSet.getNumDataPoints(); n++) {
            MultiLabel pred = bmmClassifier.predict(testSet.getRow(n));
            System.out.println(testSet.getMultiLabels()[n] + "\t" + pred);
        }
        System.out.println();
        System.out.println();
    }
}