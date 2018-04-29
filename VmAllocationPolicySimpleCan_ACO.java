/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.cloudsim_cansusam_vmallocation;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.*;

import static java.lang.Math.pow;

/**
 * @author canssm
 */
public class VmAllocationPolicySimpleCan_ACO extends VmAllocationPolicy {

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
	private double utilizationMinValue = 0.1;
	private double overUtilization = 0.8;
	private double underUtilization= 0.3;

	private static final double initialPheromoneLevel = 1;
	private double relativeImportanceOfPMsToSleep = 5;
	private double relativeImportanceOfHeuristic = 0.9;
	private double relativeImportanceOfPheromone = 1;

	private double iterationLimit = 2;
	private static int antNumber = 10;
	private double parameterQ0 = 0.9;
	private double pheromoneDecayLocal = 0.1;
	private double pheromoneDecayGlobal = 0.1;

	private int tupleLimiter = 200;
	private boolean tupleLimitON = true;

	private Random randomGen;


	/**
	 * Creates a new VmAllocationPolicySimple object.
	 *
	 * @param list the list of hosts
	 * @pre $none
	 * @post $none
	 */
	public VmAllocationPolicySimpleCan_ACO(List<? extends Host> list) {
		super(list);

		setFreePes(new ArrayList<Integer>());
		for (Host host : getHostList()) {
			getFreePes().add(host.getNumberOfPes());

		}

		setVmTable(new HashMap<String, Host>());
		setUsedPes(new HashMap<String, Integer>());
	}

	/**
	 * Allocates the host with less PEs in use for a given VM.
	 * 
	 * @param vm {@inheritDoc}
	 * @return {@inheritDoc}
	 * @pre $none
	 * @post $none
	 */
	@Override
	public boolean allocateHostForVm(Vm vm) {
		int requiredPes = vm.getNumberOfPes();
		boolean result = false;
		int tries = 0;
		List<Integer> freePesTmp = new ArrayList<Integer>();
		for (Integer freePes : getFreePes()) {
			freePesTmp.add(freePes);
		}

		if (!getVmTable().containsKey(vm.getUid())) { // if this vm was not created
			/*do {// we still trying until we find a host or until we try all of them
				int lessFree = Integer.MAX_VALUE;
				int idx = -1;

				// we want the host with less pes in use
				// vm.getCurrentRequestedTotalMips()/vm.getCurrentAllocatedMips()
				// CAN : we want the host with less pes not in use
				for (int i = 0; i < freePesTmp.size(); i++) {
					if (freePesTmp.get(i) < lessFree && freePesTmp.get(i)>= requiredPes) {
						lessFree = freePesTmp.get(i);
						idx = i;
					}
				}

				if(idx == -1) idx = 0;

				Host host = getHostList().get(idx);
				result = host.vmCreate(vm);

				if (result) { // if vm were succesfully created in the host
					getVmTable().put(vm.getUid(), host);
					getUsedPes().put(vm.getUid(), requiredPes);
					getFreePes().set(idx, getFreePes().get(idx) - requiredPes);
					result = true;
					break;
				} else {
					freePesTmp.set(idx, Integer.MIN_VALUE);
				}
				tries++;
			} while (!result && tries < getFreePes().size());*/

			do {// we still trying until we find a host or until we try all of them
				int moreFree = Integer.MIN_VALUE;
				int idx = -1;

				// we want the host with less pes in use
				for (int i = 0; i < freePesTmp.size(); i++) {
					if (freePesTmp.get(i) > moreFree) {
						moreFree = freePesTmp.get(i);
						idx = i;
					}
				}

				Host host = getHostList().get(idx);
				result = host.vmCreate(vm);

				if (result) { // if vm were succesfully created in the host
					getVmTable().put(vm.getUid(), host);
					getUsedPes().put(vm.getUid(), requiredPes);
					getFreePes().set(idx, getFreePes().get(idx) - requiredPes);
					result = true;
					break;
				} else {
					freePesTmp.set(idx, Integer.MIN_VALUE);
				}
				tries++;
			} while (!result && tries < getFreePes().size());

		}

		return result;
	}

	@Override
	public void deallocateHostForVm(Vm vm) {
		if(vm == null) return;
		Host host = getVmTable().remove(vm.getUid());
		int idx = getHostList().indexOf(host);
		int pes = getUsedPes().remove(vm.getUid());
		if (host != null) {
			host.vmDestroy(vm);
			getFreePes().set(idx, getFreePes().get(idx) + pes);
		}
	}

	@Override
	public Host getHost(Vm vm) {
		return getVmTable().get(vm.getUid());
	}

	@Override
	public Host getHost(int vmId, int userId) {
		return getVmTable().get(Vm.getUid(userId, vmId));
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
	 * Gets the used pes.
	 * 
	 * @return the used pes
	 */
	protected Map<String, Integer> getUsedPes() {
		return usedPes;
	}

	/**
	 * Sets the used pes.
	 * 
	 * @param usedPes the used pes
	 */
	protected void setUsedPes(Map<String, Integer> usedPes) {
		this.usedPes = usedPes;
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

	@Override
	public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {

		if(vmList.isEmpty()) return null;

		randomGen = new Random(System.currentTimeMillis());

		LinkedList<Map<String, Object>> bestMigrationPlan = new LinkedList<Map<String, Object>>();
		Double bestFitness = -1.0;

		List<Host> overUtilizedHosts = new ArrayList<>();
		List<Host> underUtilizedHosts = new ArrayList<>();
		List<Host> normalUtilizedHosts = new ArrayList<>();
		List<Host> sleepingHosts = new ArrayList<>();

		List<Host> sourcePMs = new ArrayList<>();
		List<Host> destinationPMs = new ArrayList<>();

		List<Integer[]> tuples = new ArrayList<Integer[]>();
		Integer[] singleTuple = new Integer[4];

		List<Double> pheromoneList = new ArrayList<>();
		List<Double> heuristicList = new ArrayList<>();

		// VMs with null host (migration cause)
		ArrayList<Integer> vmIDOfNullHostedVms = new ArrayList<>();

		// Get current host list
		int hostNumber = 0;
		List<Host> hostList = new ArrayList<Host>();
		for(int i = 0; i<vmList.size(); i++){
			if (hostList.isEmpty() || !hostList.contains(vmList.get(i).getHost())) {
				if (vmList.get(i).getHost() != null)
					hostList.add(vmList.get(i).getHost());
				else
					vmIDOfNullHostedVms.add(vmList.get(i).getId());
			}
		}

		// Host utilization matrix, shows remaining capacity to reach to sleep
		double[] hostTotalRequestedMIPSSingle = new double[hostList.size()];

		// Calculate each hosts utilization and classify
		double totalRequestedMips;
		List<Double> utilization = new ArrayList<Double>();
		double currentUtilization;
		try {
			for (int i = 0; i < hostList.size(); i++) {
                totalRequestedMips = hostList.get(i).getTotalMips()-hostList.get(i).getVmScheduler().getAvailableMips();
				currentUtilization = totalRequestedMips / hostList.get(i).getTotalMips();
				// update host utilization
				hostTotalRequestedMIPSSingle[i] = totalRequestedMips;
				utilization.add(currentUtilization);
				if (currentUtilization > overUtilization) overUtilizedHosts.add(hostList.get(i));
				else if (currentUtilization > underUtilization) normalUtilizedHosts.add(hostList.get(i));
				else if (currentUtilization > 0) underUtilizedHosts.add(hostList.get(i));
				else sleepingHosts.add(hostList.get(i));
			}
		} catch (java.lang.NullPointerException e) {
			Log.print("");
		}


		// Creating sourcePM list
		sourcePMs.addAll(underUtilizedHosts);
		sourcePMs.addAll(overUtilizedHosts);

		// Creating destinationPM list
		destinationPMs.addAll(underUtilizedHosts);
		destinationPMs.addAll(normalUtilizedHosts);

		// If there is no host as a source or no host to migrate, quit
		if(sourcePMs.isEmpty() || destinationPMs.isEmpty()) return null;

		// Creating tuple list
		if (!tupleLimitON) {
			for (Host sourceHost : sourcePMs) {
				for (Vm vm : sourceHost.getVmList()) {
					boolean vmAvailable = true;
					for(int k=0; k<vmIDOfNullHostedVms.size(); k++) {
						if(vm.getId() == vmIDOfNullHostedVms.get(k))
							vmAvailable = false;
					}
					if (vmAvailable) {
						for (Host destinationHost : destinationPMs) {
							// Check if destination PM has enough space/memory/etc. to host the vm
							if (sourceHost.getId() != destinationHost.getId()
									&& !vm.isInMigration()
									&& destinationHost.isSuitableForVm(vm)) {
								singleTuple[0] = sourceHost.getId();
								singleTuple[1] = vm.getId();
								singleTuple[2] = destinationHost.getId();
								singleTuple[3] = vm.getUserId();
								tuples.add(Arrays.copyOf(singleTuple, singleTuple.length));
							}
						}
					}
				}
			}
		} else {
			int limitCounter = tupleLimiter;
			Random randInt = new Random();
			while (limitCounter > 0) {
				Host sourceHost = sourcePMs.get(randInt.nextInt(sourcePMs.size()));
				Host destinationHost = destinationPMs.get(randInt.nextInt(destinationPMs.size()));
				Vm vm = sourceHost.getVmList().get(randInt.nextInt(sourceHost.getVmList().size()));
				if (sourceHost.getId() != destinationHost.getId()
						&& !vm.isInMigration()
						&& destinationHost.isSuitableForVm(vm)) {
					singleTuple[0] = sourceHost.getId();
					singleTuple[1] = vm.getId();
					singleTuple[2] = destinationHost.getId();
					singleTuple[3] = vm.getUserId();
					tuples.add(Arrays.copyOf(singleTuple, singleTuple.length));
					limitCounter--;
				}
			}
		}


		//


		//tuple remover for limiting purpose
		/*for(int i=0; i<(tuples.size()-tupleLimiter); i++) {
			Random randomInt = new Random();
			int tupleToRemove = randomInt.nextInt(tuples.size());
			tuples.remove(tupleToRemove);
		}*/

		// Host utilization matrix, shows remaining capacity to reach to sleep
		// Previously calculated value copied for each ant to simplify their calculation step

		pheromoneList = pheromoneInitialization(pheromoneList, tuples.size());
		heuristicList = heuristicInitialization(tuples,sourcePMs,destinationPMs);
		/** ACO simulation starts from this point */

		int iteration = 0;
		int bestNumberOfSleepingPMs = -1;
		while(iterationLimit > iteration){
			List<ArrayList<Double>> hostUtilization = new ArrayList<ArrayList<Double>>();
			List<antHost> antHostList = new ArrayList<antHost>();
			List<LinkedList<Map<String, Object>>> migrationMapList = new ArrayList<LinkedList<Map<String, Object>>>();
			List<ArrayList<Integer[]>> antTuples = new ArrayList<ArrayList<Integer[]>>();

			/** All tuples are available at the beginning */
			List<ArrayList<Integer[]>> antAvailableTuples = new ArrayList<ArrayList<Integer[]>>();

			List<Double> antFitness = new ArrayList<Double>();
			List<ArrayList<Integer>> antNumberOfPMsToSleep = new ArrayList<ArrayList<Integer>>();

			LinkedList<Map<String, Object>> bestLocalMigrationPlan = new LinkedList<Map<String, Object>>();
			Double bestLocalFitness = -1.0;

			for(int i=0; i<antNumber; i++){
				hostUtilization.add(new ArrayList<Double>());


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
					newMyHost.totalRequestedMIPS.add(hostTotalRequestedMIPSSingle[hostCounter]);
					hostCounter++;
				}
				antHostList.add(new antHost(newMyHost));

				migrationMapList.add(new LinkedList<Map<String, Object>>());
				antTuples.add(new ArrayList<Integer[]>());
				antAvailableTuples.add(new ArrayList<Integer[]>(tuples));
				antNumberOfPMsToSleep.add(new ArrayList<Integer>());
				antFitness.add(new Double(0.0));
			}
			//System.out.println("Tupple size : " + tuples.size());
			for(int k=0; k<antNumber; k++){ //tuples.size();
				// select new tuple
				selectNewTuple(tuples,antTuples,antAvailableTuples,antNumberOfPMsToSleep,
						antHostList,pheromoneList,heuristicList,migrationMapList,destinationPMs,sourcePMs);
				// update trails
				localPheromoneUpdate(pheromoneList);
				// fitness calculations
				//updateHostUtilization(hostUtilization,antTuples,antHostList,antNumberOfPMsToSleep);
				objectiveFunction(antFitness,antNumberOfPMsToSleep,migrationMapList);
			}
			globalPheromoneUpdate(pheromoneList);

			int indexOfBestAnt = antFitness.indexOf(Collections.max(antFitness));
			bestLocalMigrationPlan = migrationMapList.get(indexOfBestAnt);
			bestLocalFitness = Collections.max(antFitness);

			bestNumberOfSleepingPMs = antNumberOfPMsToSleep.get(indexOfBestAnt).size();
			if(bestLocalFitness > bestFitness && bestNumberOfSleepingPMs > sleepingHosts.size()){
				bestMigrationPlan = new LinkedList<Map<String, Object>>(bestLocalMigrationPlan);
			}

			iteration++;
		}

		return bestMigrationPlan;

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
	 * @param tuples
	 * @param antTuples
	 * @param antAvailableTuples
	 * @param pheromoneList
	 * @param heuristicList
	 * @param migrationMapList
	 * @param destinationPMs
	 * @param sourcePMs
	 */
	public void selectNewTuple(List<Integer[]> tuples,
							   List<ArrayList<Integer[]>> antTuples,
							   List<ArrayList<Integer[]>> antAvailableTuples,
							   List<ArrayList<Integer>> antNumberOfPMsToSleep,
							   List<antHost> antHostList,
							   List<Double> pheromoneList,
							   List<Double> heuristicList,
							   List<LinkedList<Map<String,Object>>> migrationMapList,
							   List<Host> destinationPMs,
							   List<Host> sourcePMs){
		Integer[] newTuple = new Integer[4];
		Double random;
		double[] probabilityDistribution;

		for(int i=0; i<antNumber; i++){
			/** If there is no available tuple, exit*/
			if (antAvailableTuples.get(i).size() != 0) {
				random = randomGen.nextDouble();
				if( random > parameterQ0) {
					probabilityDistribution = pheromoneDistribution(tuples,pheromoneList,antAvailableTuples.get(i),heuristicList);
					random = randomGen.nextDouble();
					for (int j = 0; j < antAvailableTuples.get(i).size(); j++) {
						//if (random <= probabilityDistribution[j]) {
						if (random <= probabilityDistribution[j]) {
							newTuple = antAvailableTuples.get(i).get(j);
							break;
						}
					}
					theFunction(i,newTuple,antTuples.get(i),antAvailableTuples.get(i),antHostList.get(i),antNumberOfPMsToSleep.get(i),
							migrationMapList,destinationPMs,sourcePMs);
				}else{
					// Create a list of index values of available tuples
					ArrayList<Integer> availableTupleIndexList = new ArrayList<Integer>();
					// Create a list of heuristic values of available tuples
					ArrayList<Double> availableTupleHeuristicList = new ArrayList<Double>();
					for(int u=0; u<antAvailableTuples.get(i).size(); u++){
						availableTupleIndexList.add(tuples.indexOf(antAvailableTuples.get(i).get(u)));
						availableTupleHeuristicList.add(heuristicList.get(availableTupleIndexList.get(u)));
					}
					newTuple = antAvailableTuples.get(i).get(availableTupleHeuristicList.indexOf(Collections.max(availableTupleHeuristicList)));
					theFunction(i,newTuple,antTuples.get(i),antAvailableTuples.get(i),antHostList.get(i),antNumberOfPMsToSleep.get(i),
							migrationMapList,destinationPMs,sourcePMs);
				}
			}
		}
	}

	/**
	 *
	 * @param i
	 * @param newTuple
	 * @param antTuples
	 * @param antAvailableTuples
	 * @param antHostList
	 * @param antNumberOfPMsToSleep
	 * @param migrationMapList
	 * @param destinationPMs
	 * @param sourcePMs
	 */
	public void theFunction(int i,Integer[] newTuple,
							List<Integer[]> antTuples,
							List<Integer[]> antAvailableTuples,
							antHost antHostList,
							List<Integer> antNumberOfPMsToSleep,
							List<LinkedList<Map<String,Object>>> migrationMapList,
							List<Host> destinationPMs,
							List<Host> sourcePMs){

		// if the migration destination is already in sleep mode, return, do not wake it up
		if(antNumberOfPMsToSleep.contains(newTuple[2])) return;

		Integer source = 0, destination = 0, vm = 0, userId = 0;
		int indexOfSource, indexOfDestination;
		double previousSourceHostUtilizationValue = 0;
		double previousDestinationHostUtilizationValue = 0;
		double newSourceHostUtilizationValue = 0;
		double newDestinationHostUtilizationValue = 0;

		source = newTuple[0];
		vm = newTuple[1];
		destination = newTuple[2];
		userId = newTuple[3];

		indexOfSource = antHostList.Host.indexOf(source);
		indexOfDestination = antHostList.Host.indexOf(destination);

		Vm vmToMigrate = findHostByID(sourcePMs,source).getVm(vm,userId);
		previousSourceHostUtilizationValue = antHostList.totalRequestedMIPS.get(indexOfSource);
		previousDestinationHostUtilizationValue = antHostList.totalRequestedMIPS.get(indexOfDestination);
		// host utilization values of source and destination will be updated
		newSourceHostUtilizationValue = previousSourceHostUtilizationValue - vmToMigrate.getCurrentRequestedTotalMips();
		newDestinationHostUtilizationValue = previousDestinationHostUtilizationValue + vmToMigrate.getCurrentRequestedTotalMips();
		if(newSourceHostUtilizationValue < 0 || newDestinationHostUtilizationValue < 0 ){
			// this utilization must be skipped, it is false
			removeRelatedTuples(newTuple,antAvailableTuples,false);
			return;
		}else{
		    // TODO Couldn't find the reason, sometimes a VM previously migrated can be found in available tuple list, must be fixed
		    for(int t = 0; t<migrationMapList.get(i).size(); t++) {
                if (migrationMapList.get(i).get(t).containsValue(vmToMigrate)) {
                    removeRelatedTuples(newTuple,antAvailableTuples,false);
                    return;
                }
            }
			antHostList.totalRequestedMIPS.set(indexOfSource,newSourceHostUtilizationValue);
			antHostList.totalRequestedMIPS.set(indexOfDestination,newDestinationHostUtilizationValue);
			int indexOfVm = antHostList.Vm.get(indexOfSource).indexOf(vm);

			// TODO Couldn't find the reason, sometimes a VM previously migrated can be found in available tuple list, must be fixed
			try {
				antHostList.Vm.get(indexOfSource).remove(indexOfVm);
			} catch (IndexOutOfBoundsException e) {
				Log.print("");
			}

			antHostList.Vm.get(indexOfDestination).add(vm);
			if(newSourceHostUtilizationValue == 0){
				if(!antNumberOfPMsToSleep.contains(source))
					antNumberOfPMsToSleep.add(source);
			}else{
				if(antNumberOfPMsToSleep.contains(source)){
					int removeIndex = antNumberOfPMsToSleep.indexOf(source);
					antNumberOfPMsToSleep.remove(removeIndex);
				}
			}
		}

		antTuples.add(newTuple.clone());
		Host sourceHostMigration = findHostByID(sourcePMs,source);
		Host destinationHostMigration = findHostByID(destinationPMs,destination);
		// migration plan update
		migrationMapList.set(i,migrationUpdate(migrationMapList.get(i),sourceHostMigration,destinationHostMigration,vm,userId));
		// remove the tuple and related ones
		removeRelatedTuples(newTuple,antAvailableTuples,false);
	}

	/**
	 *
	 * @param antTuples
	 * @param antAvailableTuples
	 */
	public void removeRelatedTuples(Integer[] newTuple,
									List<Integer[]> antAvailableTuples,
									boolean onlyRemoveOriginalTuple) {

		/*List<Integer> removeList = new ArrayList<Integer>();
		for(int j=0; j<antAvailableTuples.size(); j++){
		// check for source and vm
				if(antTuples[0] == antAvailableTuples.get(j)[0]
						&& antTuples[1] == antAvailableTuples.get(j)[1]){
					if(!onlyRemoveOriginalTuple)
						removeList.add(j);
						// check for destination
					else if(antTuples[2] == antAvailableTuples.get(j)[2]){
						removeList.add(j);
						break;
					}
				}
		}*/
		int counter = 0;
        for(int j=0; j<antAvailableTuples.size(); j++){
            // remove tuples including the same vm
            if(newTuple[1] == antAvailableTuples.get(j)[1]){
                antAvailableTuples.remove(j);
                j--;
                counter++;
            }
        }
        if (counter == 0) {
            Log.printLine("No remove process!");
        }

	}

	/**
	 *
	 * @param pheromoneList
	 */
	public void globalPheromoneUpdate (List<Double> pheromoneList){
		for(int i=0;i<pheromoneList.size(); i++)
			pheromoneList.set(i,((1- pheromoneDecayGlobal)*pheromoneList.get(i)+ pheromoneDecayGlobal *initialPheromoneLevel));
	}

	/**
	 *
	 * @param pheromoneList
	 */
	public void localPheromoneUpdate (List<Double> pheromoneList){
		for(int i=0;i<pheromoneList.size(); i++)
			pheromoneList.set(i,((1- pheromoneDecayLocal)*pheromoneList.get(i)+ pheromoneDecayLocal *initialPheromoneLevel));
	}

	/**
	 *
	 * @param migrationMap
	 * @param sourcePM
	 * @param destinationPM
	 * @param vm
	 * @return
	 */
	public LinkedList<Map<String, Object>> migrationUpdate(LinkedList<Map<String, Object>> migrationMap,
																 Host sourcePM, Host destinationPM, int vm, int userId){
		Map<String, Object> migrate = new HashMap<String, Object>();
		if (sourcePM.getVm(vm,userId) == null) {
			System.out.println("problem here..");
		}
		migrate.put("vm", sourcePM.getVm(vm,userId));
		migrate.put("host", destinationPM);
		migrationMap.add(migrate);
		return migrationMap;
	}



	/**
	 *
	 * @param tuples
	 * @param pheromoneList
	 * @param antAvailableTuples
	 * @param heuristicList
	 * @return
	 */
	public double[] pheromoneDistribution(List<Integer[]> tuples, List<Double> pheromoneList,
										  List<Integer[]>  antAvailableTuples, List<Double> heuristicList){
		int antAvailableTupleSize = antAvailableTuples.size();
		double totalSum = 0;
		double[] cumulativeDistribution = new double[antAvailableTupleSize];
		int tupleId;
		for(int i = 0; i<antAvailableTupleSize; i++){
			tupleId = tuples.indexOf(antAvailableTuples.get(i));
			totalSum += Math.pow(pheromoneList.get(tupleId),relativeImportanceOfPheromone)/Math.pow(heuristicList.get(tupleId),relativeImportanceOfHeuristic);
		}
		for(int i = 0; i<antAvailableTupleSize; i++){
			tupleId = tuples.indexOf(antAvailableTuples.get(i));
			cumulativeDistribution[i] = (Math.pow(pheromoneList.get(tupleId),relativeImportanceOfPheromone)/Math.pow(heuristicList.get(tupleId),relativeImportanceOfHeuristic)) / totalSum;
		}
		for(int i=1; i<antAvailableTupleSize; i++){
			cumulativeDistribution[i] += cumulativeDistribution[i-1];
		}
		return cumulativeDistribution;
	}

	/**
	 *
	 * @param destinationHost
	 * @param vm
	 * @return
	 */
	public double heuristicCalculation(Host destinationHost, Vm vm){
		if (destinationHost.isSuitableForVm(vm)){
			return pow(Math.abs(destinationHost.getNumberOfFreePes()-vm.getNumberOfPes()),-1);
		}else
			return 0;
	}

	/**
	 *
	 * @param tuples
	 * @param sourcePMs
	 * @param destinationPMs
	 * @return
	 */
	public List<Double> heuristicInitialization(List<Integer[]> tuples, List<Host> sourcePMs, List<Host> destinationPMs) {
		List<Double> heuristicList = new ArrayList<Double>();
		Host destination;
		Host source;
		Integer vm = new Integer(0);
		Integer userId = new Integer(0);
		for(int i = 0; i<tuples.size(); i++) {
			source = findHostByID(sourcePMs,tuples.get(i)[0]);
			vm = tuples.get(i)[1];
			destination = findHostByID(destinationPMs,tuples.get(i)[2]);
			userId = tuples.get(i)[3];
			heuristicList.add(heuristicCalculation(destination,source.getVm(vm,userId)));
		}
		return  heuristicList;
	}


	/**
	 *
	 * @param pheromoneList
	 * @param tuppleListSize
	 * @return
	 */
	public List<Double> pheromoneInitialization(List<Double> pheromoneList, int tuppleListSize){
		for(int i = 0; i<tuppleListSize; i++){
			pheromoneList.add(initialPheromoneLevel);
		}
		return pheromoneList;
	}

	/**
	 * f(M) = |P|^GAMMA + |M|^(-1)
	 *
	 * @param fitness
	 * @param numberOfPMsToSleep
	 * @param migrationMapList
	 */
	public void objectiveFunction(List<Double> fitness, List<ArrayList<Integer>> numberOfPMsToSleep, List<LinkedList<Map<String, Object>>> migrationMapList){
		for(int i=0; i<antNumber; i++){
            try {
                fitness.set(i, (pow((numberOfPMsToSleep.get(i).size()), relativeImportanceOfPMsToSleep) + (double) (1 / migrationMapList.get(i).size())));
            } catch (ArithmeticException e) {
                fitness.set(i, 0.0);
            }
		}
	}

	/**
	 *
	 * @param vm
	 * @return
	 */
	public Host findHostForVm(Vm vm) {
		for (Host host : this.<Host> getHostList()) {
			if (host.isSuitableForVm(vm) && host != vm.getHost()) {
				return host;
			}
		}
		return null;
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
}
