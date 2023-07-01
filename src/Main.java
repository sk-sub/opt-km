import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class Main {

    /** 测试程序 */
    public static void main(String[] args) {
        int SIZE = 16;
        double[][] lb = generateRandomMatrix(SIZE, SIZE, 0, 100);           // lower bound
        double[][] delta = generateRandomMatrix(SIZE, SIZE, 0, 30);
        double[][] real = new double[SIZE][SIZE];                                             // real cost

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                real[i][j] = lb[i][j] + delta[i][j];
            }
        }

        Set<Integer> U = new HashSet<>(), V = new HashSet<>();
        for (int i = 0; i < SIZE; i++) {
            U.add(i);
            V.add(i);
        }

        class TestCalculator implements Calculator<Integer, Integer> {
            @Override
            public Double getCost(Integer u, Integer v) {
                return real[u][v];
            }

            @Override
            public Double getLowerBound(Integer u, Integer v) {
                return lb[u][v];
            }
        }


        OptKM<Integer, Integer> km = new OptKM<>(U, V, new TestCalculator());
        km.optKuhnMunkres();
    }

    public static double[][] generateRandomMatrix(int rows, int columns, double minValue, double maxValue) {
        double[][] matrix = new double[rows][columns];
        Random random = new Random();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                double randomValue = minValue + (maxValue - minValue) * random.nextDouble();
                matrix[i][j] = randomValue;
            }
        }

        return matrix;
    }

}

