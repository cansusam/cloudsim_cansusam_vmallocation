package org.cloudbus.cloudsim.cloudsim_cansusam_vmallocation.power;

/**
 * @author canssm
 */

public class canConstants {

    public class NSGAII{
        public static final double utilizationMinValue = 0.1;
        public static final double overUtilization = 0.9;
        public static final double underUtilization= 0.3;

        public static final double initialPheromoneLevelVmAssign = 0.5;
        public static final double initialPheromoneLevelHostSelect = 0.5;
        public static final double relativeImportanceOfPMsToSleep = 1;
        public static final double relativeImportanceOfHeuristic = 1;
        public static final double relativeImportancePheromone = 1;

        public static final int iterationLimit = 10;
        public static final int antNumber = 20;
        public static final int antVMNumber = 40;
        public static final double parameterQ0 = 0.5;
        public static final double pheromoneDecayGlobal = 0.1;

        public static final int rankLimit = antNumber/2;
    }
    // not a migration abstract class  (!PowerVmAllocationPolicyMigrationAbstract)
    public static class NSGAII_nonmigabst{

        public static final double relativeImportanceOfPMsToSleep = 5;
        public static final double relativeImportanceOfEnergy = 5;
        public static final double relativeImportanceOfVMMigration = 1;
        public static final double relativeImportanceOfHeuristic = 1;
        public static final double relativeImportancePheromone = 2;

        public static final double pheromonePowerConstant = 10; // Math.pow(pheromonePowerConstant,ranksLength-k)/ranksLength;

        public static int iterationLimit = 20;
        public static int antNumber = 20;
        public static int antVMNumber = 20;
//        public static final int antVMNumberRatio = 40;
        public static final double parameterQ0 = 0;
        public static final double pheromoneDecayGlobal = 0.05;

        public static final boolean hostSleepControlInsteadEnergy = false;

        public static final boolean elitistAntON = false;
        public static final int elitistAntNumber = 3;
        public static final int elitistRepeat = 1; //additional update number

        public int rankLimit = antNumber;///2;
        public double initialPheromoneLevelVmAssign = Math.pow(2,rankLimit)/(rankLimit*2);
        public double initialPheromoneLevelHostSelect = Math.pow(2,rankLimit)/(rankLimit*2);

        public static final int seed = 10; // System.currentTimeMillis()

    }

    public class GAPACO{
        public static final double utilizationMinValue = 0.1;
        public static final double overUtilization = 0.8;
        public static final double underUtilization= 0.3;

        public static final double initialPheromoneLevelVmAssign = 1;
        public static final double initialPheromoneLevelHostSelect = 1;
        public static final double relativeImportanceOfPMsToSleep = 1;
        public static final double relativeImportanceOfHeuristic = 1;
        public static final double relativeImportancePheromone = 1;

        public static final int iterationLimit = 10;
        public static final int antNumber = 20;
        public static final int antVMNumber = 50;
        public static final double parameterQ0 = 0.5;
        public static final double pheromoneDecayLocal = 0.2;
        public static final double pheromoneDecayGlobal = 0.3;
    }

    public class GAPACO_OLD{
        public static final double initialPheromoneLevelVmAssign = 1;
        public static final double initialPheromoneLevelHostSelect = 1;
        public static final double relativeImportanceOfPMsToSleep = 1;
        public static final double relativeImportanceOfHeuristic = 1;
        public static final double relativeImportancePheromone = 1;

        public static final double iterationLimit = 2;
        public static final int antNumber = 3;
        public static final double parameterQ0 = 0.5;
        public static final double pheromoneDecayLocal = 0.2;
        public static final double pheromoneDecayGlobal = 0.3;
    }

}
