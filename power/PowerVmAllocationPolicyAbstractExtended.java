package org.cloudbus.cloudsim.cloudsim_cansusam_vmallocation.power;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyAbstract;

import java.util.*;
import java.util.stream.IntStream;

abstract class PowerVmAllocationPolicyAbstractExtended extends PowerVmAllocationPolicyAbstract {
    /**
     * Creates a list of host capacities in descending order
     * Places VM into the first host with highest capacity
     * @param vm the vm to find a host for it
     * @return
     */
    List<? extends Host> hostList;

    /**
     * Creates a new VmAllocationPolicy object.
     *
     * @param list Machines available in a {@link Datacenter}
     * @pre $none
     * @post $none
     */
    public PowerVmAllocationPolicyAbstractExtended(List<? extends Host> list) {
        super(list);
    }

    @Override
    public org.cloudbus.cloudsim.power.PowerHost findHostForVm(Vm vm) {
        // list hosts by their capacity
        int[] sortedIndicesOfHostsByCapacity = sortIndicesOfHostByCapacity();

        for (int j = sortedIndicesOfHostsByCapacity.length-1; j>=0; j--) {
            if (this.getHostList().get(sortedIndicesOfHostsByCapacity[j]).isSuitableForVm(vm)) {
                return (PowerHost) this.getHostList().get(sortedIndicesOfHostsByCapacity[j]);
            }
        }
        return null;
    }

    public int[] sortIndicesOfHostByCapacity(){
        ArrayList<Integer> hostCapacities = new ArrayList<Integer>();
        for (org.cloudbus.cloudsim.power.PowerHost host : this.<org.cloudbus.cloudsim.power.PowerHost> getHostList()) {
            hostCapacities.add(host.getTotalMips());
        }
        return IntStream.range(0, hostCapacities.size())
                .boxed().sorted((i, j) -> hostCapacities.get(i).compareTo(hostCapacities.get(j)))
                .mapToInt(ele -> ele).toArray();
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
        // not found
        int[] a = new int[]{-1, -1};
        return a;
    }
}
