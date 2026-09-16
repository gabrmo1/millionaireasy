package br.com.bot_mexc.utils.buffers;

/**
 * Primitive Ring Buffer otimizado para HFT e janelas móveis (Mechanical Sympathy).
 * Garante O(1) para inserção e soma móvel (SMA).
 * Zero-allocation no Heap após a inicialização. Sem autoboxing.
 *
 * NOTA: Estrutura desenhada para Thread-Confinement (Actor Model) no WebSocket.
 * Não possui locks internos para máxima performance no L1 Cache.
 */
public class PrimitiveRingBuffer {
    private final double[] buffer;
    private final int capacity;

    private int head = 0;
    private int count = 0;
    private double runningSum = 0.0d;

    public PrimitiveRingBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new double[capacity];
    }

    /**
     * Insere um novo valor no buffer circular.
     * Mantém a soma atualizada em O(1) removendo a cauda.
     */
    public void add(double value) {
        if (count == capacity) {
            runningSum -= buffer[head];
        } else {
            count++;
        }

        buffer[head] = value;
        runningSum += value;

        // Bitwise AND é mais rápido que módulo (%), mas exige que capacity seja potência de 2.
        // Usamos módulo padrão para flexibilidade de períodos (ex: 14, 20), já que o custo na JVM moderna é mitigado.
        head = (head + 1) % capacity;
    }

    public double getSum() {
        return runningSum;
    }

    public double getAverage() {
        return count == 0 ? 0.0d : runningSum / count;
    }

    /**
     * Cálculo de Desvio Padrão O(N) confinado no array primitivo L1.
     * Essencial para Bollinger Bands.
     */
    public double getStandardDeviation() {
        if (count == 0) return 0.0d;

        double mean = getAverage();
        double varianceSum = 0.0d;

        // Loop otimizado para branch prediction e vetorização (SIMD)
        for (int i = 0; i < count; i++) {
            double diff = buffer[i] - mean;
            varianceSum += diff * diff;
        }

        return Math.sqrt(varianceSum / count);
    }

    public boolean isFull() {
        return count == capacity;
    }

    public int getCount() {
        return count;
    }

    public void reset() {
        this.head = 0;
        this.count = 0;
        this.runningSum = 0.0d;
    }
}