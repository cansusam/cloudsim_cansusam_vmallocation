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

/**
 * @author canssm
 */
public class VmAllocationPolicySimpleCan extends VmAllocationPolicy {

	/** The map between each VM and its allocated host.
         * The map key is a VM UID and the value is the allocated host for that VM. */
	private Map<String, Host> vmTable;

	/** The map between each VM and the number of Pes used.
         * The map key is a VM UID and the value is the number of used Pes for that VM. */
	private Map<String, Integer> usedPes;

	/** The number of free Pes for each host from {@link #getHostList() }. */
	private List<Integer> freePes;

	private double utilizationMinValue = 0.1;

	/**
	 * Creates a new VmAllocationPolicySimple object.
	 *
	 * @param list the list of hosts
	 * @pre $none
	 * @post $none
	 */
	public VmAllocationPolicySimpleCan(List<? extends Host> list) {
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

		List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();

		// Get current host list
		int hostNumber = 0;
		List<Host> myHostList = new ArrayList<Host>();
		for(int i = 0; i<vmList.size(); i++){
			if(myHostList.isEmpty() ||!myHostList.contains(vmList.get(i).getHost()))
				myHostList.add(vmList.get(i).getHost());
		}

		// Calculate each hosts utilization
		double totalRequestedMips;
		List<Double> utilization = new ArrayList<Double>();
		for(int i=0; i<myHostList.size(); i++) {
			totalRequestedMips = 0;
			for (Vm vm : myHostList.get(i).getVmList()) {
				totalRequestedMips += vm.getCurrentRequestedTotalMips();
			}
			utilization.add(totalRequestedMips / myHostList.get(i).getTotalMips());
		}

		List<Vm> migratableVMs = new ArrayList<>();

		// Finding host with minimum utilization
		double minValueOfUtilization = 1.0;
		int minValueIndex = 0;
		for(int i=0; i<utilization.size(); i++) {
			if (utilization.get(i) < minValueOfUtilization) {
				minValueOfUtilization = utilization.get(i);
				minValueIndex = i;
			}
		}

		// Add VMs from the min. utilized host
		if ( minValueOfUtilization < utilizationMinValue ) {
			//for (int i = 0; i < myHostList.size(); i++) {
				for (Vm vm : myHostList.get(minValueIndex).getVmList()) {
					if (!vm.isInMigration()) {
						migratableVMs.add(vm);
					}
				}
			//}
		}

		//
		if (migratableVMs.isEmpty()) {
			return null;
		} else{
			for (Vm vm : migratableVMs) {
				Host allocatedHost = findHostForVm(vm);
				if (allocatedHost != null) {
					Map<String, Object> migrate = new HashMap<String, Object>();
					migrate.put("vm", vm);
					migrate.put("host", allocatedHost);
					migrationMap.add(migrate);
				}
			}
		}

		return migrationMap;
	}

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
