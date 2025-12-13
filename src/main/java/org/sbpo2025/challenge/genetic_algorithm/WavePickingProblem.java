package org.sbpo2025.challenge.genetic_algorithm;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.ArrayList;
import java.util.Collections;

import org.sbpo2025.challenge.Item;
import org.uma.jmetal.problem.AbstractGenericProblem;

public class WavePickingProblem extends AbstractGenericProblem<WaveSolution> {

    protected List<Map<Integer, Integer>> orders;
    protected List<Map<Integer, Integer>> aisles;
    protected List<Item> items;
    protected int waveSizeLB;
    protected int waveSizeUB;
    protected Random random;
    protected boolean showOutput;
    protected boolean warmStart;

    protected double waveSizePenalty;

    public WavePickingProblem(List<Map<Integer, Integer>> orders,
      List<Map<Integer, Integer>> aisles,
      List<Item> items,
      int waveSizeLB,
      int waveSizeUB,
      Random random
    ) {
    
      this.orders = orders;
      this.aisles = aisles;
      this.waveSizeLB = waveSizeLB;
      this.waveSizeUB = waveSizeUB;
      this.items = items;
      
      this.random = random;
      this.waveSizePenalty = orders.size() - waveSizeLB/aisles.size(); // default penalty
      this.showOutput = false;
      this.warmStart = true;

      this.setNumberOfVariables(2);
      this.setNumberOfObjectives(1);
      this.setName("WavePickingProblem");      
      
    }


    public void setWaveSizePenalty(double penalty) {
        this.waveSizePenalty = penalty;
    }

    public void showOutput() {
        this.showOutput = true;
    }

    public void randomStart() {
        this.warmStart = false;
    }


    @Override
    public void evaluate(WaveSolution solution) {
        
        feasibilityCorrection(solution);

        // Objective function: total units picked / number of visited aisles
        double objectiveValue = computeObjectiveValue(solution);
        int penalization = waveSizePenalization(solution);
        solution.setObjective(0, -(objectiveValue - waveSizePenalty*(double)penalization));
        
        if (showOutput) System.out.println(String.format("""
            Evaluated solution with objective value: %f %s
                fitness: %f""",
            objectiveValue, 
            penalization != 0 ? String.format("and penalization: %d",penalization) : "",
            solution.getObjective(0)));
    }

    @Override
    public WaveSolution createSolution() {

        List<Integer> selectedAisles = getRandomSubset(IntStream.range(0, aisles.size()).boxed().collect(Collectors.toList()));

        if (!warmStart) { // totally random solution
            return new WaveSolution(getRandomSubset(IntStream.range(0, orders.size()).boxed().collect(Collectors.toList())), selectedAisles);
        }

        // set items total stock
        for (Item item : items) {
            item.resetStock();
            for (Map.Entry<Integer, Integer> aisle : item.aisles.entrySet()) { // for order with this item
                if (selectedAisles.contains(aisle.getKey())) {
                    item.addStock(aisle.getValue()); // Add stock from selected aisles
                }
            }
        }

        List<Integer> selectedOrders = selectRandomOrders();

        return new WaveSolution(selectedOrders, selectedAisles);

    }

    private double computeObjectiveValue(WaveSolution solution) {
        
        List<Integer> selectedOrders = solution.getOrders();
        List<Integer> visitedAisles = solution.getAisles();
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
        return (double) totalUnitsPicked / numVisitedAisles;
        // return (double) selectedOrders.size();
        // return (double) 1.0 / numVisitedAisles;
        // return (double) numVisitedAisles;
        // return (double) selectedOrders.size() + numVisitedAisles;
    }

    private int waveSizePenalization(WaveSolution solution) {
        
        int totalUnitsPicked = totalDemand(solution.getOrders());

        if (totalUnitsPicked < waveSizeLB) {
            return waveSizeLB - totalUnitsPicked;
        } else if (totalUnitsPicked > waveSizeUB) {
            return totalUnitsPicked - waveSizeUB;
        } else {
            return 0;
        }
    }

    
    private void feasibilityCorrection(WaveSolution solution) {
        
        for (Item item : items) {
            
            int itemDemand = totalDemand(solution.getOrders(), List.of(item));
            if (itemDemand <= 0) continue;
            int itemCapacity = totalCapacity(solution.getAisles(), List.of(item));
            
            List<Integer> itemOrders = item.orders.keySet().stream() // orders that contain item i
                    .filter(o -> solution.getOrders().contains(o)) // get only selected orders
                    .collect(Collectors.toList());
            
            while (itemDemand > itemCapacity) {
                // remove random order that contains item i
                int oToRemove = itemOrders.get(random.nextInt(itemOrders.size()));
                solution.removeOrder(oToRemove);
                itemOrders.remove(Integer.valueOf(oToRemove));
                itemDemand -= item.getOrderDemand(oToRemove);
            }
        }
        // while (demand > waveSizeUB) {
        //     // remove random order
        //     List<Integer> selectedOrders = solution.getOrders().stream().collect(Collectors.toList());
        //     if (selectedOrders.isEmpty()) {
        //         break;
        //     }
        //     int oToRemove = selectedOrders.get(random.nextInt(selectedOrders.size()));
        //     solution.removeOrder(oToRemove);
        //     demand -= totalDemand(List.of(oToRemove));
        // }

        // removeUnusedAisles(solution);

        int capacity = totalCapacity(solution.getAisles());
        while (capacity < waveSizeLB) {
            // add random aisle
            int a = random.nextInt(aisles.size());
            if (!solution.getAisles().contains(a)) {
                solution.addAisle(a);
                capacity += totalCapacity(List.of(a));
            }
        }
        
    }

    // private void removeUnusedAisles(WaveSolution solution) {

    //     List<Integer> selectedOrders = solution.getOrders();
    //     List<Integer> visitedAisles = solution.getAisles();

    //     for (int aisle : visitedAisles) {
    //         // Check if removing this aisle still keeps the solution feasible
    //         List<Integer> aislesWithoutCurrent = visitedAisles.stream()
    //             .filter(a -> a != aisle)
    //             .collect(Collectors.toList());
    //         if (availableCapacity(selectedOrders, aislesWithoutCurrent)) {
    //             // Remove the aisle
    //             solution.removeAisle(aisle);
    //             removeUnusedAisles(solution);
    //             break;
    //         }
    //     }

    // }

    // private boolean feasible(WaveSolution solution) {
    //     return availableCapacity(solution.getOrders(), solution.getAisles());
    // }

    private int totalCapacity(List<Integer> aislesList, List<Item> itemsList) {

        int totalCapacity = 0;

        for (Item item : itemsList) {
            for (Map.Entry<Integer, Integer> aisle : item.aisles.entrySet()) {
                if (aislesList.contains(aisle.getKey())) {
                    totalCapacity += aisle.getValue();
                }
            }
        }

        return totalCapacity;
    }
    private int totalCapacity(List<Integer> aislesList) {
        return totalCapacity(aislesList, items);
    }

    private int totalDemand(List<Integer> ordersList, List<Item> itemsList) {
        int totalDemand = 0;

        for (Item item : itemsList) {
            for (Map.Entry<Integer, Integer> order : item.orders.entrySet()) {
                if (ordersList.contains(order.getKey())) {
                    totalDemand += order.getValue();
                }
            }
        }

        return totalDemand;
    }
    private int totalDemand(List<Integer> ordersList) {
        return totalDemand(ordersList, items);
    }

    // private boolean availableCapacity(List<Integer> ordersList, List<Integer> aislesList) {
        
    //   for (Item item : items) {
    //         if (totalDemand(ordersList, List.of(item)) > totalCapacity(aislesList, List.of(item))) {
    //             return false;
    //         }
    //     }
    //     return true;
    // }

    private List<Integer> getRandomSubset(List<Integer> set) {
        
        if (set.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> subset = new ArrayList<>(set); // copy the original set
        Collections.shuffle(subset, random);  // shuffle the copy

        int ordersCount = random.nextInt(set.size()) + 1; // at least one element
        return new ArrayList<>(subset.subList(0, ordersCount));  // return the first 'ordersCount' elements
    }


    public List<Integer> selectRandomOrders() {

        List<Integer> selectedOrders = new ArrayList<>();

        List<Item> shuffledItems = new ArrayList<>(items);
        Collections.shuffle(shuffledItems, random);

        for (Item item : shuffledItems) { // for item

            List<Map.Entry<Integer, Integer>> shuffledItemOrders = new ArrayList<>(item.orders.entrySet());
            Collections.shuffle(shuffledItemOrders, random); // shuffle orders with this item

            for (Map.Entry<Integer, Integer> order : shuffledItemOrders) { // for order with this item

                // Check if the order can be fulfilled

                // 1. check only "item"
                if (item.stock < order.getValue()) continue;  // skip this order

                int orderId = order.getKey();
                if (selectedOrders.contains(orderId)) continue; // skip if already selected
                
                // 2. check all items required in the order
                boolean enoughStock = true;
                for (Map.Entry<Integer, Integer> entry : orders.get(orderId).entrySet()) {

                    Item itemNeeded = items.get(entry.getKey());
                    int itemQuantity = entry.getValue();

                    if (itemNeeded.stock < itemQuantity) {
                        enoughStock = false; // Not enough stock for item found
                        break;
                    }
                }
                
                if (!enoughStock) continue;  // skip this order

                selectedOrders.add(orderId);

                // update stock
                for (Map.Entry<Integer, Integer> entry : orders.get(orderId).entrySet()) { // for item in order
                    Item itemNeeded = items.get(entry.getKey());
                    int itemQuantity = entry.getValue();
                    
                    itemNeeded.removeStock(itemQuantity);
                }

            }
        }

        return selectedOrders;
    }


}