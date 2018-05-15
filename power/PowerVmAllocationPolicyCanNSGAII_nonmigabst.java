package org.cloudbus.cloudsim.cloudsim_cansusam_vmallocation.power;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyAbstract;

import java.util.*;
import java.util.stream.IntStream;

import static java.lang.Math.pow;



/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */



        import org.cloudbus.cloudsim.Host;
        import org.cloudbus.cloudsim.Log;
        import org.cloudbus.cloudsim.Vm;
        import org.cloudbus.cloudsim.core.CloudSim;

        import java.util.*;
        import java.util.stream.IntStream;

        import static java.lang.Math.max;
        import static java.lang.Math.min;
        import static java.lang.Math.pow;

/**
 * @author canssm
 */
public class PowerVmAllocationPolicyCanNSGAII_nonmigabst extends PowerVmAllocationPolicyAbstract {

    /** The map between each VM and its allocated host.
     * The map key is a VM UID and the value is the allocated host for that VM. */
    private Map<String, Host> vmTable;

    /** The map between each VM and the number of Pes used.
     * The map key is a VM UID and the value is the number of used Pes for that VM. */
    private Map<String, Integer> usedPes;

    /** The number of free Pes for each host from {@link #getHostList() }. */
    private List<Integer> freePes;

    /** Parameters. */

    /** Utilization boundaries. */

    double relativeImportanceOfPMsToSleep;
    double relativeImportanceOfEnergy;
    double relativeImportanceOfVMMigration;
    double relativeImportanceOfHeuristic;
    double relativeImportancePheromone;

    int iterationLimit;
    int antNumber;
    //int antVMRatio;
    int antVMNumber;
    double parameterQ0;
    double pheromoneDecayGlobal;

    double pheromonePowerConstant;

    int rankLimit;

    double initialPheromoneLevelVmAssign;
    double initialPheromoneLevelHostSelect;

    boolean hostSleepControlInsteadEnergy;

    boolean elitistAntON;
    int elitistAntNumber;
    int elitistRepeat;

    int seed;

    double crowdMaxValue =  Double.POSITIVE_INFINITY;

    private Random randomGen;

    /**
     * Instantiates a new PowerVmAllocationPolicySimple.
     *
     * @param list the list
     */
    public PowerVmAllocationPolicyCanNSGAII_nonmigabst(List<? extends Host> list) {
        super(list);

        setFreePes(new ArrayList<Integer>());
        for (Host host : getHostList()) {
            getFreePes().add(host.getNumberOfPes());

        }

        setVmTable(new HashMap<String, Host>());
        setUsedPes(new HashMap<String, Integer>());

        canConstants.NSGAII_nonmigabst myConstants = new canConstants.NSGAII_nonmigabst();

        relativeImportanceOfPMsToSleep = myConstants.relativeImportanceOfPMsToSleep;
        relativeImportanceOfEnergy = myConstants.relativeImportanceOfEnergy;
        relativeImportanceOfVMMigration = myConstants.relativeImportanceOfVMMigration;
        relativeImportanceOfHeuristic = myConstants.relativeImportanceOfHeuristic;
        relativeImportancePheromone = myConstants.relativeImportancePheromone;

        iterationLimit = myConstants.iterationLimit;
        antNumber = myConstants.antNumber;
        //antVMRatio = myConstants.antVMNumberRatio;
        antVMNumber = myConstants.antVMNumber;
        parameterQ0 = myConstants.parameterQ0;
        pheromoneDecayGlobal = myConstants.pheromoneDecayGlobal;

        pheromonePowerConstant = myConstants.pheromonePowerConstant;

        rankLimit = myConstants.rankLimit;


        initialPheromoneLevelVmAssign = myConstants.initialPheromoneLevelVmAssign;
        initialPheromoneLevelHostSelect = myConstants.initialPheromoneLevelHostSelect;

        hostSleepControlInsteadEnergy = myConstants.hostSleepControlInsteadEnergy;
        elitistAntON = myConstants.elitistAntON;
        elitistAntNumber = myConstants.elitistAntNumber;
        elitistRepeat = myConstants.elitistRepeat;

        seed = myConstants.seed;
    }

    /**
     * The method doesn't perform any VM allocation optimization
     * and in fact has no effect.
     * @param vmList
     * @return
     */
    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        if(vmList.isEmpty()) return null;

        randomGen = new Random(seed);
//		int antVMNumber = vmList.size(); //vmList.size()/antVMRatio;

        LinkedList<Map<String, Object>> bestMigrationPlan = new LinkedList<Map<String, Object>>();
//		Double bestFitness = -1.0;

        List<Host> sleepingHosts = new ArrayList<>();
        List<ArrayList<Double>> pheromoneListVmAssign = new ArrayList<ArrayList<Double>>();
//		List<ArrayList<Double>> pheromoneListHostSelect = new ArrayList<ArrayList<Double>>();
        List<Double> heuristicVm = new ArrayList<>();
        List<Double> heuristicHost = new ArrayList<>();

        ArrayList<Integer> vmIDOfNullHostedVms = new ArrayList<>();

        // deallocated vms
        List<Vm> vmListWithoutNullHosts = new ArrayList<>();

        // deallocated vms
        List<Vm> vmsInMigraiton = new ArrayList<>();

        // Get current host list
//		List<Host> hostList2 = new ArrayList<Host>();
        List<Host> hostList = getHostList();
        for (Vm vm : vmList){
            if (vm.isInMigration()) {
                vmsInMigraiton.add(vm);
            }
            if (vm.getHost() != null) {
//				if (!hostList.contains(vm.getHost())) {
//					hostList.add(vm.getHost());
//				}
                //vmListWithoutNullHosts.add(vm);
            }else {
                vmIDOfNullHostedVms.add(vm.getId());
            }
        }


        // Host utilization matrix, shows remaining capacity to reach to sleep
        ArrayList<Double> hostUtilizationSingle = new ArrayList<Double>();

        // Calculate each hosts utilization and classify
        double totalRequestedMips;//, totalRequestedMips_NOTUSED;
        for(Host h: hostList) {
//			totalRequestedMips_NOTUSED = hostList.get(i).getTotalMips()-hostList.get(i).getVmScheduler().getAvailableMips();
            totalRequestedMips = 0;
            for (Vm vm : h.getVmList()) {
                totalRequestedMips +=  vm.getMips(); //vm.getCurrentRequestedTotalMips(); //
                vmListWithoutNullHosts.add(vm);
            }
//			if(Math.abs(totalRequestedMips - totalRequestedMips_NOTUSED) > 0.1)
//				Log.print("");



            // update host utilization, according to indexes in hostlist*/

            hostUtilizationSingle.add(totalRequestedMips);
            //if(totalRequestedMips == hostList.get(i).getTotalMips()) sleepingHosts.add(hostList.get(i));
            //if(hostList.get(i).getTotalMips() == hostList.get(i).getAvailableMips()) sleepingHosts.add(hostList.get(i));
            if (h.getVmList().size() == 0) {
                sleepingHosts.add(h);
            }
        }

//		int bestSleepingHost = sleepingHosts.size();

        // while migration, destination hosts MIPS should be decreased for further calculations
//		for(int k=0; k<vmIDOfNullHostedVms.size(); k++) {
//			Vm vm = findVmWithID(vmList, vmIDOfNullHostedVms.get(k));
//			Log.print("");
//		}


        // Host utilization matrix, shows remaining capacity to reach to sleep
        // Previously calculated value copied for each ant to simplify their calculation step

        pheromoneListVmAssign = pheromoneInitialization(pheromoneListVmAssign,vmListWithoutNullHosts.size(),
                vmListWithoutNullHosts.size(), initialPheromoneLevelVmAssign);

//		pheromoneListHostSelect = pheromoneInitialization(pheromoneListHostSelect,vmListWithoutNullHosts.size(),
//				hostList.size(), initialPheromoneLevelHostSelect);

        heuristicVm = heuristicInitializationVm(vmListWithoutNullHosts);
//		heuristicHost = heuristicInitializationHost(hostList);
        /** ACO simulation starts from this point */

        int iteration = 0;
//		int bestNumberOfSleepingPMs = -1;

        double bestEnergy = Double.POSITIVE_INFINITY;
        double bestSleep = Double.POSITIVE_INFINITY;
//		int bestVM = antVMNumber;

        double globalMaxHost = Double.NEGATIVE_INFINITY;
        double globalMinHost = Double.POSITIVE_INFINITY;
        double globalMaxEnergy = Double.NEGATIVE_INFINITY;
        double globalMinEnergy = Double.POSITIVE_INFINITY;
        int globalMaxVM = 0;
        int globalMinVM = antVMNumber;

        /**
         * "no enough MIPS" error occurs, when?
         * answer: sometimes peprovisioner could not find enough mips to accomodate incoming VM.
         * This is because,
         * VM mips was calculated by using vm.getCurrentRequestedTotalMips() instead of vm.getmips().
         */
        while(iterationLimit > iteration){
            List<ArrayList<Double>> hostUtilization = new ArrayList<ArrayList<Double>>();
            List<antHost> antHostList = new ArrayList<antHost>();
            List<LinkedList<Map<String, Object>>> migrationMapList = new ArrayList<LinkedList<Map<String, Object>>>();

            Double[] antEnergyInfo = new Double[antNumber];
            Integer[] antVMMigrationNumber = new Integer[antNumber];
            Double[] antSleepingHostInfo = new Double[antNumber];

            for(int i=0; i<antNumber; i++) {
                antEnergyInfo[i] = 0.0;
                antVMMigrationNumber[i] = 0;
            }

            // holds vm id to be selected for each ant
            int[] antSelectedVm = new int[antNumber];

            List<Double> antFitness = new ArrayList<Double>();
            List<ArrayList<Integer>> antNumberOfPMsToSleep = new ArrayList<ArrayList<Integer>>();

            /**
             * Each ant holds its own hostUtilization, host-vm list, migrationMap list, sleeping host number and fitness.
             * Host information:
             * 		ID of Host
             * 		Utilization of Host
             * VM information:
             * 		ID of VM
             * 		UserID of VM
             */
            for(int i=0; i<antNumber; i++){
                hostUtilization.add(new ArrayList<Double>(hostUtilizationSingle));
                int hostCounter=0;
                antHost newMyHost = new antHost();
                for(Host h:hostList){
                    newMyHost.Host.add(h.getId());
                    newMyHost.Vm.add(new ArrayList<Integer>());
                    newMyHost.UserId.add(new ArrayList<Integer>());
                    for(Vm v:h.getVmList()){
                        newMyHost.Vm.get(hostCounter).add(v.getId());
                        newMyHost.UserId.get(hostCounter).add(v.getUserId());
                    }
                    newMyHost.utilVal.add(hostUtilizationSingle.get(hostCounter));
                    hostCounter++;
                }
                antHostList.add(new antHost(newMyHost));
                migrationMapList.add(new LinkedList<Map<String, Object>>());
                antNumberOfPMsToSleep.add(new ArrayList<Integer>());
                antFitness.add(new Double(0.0));

                // As a first step, a VM randomly assigned to each ant
                //vmList.get(random).getId();
                antSelectedVm[i] = randomGen.nextInt(vmListWithoutNullHosts.size());
            }

            for(int k=0; k<antVMNumber; k++){ // TODO antnumber <-> vmList.size()
                for(int l=0; l<antNumber; l++){
                    // select new VM
                    // for first condition, vm selected by randomly
                    // ant may not have a previously selected vm because it could not find appropriate one
                    if(k!=0 && antSelectedVm[l] != -1)
                        antSelectedVm[l] = selectNewVm(vmListWithoutNullHosts,
                                pheromoneListVmAssign.get(antSelectedVm[l]),
                                migrationMapList.get(l),heuristicVm);
                    // select new Host
                    if (antSelectedVm[l] != -1)
                        selectNewHost(vmListWithoutNullHosts.get(antSelectedVm[l]),
                                hostList,
//								pheromoneListHostSelect.get(antSelectedVm[l]),
//								heuristicHost,
                                antHostList.get(l),
                                antNumberOfPMsToSleep.get(l),
                                migrationMapList.get(l));
                }
            }


            calculateMigrationNumber(antVMMigrationNumber,migrationMapList);
            // Objective 2
            int[] sortedIndicesOfVMMigration = IntStream.range(0, antVMMigrationNumber.length)
                    .boxed().sorted((i, j) -> antVMMigrationNumber[i].compareTo(antVMMigrationNumber[j]) )
                    .mapToInt(ele -> ele).toArray();

            int[] sortedIndicesOfEnergyAfter = {};
            int[] sortedIndicesOfSleepingHosts = {};
            int[] ranks = {};
            if(hostSleepControlInsteadEnergy) {
                calculateSleepingHostNumber(antSleepingHostInfo, antNumberOfPMsToSleep);
                // Objective 3
                sortedIndicesOfSleepingHosts = IntStream.range(0, antSleepingHostInfo.length)
                        .boxed().sorted((i, j) -> antSleepingHostInfo[i].compareTo(antSleepingHostInfo[j]))
                        .mapToInt(ele -> ele).toArray();
                // obj2 + obj3
                ranks = ranking(nonDominatedSort(antSleepingHostInfo,antVMMigrationNumber),
                        crowdingDistance(antSleepingHostInfo,antVMMigrationNumber,
                                sortedIndicesOfSleepingHosts,sortedIndicesOfVMMigration));
            }
            else{
                //calculateEnergy(antEnergyInfo,migrationMapList,antHostList);
                calculateEnergy2(antEnergyInfo,migrationMapList,antHostList);
                // Objective 1
                sortedIndicesOfEnergyAfter = IntStream.range(0, antEnergyInfo.length)
                        .boxed().sorted((i, j) -> antEnergyInfo[i].compareTo(antEnergyInfo[j]) )
                        .mapToInt(ele -> ele).toArray();
                // obj1 + obj2
                ranks = ranking(nonDominatedSort(antEnergyInfo,antVMMigrationNumber),
                        crowdingDistance(antEnergyInfo,antVMMigrationNumber,
                                sortedIndicesOfEnergyAfter,sortedIndicesOfVMMigration));
            }

            int[] ranksFirstHalf = new int[rankLimit];
            System.arraycopy(ranks,0,ranksFirstHalf,0,ranksFirstHalf.length);

            // obj2 max, min
            int maxVM = antVMMigrationNumber[sortedIndicesOfVMMigration[antVMMigrationNumber.length-1]];
            int minVM = antVMMigrationNumber[sortedIndicesOfVMMigration[0]];
            if(maxVM > globalMaxVM ) globalMaxVM = maxVM;
            if(minVM < globalMinVM ) globalMinVM = minVM;

            double maxHost = 0;
            double minHost = 0;
            double maxEnergy = 0;
            double minEnergy = 0;
            if(hostSleepControlInsteadEnergy) {
                // obj3 max, min
                maxHost = antSleepingHostInfo[sortedIndicesOfSleepingHosts[antSleepingHostInfo.length-1]];
                minHost = antSleepingHostInfo[sortedIndicesOfSleepingHosts[0]];
                if(maxHost > globalMaxHost ) globalMaxHost = maxHost;
                if(minHost < globalMinHost ) globalMinHost = minHost;
                setAntFitnesses(antFitness,antNumberOfPMsToSleep,antVMMigrationNumber);
//				setAntFitnesses2(antFitness,antEnergyInfo,antVMMigrationNumber,globalMaxHost,globalMinHost,globalMaxVM,globalMinVM);
            }else{
                // obj1 max, min
                maxEnergy = antEnergyInfo[sortedIndicesOfEnergyAfter[antEnergyInfo.length-1]];
                minEnergy = antEnergyInfo[sortedIndicesOfEnergyAfter[0]];
                //update global values
                if(maxEnergy > globalMaxEnergy ) globalMaxEnergy = maxEnergy;
                if(minEnergy < globalMinEnergy ) globalMinEnergy = minEnergy;
                //setAntFitnesses2(antFitness,antEnergyInfo,antVMMigrationNumber,maxEnergy,minEnergy,maxVM,minVM);
                setAntFitnesses2(antFitness,antEnergyInfo,antVMMigrationNumber,globalMaxEnergy,globalMinEnergy,globalMaxVM,globalMinVM);
            }

            pheromoneUpdate(pheromoneListVmAssign,pheromoneDecayGlobal,migrationMapList,
                    antFitness, vmListWithoutNullHosts, hostList, "vm2vm", ranksFirstHalf);

//			pheromoneUpdate(pheromoneListHostSelect,pheromoneDecayGlobal,migrationMapList,
//					antFitness, vmListWithoutNullHosts, hostList,"vm2host", ranksFirstHalf);

            int indexOfBestAnt;
            if(hostSleepControlInsteadEnergy) {
                indexOfBestAnt = sortedIndicesOfSleepingHosts[0];//ranks[0];
            }else{
                indexOfBestAnt = sortedIndicesOfEnergyAfter[0];//ranks[0];
            }
            LinkedList<Map<String, Object>> bestLocalMigrationPlan = new LinkedList<Map<String, Object>>();
            bestLocalMigrationPlan = migrationMapList.get(indexOfBestAnt);
//          double bestLocalFitness = antFitness.get(indexOfBestAnt);


//			int bestLocalVM = antVMMigrationNumber[indexOfBestAnt];

//			bestNumberOfSleepingPMs = antNumberOfPMsToSleep.get(indexOfBestAnt).size();

            if(hostSleepControlInsteadEnergy) {
                double bestLocalSleep = antSleepingHostInfo[indexOfBestAnt];
                if(bestLocalSleep < bestSleep){
                    bestMigrationPlan = new LinkedList<Map<String, Object>>(bestLocalMigrationPlan);
                    bestEnergy = bestLocalSleep;
                }
            }else{
                double bestLocalEnergy = antEnergyInfo[indexOfBestAnt];
                //if(bestLocalFitness >= bestFitness && bestNumberOfSleepingPMs > bestSleepingHost){
//            if(bestNumberOfSleepingPMs > bestSleepingHost){
//            if( bestLocalEnergy < bestEnergy || (bestLocalEnergy == bestEnergy && bestLocalVM < bestVM )){
                if(bestLocalEnergy < bestEnergy){
//			if (iterationLimit - 1 == iteration){
                    bestMigrationPlan = new LinkedList<Map<String, Object>>(bestLocalMigrationPlan);
//				LinkedList<Map<String, Object>> bestLocalMigrationPlan = new LinkedList<Map<String, Object>>();
//				bestMigrationPlan = new LinkedList<Map<String, Object>>(migrationMapList.get(sortedIndicesOfEnergyAfter[0]));
//                bestVM = bestLocalVM;
                    bestEnergy = bestLocalEnergy;
//				bestFitness = bestLocalFitness;
//				bestSleepingHost = bestNumberOfSleepingPMs;
                }
            }
            if(elitistAntON)
                elitistUpdate(bestMigrationPlan,vmListWithoutNullHosts,hostList,pheromoneListVmAssign,rankLimit);

            iteration++;
        }
        return bestMigrationPlan;
    }

    /**
     *
     * @param nonDomination
     * @param crowding
     * @return
     */
    public int[] ranking(List<ArrayList<Integer>> nonDomination, double[] crowding) {
        int[] ranks = new int[antNumber];
        int counter = 0;
        for(ArrayList<Integer> front : nonDomination) {
            //sort front by crowding distance
            int frontSize = front.size();
            Double[] crowdingDistanceOfFront = new Double[frontSize];
            for(int i=0; i<frontSize;i++) {
                crowdingDistanceOfFront[i] = crowding[front.get(i)];
            }
            int[] sortedCrowdedDistanceIndices = IntStream.range(0, crowdingDistanceOfFront.length)
                    .boxed().sorted((i, j) -> crowdingDistanceOfFront[i].compareTo(crowdingDistanceOfFront[j]) )
                    .mapToInt(ele -> ele).toArray();
            for(int i=0; i<frontSize; i++) {
                ranks[counter] = front.get(sortedCrowdedDistanceIndices[i]);
                counter++;
            }
        }
        return ranks;
    }

    /**
     *
     * @param antEnergyInfo
     * @param antVMMigrationNumber
     * @return
     */
    public List<ArrayList<Integer>> nonDominatedSort(Double[] antEnergyInfo, Integer[] antVMMigrationNumber) {
        // keeps the indices of solutions in the first front
        List<ArrayList<Integer>> fronts = new ArrayList<ArrayList<Integer>>();
        fronts.add(new ArrayList<Integer>());
        // the number of solutions that dominates the selected solution
        int[] numberOfDominatingSolutions = new int[antNumber];
        // solution indices which are dominatED by the solution
        List<ArrayList<Integer>> solutionsDominated = new ArrayList<ArrayList<Integer>>();

        for(int i=0; i<antNumber; i++) {
            solutionsDominated.add(new ArrayList<Integer>());
            for(int j=0; j<antNumber; j++) {
                if ( i == j ) continue;
                // check for domination
                // if ant i dominates ant j
                if(dominationCheck(antEnergyInfo,antVMMigrationNumber,i,j))
                    solutionsDominated.get(i).add(j);
                else if(dominationCheck(antEnergyInfo,antVMMigrationNumber,j,i))
                    numberOfDominatingSolutions[i]++;
            }
            if(numberOfDominatingSolutions[i] == 0)
                fronts.get(0).add(i);
        }

        int counter = 0;
        ArrayList<Integer> subfronts;
        while (fronts.get(counter).size() > 0) {
            subfronts = new ArrayList<Integer>();
            for(int i=0; i<fronts.get(counter).size(); i++) {
                int indexOfFrontMember = fronts.get(counter).get(i);
                for(int j=0; j<solutionsDominated.get(indexOfFrontMember).size(); j++) {
                    int indexOfSubMember = solutionsDominated.get(indexOfFrontMember).get(j);
                    numberOfDominatingSolutions[indexOfSubMember]--;
                    if (numberOfDominatingSolutions[indexOfSubMember] == 0) {
                        subfronts.add(indexOfSubMember);
                    }
                }
            }
            counter++;
            if(subfronts.size() == 0) break;
            fronts.add(subfronts);
        }

        return fronts;
    }

    /**
     *
     * @param antEnergyInfo
     * @param antVMMigrationNumber
     * @param first
     * @param second
     * @return
     */
    public boolean dominationCheck(Double[] antEnergyInfo, Integer[] antVMMigrationNumber, int first, int second){
        double energyInfo1 = antEnergyInfo[first], energyInfo2 = antEnergyInfo[second];
        int VMMigrationNumber1 = antVMMigrationNumber[first], VMMigrationNumber2 = antVMMigrationNumber[second];

        if( (energyInfo1<energyInfo2 || VMMigrationNumber1<VMMigrationNumber2)
                && (energyInfo1<=energyInfo2 && VMMigrationNumber1<=VMMigrationNumber2))
            return true;
        else
            return false;
    }

    /**
     * Crowding distance calculated by considering 2 objectives, energy consumption and vm migration number
     * @param antEnergyInfo
     * @param antVMMigrationNumber
     */
    public double[] crowdingDistance(Double[] antEnergyInfo, Integer[] antVMMigrationNumber,
                                     int[] sortedIndicesOfEnergyAfter, int[] sortedIndicesOfVMMigration) {

        double[] crowdingDistances = new double[antNumber];

        crowdingDistances[sortedIndicesOfEnergyAfter[0]] += crowdMaxValue;
        crowdingDistances[sortedIndicesOfEnergyAfter[antNumber-1]] += crowdMaxValue;

        crowdingDistances[sortedIndicesOfVMMigration[0]] += crowdMaxValue;
        crowdingDistances[sortedIndicesOfVMMigration[antNumber-1]] += crowdMaxValue;

        double obj1Value, obj2Value;
        for(int j=1; j<antNumber-1; j++) {
            obj1Value = antEnergyInfo[sortedIndicesOfEnergyAfter[j+1]]
                    -antEnergyInfo[sortedIndicesOfEnergyAfter[j-1]];
            obj2Value = antVMMigrationNumber[sortedIndicesOfVMMigration[j+1]]
                    -antVMMigrationNumber[sortedIndicesOfVMMigration[j-1]];
            crowdingDistances[j] += obj1Value + obj2Value;
        }
        return crowdingDistances;
    }

    public void calculateSleepingHostNumber(Double[] antSleepingHostNumber, List<ArrayList<Integer>> antNumberOfPMsToSleep){
        for(int i=0; i<antNumber; i++) {
            // to convert this to a minimization problem
            antSleepingHostNumber[i] =  1.0/antNumberOfPMsToSleep.get(i).size();
        }
    }

    /**
     *
     * @param antVMMigrationNumber
     * @param migrationMapList
     */
    public void calculateMigrationNumber(Integer[] antVMMigrationNumber, List<LinkedList<Map<String,Object>>> migrationMapList){
        for(int i=0; i<antNumber; i++) {
            antVMMigrationNumber[i] =  migrationMapList.get(i).size();
        }
    }

    /** Calculate by summing all utilization values of an ant
     *
     * @param antEnergyInfo
     * @param migrationMapList
     * @param antHostList
     */
    public void calculateEnergy2(Double[] antEnergyInfo, List<LinkedList<Map<String, Object>>> migrationMapList,
                                 List<antHost> antHostList) {
        /**
         * currentUtil (to) = currentMIPS/TotalMIPS
         * pastUtil (from) = pastMIPS/TotalMIPS
         * model used in this framework (fromPower + (toPower - fromPower) / 2)
         * BUT applied calculation is toPower-fromPower
         */
        int antCounter = 0;
        for(antHost currentAntHost : antHostList) {
            int vmCounter = 0;
            ArrayList<Host> hostsUsedByAnt = new ArrayList<>();
            /**
             * Each ant may migrate VMs to/from the same host multiple times.
             * Unique host efficiencies are summed for ant utilization calculation.
             */
            for (Map<String, Object> migrate : migrationMapList.get(vmCounter)) {
                Vm vm = (Vm) migrate.get("vm");
                PowerHost sourceHost = (PowerHost) vm.getHost();
                PowerHost targetHost = (PowerHost) migrate.get("host");
//				int sourceID = sourceHost.getId();
//				int targetID = targetHost.getId();
                if (hostsUsedByAnt.isEmpty()) {
                    hostsUsedByAnt.add(sourceHost);
                    hostsUsedByAnt.add(targetHost);
                } else{
                    if (!hostsUsedByAnt.contains(sourceHost)) {
                        hostsUsedByAnt.add(sourceHost);
                    }
                    if (!hostsUsedByAnt.contains(targetHost)) {
                        hostsUsedByAnt.add(targetHost);
                    }
                }
                vmCounter++;
            }
            for(int i=0; i<hostsUsedByAnt.size(); i++) {
                PowerHost host = (PowerHost) hostsUsedByAnt.get(i);
                /*
                    While calculation of energy, it is better to use current mips usage for utilization
                 */
                int indexOfHost, indexOfTargetHost;
                indexOfHost = currentAntHost.Host.indexOf(host.getId());

//                double vmMips = vm.getMips();

                double hostFinalUtilization = currentAntHost.utilVal.get(indexOfHost);
                double hostPrevUtilization = host.getUtilizationMips();//currentAntHost.utilVal.get(indexOfSourceHost) + vmCurrentMips;

                double hostFinalPower = host.getPower(hostFinalUtilization/host.getTotalMips());
                double hostPrevPower = host.getPower(hostPrevUtilization/host.getTotalMips());

                /*double oldEnergyConsumption = sourceHost.getEnergyLinearInterpolation();
                double newEnergyConsumption = targetHost.getEnergyLinearInterpolation();*/

                antEnergyInfo[antCounter] += (hostFinalPower-hostPrevPower);

            }
            antCounter++;
        }
    }


    /**
     *
     * @param antEnergyInfo
     * @param migrationMapList
     * @param antHostList
     */
    public void calculateEnergy(Double[] antEnergyInfo, List<LinkedList<Map<String, Object>>> migrationMapList,
                                List<antHost> antHostList) {
        /**
         * currentUtil (to) = currentMIPS/TotalMIPS
         * pastUtil (from) = pastMIPS/TotalMIPS
         * model used in this framework (fromPower + (toPower - fromPower) / 2)
         * BUT applied calculation is toPower-fromPower
         */
        int antCounter = 0;
        for(antHost currentAntHost : antHostList) {
            int vmCounter = 0;
            for (Map<String, Object> migrate : migrationMapList.get(vmCounter)) {
                Vm vm = (Vm) migrate.get("vm");
                PowerHost sourceHost = (PowerHost) vm.getHost();
                PowerHost targetHost = (PowerHost) migrate.get("host");
                /*
                    While calculation of energy, it is better to use current mips usage for utilization
                 */
                int indexOfSourceHost, indexOfTargetHost;
                indexOfSourceHost = currentAntHost.Host.indexOf(sourceHost.getId());
                indexOfTargetHost = currentAntHost.Host.indexOf(targetHost.getId());

//                double vmMips = vm.getMips();

                double sourceFinalUtilization = currentAntHost.utilVal.get(indexOfSourceHost);
                double sourcePrevUtilization = sourceHost.getUtilizationMips();//currentAntHost.utilVal.get(indexOfSourceHost) + vmCurrentMips;
                // TODO getUtilization turns current mips, try all mips with


                double targetFinalUtilization = currentAntHost.utilVal.get(indexOfTargetHost);
                double targetPrevUtilization = targetHost.getUtilizationMips();//currentAntHost.utilVal.get(indexOfTargetHost) - vmCurrentMips;

                double sourceFinalPower = sourceHost.getPower(sourceFinalUtilization/sourceHost.getTotalMips());
                double sourcePrevPower = sourceHost.getPower(sourcePrevUtilization/sourceHost.getTotalMips());

                double targetFinalPower = targetHost.getPower(targetFinalUtilization/targetHost.getTotalMips());
                double targetPrevPower = targetHost.getPower(targetPrevUtilization/targetHost.getTotalMips());

                /*double oldEnergyConsumption = sourceHost.getEnergyLinearInterpolation();
                double newEnergyConsumption = targetHost.getEnergyLinearInterpolation();*/
                if (sourceFinalPower < 0 || sourcePrevPower < 0 || targetFinalPower < 0 || targetPrevPower < 0 )
                    Log.print("");

                if((sourceFinalPower-sourcePrevPower) < 0 || (targetFinalPower-targetPrevPower)<0)
                    Log.print("");

                antEnergyInfo[antCounter] += (sourceFinalPower-sourcePrevPower)+(targetFinalPower-targetPrevPower);
                vmCounter++;
            }
            antCounter++;
        }
    }


    /**
     * Set ant fitnesses according to objective function evaluation
     * @param antFitness
     * @param antNumberOfPMsToSleep
     * @param antNumberOfPMsToSleep
     */
    public void setAntFitnesses(List<Double> antFitness,List<ArrayList<Integer>> antNumberOfPMsToSleep,
                                Integer[] antVMMigrations){
        for(int i=0; i<antNumber; i++) {
            antFitness.set(i,objectiveFunction(antNumberOfPMsToSleep.get(i),antVMMigrations[i]));
        }
    }

    /**
     * Set ant fitnesses according to objective function evaluation
     * @param antFitness
     * @param antEnergy
     * @param antVmMigration
     */
    public void setAntFitnesses2(List<Double> antFitness,Double[] antEnergy,
                                 Integer[] antVmMigration,
                                 Double maxEnergy, Double minEnergy, Integer maxVMNumber, Integer minVMNumber){
        for(int i=0; i<antNumber; i++) {
            antFitness.set(i,objectiveFunction2(antEnergy[i],maxEnergy,minEnergy,
                    antVmMigration[i],maxVMNumber,minVMNumber));
        }
    }

    /**
     *
     * @param hostList
     * @param ID
     * @return
     */
    public PowerHost findHostByID(List<Host> hostList, int ID) {
        for (Host h : hostList) {
            if (h.getId() == ID) {
                return (PowerHost) h;
            }
        }
        return null;
    }

    /**
     *
     */
    public int selectNewVm(List<Vm> vmList,
                           List<Double> pheromoneList,
                           LinkedList<Map<String,Object>> migrationMap,
                           List<Double> heuristicList
    ){
        List<Integer> notYetSelectedVMs = new ArrayList<Integer>();	// index of not selected vms in vmlist, to prevent to migrate a vm more than once
        List<Double> nonSelectedPheromones = new ArrayList<Double>();	// pheromones of vms according to index
        List<Double> nonSelectedHeuristics = new ArrayList<Double>();	// pheromones of vms according to index
        List<Integer> previouslySelectedVMs = new ArrayList<Integer>(); // index of previously selected vms in vmlist

        // Previously selected vms are put into a list
        for (Map<String, Object> migrate : migrationMap) {
            Vm vm = (Vm) migrate.get("vm");
            previouslySelectedVMs.add(vmList.indexOf(vm));
        }

        // Non-selected vms are put into a list, also their probabilities
        int counter = 0;
        for(int i=0; i<vmList.size(); i++) {
            if (!previouslySelectedVMs.contains(i)) {
                notYetSelectedVMs.add(i);
                nonSelectedPheromones.add(pheromoneList.get(i));
                nonSelectedHeuristics.add(heuristicList.get(i));
                counter++;
            }
        }

        // There is no available VM, all of them migrated at least once
        if(notYetSelectedVMs.size() == 0)
            return -1;

        return probabilisticResult(nonSelectedPheromones,nonSelectedHeuristics,notYetSelectedVMs);
    }

    public int selectNewHost(Vm selectedVm,
                             List<Host> hostList,
//							 List<Double> pheromoneList,
//							 List<Double> heuristicList,
                             antHost theAntHost,
                             ArrayList<Integer> antNumberOfPMsToSleep,
                             LinkedList<Map<String,Object>> migrationMap
    ) {

        List<Integer> availableHosts = new ArrayList<Integer>(); // index of available hosts in antHost
//		List<Double> probabilityDistribution = new ArrayList<Double>();	// probabilities of vms according to index

        int[] indexOfVmAndItsHostInAntHost = findIndexOfVmInAntHost(theAntHost,selectedVm);
        int indexOfVm = indexOfVmAndItsHostInAntHost[0];
        int indexOfSourceHost = indexOfVmAndItsHostInAntHost[1];
        int sourceId = theAntHost.Host.get(indexOfSourceHost);
        for (int i=0; i<theAntHost.Host.size(); i++) {
            // the host should not contain the original VM
            // must be suitable for vm
            // and should not be in sleeping mode
            if (
                    !theAntHost.Vm.get(i).contains(selectedVm.getId())
                            && isAntHostIsSuitable(theAntHost.utilVal.get(i), selectedVm, hostList.get(i))
                            && !antNumberOfPMsToSleep.contains(theAntHost.Host.get(i))
//					&& isUtilizationImproved(theAntHost.utilVal.get(i),theAntHost.utilVal.get(indexOfSourceHost),
//                     selectedVm, hostList.get(i))
                    ) {
                availableHosts.add(i); // index of host is added
//				if(Double.isNaN(pheromoneList.get(i)))
//					Log.print("");
//				probabilityDistribution.add(pheromoneList.get(i));
            }
        }
        // there is no available host
        if(availableHosts.size() == 0)
            return -1;

        double vmMips = selectedVm.getMips();//getCurrentRequestedTotalMips();//

        int indexOfDestinationHost;
        // since the antHost host list created according to hostList, indexes must match
        /**
         *  host selection with pheromone matrix
         */
        // indexOfDestinationHost = probabilisticResult(probabilityDistribution,pheromoneList,heuristicList,availableHosts);

        /**
         *  host selection with max util value
         */
        indexOfDestinationHost = findMaxUtiledhost(availableHosts,theAntHost.utilVal,hostList,vmMips);
        if (indexOfDestinationHost == -1)
            return -1;



        //update host information

        double destinationUtilization = theAntHost.utilVal.get(indexOfDestinationHost) + vmMips;

        if (destinationUtilization < 0) {
            //System.out.println("Destination utilization is negative!");
            System.exit(0);
        }

        // destination hosts utilization is exceeding its limit, not correct
        if (destinationUtilization > hostList.get(indexOfDestinationHost).getTotalMips()){
            return -1;
        }

        // source hosts utilization is below zero, not correct
        double sourceUtilization = theAntHost.utilVal.get(indexOfSourceHost) - vmMips;
        if (sourceUtilization < 0) {
            //System.out.println("Source utilization is negative!");
            //System.exit(0);
            return -1;
        }
        //update vm information
        theAntHost.Vm.get(indexOfSourceHost).remove(indexOfVm);
        theAntHost.Vm.get(indexOfDestinationHost).add(selectedVm.getId());
        //update userId information
        theAntHost.UserId.get(indexOfSourceHost).remove(indexOfVm);
        theAntHost.UserId.get(indexOfDestinationHost).add(selectedVm.getUserId());
        //update utilization information
        theAntHost.utilVal.set(indexOfSourceHost,sourceUtilization);
        theAntHost.utilVal.set(indexOfDestinationHost,destinationUtilization);

        //if(sourceUtilization <= 0)
        if(theAntHost.Vm.get(indexOfSourceHost).size() == 0)
            if (!antNumberOfPMsToSleep.contains(sourceId))
                antNumberOfPMsToSleep.add(sourceId);

        //add to migration list
        migrationUpdate(migrationMap,hostList.get(indexOfDestinationHost),selectedVm);

        return indexOfDestinationHost;
    }

    // return host index which has max utilization
    int findMaxUtiledhost (List<Integer> availableHosts, ArrayList<Double> utilVal, List<Host> hostList, double vmMips){
        int maxIndex = -1;
        double maxValue = 0;
        int index, totalMips;
        double util;
        for (int i = 0; i < availableHosts.size(); i++) {
            index = availableHosts.get(i);
            util = utilVal.get(index);
            totalMips = hostList.get(index).getTotalMips();
            if ( util > maxValue ) { // TODO maximum utilization constraint can be added here
                maxValue = utilVal.get(availableHosts.get(i));
                maxIndex = availableHosts.get(i);
            }
        }
//		if(maxIndex == -1)
//			Log.print("");
        return maxIndex;
    }


    /**
     * Will vm migration improve the ratio of newHostUtilization/oldHostUtilization?
     * @param candidateHostUsedMips
     * @param vm
     * @param h
     * @return
     */
    public boolean isUtilizationImproved(Double candidateHostUsedMips, Double originalHostUsedMips,Vm vm, Host h) {
        // original -> current host, current and candidate has similar phonetics, to prevent confusion original preferred
        double originalHostTotalMips = vm.getHost().getTotalMips();
        double candidateHostTotalMips = h.getTotalMips();

        double utilOfOriginalHost = originalHostUsedMips/originalHostTotalMips;
        double utilOfCandidateHost = candidateHostUsedMips/candidateHostTotalMips;

        double nextUtilOfOriginalHost = (originalHostUsedMips-vm.getMips())/originalHostTotalMips; //vm.getCurrentRequestedTotalMips())/originalHostTotalMips; //
        double nextUtilOfCandidateHost = (candidateHostUsedMips+vm.getMips())/candidateHostTotalMips; //vm.getCurrentRequestedTotalMips())/candidateHostTotalMips; //

        double currentRatio = utilOfCandidateHost/utilOfOriginalHost;
        double nextRatio = nextUtilOfCandidateHost/nextUtilOfOriginalHost;

		/*if(currentRatio < 0 || nextRatio < 0)
			Log.print("");*/

        if(nextRatio>currentRatio) return true;
        else return false;
    }

    public boolean isAntHostIsSuitable(Double hostUtilization, Vm vm, Host h) {
//	    hostUtilization shows the currently used MIPS amount of the Host
//		if((h.getTotalMips() - hostUtilization) - vm.getCurrentRequestedTotalMips() < 0
//				&& h.getVmScheduler().getPeCapacity() < vm.getCurrentRequestedMaxMips())
//		double avMips = h.getVmScheduler().getAvailableMips()-(hostUtilization-h.getTotalMips());

        // if (destinationUtilization > hostList.get(destinationHost).getTotalMips())
        double newAvailableMips =  h.getTotalMips() - hostUtilization;
        double vmMips = vm.getMips();//vm.getCurrentRequestedTotalMips();//
        return (h.getVmScheduler().getPeCapacity() >= vmMips
                && newAvailableMips >= vmMips
                && (hostUtilization + vmMips) < h.getTotalMips()
                && h.getRamProvisioner().isSuitableForVm(vm, vm.getCurrentRequestedRam()) && h.getBwProvisioner()
                .isSuitableForVm(vm, vm.getCurrentRequestedBw()));
    }


    /**
     * Returns id of Host int AntHost structure for given Vm
     * @param theAntHost
     * @param vm
     * @return
     */
    public int[] findIndexOfVmInAntHost(antHost theAntHost, Vm vm) {
        int userId = vm.getUserId();
        int vmId = vm.getId();
        for(int i=0; i<theAntHost.Host.size(); i++) {
            for(int j=0; j<theAntHost.Vm.get(i).size(); j++) {
                if (userId == theAntHost.UserId.get(i).get(j) && vmId == theAntHost.Vm.get(i).get(j)) {
                    int[] indexOfVmAndItsHostInAntHost = new int[]{j,i};
                    return indexOfVmAndItsHostInAntHost;
                }
            }
        }
//		try {
//
//		}catch (IndexOutOfBoundsException e){
//			System.out.println("Index out of bounds for vm, userId or host");
//		}
        // not found
        int[] a = new int[]{-1, -1};
        return a;
    }

    public int probabilisticResult(List<Double> nonSelectedPheromones,
                                   List<Double> nonSelectedHeuristics,
                                   List<Integer> notYetSelected
    ) {
        Double random = randomGen.nextDouble();
        if( random > parameterQ0) {
            double[] probabilityDistributionResult =
                    pheromoneDistribution(nonSelectedPheromones,nonSelectedHeuristics);
            random = randomGen.nextDouble();
            for (int j = 0; j < nonSelectedPheromones.size(); j++) {
                //if (random <= nonSelectedPheromones[j]) {
                if (random < probabilityDistributionResult[j]) {
                    return notYetSelected.get(j);
                }
            }
        }else{
            // Select the Vm which has higher probability
            return notYetSelected.get(nonSelectedPheromones.indexOf(Collections.max(nonSelectedPheromones)));
        }

//		Log.print(" ");

        return -1;
    }

    public void elitistUpdate(List<Map<String, Object>>  migrationMapList,
                              List<Vm> vmList, List<Host> hostList,
                              List<ArrayList<Double>>  pheromoneList,
                              int ranksLength
    ) {
        for (int j=0; j<migrationMapList.size()-1; j++) {
            Vm vmCurrent = (Vm) migrationMapList.get(j).get("vm");
            Vm vmNext = (Vm) migrationMapList.get(j+1).get("vm");
            int vmCurrentIndex = vmList.indexOf(vmCurrent);
            int vmNextIndex = vmList.indexOf(vmNext);
            double additionValue = Math.pow(pheromonePowerConstant,ranksLength)/ranksLength; //antNumber*(1/Math.log(k+2)); // antFitness.get(i)/divider
            for(int p=0; p<elitistRepeat; p++) {
                pheromoneList.get(vmCurrentIndex)
                        .set(vmNextIndex,
                                (pheromoneList.get(vmCurrentIndex).get(vmNextIndex) + additionValue));
            }
        }
		/*for (Map<String, Object> migrate : migrationMapList) {
			Vm vm = (Vm) migrate.get("vm");
			PowerHost targetHost = (PowerHost) migrate.get("host");
			int vmIndex = vmList.indexOf(vm);
			int hostIndex = hostList.indexOf(targetHost);
			double additionValue = Math.pow(pheromonePowerConstant,ranksLength)/ranksLength; //antNumber*(1/Math.log(k+2)); // antFitness.get(i)/divider
			for(int p=0; p<elitistRepeat; p++) {
				try {
					pheromoneList.get(vmIndex)
							.set(hostIndex,(pheromoneList.get(vmIndex).get(hostIndex) + additionValue));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}*/
    }

    /**
     *
     * @param pheromoneList
     */
    public void pheromoneUpdate(List<ArrayList<Double>>  pheromoneList, double pheromoneDecay,
                                List<LinkedList<Map<String, Object>>>  migrationMapList, List<Double> antFitness,
                                List<Vm> vmList, List<Host> hostList, String whichUpdate, int[] ranks){

//		double divider = Collections.max(antFitness);
//		if( divider == 0)	// at first divider is empty
//			divider = 1;
        int ranksLength = ranks.length;
        switch (whichUpdate) {
            case "vm2host":{
                for(int k=0; k<ranksLength; k++) {
                    int i = ranks[k];
                    for (Map<String, Object> migrate : migrationMapList.get(i)) {
                        Vm vm = (Vm) migrate.get("vm");
                        PowerHost targetHost = (PowerHost) migrate.get("host");
                        int vmIndex = vmList.indexOf(vm);
                        int hostIndex = hostList.indexOf(targetHost);
                        double additionValue = Math.pow(pheromonePowerConstant,ranksLength-k)/ranksLength; //antNumber*(1/Math.log(k+2)); // antFitness.get(i)/divider
                        pheromoneList.get(vmIndex)
                                .set(hostIndex,(pheromoneList.get(vmIndex).get(hostIndex) + additionValue));
                        /*if (elitistAntON && k < elitistAntNumber) {
                            for(int p=0; p<elitistRepeat; p++) {
                                pheromoneList.get(vmIndex)
                                        .set(hostIndex,(pheromoneList.get(vmIndex).get(hostIndex) + additionValue));
                            }
                        }*/
                    }
                }
            }
            case "vm2vm":{
                for(int k=0; k<ranksLength; k++) {
                    int i = ranks[k];
                    for (int j=0; j<migrationMapList.get(i).size()-1; j++) {
                        Vm vmCurrent = (Vm) migrationMapList.get(i).get(j).get("vm");
                        Vm vmNext = (Vm) migrationMapList.get(i).get(j+1).get("vm");
                        int vmCurrentIndex = vmList.indexOf(vmCurrent);
                        int vmNextIndex = vmList.indexOf(vmNext);
                        double additionValue = Math.pow(pheromonePowerConstant,ranksLength-k)/ranksLength; //antNumber*(1/Math.log(k+2)); // antFitness.get(i)/divider
                        pheromoneList.get(vmCurrentIndex)
                                .set(vmNextIndex,
                                        (pheromoneList.get(vmCurrentIndex).get(vmNextIndex) + additionValue));
                        /*if (elitistAntON && k < elitistAntNumber) {
                            for(int p=0; p<elitistRepeat; p++) {
                                pheromoneList.get(vmCurrentIndex)
                                        .set(vmNextIndex,
                                                (pheromoneList.get(vmCurrentIndex).get(vmNextIndex) + additionValue));
                            }
                        }*/
                    }
                }
            }
        }

        // Pheromone evaporation
        // TODO is there a better method to update a matrix instead of for loops? vectors like
        for(int i=0;i<pheromoneList.size(); i++) {
            for(int j=0;j<pheromoneList.get(i).size(); j++) {
//				pheromoneList.get(i).set(j,((1- pheromoneDecay)*pheromoneList.get(i).get(j)+ pheromoneDecay * initialPheromone));
                pheromoneList.get(i).set(j,((1- pheromoneDecay)*pheromoneList.get(i).get(j)));
            }
        }
    }

    /**
     *
     * @param migrationMap
     * @param destinationPM
     * @param vm
     * @return
     */
    public void migrationUpdate(LinkedList<Map<String, Object>> migrationMap,
                                Host destinationPM, Vm vm){
        Map<String, Object> migrate = new HashMap<String, Object>();
        migrate.put("vm", vm);
        migrate.put("host", destinationPM);
        migrationMap.add(migrate);
    }



    /**
     *
     */
    public double[] pheromoneDistribution(List<Double> nonSelectedPheromones,
                                          List<Double> nonSelectedHeuristics){
        int antAvailableVmSize = nonSelectedPheromones.size();
        double totalSum = 0;
        double[] cumulativeDistribution = new double[antAvailableVmSize];
        for(int i = 0; i<antAvailableVmSize; i++){
            cumulativeDistribution[i] = Math.pow(nonSelectedPheromones.get(i),relativeImportancePheromone)
                    /Math.pow(nonSelectedHeuristics.get(i),relativeImportanceOfHeuristic);
            totalSum += cumulativeDistribution[i];
        }
        for(int i = 0; i<antAvailableVmSize; i++){
            cumulativeDistribution[i] /= totalSum;
        }
        for(int i=1; i<antAvailableVmSize; i++){
            cumulativeDistribution[i] += cumulativeDistribution[i-1];
        }
        return cumulativeDistribution;
    }

    /**
     *
     * @param vm
     * @return
     */
    public double heuristicCalculationVm(Vm vm){
        //@TODO: this calculation should be more realistic: bw, ram, mips also be included to the calculations
        //return vm.getMips();//.getCurrentRequestedTotalMips();

        // utilization of vms' host considered
        double totalMips = vm.getHost().getTotalMips();
        return (totalMips-vm.getHost().getAvailableMips())/(totalMips*vm.getMips()); //getCurrentRequestedTotalMips()); //
    }

    public double heuristicCalculationHost(Host host){
        //@TODO: this calculation should be more realistic: bw, ram, mips also be included to the calculations
        //return pow(Math.abs((host.getTotalMips()-host.getAvailableMips())/host.getTotalMips()),-1);
        return (host.getTotalMips()-host.getAvailableMips())/host.getTotalMips();
    }

    /**
     *
     * @return
     */
    public List<Double> heuristicInitializationVm(List<? extends Vm> vmList) {
        List<Double> heuristicList = new ArrayList<Double>();
        for(int i=0; i<vmList.size(); i++) {
            heuristicList.add(heuristicCalculationVm(vmList.get(i)));
        }
        return  heuristicList;

    }

    public List<Double> heuristicInitializationHost(List<Host> hostList) {
        List<Double> heuristicList = new ArrayList<Double>();
        for(int i=0; i<hostList.size(); i++) {
            heuristicList.add(heuristicCalculationHost(hostList.get(i)));
        }
        return  heuristicList;
    }


    /**
     *
     * @param pheromoneList
     * @return
     */
    public List<ArrayList<Double>> pheromoneInitialization(List<ArrayList<Double>> pheromoneList,
                                                           int rowSize,
                                                           int columnSize,
                                                           double pheromoneInitializer){
        for(int i = 0; i<rowSize; i++){
            pheromoneList.add(new ArrayList<Double>());
            for(int j=0; j<columnSize; j++) {
                pheromoneList.get(i).add(pheromoneInitializer);
            }
        }
        return pheromoneList;
    }

    /**
     * f(M) = |P|^GAMMA + |M|^(-1)
     *
     * @param numberOfPMsToSleep
     * @param vmMigrationNumber
     */
    public Double objectiveFunction(ArrayList<Integer> numberOfPMsToSleep, Integer vmMigrationNumber){
        int vmEffect;
        if (vmMigrationNumber == 0) vmEffect = 1;
        else vmEffect = 1/vmMigrationNumber;
        return (pow((numberOfPMsToSleep.size()), relativeImportanceOfPMsToSleep)+vmEffect);
    }


    /**
     *
     * @param antEnergy
     * @param maxEnergy
     * @param minEnergy
     * @param antVMNumber
     * @param maxVMNumber
     * @param minVMNumber
     * @return
     */
    public Double objectiveFunction2(Double antEnergy, Double maxEnergy, Double minEnergy, Integer antVMNumber,
                                     Integer maxVMNumber, Integer minVMNumber){
        // normalized between 0 - 1
        double normalizedEnergy = 0;
        double normalizedVM = 0;
        if(maxEnergy - minEnergy == 0){
            normalizedEnergy = 0;
        } else if (maxEnergy.isInfinite() && minEnergy.isInfinite()) {
            normalizedEnergy = 0;
        } else
            normalizedEnergy = 1 - (antEnergy - minEnergy)/(maxEnergy - minEnergy);
        if(maxVMNumber - minVMNumber == 0){
            normalizedVM = 0;
        }else
            normalizedVM = 1 - (antVMNumber - minVMNumber)/(maxVMNumber - minVMNumber);
        //return (pow(normalizedEnergy, relativeImportanceOfEnergy)+pow(normalizedVM,relativeImportanceOfVMMigration));
        return (pow(normalizedEnergy, relativeImportanceOfEnergy)+pow(normalizedVM,relativeImportanceOfVMMigration));
    }


    @Override
    public boolean allocateHostForVm(Vm vm, Host host) {
        if (host.vmCreate(vm)) { // if vm has been succesfully created in the host
            getVmTable().put(vm.getUid(), host);

            int requiredPes = vm.getNumberOfPes();
            int idx = getHostList().indexOf(host);
            getUsedPes().put(vm.getUid(), requiredPes);
            getFreePes().set(idx, getFreePes().get(idx) - requiredPes);

            Log.formatLine(
                    "%.2f: VM #" + vm.getId() + " has been allocated to the host #" + host.getId(),
                    CloudSim.clock());
            return true;
        }
        return false;
    }

    /**
     * Gets the used pes.
     *
     * @return the used pes
     */
    protected Map<String, Integer> getUsedPes() {
        return usedPes;
    }

    /**
     * Gets the free pes.
     *
     * @return the free pes
     */
    protected List<Integer> getFreePes() {
        return freePes;
    }

    /**
     * Sets the free pes.
     *
     * @param freePes the new free pes
     */
    protected void setFreePes(List<Integer> freePes) {
        this.freePes = freePes;
    }

    /**
     * Gets the vm table.
     *
     * @return the vm table
     */
    public Map<String, Host> getVmTable() {
        return vmTable;
    }

    /**
     * Sets the vm table.
     *
     * @param vmTable the vm table
     */
    protected void setVmTable(Map<String, Host> vmTable) {
        this.vmTable = vmTable;
    }

    /**
     * Sets the used pes.
     *
     * @param usedPes the used pes
     */
    protected void setUsedPes(Map<String, Integer> usedPes) {
        this.usedPes = usedPes;
    }

    protected Vm findVmWithID(List<? extends Vm> vmList, int id) {
        for (Vm vm : vmList) {
            if(vm.getId() == id)
                return vm;
        }
        // not found
        System.out.println("Vm not found in vmList!!!");
        return null;
    }
}
