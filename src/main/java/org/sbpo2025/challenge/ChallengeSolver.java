package org.sbpo2025.challenge;

import org.apache.commons.lang3.time.StopWatch;
import org.sbpo2025.challenge.genetic_algorithm.binary_genetic_algorithm.BinaryGeneticAlgorithmRunner;
import org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm.GeneticAlgorithmRunner;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChallengeSolver {
    private final long MAX_RUNTIME = 600000; // milliseconds

    public List<Map<Integer, Integer>> orders;
    public List<Map<Integer, Integer>> aisles;
    public List<Item> items;
    public int nItems;
    public int waveSizeLB;
    public int waveSizeUB;
    public boolean showOutput = false;

    public ChallengeSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        
        this.orders = orders;
        this.aisles = aisles;
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
        initializeItems();

    }

    public ChallengeSolution solve(StopWatch stopWatch, Map<String, Object> params) {
        
        if ((boolean) params.getOrDefault("showStats", false)) showStats();
        if ((boolean) params.getOrDefault("showOutput", false)) this.showOutput = true;

        PartialResult bestSolution = new PartialResult(null, 0);

        switch ((String) params.getOrDefault("algorithm", "")) {
                
            case "greedy":
                // Algoritmo Greedy
                bestSolution = solveGreedySelection(bestSolution, stopWatch);
                    
                break;
                
            case "genetic":

                Random random = new Random((long) params.getOrDefault("randomSeed", 1234L));
                
                // Algoritmo Genético
                for (int i = 0; i < (int) params.getOrDefault("maxIterations", 1); i++) {
                    params.put("randomSeed", random.nextLong());
                    bestSolution = solveGeneticAlgorithm(bestSolution, stopWatch, params);
                }
                break;

            default:
                System.out.println("No valid algorithm selected.");
                return null;
            
        }
    
        
        if (bestSolution.partialSolution() == null) {
            
            System.out.println("No feasible solution found.");

        } else {
        
            // retrieve the final best solution
            System.out.println("\nBest solution found with value " + bestSolution.objValue());

            System.out.println(String.format("%d aisles / %d orders",
                bestSolution.partialSolution().aisles().size(),
                bestSolution.partialSolution().orders().size()));

        }

        System.out.println("Total execution time: " + (double) stopWatch.getTime(TimeUnit.MILLISECONDS)/1000 + " s");

        return bestSolution.partialSolution();
    }


    // solving methods

    protected PartialResult solveGeneticAlgorithm(PartialResult bestSolution, StopWatch stopWatch, Map<String, Object> params) {
        System.out.println("\n>> solveGeneticAlgorithm");

        if (getRemainingTime(stopWatch) < 1) {
            System.out.println("Max runtime reached");
            return bestSolution;
        }
        if (showOutput) System.out.println("Remaining time: " + getRemainingTime(stopWatch) + " seconds");

        ChallengeSolution gaSolution;

        if ((boolean) params.getOrDefault("binaryEncoding", false)) {
            gaSolution = BinaryGeneticAlgorithmRunner.run(this, params);
        } else {
            gaSolution = GeneticAlgorithmRunner.run(this, params);
        }

        if (gaSolution == null || !isSolutionFeasible(gaSolution)) {
            if (showOutput) System.out.println("No feasible solution found");
            return bestSolution;
        }

        double objValue = computeObjectiveFunction(gaSolution);
        System.out.println("Objective value = " + objValue);

        // update best solution
        if (objValue > bestSolution.objValue()) {
            bestSolution = new PartialResult(gaSolution, objValue);
        }

        return bestSolution;
    }


    protected PartialResult solveGreedySelection(PartialResult bestSolution, StopWatch stopWatch) {
        System.out.println("\n>> solveGreedySelection");

        Set<Integer> selectedAisles = new HashSet<>();
        Set<Integer> remainingAisles = IntStream.range(0, aisles.size()).boxed().collect(Collectors.toSet());

        int waveSize = 0;

        // iterate over the number of aisles
        for (int k = 1; k <= aisles.size(); k++) {

            if (getRemainingTime(stopWatch) < 1) {
                System.out.println("Max runtime reached, stopping iteration over k.");
                break;
            } // stop iteration if no time left
            // System.out.println("Remaining time: " + getRemainingTime(stopWatch) + " seconds");

            // if (waveSize >= waveSizeLB) {
            //     System.out.println("LB reached");
            //     break; // opcional
            // }
            
            if (waveSize >= waveSizeLB && waveSizeUB/k <= bestSolution.objValue()) {
                // stopping condition due to optimality
                if (showOutput) System.out.println("Current best solution with value " + bestSolution.objValue() + " is already better than the maximum possible for k >= " + k);
                break;
            }

            if (showOutput) System.out.println("\nSelecting orders of available items from " + k + " aisles");

            int aisle = maxCapacityAisle(remainingAisles);
            if (aisle == -1) {
                System.out.println("No aisles found in the list.");
                break;
            }
            remainingAisles.remove(aisle);
            selectedAisles.add(aisle);

            PartialResult partialResult = solveSuperAisleGreedySelection(stopWatch, selectedAisles);

            if (partialResult.partialSolution() == null) {
                if (showOutput) System.out.println("No feasible solution found");
            } else {

                waveSize = totalDemand(partialResult.partialSolution().orders());
                
                if (showOutput) System.out.println("Objective value = " + partialResult.objValue());
                
                if (isSolutionFeasible(partialResult.partialSolution()) && partialResult.objValue() > bestSolution.objValue()) {
                    bestSolution = partialResult; // update best solution
                }
            }

        }

        if (showOutput) System.out.println("Done iterating over selected aisles");
        if (showOutput) System.out.println("Best solution found with value " + bestSolution.objValue());

        return bestSolution;
    }

    protected PartialResult solveSuperAisleGreedySelection(StopWatch stopWatch, Set<Integer> selectedAisles) {

        // Implementar el algoritmo greedy para seleccionar órdenes sobre un subconjunto de pasillos
        
        // Crear un "super-pasillo" ficticio que combine los pasillos seleccionados
        // set items stock
        for (Item item : items) {
            item.resetStock();
            for (Map.Entry<Integer, Integer> aisle : item.aisles.entrySet()) { // for order with this item
                if (selectedAisles.contains(aisle.getKey())) {
                    item.addStock(aisle.getValue()); // Add stock from selected aisles
                }
            }
        }

        Set<Integer> selectedOrders = selectOrders();

        return generatePartialResult(selectedOrders, selectedAisles);

    }


    public Set<Integer> selectOrders() {

        Set<Integer> selectedOrders = new HashSet<>();

        // recorrer items para seleccionar ordenes
        int waveSize = 0;

        List<Item> sortedItems = new ArrayList<>(items).stream()
                .sorted(Comparator.comparingInt(item -> -item.stock)).toList();
        
        for (Item item : sortedItems) { // for item in aisle

            List<Map.Entry<Integer, Integer>> sortedOrders = new ArrayList<>(item.orders.entrySet());
            sortedOrders.sort(Map.Entry.<Integer, Integer>comparingByValue().reversed());

            for (Map.Entry<Integer, Integer> order : sortedOrders) { // for order with this item

                int orderId = order.getKey();
                if (selectedOrders.contains(orderId)) continue; // already selected

                int orderDemand = 0;

                boolean enoughStock = true;
                // Check if the order can be fulfilled
                if (item.stock < order.getValue()) { // check only "item"
                    enoughStock = false;
                }
                for (Map.Entry<Integer, Integer> entry : orders.get(orderId).entrySet()) { // check all items
                    Item itemNeeded = items.get(entry.getKey());
                    int itemQuantity = entry.getValue();
                    orderDemand += itemQuantity;

                    if (itemNeeded.stock < itemQuantity) {
                        enoughStock = false; // Not enough stock for item found
                        break;
                    }
                }
                if (!enoughStock || waveSize + orderDemand > waveSizeUB) { // do not exceed upper bound
                    continue;
                }

                selectedOrders.add(orderId);

                // update stock
                for (Map.Entry<Integer, Integer> entry : orders.get(orderId).entrySet()) { // for item in order
                    Item itemNeeded = items.get(entry.getKey());
                    int itemQuantity = entry.getValue();
                    
                    itemNeeded.removeStock(itemQuantity);
                }

                waveSize += orderDemand;
            }
        }

        return selectedOrders;
    }
   

    /*
     * Get the remaining time in seconds
     */
    protected long getRemainingTime(StopWatch stopWatch) {
        return Math.max(
                TimeUnit.SECONDS.convert(MAX_RUNTIME - stopWatch.getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
                0);
    }

    public int maxCapacityAisle(Set<Integer> aislesList) {

        int maxAisle = -1;
        int max = 0;
        int capacity = 0;

        for (int aisle : aislesList) { // each aisle
            capacity = 0;
            for (Map.Entry<Integer, Integer> entry : aisles.get(aisle).entrySet()) {
                capacity += entry.getValue(); // each item
            }
            if (capacity > max) { // update
                max = capacity;
                maxAisle = aisle;
            }
        }

        if (maxAisle == -1) {
            System.out.println("Max aisle not found.");
        }
        // System.out.println("Max aisle: " + maxAisle + " with capacity " + max);
        return maxAisle;
    }

    public int totalCapacity(Set<Integer> aislesList) {

        int totalCapacity = 0;

        for (int aisle : aislesList) {
            for (Map.Entry<Integer, Integer> entry : aisles.get(aisle).entrySet()) {
                totalCapacity += entry.getValue();
            }
        }

        return totalCapacity;
    }

    public int totalDemand(Set<Integer> ordersList) {

        int totalDemand = 0;

        for (int order : ordersList) {
            for (Map.Entry<Integer, Integer> entry : orders.get(order).entrySet()) {
                totalDemand += entry.getValue();
            }
        }

        return totalDemand;
    }

    public double totalCapacityLeft(ChallengeSolution partialSolution) { // between 0 and 1
        if (partialSolution == null) {
            return 0;
        }

        int demand = totalDemand(partialSolution.orders());
        int capacity = totalCapacity(partialSolution.aisles());

        if (capacity == 0) {
            return 0;
        }
        return (capacity-demand)/(double)capacity;
    }

    public double[] calculateMeanAisleCapacity(Set<Integer> aislesList) {

        double meanSize = 0;
        double meanItems = 0;
        int aisleCapacity = 0;
        int count = 0;

        for (int aisle : aislesList) { // each aisle
            aisleCapacity = 0;
            count = 0;
            for (Map.Entry<Integer, Integer> entry : aisles.get(aisle).entrySet()) {
                aisleCapacity += entry.getValue(); // each item
                count++;
            }
            meanSize += aisleCapacity;
            meanItems += count;
        }

        return new double[] {meanSize / (double) aisles.size(), meanItems / (double) aisles.size()};
    }

    public double[] calculateMeanOrderSize(Set<Integer> ordersList) {

        double meanSize = 0;
        double meanItems = 0;
        int orderSize = 0;
        int count = 0;

        for (int order : ordersList) { // each order
            orderSize = 0;
            count = 0;
            for (Map.Entry<Integer, Integer> entry : orders.get(order).entrySet()) {
                orderSize += entry.getValue(); // each item
                count++;
            }
            meanSize += orderSize;
            meanItems += count;
        }

        return new double[] {meanSize / (double) orders.size(), meanItems / (double) orders.size()};
    }

    public void showStats() {
        System.out.println("\n>> Problem Stats");

        double[] aisleStats = calculateMeanAisleCapacity(IntStream.range(0, aisles.size()).boxed().collect(Collectors.toSet()));
        System.out.println(String.format("Mean aisle capacity: %.2f", aisleStats[0]));
        System.out.println(String.format("Mean aisle items: %.2f", aisleStats[1]));

        double[] orderStats = calculateMeanOrderSize(IntStream.range(0, orders.size()).boxed().collect(Collectors.toSet()));
        System.out.println(String.format("Mean order size: %.2f", orderStats[0]));
        System.out.println(String.format("Mean order items: %.2f", orderStats[1]));

        System.out.println(String.format("Total items: %d", nItems));
        System.out.println(String.format("Total orders: %d", orders.size()));
        System.out.println(String.format("Total aisles: %d", aisles.size()));

        System.out.println(String.format("Wave size bounds: %d - %d", waveSizeLB, waveSizeUB));

    }

    protected PartialResult generatePartialResult(Set<Integer> selectedOrders, Set<Integer> selectedAisles) {
        // Implement the logic to calculate the partial result based on selected orders and aisles

        Set<Integer> finalAisles = new HashSet<>();
        for (Integer a : selectedAisles) {
            finalAisles.add(a);
        }
        Set<Integer> finalOrders = new HashSet<>();
        for (Integer o : selectedOrders) {
            finalOrders.add(o);
        }

        ChallengeSolution challengeSolution = new ChallengeSolution(finalOrders, finalAisles);

        if (!isSolutionFeasible(challengeSolution)) {
            return new PartialResult(null, 0);
        }

        return new PartialResult(challengeSolution, computeObjectiveFunction(challengeSolution));
    }


    private void initializeItems() {
        
        this.items = new ArrayList<>();
        for (int i = 0; i < nItems; i++) {
            this.items.add(new Item(i, new HashMap<>(), new HashMap<>()));
        }
        for (int orderId = 0; orderId < orders.size(); orderId++) {
            Map<Integer, Integer> order = orders.get(orderId);
            for (Map.Entry<Integer, Integer> entry : order.entrySet()) {
                int itemId = entry.getKey();
                int quantity = entry.getValue();
                this.items.get(itemId).addOrder(orderId, quantity);
            }
        }
        for (int aisleId = 0; aisleId < aisles.size(); aisleId++) {
            Map<Integer, Integer> aisle = aisles.get(aisleId);
            for (Map.Entry<Integer, Integer> entry : aisle.entrySet()) {
                int itemId = entry.getKey();
                int capacity = entry.getValue();
                this.items.get(itemId).addAisle(aisleId, capacity);
            }
        }
        
    }

    protected boolean isSolutionFeasible(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return false;
        }

        int[] totalUnitsPicked = new int[nItems];
        int[] totalUnitsAvailable = new int[nItems];

        // Calculate total units picked
        for (int order : selectedOrders) {
            for (Map.Entry<Integer, Integer> entry : orders.get(order).entrySet()) {
                totalUnitsPicked[entry.getKey()] += entry.getValue();
            }
        }

        // Calculate total units available
        for (int aisle : visitedAisles) {
            for (Map.Entry<Integer, Integer> entry : aisles.get(aisle).entrySet()) {
                totalUnitsAvailable[entry.getKey()] += entry.getValue();
            }
        }

        // Check if the total units picked are within bounds
        int totalUnits = Arrays.stream(totalUnitsPicked).sum();
        if (totalUnits < waveSizeLB || totalUnits > waveSizeUB) {
            return false;
        }

        // Check if the units picked do not exceed the units available
        for (int i = 0; i < nItems; i++) {
            if (totalUnitsPicked[i] > totalUnitsAvailable[i]) {
                return false;
            }
        }

        return true;
    }

    protected double computeObjectiveFunction(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return 0.0;
        }
        int totalUnitsPicked = 0;

        // Calculate total units picked
        for (int order : selectedOrders) {
            totalUnitsPicked += orders.get(order).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        // Calculate the number of visited aisles
        int numVisitedAisles = visitedAisles.size();

        // Objective function: total units picked / number of visited aisles
        return (double) totalUnitsPicked / numVisitedAisles;
    }
}
