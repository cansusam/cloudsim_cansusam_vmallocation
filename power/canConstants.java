package org.cloudbus.cloudsim.cloudsim_cansusam_vmallocation.power;

/**
 * @author canssm
 */

public class canConstants {

    public static class NSGAII{
        public static double utilizationMinValue = 0.1;
        public static double overUtilization = 0.9;
        public static double underUtilization= 0.3;

        public static double initialPheromoneLevelVmAssign = 0.5;
        public static double initialPheromoneLevelHostSelect = 0.5;
        public static double relativeImportanceOfPMsToSleep = 1;
        public static double relativeImportanceOfHeuristic = 1;
        public static double relativeImportancePheromone = 1;

        public static int iterationLimit = 10;
        public static int antNumber = 20;
        public static int antVMNumber = 40;
        public static double parameterQ0 = 0.5;
        public static double pheromoneDecayGlobal = 0.1;

        public static int rankLimit = antNumber/2;
    }
    // not a migration abstract class  (!PowerVmAllocationPolicyMigrationAbstract)
    public static class NSGAII_nonmigabst{

        public static double relativeImportanceOfPMsToSleep = 5;
        public static double relativeImportanceOfEnergy = 5;
        public static double relativeImportanceOfVMMigration = 1;
        public static double relativeImportanceOfHeuristic = 0;
        public static double relativeImportancePheromone = 2;

        public static double pheromonePowerConstant = 10; // Math.pow(pheromonePowerConstant,ranksLength-k)/ranksLength;

        public static int iterationLimit = 3;
        public static int antNumber = 20;
        public static int antVMNumber = 20;
//        public static int antVMNumberRatio = 40;
        public static double parameterQ0 = 0;
        public static double pheromoneDecayGlobal = 0.05;

        public static boolean hostSleepControlInsteadEnergy = false;

        public static boolean elitistAntON = false;
        public static int elitistAntNumber = 3;
        public static int elitistRepeat = 1; //additional update number

        public static int rankLimit = antNumber;///2;
        public static double initialPheromoneLevelVmAssign = Math.pow(2,rankLimit)/(rankLimit*2);
        public static double initialPheromoneLevelHostSelect = Math.pow(2,rankLimit)/(rankLimit*2);

        public static int seed = 10; // System.currentTimeMillis()
        public static String folderName = "";

    }

    public static class GAPACO{
        public static double utilizationMinValue = 0.1;
        public static double overUtilization = 0.8;
        public static double underUtilization= 0.3;

        public static double initialPheromoneLevelVmAssign = 1;
        public static double initialPheromoneLevelHostSelect = 1;
        public static double relativeImportanceOfPMsToSleep = 1;
        public static double relativeImportanceOfHeuristic = 1;
        public static double relativeImportancePheromone = 1;

        public static int iterationLimit = 10;
        public static int antNumber = 20;
        public static int antVMNumber = 50;
        public static double parameterQ0 = 0.5;
        public static double pheromoneDecayLocal = 0.2;
        public static double pheromoneDecayGlobal = 0.3;
    }

    public static class GAPACO_OLD{
        public static double initialPheromoneLevelVmAssign = 1;
        public static double initialPheromoneLevelHostSelect = 1;
        public static double relativeImportanceOfPMsToSleep = 1;
        public static double relativeImportanceOfHeuristic = 1;
        public static double relativeImportancePheromone = 1;

        public static double iterationLimit = 2;
        public static int antNumber = 3;
        public static double parameterQ0 = 0.5;
        public static double pheromoneDecayLocal = 0.2;
        public static double pheromoneDecayGlobal = 0.3;
    }

}
