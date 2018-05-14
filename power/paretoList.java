package org.cloudbus.cloudsim.cloudsim_cansusam_vmallocation.power;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class paretoList {

    static int listLimit = 10;
    static int currentElementNumber = 0;
    List<Map<String, Object>> migrationPlans;
    ArrayList<migrationPlanScores> scores;

    public paretoList(List<Map<String, Object>> migrationPlans, ArrayList<migrationPlanScores> scores) {
        this.migrationPlans = migrationPlans;
        this.scores = scores;
    }

    public void compareAddRemove(Map<String, Object> newPlan, migrationPlanScores newScores) {
        if (currentElementNumber < listLimit) {
            boolean flagOfNewPlanAdd = false;
            for (migrationPlanScores m : scores) {
                if (PowerVmAllocationPolicyAICS.dominationCheck(m.energyChange,
                        newScores.energyChange,
                        m.vmMigrationNumber,
                        newScores.vmMigrationNumber)) {
                    int indexOfRemove = scores.indexOf(m);

                    // remove dominated plan
                    migrationPlans.remove(indexOfRemove);
                    scores.remove(indexOfRemove);
                    currentElementNumber--;

                    // set the flag of addition
                    if (!flagOfNewPlanAdd) {
                        flagOfNewPlanAdd = true;
                    }
                }
            }
            // add new plan
            if (flagOfNewPlanAdd) {
                migrationPlans.add(newPlan);
                scores.add(newScores);
            }
            currentElementNumber++;
        }
    }
}
