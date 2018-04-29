/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.cloudsim_cansusam_vmallocation.power;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.cloudsim_cansusam_vmallocation.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationAbstract;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicy;

import java.util.*;
import java.util.stream.IntStream;

import static java.lang.Math.pow;

/**
 * @author canssm
 */
public class PowerVmAllocationPolicyCanNSGAII extends PowerVmAllocationPolicyMigrationAbstract {

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
	double utilizationMinValue = canConstants.NSGAII.utilizationMinValue;
	double overUtilization = canConstants.NSGAII.overUtilization;
	double underUtilization= canConstants.NSGAII.underUtilization;

	double initialPheromoneLevelVmAssign = canConstants.NSGAII.initialPheromoneLevelVmAssign;
	double initialPheromoneLevelHostSelect = canConstants.NSGAII.initialPheromoneLevelHostSelect;
	double relativeImportanceOfPMsToSleep = canConstants.NSGAII.relativeImportanceOfPMsToSleep;
	double relativeImportanceOfHeuristic = canConstants.NSGAII.relativeImportanceOfHeuristic;
	double relativeImportancePheromone = canConstants.NSGAII.relativeImportancePheromone;

	int iterationLimit = canConstants.NSGAII.iterationLimit;
	static int antNumber = canConstants.NSGAII.antNumber;
	static int antVMNumber = canConstants.NSGAII.antVMNumber;
	double parameterQ0 = canConstants.NSGAII.parameterQ0;
	double pheromoneDecayGlobal = canConstants.NSGAII.pheromoneDecayGlobal;

	int rankLimit = canConstants.NSGAII.rankLimit;
	double crowdMaxValue =  Double.POSITIVE_INFINITY;

	private Random randomGen;

	/**
	 * Instantiates a new PowerVmAllocationPolicySimple.
	 *
	 * @param list the list
	 */
	public PowerVmAllocationPolicyCanNSGAII(List<? extends Host> list,
                                            PowerVmSelectionPolicy vmSelectionPolicy) {
		super(list,vmSelectionPolicy);

		setFreePes(new ArrayList<Integer>());
		for (Host host : getHostList()) {
			getFreePes().add(host.getNumberOfPes());

		}

		setVmTable(new HashMap<String, Host>());
		setUsedPes(new HashMap<String, Integer>());
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
		randomGen = new Random(System.currentTimeMillis());

		LinkedList<Map<String, Object>> bestMigrationPlan = new LinkedList<Map<String, Object>>();
		Double bestFitness = -1.0;

		List<Host> sleepingHosts = new ArrayList<>();
		List<ArrayList<Double>> pheromoneListVmAssign = new ArrayList<ArrayList<Double>>();
		List<ArrayList<Double>> pheromoneListHostSelect = new ArrayList<ArrayList<Double>>();
		List<Double> heuristicVm = new ArrayList<>();
		List<Double> heuristicHost = new ArrayList<>();

		ArrayList<Integer> vmIDOfNullHostedVms = new ArrayList<>();

		// deallocated vms
		List<Vm> vmListWithoutNullHosts = new ArrayList<>();

		// deallocated vms
		List<Vm> vmsInMigraiton = new ArrayList<>();

		// Get current host list
		int hostNumber = 0;
		List<Host> hostList = new ArrayList<Host>();
		for (Vm vm : vmList){
			if (vm.isInMigration()) {
				vmsInMigraiton.add(vm);
			}
			if (!hostList.contains(vm.getHost())) {
				if (vm.getHost() != null) {
					hostList.add(vm.getHost());
					vmListWithoutNullHosts.add(vm);
				} else {
					vmIDOfNullHostedVms.add(vm.getId());
				}
			}
		}

		// Host utilization matrix, shows remaining capacity to reach to sleep
		ArrayList<Double> hostUtilizationSingle = new ArrayList<Double>();

		// Calculate each hosts utilization and classify
		double totalRequestedMips;//, totalRequestedMips_NOTUSED;
		for(int i=0; i<hostList.size(); i++) {
//			totalRequestedMips_NOTUSED = hostList.get(i).getTotalMips()-hostList.get(i).getVmScheduler().getAvailableMips();
			totalRequestedMips = 0;
			for(Vm vm : hostList.get(i).getVmList())
				totalRequestedMips += vm.getMips(); // .getCurrentRequestedTotalMips();
//			if(Math.abs(totalRequestedMips - totalRequestedMips_NOTUSED) > 0.1)
//				Log.print("");

			// update host utilization, according to indexes in hostlist*/
			hostUtilizationSingle.add(totalRequestedMips);
			//if(totalRequestedMips == hostList.get(i).getTotalMips()) sleepingHosts.add(hostList.get(i));
			//if(hostList.get(i).getTotalMips() == hostList.get(i).getAvailableMips()) sleepingHosts.add(hostList.get(i));
			if(hostList.get(i).getVmList().size() == 0) sleepingHosts.add(hostList.get(i));
		}

		int bestSleepingHost = sleepingHosts.size();

		// while migration, destination hosts MIPS should be decreased for further calculations
		for(int k=0; k<vmIDOfNullHostedVms.size(); k++) {
			Vm vm = findVmWithID(vmList, vmIDOfNullHostedVms.get(k));
			Log.print("");
		}


		// Host utilization matrix, shows remaining capacity to reach to sleep
		// Previously calculated value copied for each ant to simplify their calculation step

		pheromoneListVmAssign = pheromoneInitialization(pheromoneListVmAssign,vmListWithoutNullHosts.size(),
				vmListWithoutNullHosts.size(), initialPheromoneLevelVmAssign);
		pheromoneListHostSelect = pheromoneInitialization(pheromoneListHostSelect,vmListWithoutNullHosts.size(),
				hostList.size(), initialPheromoneLevelHostSelect);

		heuristicVm = heuristicInitializationVm(vmListWithoutNullHosts);
		heuristicHost = heuristicInitializationHost(hostList);
		/** ACO simulation starts from this point */

		int iteration = 0;
		int bestNumberOfSleepingPMs = -1;

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

			for(int i=0; i<antNumber; i++) {
                antEnergyInfo[i] = 0.0;
                antVMMigrationNumber[i] = 0;
            }


            int[] antSelectedVm = new int[antNumber];

			List<Double> antFitness = new ArrayList<Double>();
			List<ArrayList<Integer>> antNumberOfPMsToSleep = new ArrayList<ArrayList<Integer>>();

			LinkedList<Map<String, Object>> bestLocalMigrationPlan = new LinkedList<Map<String, Object>>();
			Double bestLocalFitness = -1.0;

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
				int random = randomGen.nextInt(vmListWithoutNullHosts.size());
				antSelectedVm[i] = random;//vmList.get(random).getId();
			}

			for(int k=0; k<antVMNumber; k++){ // TODO antnumber <-> vmList.size()
				for(int l=0; l<antNumber; l++){
					// select new VM
					if(k!=0) // for first condition, vm selected by randomly
						if(antSelectedVm[l] != -1)
							antSelectedVm[l] = selectNewVm(vmListWithoutNullHosts,
									pheromoneListVmAssign.get(antSelectedVm[l]),
									migrationMapList.get(l),heuristicVm);
					// select new Host
					int selectedHost = -1;
					if (antSelectedVm[l] != -1) {
						selectedHost = selectNewHost(vmListWithoutNullHosts.get(antSelectedVm[l]),
								hostList,
								pheromoneListHostSelect.get(antSelectedVm[l]),
								heuristicHost,
								antHostList.get(l),
								antNumberOfPMsToSleep.get(l),
								migrationMapList.get(l));
					}
				}
			}

			calculateMigrationNumber(antVMMigrationNumber,migrationMapList);
			calculateEnergy(antEnergyInfo,migrationMapList,antHostList);

			int[] ranks = ranking(nonDominatedSort(antEnergyInfo,antVMMigrationNumber),
            			crowdingDistance(antEnergyInfo,antVMMigrationNumber));
			int[] ranksFirstHalf = new int[rankLimit];
			System.arraycopy(ranks,0,ranksFirstHalf,0,ranksFirstHalf.length);

			// TODO check crowding distance calculation, there are negative results

			setAntFitnesses(antFitness,antNumberOfPMsToSleep,migrationMapList);
			pheromoneUpdate(pheromoneListVmAssign,initialPheromoneLevelVmAssign,pheromoneDecayGlobal,migrationMapList,
					antFitness, vmListWithoutNullHosts, hostList, "vm2vm", ranksFirstHalf);
			pheromoneUpdate(pheromoneListHostSelect,initialPheromoneLevelHostSelect,pheromoneDecayGlobal,migrationMapList,
					antFitness, vmListWithoutNullHosts, hostList,"vm2host", ranksFirstHalf);

			int indexOfBestAnt = ranks[0];
			bestLocalMigrationPlan = migrationMapList.get(indexOfBestAnt);
			bestLocalFitness = antFitness.get(indexOfBestAnt);
			bestNumberOfSleepingPMs = antNumberOfPMsToSleep.get(indexOfBestAnt).size();

			if(bestLocalFitness > bestFitness && bestNumberOfSleepingPMs > bestSleepingHost){
				bestMigrationPlan = new LinkedList<Map<String, Object>>(bestLocalMigrationPlan);
				bestFitness = bestLocalFitness;
				bestSleepingHost = bestNumberOfSleepingPMs;
			}
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
	public double[] crowdingDistance(Double[] antEnergyInfo, Integer[] antVMMigrationNumber) {
		// Objective 1
		int[] sortedIndicesOfEnergyAfter = IntStream.range(0, antEnergyInfo.length)
				.boxed().sorted((i, j) -> antEnergyInfo[i].compareTo(antEnergyInfo[j]) )
				.mapToInt(ele -> ele).toArray();
		// Objective 2
		int[] sortedIndicesOfVMMigration = IntStream.range(0, antVMMigrationNumber.length)
				.boxed().sorted((i, j) -> antVMMigrationNumber[i].compareTo(antVMMigrationNumber[j]) )
				.mapToInt(ele -> ele).toArray();

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

                double sourceFinalUtilization = currentAntHost.utilVal.get(indexOfSourceHost);
                double sourcePrevUtilization = sourceHost.getUtilizationMips();//currentAntHost.utilVal.get(indexOfSourceHost) + vmCurrentMips;

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
     * @param migrationMapList
     */
	public void setAntFitnesses(List<Double> antFitness,List<ArrayList<Integer>> antNumberOfPMsToSleep,
								List<LinkedList<Map<String, Object>>> migrationMapList){
		for(int i=0; i<antNumber; i++) {
			antFitness.set(i,objectiveFunction(antNumberOfPMsToSleep.get(i),migrationMapList.get(i)));
		}
	}

	/**
	 *
	 * @param hostList
	 * @param ID
	 * @return
	 */
	public Host findHostByID(List<Host> hostList, int ID) {
		for (Host h : hostList) {
			if (h.getId() == ID) {
				return h;
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
		List<Double> probabilityDistribution = new ArrayList<Double>();	// probabilities of vms according to index
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
				probabilityDistribution.add(pheromoneList.get(i));
				counter++;
			}
		}

		// There is no available VM, all of them migrated at least once
		if(notYetSelectedVMs.size() == 0)
			return -1;

		return probabilisticResult(probabilityDistribution,pheromoneList,heuristicList,notYetSelectedVMs);
	}

	public int selectNewHost(Vm selectedVm,
							 List<Host> hostList,
							 List<Double> pheromoneList,
							 List<Double> heuristicList,
							 antHost theAntHost,
							 ArrayList<Integer> antNumberOfPMsToSleep,
							 LinkedList<Map<String,Object>> migrationMap
	) {

		List<Integer> availableHosts = new ArrayList<Integer>(); // index of available hosts in antHost
		List<Double> probabilityDistribution = new ArrayList<Double>();	// probabilities of vms according to index

		int[] indexOfVmAndItsHostInAntHost = findIndexOfVmInAntHost(theAntHost,selectedVm);
		int indexOfVm = indexOfVmAndItsHostInAntHost[0];
		int indexOfSourceHost = indexOfVmAndItsHostInAntHost[1];
		int sourceId = theAntHost.Host.get(indexOfSourceHost);
		for (int i=0; i<theAntHost.Host.size(); i++) {
			// the host should not contain the original VM
			// must be suitable for vm
			// and should not be in sleeping mode
			if (!theAntHost.Vm.get(i).contains(selectedVm.getId())
					&& isAntHostIsSuitable(theAntHost.utilVal.get(i), selectedVm, hostList.get(i))
					&& !antNumberOfPMsToSleep.contains(theAntHost.Host.get(i))
					&& isUtilizationImproved(theAntHost.utilVal.get(i),theAntHost.utilVal.get(indexOfSourceHost),
												selectedVm, hostList.get(i))) {
				availableHosts.add(i); // index of host is added
//				if(Double.isNaN(pheromoneList.get(i)))
//					Log.print("");
				probabilityDistribution.add(pheromoneList.get(i));
			}
		}
		// there is no available host
		if(availableHosts.size() == 0)
			return -1;

		// since the antHost host list created according to hostList, indexes must match
		int destinationHost = probabilisticResult(probabilityDistribution,pheromoneList,heuristicList,availableHosts);

		//update host information
		double vmMips = selectedVm.getMips();//.getCurrentRequestedTotalMips();
		double destinationUtilization = 0;
		destinationUtilization = theAntHost.utilVal.get(destinationHost) + vmMips;
		if (destinationUtilization < 0) {
			//System.out.println("Destination utilization is negative!");
			System.exit(0);
		}

		double sourceUtilization = theAntHost.utilVal.get(indexOfSourceHost) - vmMips;
        if (sourceUtilization < -0.1) {
            //System.out.println("Source utilization is negative!");
			//System.exit(0);
			return -1;
        }

		//update vm information
		theAntHost.Vm.get(indexOfSourceHost).remove(indexOfVm);
		theAntHost.Vm.get(destinationHost).add(selectedVm.getId());
		//update userId information
		theAntHost.UserId.get(indexOfSourceHost).remove(indexOfVm);
		theAntHost.UserId.get(destinationHost).add(selectedVm.getId());
		//update utilization information
		theAntHost.utilVal.set(indexOfSourceHost,sourceUtilization);
		theAntHost.utilVal.set(destinationHost,destinationUtilization);

		//if(sourceUtilization <= 0)
		if(theAntHost.Vm.get(indexOfSourceHost).size() == 0)
			if (!antNumberOfPMsToSleep.contains(sourceId))
				antNumberOfPMsToSleep.add(sourceId);

		//add to migration list
		migrationUpdate(migrationMap,hostList.get(destinationHost),selectedVm);

		return destinationHost;
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

		double nextUtilOfOriginalHost = (originalHostUsedMips-vm.getMips())/originalHostTotalMips;
		double nextUtilOfCandidateHost = (candidateHostUsedMips+vm.getMips())/candidateHostTotalMips;

		double currentRatio = utilOfCandidateHost/utilOfOriginalHost;
		double nextRatio = nextUtilOfCandidateHost/nextUtilOfOriginalHost;

		/*if(currentRatio < 0 || nextRatio < 0)
			Log.print("");*/

		if(nextRatio>currentRatio) return true;
		else return false;
	}

	/**
	 * Checks if a host is over utilized, based on CPU usage.
	 *
	 * @param host the host
	 * @return true, if the host is over utilized; false otherwise
	 */
	@Override
	protected boolean isHostOverUtilized(org.cloudbus.cloudsim.power.PowerHost host) {
		addHistoryEntry(host, overUtilization);
		double totalRequestedMips = 0;
		for (Vm vm : host.getVmList()) {
			totalRequestedMips += vm.getCurrentRequestedTotalMips();
		}
		double utilization = totalRequestedMips / host.getTotalMips();
		return utilization > overUtilization;
	}

	public boolean isAntHostIsSuitable(Double hostUtilization, Vm vm, Host h) {
//	    hostUtilization shows the currently used MIPS amount of the Host
//		if((h.getTotalMips() - hostUtilization) - vm.getCurrentRequestedTotalMips() < 0
//				&& h.getVmScheduler().getPeCapacity() < vm.getCurrentRequestedMaxMips())
//		double avMips = h.getVmScheduler().getAvailableMips()-(hostUtilization-h.getTotalMips());

        double newAvailableMips =  h.getTotalMips() - hostUtilization;
		return (h.getVmScheduler().getPeCapacity() >= vm.getMips() // vm.getCurrentRequestedMaxMips()
				&& newAvailableMips >= vm.getMips() //vm.getCurrentRequestedTotalMips()
				&& h.getRamProvisioner().isSuitableForVm(vm, vm.getCurrentRequestedRam()) && h.getBwProvisioner()
				.isSuitableForVm(vm, vm.getCurrentRequestedBw()));
//		if((h.getTotalMips() - hostUtilization) - vm.getMips() < 0)
//			return false;
//		return true;
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

	public int probabilisticResult(List<Double> probabilityDistribution,
								   List<Double> pheromoneList,
								   List<Double> heuristicList,
								   List<Integer> notYetSelected
	) {
		int selected = -1;
		Double random = randomGen.nextDouble();
		if( random > parameterQ0) {
			double[] probabilityDistributionResult =
					pheromoneDistribution(probabilityDistribution,pheromoneList,heuristicList,notYetSelected);
			random = randomGen.nextDouble();
			for (int j = 0; j < probabilityDistribution.size(); j++) {
				//if (random <= probabilityDistribution[j]) {
				if (random <= probabilityDistributionResult[j]) {
					selected = notYetSelected.get(j);
					break;
				}
			}
		}else{
			// Select the Vm which has higher probability
			selected = notYetSelected.get(probabilityDistribution.indexOf(Collections.max(probabilityDistribution)));
		}
//		if (selected == -1) {
//			Log.print(" ");
//		}
		return selected;
	}

	/**
	 *
	 * @param pheromoneList
	 */
	public void pheromoneUpdate(List<ArrayList<Double>>  pheromoneList, double initialPheromone, double pheromoneDecay,
								List<LinkedList<Map<String, Object>>>  migrationMapList, List<Double> antFitness,
								List<Vm> vmList, List<Host> hostList, String whichUpdate, int[] ranks){

		double divider = Collections.max(antFitness);
		if( divider == 0)	// at first divider is empty
			divider = 1;
		switch (whichUpdate) {
			case "vm2host":{
				for(int i=0; i<migrationMapList.size(); i++) {
					int a = i;
					// if the owner ant of the migrationMap is not in the first half of the ranking
					if(!(IntStream.of(ranks).anyMatch(x -> x == a)))
						continue;
					for (Map<String, Object> migrate : migrationMapList.get(i)) {
						Vm vm = (Vm) migrate.get("vm");
						PowerHost targetHost = (PowerHost) migrate.get("host");
						int vmIndex = vmList.indexOf(vm);
						int hostIndex = hostList.indexOf(targetHost);
						pheromoneList.get(vmIndex)
								.set(hostIndex,(pheromoneList.get(vmIndex).get(hostIndex) + antFitness.get(i)/divider));
					}
				}
			}
			case "vm2vm":{
				for(int i=0; i<migrationMapList.size(); i++) {
					int a = i;
					if(!(IntStream.of(ranks).anyMatch(x -> x == a))) continue;;
					for (int j=0; j<migrationMapList.get(i).size()-1; j++) {
						Vm vmCurrent = (Vm) migrationMapList.get(i).get(j).get("vm");
						Vm vmNext = (Vm) migrationMapList.get(i).get(j+1).get("vm");
						int vmCurrentIndex = vmList.indexOf(vmCurrent);
						int vmNextIndex = vmList.indexOf(vmNext);
						pheromoneList.get(vmCurrentIndex)
								.set(vmNextIndex,
										(pheromoneList.get(vmCurrentIndex).get(vmNextIndex) + antFitness.get(i)/divider));
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
	 * @param pheromoneList
	 * @param heuristicList
	 * @return
	 */
	public double[] pheromoneDistribution(List<Double> probabilityDistribution,
										  List<Double> pheromoneList,
										  List<Double> heuristicList,
										  List<Integer> notYetSelectedVMs){
		int antAvailableVmSize = probabilityDistribution.size();
		double totalSum = 0;
		double[] cumulativeDistribution = new double[antAvailableVmSize];
		for(int i = 0; i<antAvailableVmSize; i++){
			totalSum += Math.pow(pheromoneList.get(i),relativeImportancePheromone)
					/Math.pow(heuristicList.get(notYetSelectedVMs.get(i)),relativeImportanceOfHeuristic);
		}
		for(int i = 0; i<antAvailableVmSize; i++){
			cumulativeDistribution[i] = (Math.pow(pheromoneList.get(i),relativeImportancePheromone)
					/Math.pow(heuristicList.get(notYetSelectedVMs.get(i)),relativeImportanceOfHeuristic)) / totalSum;
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
		return (totalMips-vm.getHost().getAvailableMips())/totalMips;
	}

	public double heuristicCalculationHost(Host host){
		//@TODO: this calculation should be more realistic: bw, ram, mips also be included to the calculations
		return pow(Math.abs(host.getAvailableMips()),-1);
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
	 * @param migrationMapList
	 */
	public Double objectiveFunction(ArrayList<Integer> numberOfPMsToSleep, LinkedList<Map<String,
			Object>> migrationMapList){
		double vmCoefficient;
		if (migrationMapList.size() > 0) {
			vmCoefficient = (double)(1/migrationMapList.size());
		}else
			vmCoefficient = 0.0;
		return (pow((numberOfPMsToSleep.size()), relativeImportanceOfPMsToSleep)+vmCoefficient);
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
