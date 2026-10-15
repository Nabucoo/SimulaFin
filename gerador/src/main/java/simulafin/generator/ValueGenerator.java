package simulafin.generator;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Gerador de valores financeiros que implementa as quatro distribuições estatísticas requeridas:
 * 1. UNIFORME
 * 2. NORMAL (gaussiana truncada por rejeição/reamostragem)
 * 3. EXPONENCIAL_DECRESCENTE (maior concentração próxima ao valor mínimo)
 * 4. EXPONENCIAL_CRESCENTE (maior concentração próxima ao valor máximo)
 *
 * Todos os valores gerados respeitam rigorosamente o intervalo [minValue, maxValue].
 */
public final class ValueGenerator {

    private final double minValue;
    private final double maxValue;
    private final double range;
    private final DistributionType distributionType;

    // Parâmetros da distribuição Normal
    private final double mean;
    private final double stdDev;

    // Parâmetro da distribuição Exponencial Truncada
    // Lambda controla o decaimento/inclinação da curva exponencial.
    // Com lambda = 3.0, a densidade no pico é cerca de e^3 = ~20 vezes maior que no extremo oposto.
    private final double lambda;
    private final double expNegLambda;
    private final double expFactor;

    public ValueGenerator(double minValue, double maxValue, DistributionType distributionType) {
        this(minValue, maxValue, distributionType, (maxValue - minValue) / 6.0, 3.0);
    }

    public ValueGenerator(double minValue, double maxValue, DistributionType distributionType, double stdDev, double lambda) {
        if (minValue < 0) {
            throw new IllegalArgumentException("minValue deve ser maior ou igual a zero: " + minValue);
        }
        if (maxValue <= minValue) {
            throw new IllegalArgumentException("maxValue deve ser estritamente maior que minValue: " + minValue + " >= " + maxValue);
        }
        if (stdDev <= 0) {
            throw new IllegalArgumentException("stdDev deve ser maior que zero: " + stdDev);
        }
        if (lambda <= 0) {
            throw new IllegalArgumentException("lambda deve ser maior que zero: " + lambda);
        }

        this.minValue = minValue;
        this.maxValue = maxValue;
        this.range = maxValue - minValue;
        this.distributionType = distributionType;

        this.mean = (minValue + maxValue) / 2.0;
        this.stdDev = stdDev;

        this.lambda = lambda;
        this.expNegLambda = Math.exp(-lambda);
        this.expFactor = 1.0 - expNegLambda;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public DistributionType getDistributionType() {
        return distributionType;
    }

    public double getMean() {
        return mean;
    }

    public double getStdDev() {
        return stdDev;
    }

    public double getLambda() {
        return lambda;
    }

    /**
     * Gera o próximo valor financeiro bruto de acordo com a distribuição configurada.
     */
    public double nextValue() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return switch (distributionType) {
            case UNIFORME -> nextUniform(random);
            case NORMAL -> nextNormal(random);
            case EXPONENCIAL_DECRESCENTE -> nextExponentialDecreasing(random);
            case EXPONENCIAL_CRESCENTE -> nextExponentialIncreasing(random);
        };
    }

    /**
     * Distribuição Uniforme:
     * V = minValue + U * range, onde U in [0, 1).
     */
    private double nextUniform(ThreadLocalRandom random) {
        return minValue + random.nextDouble() * range;
    }

    /**
     * Distribuição Normal Truncada via Rejeição:
     * V = mean + Z * stdDev, com reamostragem caso V < minValue ou V > maxValue.
     * Como stdDev = range / 6 (regra 3-sigma), a probabilidade teórica de rejeição é apenas ~0.27%,
     * mantendo o custo computacional desprezível e preservando a simetria gaussiana exata.
     */
    private double nextNormal(ThreadLocalRandom random) {
        while (true) {
            double v = mean + random.nextGaussian() * stdDev;
            if (v >= minValue && v <= maxValue) {
                return v;
            }
        }
    }

    /**
     * Distribuição Exponencial Decrescente Truncada em [minValue, maxValue]:
     * Utiliza o método da Transformada Inversa da Função de Distribuição Acumulada (CDF)
     * condicionada ao intervalo fechado [0, 1]:
     *
     * F(x) = (1 - e^(-lambda * x)) / (1 - e^(-lambda)) para x in [0, 1].
     * Invertendo:
     * x = -ln(1 - U * (1 - e^(-lambda))) / lambda
     *
     * V = minValue + x * range.
     * Produz valores com densidade máxima em minValue decaindo continuamente até maxValue.
     */
    private double nextExponentialDecreasing(ThreadLocalRandom random) {
        double u = random.nextDouble();
        // Evita u == 1.0 exato que levaria ln(0)
        if (u >= 0.9999999999999999) {
            u = 0.9999999999999999;
        }
        double x = -Math.log(1.0 - u * expFactor) / lambda;
        // Garantia de clamping numérico estrito
        if (x < 0.0) x = 0.0;
        if (x > 1.0) x = 1.0;
        return minValue + x * range;
    }

    /**
     * Distribuição Exponencial Crescente Truncada em [minValue, maxValue]:
     * Reflexão simétrica da exponencial decrescente:
     * x in [0, 1] com maior probabilidade em 0.
     * Fazendo V = maxValue - x * range, a concentração migra para maxValue e diminui até minValue.
     */
    private double nextExponentialIncreasing(ThreadLocalRandom random) {
        double u = random.nextDouble();
        if (u >= 0.9999999999999999) {
            u = 0.9999999999999999;
        }
        double x = -Math.log(1.0 - u * expFactor) / lambda;
        if (x < 0.0) x = 0.0;
        if (x > 1.0) x = 1.0;
        return maxValue - x * range;
    }
}
