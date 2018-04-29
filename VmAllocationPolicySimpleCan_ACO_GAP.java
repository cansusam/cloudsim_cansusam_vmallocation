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
 * @canssm
 * ACO adaptation of General Assignment Problem (GAP) for VM Allocation
 */

/**
 * @author canssm
 */
public class VmAllocationPolicySimpleCan_ACO_GAP extends VmAllocationPolicy {

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

	private static final double initialPheromoneLevelVmAssign = 1;
    private static final double initialPheromoneLevelHostSelect = 1;
	private double relativeImportanceOfPMsToSleep = 1;
	private double relativeImportanceOfHeuristic = 1;
	private double relativeImportancePheromone = 1;

	private double iterationLimit = 1;
	private static int antNumber = 2;
	private double parameterQ0 = 0.5;
	private double pheromoneDecayLocal = 0.2;
	private double pheromoneDecayGlobal = 0.3;

	private Random randomGen;


	/**
	 * Creates a new VmAllocationPolicySimple object.
	 *
	 * @param list the list of hosts
	 * @pre $none
	 * @post $none
	 */
	public VmAllocationPolicySimpleCan_ACO_GAP(List<? extends Host> list) {
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

		randomGen = new Random(System.currentTimeMillis());

		LinkedList<Map<String, Object>> bestMigrationPlan = new LinkedList<Map<String, Object>>();
		Double bestFitness = -1.0;

		List<Host> sleepingHosts = new ArrayList<>();
		List<ArrayList<Double>> pheromoneListVmAssign = new ArrayList<ArrayList<Double>>();
        List<ArrayList<Double>> pheromoneListHostSelect = new ArrayList<ArrayList<Double>>();
		List<Double> heuristicVm = new ArrayList<>();
		List<Double> heuristicHost = new ArrayList<>();

		// Get current host list
		int hostNumber = 0;
		List<Host> hostList = new ArrayList<Host>();
		for(int i = 0; i<vmList.size(); i++){
			if(hostList.isEmpty() ||!hostList.contains(vmList.get(i).getHost()))
				hostList.add(vmList.get(i).getHost());
		}

		// Host utilization matrix, shows remaining capacity to reach to sleep
		ArrayList<Double> hostUtilizationSingle = new ArrayList<Double>();

		// Calculate each hosts utilization and classify
		double totalRequestedMips;
		double currentUtilization;
		for(int i=0; i<hostList.size(); i++) {
			totalRequestedMips = 0;
			for (Vm vm : hostList.get(i).getVmList()) {
				totalRequestedMips += vm.getCurrentRequestedTotalMips();
			}
			currentUtilization = totalRequestedMips / hostList.get(i).getTotalMips();
			// update host utilization, according to indexes in hostlist
			hostUtilizationSingle.add(totalRequestedMips);
			if(currentUtilization == 0) sleepingHosts.add(hostList.get(i));
		}


		// Host utilization matrix, shows remaining capacity to reach to sleep
		// Previously calculated value copied for each ant to simplify their calculation step

		pheromoneListVmAssign = pheromoneInitialization(pheromoneListVmAssign,vmList.size(),vmList.size(), initialPheromoneLevelVmAssign);
        pheromoneListHostSelect = pheromoneInitialization(pheromoneListHostSelect,vmList.size(),hostList.size(), initialPheromoneLevelHostSelect);

		heuristicVm = heuristicInitializationVm(vmList);
		heuristicHost = heuristicInitializationHost(hostList);
		/** ACO simulation starts from this point */

		int iteration = 0;
		int bestNumberOfSleepingPMs = -1;
		while(iterationLimit > iteration){
			List<ArrayList<Double>> hostUtilization = new ArrayList<ArrayList<Double>>();
			List<antHost> antHostList = new ArrayList<antHost>();
			List<LinkedList<Map<String, Object>>> migrationMapList = new ArrayList<LinkedList<Map<String, Object>>>();

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
					newMyHost.totalRequestedMIPS.add(hostUtilizationSingle.get(hostCounter));
					hostCounter++;
				}
				antHostList.add(new antHost(newMyHost));
				migrationMapList.add(new LinkedList<Map<String, Object>>());
				antNumberOfPMsToSleep.add(new ArrayList<Integer>());
				antFitness.add(new Double(0.0));

				// As a first step, a VM randomly assigned to each ant
				int random = randomGen.nextInt(vmList.size());
				antSelectedVm[i] = random;//vmList.get(random).getId();
			}

			for(int k=0; k<vmList.size(); k++){
				for(int l=0; l<antNumber; l++){
					// select new VM
					if(k!=0) // for first condition, vm selected by randomly
						if(antSelectedVm[l] != -1)
							antSelectedVm[l] = selectNewVm(vmList,pheromoneListVmAssign.get(antSelectedVm[l]),
														migrationMapList.get(l),heuristicVm);
					// select new Host
					int selectedHost = -1;
					if (antSelectedVm[l] != -1) {
						selectedHost = selectNewHost(vmList.get(antSelectedVm[l]),
								hostList,
								pheromoneListHostSelect.get(antSelectedVm[l]),
								heuristicHost,
								antHostList.get(l),
								antNumberOfPMsToSleep.get(l),
								migrationMapList.get(l));
					}
					// if a host found for vm, update trails
					if (selectedHost != -1) {
						pheromoneUpdate(pheromoneListVmAssign,initialPheromoneLevelVmAssign,pheromoneDecayLocal);
						pheromoneUpdate(pheromoneListHostSelect,initialPheromoneLevelHostSelect,pheromoneDecayLocal);
						//fitness calculations
						//updateHostUtilization(hostUtilization,antTuples,antHostList,antNumberOfPMsToSleep);
						objectiveFunction(antFitness.get(l),antNumberOfPMsToSleep.get(l),migrationMapList.get(l));
					}
				}
			}
			pheromoneUpdate(pheromoneListVmAssign,initialPheromoneLevelVmAssign,pheromoneDecayGlobal);
			pheromoneUpdate(pheromoneListHostSelect,initialPheromoneLevelHostSelect,pheromoneDecayGlobal);

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
	 */
	public int selectNewVm(List<? extends Vm> vmList,
							List<Double> pheromoneList,
							LinkedList<Map<String,Object>> migrationMap,
							List<Double> heuristicList
							){
		List<Integer> notYetSelectedVMs = new ArrayList<Integer>();	// index of not selected vms in vmlist
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
			// TODO: remaining host may not have required conditions for the vm
			// the host should not contain the original VM, must be suitable for vm and should not be in sleeping mode
			if (!theAntHost.Vm.get(i).contains(selectedVm.getId())
					&& isAntHostIsSuitable(theAntHost.totalRequestedMIPS.get(i), selectedVm.getCurrentRequestedTotalMips())
					&& !antNumberOfPMsToSleep.contains(theAntHost.Host.get(i))) {
				availableHosts.add(i);
				probabilityDistribution.add(pheromoneList.get(i));
			}
		}
		// there is no available host
		if(availableHosts.size() == 0)
			return -1;

		// since the antHost host list created according to hostList, indexes must match
		int destinationHost = probabilisticResult(probabilityDistribution,pheromoneList,heuristicList,availableHosts);

		//update host informations
		double vmMips = selectedVm.getCurrentRequestedTotalMips();
		double destinationUtilization = theAntHost.totalRequestedMIPS.get(destinationHost) + vmMips;
		if(destinationUtilization < 0)
			System.out.println("Destination utilization is negative?");
		double sourceUtilization = theAntHost.totalRequestedMIPS.get(indexOfSourceHost) - vmMips;
		if (sourceUtilization < 0) {

			System.out.println("Source utilization is negative?");
			return -1;
		}
		if(sourceUtilization == 0)
			if(!antNumberOfPMsToSleep.contains(sourceId))

				antNumberOfPMsToSleep.add(sourceId);

		//update vm information
		theAntHost.Vm.get(indexOfSourceHost).remove(indexOfVm);
		theAntHost.Vm.get(destinationHost).add(selectedVm.getId());
		//update userId information
		theAntHost.UserId.get(indexOfSourceHost).remove(indexOfVm);
		theAntHost.UserId.get(destinationHost).add(selectedVm.getId());
		//update utilization information
        theAntHost.totalRequestedMIPS.set(indexOfSourceHost,sourceUtilization);
        theAntHost.totalRequestedMIPS.set(destinationHost,destinationUtilization);

		//add to migration list
		migrationUpdate(migrationMap,hostList.get(destinationHost),selectedVm);

		return destinationHost;
	}

	public boolean isAntHostIsSuitable(Double hostUtilization, Double vmMips) {
		if(hostUtilization - vmMips < 0 )
			return false;
		return true;
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

		try {
			for(int i=0; i<theAntHost.Host.size(); i++) {
				for(int j=0; j<theAntHost.Vm.get(i).size(); j++) {
					if (userId == theAntHost.UserId.get(i).get(j) && vmId == theAntHost.Vm.get(i).get(j)) {
						int[] indexOfVmAndItsHostInAntHost = new int[]{j,i};
						return indexOfVmAndItsHostInAntHost;
					}
				}
			}
		}catch (IndexOutOfBoundsException e){
			System.out.println("Index out of bounds for vm, userId or host");
		}
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
			double[] probabilityDistributionResult = pheromoneDistribution(probabilityDistribution,pheromoneList,heuristicList,notYetSelected);
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
		return selected;
	}

	/**
	 *
	 * @param pheromoneList
	 */
	public void pheromoneUpdate(List<ArrayList<Double>>  pheromoneList, double initialPheromone, double pheromoneDecay){
		for(int i=0;i<pheromoneList.size(); i++)
			for(int j=0;j<pheromoneList.get(i).size(); j++)
				pheromoneList.get(i).set(j,((1- pheromoneDecay)*pheromoneList.get(i).get(j)+ pheromoneDecay * initialPheromone));
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
			totalSum += Math.pow(pheromoneList.get(i),relativeImportancePheromone)/Math.pow(heuristicList.get(notYetSelectedVMs.get(i)),relativeImportanceOfHeuristic);
		}
		for(int i = 0; i<antAvailableVmSize; i++){
			cumulativeDistribution[i] = (Math.pow(pheromoneList.get(i),relativeImportancePheromone)/Math.pow(heuristicList.get(notYetSelectedVMs.get(i)),relativeImportanceOfHeuristic)) / totalSum;
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
		return vm.getCurrentRequestedTotalMips();
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
	 * @param fitness
	 * @param numberOfPMsToSleep
	 * @param migrationMapList
	 */
	public void objectiveFunction(Double fitness, ArrayList<Integer> numberOfPMsToSleep, LinkedList<Map<String, Object>> migrationMapList){
		fitness = (pow((numberOfPMsToSleep.size()), relativeImportanceOfPMsToSleep)+(double)(1/migrationMapList.size()));
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
