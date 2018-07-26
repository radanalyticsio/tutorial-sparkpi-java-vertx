package io.radanalytics.examples.vertx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class SparkPiProducer implements Serializable {
    private final Vertx vertx;

    public SparkPiProducer(Vertx vertx) {
        this.vertx = vertx;
    }

    public Single<String> getPi(int scale) {
        JavaSparkContext jsc = SparkContextProvider.getContext();
        int slices = scale;
        int n = 100000 * slices;
        return vertx.rxExecuteBlocking(
                future -> {
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
                    future.complete("Pi is rouuuughly " + 4.0 * count / n);
                }
        );

    }
}
