package org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm.operators;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm.WaveSolution;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.checking.Check;


public class WaveUniformCrossover implements CrossoverOperator<WaveSolution> {
        
    private double crossoverProbability;
    private Random random;
    private boolean ordersUnionCrossover;
    
    public WaveUniformCrossover(double crossoverProbability, boolean ordersUnionCrossover, Random random) {
        if (crossoverProbability < 0) {
            throw new JMetalException("Crossover probability is negative: " + crossoverProbability);
        }
        this.crossoverProbability = crossoverProbability;
        this.random = random;
        this.ordersUnionCrossover = ordersUnionCrossover;
    }
    @Override
    public List<WaveSolution> execute(List<WaveSolution> parents) {
        Check.isNotNull(parents);
        Check.that(parents.size() == 2, "There must be two parents instead of " + parents.size());
        if (ordersUnionCrossover) {
            return doOrdersUnionCrossover(parents);
        } else {
            return doCrossover(parents);
        }
    }
    private List<WaveSolution> doCrossover(List<WaveSolution> parents) {
        
        List<WaveSolution> offspring = new ArrayList<>(2);
        offspring.add(parents.get(0).copy());
        offspring.add(parents.get(1).copy());
        if (random.nextDouble() < crossoverProbability) {
            // 1. Swap orders from parents
            swapOrders(parents, offspring);
            // 2. Swap aisles from parents
            swapAisles(parents, offspring);
        }
        return offspring;
    }
    private List<WaveSolution> doOrdersUnionCrossover(List<WaveSolution> parents) {
        List<WaveSolution> offspring = new ArrayList<>(2);
        offspring.add(parents.get(0).copy());
        offspring.add(parents.get(1).copy());
        if (random.nextDouble() < crossoverProbability) {
            // 1. Swap aisles from parents
            swapAisles(parents, offspring);
            
            // 2. Set orders subset as the union of both parents' orders
            computeOrdersUnion(parents, offspring);
        }
        return offspring;
    }
    private void swapAisles(List<WaveSolution> parents, List<WaveSolution> offspring) {
        for (int k = 0; k < 2; k++) { // for each parent
            for (int i = 0; i < parents.get(k).getAisles().size(); i++) { // for each aisle in parent k
                int aisleId = parents.get(k).getAisles().get(i);
                if (!parents.get(1-k).getAisles().contains(aisleId) // skip if aisle is already in both parents
                    // && aisleId > crossoverPoint // aisle is after crossover point
                    && random.nextDouble() < 0.5 // half chance to swap
                ) {
                    offspring.get(k).removeAisle(aisleId);
                    offspring.get(1-k).addAisle(aisleId);
                        // swap aisle between offspring
                }
            }
        }
    }
    private void swapOrders(List<WaveSolution> parents, List<WaveSolution> offspring) {
        for (int k = 0; k < 2; k++) { // for each parent
            for (int i = 0; i < parents.get(k).getOrders().size(); i++) { // for each aisle in parent k
                int orderId = parents.get(k).getOrders().get(i);
                if (!parents.get(1-k).getOrders().contains(orderId) // skip if aisle is already in both parents
                    // && orderId > crossoverPoint // aisle is after crossover point
                    && random.nextDouble() < 0.5 // half chance to swap
                ) {
                    offspring.get(k).removeOrder(orderId);
                    offspring.get(1-k).addOrder(orderId);
                        // swap aisle between offspring
                }
            }
        }
    }
    private void computeOrdersUnion(List<WaveSolution> parents, List<WaveSolution> offspring) {
        for (int k = 0; k < 2; k++) { // for each parent
            for (int i = 0; i < parents.get(1-k).getOrders().size(); i++) { // for each order in parent 1-k
                int orderId = parents.get(1-k).getOrders().get(i);
                if (!parents.get(k).getOrders().contains(orderId)) {
                    offspring.get(k).addOrder(orderId);
                        // add missing orders to offspring
                }
            }
        }
    }
    @Override
    public int getNumberOfRequiredParents() {
        return 2;
    }
    @Override
    public int getNumberOfGeneratedChildren(){
        return 2;
    }
    @Override
    public double getCrossoverProbability() {
        return crossoverProbability;
    }
}


