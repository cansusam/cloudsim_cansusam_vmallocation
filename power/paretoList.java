package org.cloudbus.cloudsim.cloudsim_cansusam_vmallocation.power;

import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class paretoList {

    int listLimit = 10;
    int currentElementNumber = 0;
    List<LinkedList<Map<String, Object>>>  migrationPlans;
    ArrayList<migrationPlanScores> scores;
    int globalMinVM,globalMaxVM;
    double globalMaxEnergy,globalMinEnergy;
    int[] ranks = {};
    List<Double> planFitnesses = new ArrayList<Double>();

    public paretoList() {
        this.migrationPlans = new ArrayList<LinkedList<Map<String, Object>>>();
        this.scores = new ArrayList<migrationPlanScores>();
        this.globalMaxEnergy = Double.NEGATIVE_INFINITY;
        this.globalMinEnergy = Double.POSITIVE_INFINITY;
        this.globalMaxVM = 0;
        this.globalMinVM = Integer.MAX_VALUE;
    }

    public void compareAddRemove(LinkedList<Map<String, Object>> newPlan, migrationPlanScores newScores) {
        if (currentElementNumber < listLimit) {
            boolean flagOfNewPlanAdd = false;
            if (!scores.isEmpty()) {
                for (int j=0; j<currentElementNumber;j++) {
                    migrationPlanScores m = scores.get(j);
                    if (PowerVmAllocationPolicyAICS.dominationCheck(
                            newScores.energyChange,
                            m.energyChange,
                            newScores.vmMigrationNumber,
                            m.vmMigrationNumber)) {
                        int indexOfRemove = scores.indexOf(m);

                        // remove dominated plan
                        migrationPlans.remove(indexOfRemove);
                        scores.remove(indexOfRemove);
                        planFitnesses.remove(indexOfRemove);
                        currentElementNumber--;
                        j--;
                        // set the flag of addition
                        if (!flagOfNewPlanAdd) {
                            flagOfNewPlanAdd = true;
                        }
                    }
                }
            }else
                flagOfNewPlanAdd = true;
            // add new plan
            if (flagOfNewPlanAdd || suitableForPareto(newScores)) {
                migrationPlans.add(newPlan);
                scores.add(newScores);
                updateGlobalMaxAndMins(newScores.energyChange,newScores.vmMigrationNumber);
                // add plan fitness
                Double planFitness = PowerVmAllocationPolicyAICS.objectiveFunction2(newScores.energyChange,globalMaxEnergy,globalMinEnergy,newScores.vmMigrationNumber,globalMaxVM,globalMinVM);
                planFitnesses.add(planFitness);
                currentElementNumber++;
            }
        }
    }

    private boolean suitableForPareto(migrationPlanScores newScores) {
        if( (newScores.energyChange<globalMinEnergy||
                newScores.vmMigrationNumber<globalMinVM))
            return true;
        else
            return false;
    }

    private void updateGlobalMaxAndMins(Double energyChange, Integer vmMigrationNumber) {
        if (energyChange > globalMaxEnergy) {
            globalMaxEnergy = energyChange;
        } else if (energyChange < globalMinEnergy) {
            globalMinEnergy = energyChange;
        }
        if (vmMigrationNumber > globalMaxVM) {
            globalMaxVM = vmMigrationNumber;
        } else if (vmMigrationNumber < globalMinVM) {
            globalMinVM = vmMigrationNumber;
        }
    }


}
