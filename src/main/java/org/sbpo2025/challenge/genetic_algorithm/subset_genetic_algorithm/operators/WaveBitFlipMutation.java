package org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm.operators;

import java.util.Random;

import org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm.WaveSolution;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.util.checking.Check;

public class WaveBitFlipMutation implements MutationOperator<WaveSolution> {
        
    private double mutationProbability;
    private Random random;
    private int totalOrdersNumber;
    private int totalAislesNumber;
    
    public WaveBitFlipMutation(double mutationProbability, int totalOrdersNumber, int totalAislesNumber, Random random) {
        this.random = random;
        this.totalOrdersNumber = totalOrdersNumber;
        this.totalAislesNumber = totalAislesNumber;
        this.mutationProbability = mutationProbability == -1 ? 1.0 / (totalOrdersNumber + totalAislesNumber) : mutationProbability;
        // -1: set default value
    }

    @Override
    public WaveSolution execute(WaveSolution solution) {
        Check.isNotNull(solution);
        doMutation(mutationProbability, solution);
        return solution;
    }

    public void doMutation(double probability, WaveSolution solution) {
        
        for (int i = 0; i < totalOrdersNumber; i++) {
            if (random.nextDouble() < probability) {
                // flip order bit
                if (solution.getOrders().contains(i)) {
                    solution.removeOrder(i);
                } else {
                    solution.addOrder(i);
                }
            }
        }
        for (int i = 0; i < totalAislesNumber; i++) {
            if (random.nextDouble() < probability) {
                // flip aisle bit
                if (solution.getAisles().contains(i)) {
                    solution.removeAisle(i);
                } else {
                    solution.addAisle(i);
                }
            }
        }
    }

    @Override
    public double getMutationProbability() {
        return mutationProbability;
    }

}

