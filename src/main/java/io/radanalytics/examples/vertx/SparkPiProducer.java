package io.radanalytics.examples.vertx;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SparkPiProducer implements Serializable {


    public String getPi(int scale) {
        JavaSparkContext jsc = SparkContextProvider.getContext();
        int slices = scale;
        int n = 100000 * slices;

        List<Integer> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            l.add(i);
        }

        JavaRDD<Integer> dataSet = jsc.parallelize(l, slices);

        int count = dataSet.map(integer -> {
            double x = Math.random() * 2 - 1;
            double y = Math.random() * 2 - 1;
            return (x * x + y * y < 1) ? 1 : 0;
        }).reduce((integer, integer2) -> integer + integer2);

        return "Pi is rouuuughly " + 8.0 * count / n;

    }
}
