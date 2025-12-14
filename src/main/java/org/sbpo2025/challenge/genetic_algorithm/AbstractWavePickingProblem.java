package org.sbpo2025.challenge.genetic_algorithm;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.sbpo2025.challenge.Item;
import org.uma.jmetal.problem.AbstractGenericProblem;
import org.uma.jmetal.solution.Solution;

public abstract class AbstractWavePickingProblem<S extends Solution<?>>
        extends AbstractGenericProblem<S> {

    public List<Map<Integer, Integer>> orders;
    public List<Map<Integer, Integer>> aisles;
    public List<Item> items;
    public int waveSizeLB;
    public int waveSizeUB;
    public Random random;

    public AbstractWavePickingProblem(
            List<Map<Integer, Integer>> orders,
            List<Map<Integer, Integer>> aisles,
            List<Item> items,
            int waveLB,
            int waveUB,
            long randomSeed
    ) {
        this.orders = orders;
        this.aisles = aisles;
        this.items = items;
        this.waveSizeLB = waveLB;
        this.waveSizeUB = waveUB;
        this.random = new Random(randomSeed);
    }

    public void setRandomSeed(long seed) {
        this.random.setSeed(seed);
    }

    @Override
    public abstract S createSolution();

    @Override
    public abstract void evaluate(S solution);
}
