package org.sbpo2025.challenge.genetic_algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.sbpo2025.challenge.ChallengeSolution;
import org.sbpo2025.challenge.ChallengeSolver;
import org.sbpo2025.challenge.Item;

import org.uma.jmetal.algorithm.singleobjective.geneticalgorithm.GenerationalGeneticAlgorithm;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.checking.Check;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

public class GeneticAlgorithmRunner {

    public static ChallengeSolution run(ChallengeSolver solver, Map<String, Object> params) {

        List<Map<Integer, Integer>> orders = solver.orders;
        List<Map<Integer, Integer>> aisles = solver.aisles;
        List<Item> items = solver.items;
        int waveSizeLB = solver.waveSizeLB;
        int waveSizeUB = solver.waveSizeUB;

        Random random = new Random((long) params.getOrDefault("randomSeed", 1234L));
        double mutationProbability = (double) params.getOrDefault("mutationProbability", 1.0/(orders.size() + aisles.size()));
        double crossoverProbability = (double) params.getOrDefault("crossoverProbability", 0.9);

        int populationSize = (int) params.getOrDefault("populationSize", 100);
        int maxEvaluations = populationSize * (int) params.getOrDefault("generations", 100);

        WavePickingProblem problem = new WavePickingProblem(orders, aisles, items, waveSizeLB, waveSizeUB, random);

        // problem.setDistanceLambda((double) params.getOrDefault("distanceLambda", 0.5));
        // problem.setWaveSizePenalty((double) params.getOrDefault("waveSizePenalty", 10);
        

        CrossoverOperator<WaveSolution> crossover = new SinglePointCrossover(crossoverProbability, random, aisles.size(), orders.size(), (boolean) params.getOrDefault("ordersUnionCrossover", false));
        MutationOperator<WaveSolution> mutation = new BitFlipMutation(mutationProbability, random, orders.size(), aisles.size());
        SelectionOperator<List<WaveSolution>,WaveSolution> selection = new BinaryTournamentSelection(random);
        SolutionListEvaluator<WaveSolution> evaluator = new SequentialSolutionListEvaluator<>();

        GenerationalGeneticAlgorithm<WaveSolution> algorithm = new GenerationalGeneticAlgorithm<>(
                problem, maxEvaluations, populationSize, crossover, mutation, selection, evaluator);
        
        algorithm.run();

        WaveSolution result = algorithm.getResult();
        return new ChallengeSolution(
            result.getOrders().stream().collect(Collectors.toSet()),
            result.getAisles().stream().collect(Collectors.toSet())
        ); // return best solution

    }

    private static class SinglePointCrossover implements CrossoverOperator<WaveSolution> {
        
        private double crossoverProbability;
        private Random random;
        private Integer totalAislesNumber;
        private Integer totalOrdersNumber;
        private boolean ordersUnionCrossover;

        public SinglePointCrossover(double crossoverProbability, Random random, Integer totalAislesNumber, Integer totalOrdersNumber, boolean ordersUnionCrossover) {
            if (crossoverProbability < 0) {
                throw new JMetalException("Crossover probability is negative: " + crossoverProbability);
            }
            this.crossoverProbability = crossoverProbability;
            this.random = random;
            this.totalAislesNumber = totalAislesNumber;
            this.totalOrdersNumber = totalOrdersNumber;
            this.ordersUnionCrossover = ordersUnionCrossover;
        }

        @Override
        public List<WaveSolution> execute(List<WaveSolution> parents) {
            Check.isNotNull(parents);
            Check.that(parents.size() == 2, "There must be two parents instead of " + parents.size());

            if (ordersUnionCrossover) {
                return doOrdersUnionCrossover(crossoverProbability, parents);
            } else {
                return doCrossover(crossoverProbability, parents);
            }
        }

        private List<WaveSolution> doCrossover(double probability, List<WaveSolution> parents) {
            
            List<WaveSolution> offspring = new ArrayList<>(2);
            offspring.add(parents.get(0).copy());
            offspring.add(parents.get(1).copy());

            if (random.nextDouble() < probability) {

                // 1. Calculate the point to make the crossover
                int crossoverPoint = random.nextInt(totalOrdersNumber+totalAislesNumber); // between [0 and totalBits)
                // 0, 1, 2, ..... orders-1, orders, orders+1, ...., orders+aisles-1

                if (crossoverPoint < totalOrdersNumber) {
                    
                    // 2.1 Swap orders from parents after the crossover point
                    swapOrders(parents, offspring, crossoverPoint);

                } else {

                    // 2.2 Swap aisles from parents after the crossover point
                    swapAisles(parents, offspring, crossoverPoint-totalOrdersNumber-1);
                    // if crossover = #orders => crossoverPoint-totalOrdersNumber-1 = -1 => exchange aisles and orders subsets
                }

            }

            return offspring;
        }

        private List<WaveSolution> doOrdersUnionCrossover(double probability, List<WaveSolution> parents) {
            List<WaveSolution> offspring = new ArrayList<>(2);
            offspring.add(parents.get(0).copy());
            offspring.add(parents.get(1).copy());

            if (random.nextDouble() < probability) {

                // 1. Calculate the point to make the crossover
                int crossoverPoint = random.nextInt(totalAislesNumber-1);

                // 2 Swap aisles from parents after the crossover point
                swapAisles(parents, offspring, crossoverPoint);
                
                // 3 Set orders subset as the union of both parents' orders
                computeOrdersUnion(parents, offspring);

            }

            return offspring;
        }


        private void swapAisles(List<WaveSolution> parents, List<WaveSolution> offspring, int crossoverPoint) {

            for (int k = 0; k < 2; k++) { // for each parent
                for (int i = 0; i < parents.get(k).getAisles().size(); i++) { // for each aisle in parent k
                    int aisleId = parents.get(k).getAisles().get(i);
                    if (aisleId > crossoverPoint && // aisle is after crossover point
                        !parents.get(1-k).getAisles().contains(aisleId) // skip if aisle is already in both parents
                    ) {
                        offspring.get(k).removeAisle(aisleId);
                        offspring.get(1-k).addAisle(aisleId);
                            // swap aisle between offspring
                    }
                }
            }
        }

        private void swapOrders(List<WaveSolution> parents, List<WaveSolution> offspring, int crossoverPoint) {

            for (int k = 0; k < 2; k++) { // for each parent
                for (int i = 0; i < parents.get(k).getOrders().size(); i++) { // for each aisle in parent k
                    int orderId = parents.get(k).getOrders().get(i);
                    if (orderId > crossoverPoint && // aisle is after crossover point
                        !parents.get(1-k).getOrders().contains(orderId) // skip if aisle is already in both parents
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

    private static class BitFlipMutation implements MutationOperator<WaveSolution> {
        
        private double mutationProbability;
        private Random random;
        private int totalOrdersNumber;
        private int totalAislesNumber;

        public BitFlipMutation(double mutationProbability, Random random, int totalOrdersNumber, int totalAislesNumber) {
            this.mutationProbability = mutationProbability;
            this.random = random;
            this.totalOrdersNumber = totalOrdersNumber;
            this.totalAislesNumber = totalAislesNumber;
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

    private static class BinaryTournamentSelection implements SelectionOperator<List<WaveSolution>,WaveSolution> {
        private Random random;

        public BinaryTournamentSelection(Random random) {
            this.random = random;
        }

        @Override
        public WaveSolution execute(List<WaveSolution> solutionList) {
            Check.isNotNull(solutionList);
            Check.collectionIsNotEmpty(solutionList);

            WaveSolution result;
            if (solutionList.size() == 1) {
                result = solutionList.get(0);
            } else {
                List<WaveSolution> candidates = SolutionListUtils.selectNRandomDifferentSolutions(2, solutionList, (low, up) -> random.nextInt(low, up));

                System.out.println("Tournament between solutions with objectives: " + 
                    candidates.get(0).getObjective(0) + " and " + candidates.get(1).getObjective(0)
                );

                if (candidates.get(0).getObjective(0) < candidates.get(1).getObjective(0)) {
                    result = candidates.get(0);
                } else {
                    result = candidates.get(1);
                }
                
            }

            return result;
        }
    }
}

