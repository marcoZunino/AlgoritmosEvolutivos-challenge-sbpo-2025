package org.sbpo2025.challenge.binary_genetic_algorithm;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.Arrays;

import org.sbpo2025.challenge.Item;
import org.uma.jmetal.problem.binaryproblem.impl.AbstractBinaryProblem;
import org.uma.jmetal.solution.binarysolution.BinarySolution;

public class BinaryWavePickingProblem extends AbstractBinaryProblem {

    protected List<Map<Integer, Integer>> orders;
    protected List<Map<Integer, Integer>> aisles;
    protected List<Item> items;
    protected int waveSizeLB;
    protected int waveSizeUB;
    protected Random random;
    protected boolean showOutput;

    protected double waveSizePenalty;

    public BinaryWavePickingProblem(List<Map<Integer, Integer>> orders,
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

      this.setNumberOfVariables(2);
      this.setNumberOfObjectives(1);
      this.setName("BinaryWavePickingProblem");
      
    }

    public void setWaveSizePenalty(double penalty) {
        this.waveSizePenalty = penalty;
    }

    public void showOutput() {
        this.showOutput = true;
    }

    @Override
    public List<Integer> getListOfBitsPerVariable() {
        return Arrays.asList(orders.size(), aisles.size());
    }

    @Override
    public void evaluate(BinarySolution solution) {
        
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

    private double computeObjectiveValue(BinarySolution solution) {
        
        List<Integer> selectedOrders = getSelectedOrders(solution);
        List<Integer> visitedAisles = getVisitedAisles(solution);
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

        int numVisitedAisles = visitedAisles.size();
        return (double) totalUnitsPicked / numVisitedAisles;
        // return (double) selectedOrders.size();
        // return (double) 1.0 / numVisitedAisles;
        // return (double) numVisitedAisles;
        // return (double) selectedOrders.size() + numVisitedAisles;
    }

    private int waveSizePenalization(BinarySolution solution) {
        
        int totalUnitsPicked = totalDemand(getSelectedOrders(solution));

        if (totalUnitsPicked < waveSizeLB) {
            return waveSizeLB - totalUnitsPicked;
        } else if (totalUnitsPicked > waveSizeUB) {
            return totalUnitsPicked - waveSizeUB;
        } else {
            return 0;
        }
    }


    private void feasibilityCorrection(BinarySolution solution) {

        // int demand = totalDemand(getSelectedOrders(solution));
        // while (demand < waveSizeUB) {
        //     // add random order
        //     int o = random.nextInt(orders.size());
        //     if (!solution.getVariable(0).get(o) && (availableCapacity(List.of(o), getVisitedAisles(solution)))) {
        //         solution.getVariable(0).set(o, true);
        //         demand += totalDemand(List.of(o));
        //     }
        // }
        for (Item item : items) {
            
            int itemDemand = totalDemand(getSelectedOrders(solution), List.of(item));
            int itemCapacity = totalCapacity(getVisitedAisles(solution), List.of(item));
            
            List<Integer> itemOrders = item.orders.keySet().stream() // orders that contain item i
                    .filter(o -> solution.getVariable(0).get(o)) // get only selected orders
                    .collect(Collectors.toList());

            while (itemDemand > itemCapacity) {
                // remove random order that contains item i
                
                int oToRemove = itemOrders.get(random.nextInt(itemOrders.size()));
                solution.getVariable(0).set(oToRemove, false);
                itemOrders.remove(Integer.valueOf(oToRemove));
                itemDemand -= item.getOrderDemand(oToRemove);
                // demand -= totalDemand(List.of(oToRemove));

            }
        }
        // while (demand > waveSizeUB) {
        //     // remove random order
        //     List<Integer> selectedOrders = getSelectedOrders(solution).stream().collect(Collectors.toList());
        //     if (selectedOrders.isEmpty()) {
        //         break;
        //     }
        //     int oToRemove = selectedOrders.get(random.nextInt(selectedOrders.size()));
        //     solution.getVariable(0).set(oToRemove, false);
        //     demand -= totalDemand(List.of(oToRemove));
        // }

        // removeUnusedAisles(solution);

        int capacity = totalCapacity(getVisitedAisles(solution));
        while (capacity < waveSizeLB) {
            // add random aisle
            int a = random.nextInt(aisles.size());
            if (!solution.getVariable(1).get(a)) {
                solution.getVariable(1).set(a, true);
                capacity += totalCapacity(List.of(a));
            }
        }

        
    }

    // private void removeUnusedAisles(BinarySolution solution) {

    //     List<Integer> selectedOrders = getSelectedOrders(solution);
    //     List<Integer> visitedAisles = getVisitedAisles(solution);

    //     for (int aisle : visitedAisles) {
    //         // Check if removing this aisle still keeps the solution feasible
    //         List<Integer> aislesWithoutCurrent = visitedAisles.stream()
    //             .filter(a -> a != aisle)
    //             .collect(Collectors.toList());
    //         if (availableCapacity(selectedOrders, aislesWithoutCurrent)) {
    //             // Remove the aisle
    //             solution.getVariable(1).set(aisle, false);
    //             removeUnusedAisles(solution);
    //             break;
    //         }
    //     }
    // }

    // private boolean feasible(BinarySolution solution) {
    //     return availableCapacity(getSelectedOrders(solution), getVisitedAisles(solution));
    // }

    public List<Integer> getSelectedOrders(BinarySolution solution) {
        // getVariable(0) returns a BinarySet (bitstring)
        return solution.getVariable(0)
                   .stream()                 // stream over indices of set bits
                   .boxed()                  // convert IntStream to Stream<Integer>
                   .collect(Collectors.toList()); // collect into a List<Integer>
    }

    public List<Integer> getVisitedAisles(BinarySolution solution) {
        // getVariable(0) returns a BinarySet (bitstring)
        return solution.getVariable(1)
                   .stream()                 // stream over indices of set bits
                   .boxed()                  // convert IntStream to Stream<Integer>
                   .collect(Collectors.toList()); // collect into a List<Integer>
    }

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


}