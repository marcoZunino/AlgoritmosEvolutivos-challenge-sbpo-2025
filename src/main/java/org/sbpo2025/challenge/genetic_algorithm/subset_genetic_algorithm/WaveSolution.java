package org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm;

import java.util.ArrayList;
import java.util.List;

import org.uma.jmetal.solution.AbstractSolution;


public class WaveSolution extends AbstractSolution<List<Integer>> {
  
  public WaveSolution(List<Integer> orders, List<Integer> aisles) {
    super(2,1,0);
    this.setVariable(0, orders);
    this.setVariable(1, aisles);
  }

  @Override
  public WaveSolution copy() {
    return new WaveSolution(new ArrayList<>(this.getVariable(0)), new ArrayList<>(this.getVariable(1)));
  }

  @Override
  public int getNumberOfVariables() {
    return 2;
  }


  public List<Integer> getOrders() {
    return this.getVariable(0);
  }

  public List<Integer> getAisles() {
    return this.getVariable(1);
  }

  public void addOrder(int orderId) {
    if (!this.getVariable(0).contains(orderId)) this.getVariable(0).add(orderId);
  }
  public void removeOrder(int orderId) {
    this.getVariable(0).remove(Integer.valueOf(orderId));
  }

  public void addAisle(int aisleId) {
    if (!this.getVariable(1).contains(aisleId)) this.getVariable(1).add(aisleId);
  }
  public void removeAisle(int aisleId) {
    this.getVariable(1).remove(Integer.valueOf(aisleId));
  }

}
