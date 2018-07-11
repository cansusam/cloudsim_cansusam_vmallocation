package org.cloudbus.cloudsim.cloudsim_cansusam_vmallocation.power;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class basicAllocation extends PowerVmAllocationPolicyAbstractExtended {


    /**
     * Creates a new VmAllocationPolicy object.
     *
     * @param list Machines available in a {@link Datacenter}
     * @pre $none
     * @post $none
     */
    public basicAllocation(List<? extends Host> list) {
        super(list);
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {

        List<Host> hostList = getHostList();
        ArrayList<Double> hostUtilizationSingle = new ArrayList<Double>();

        // Calculate each hosts utilization and classify
        double totalRequestedMips;//, totalRequestedMips_NOTUSED;
        for(Host h: hostList) {
            totalRequestedMips = 0;
            for (Vm vm : h.getVmList()) {
                totalRequestedMips +=  vm.getMips(); //vm.getCurrentRequestedTotalMips(); //
            }
            hostUtilizationSingle.add(totalRequestedMips);
        }

        // find minimum vm mips capacity
        double minVMCapacity = Double.POSITIVE_INFINITY;
        for (Vm vm : vmList) {
            if(minVMCapacity > vm.getMips())
                minVMCapacity = vm.getMips();
        }


        int[] sortedIndicesOfHostsByCapacity = sortIndicesOfHostByCapacity();
        int numberOfHosts = sortedIndicesOfHostsByCapacity.length;

        ArrayList<Integer> groupValues = new ArrayList<>();
        ArrayList<Integer> groupStartIndex = new ArrayList<>();
        int lastValue = -1;
        for (int i = 0; i < numberOfHosts; i++) {
            int hostCapacity = hostList.get(sortedIndicesOfHostsByCapacity[i]).getTotalMips();
            if (lastValue != hostCapacity) {
                lastValue = hostCapacity;
                groupValues.add(hostCapacity);
                groupStartIndex.add(i);
            }
        }

        int hostCounter=0;
        antHost hostListInfo = new antHost();
        for(Host h:hostList){
            hostListInfo.Host.add(h.getId());
            hostListInfo.Vm.add(new ArrayList<Integer>());
            hostListInfo.UserId.add(new ArrayList<Integer>());
            for(Vm v:h.getVmList()){
                hostListInfo.Vm.get(hostCounter).add(v.getId());
                hostListInfo.UserId.get(hostCounter).add(v.getUserId());
            }
            hostListInfo.utilVal.add(hostUtilizationSingle.get(hostCounter));
            hostCounter++;
        }


        LinkedList<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();

        int numberOfMigrations = 0;
        for (int i = numberOfHosts-1; i >= 0 ; i--) {
            int destinationHostIndex = sortedIndicesOfHostsByCapacity[i];
            Host destinationHost = getHostList().get(destinationHostIndex);
            double destinationHostEmptySlot = destinationHost.getTotalMips()-hostListInfo.utilVal.get(destinationHostIndex);
            if (destinationHostEmptySlot > minVMCapacity) {
                for (int j = 0; j < i; j++) { //TODO comparison should be made by host capacities, if they are equal, stop
                    int sourceHostIndex = sortedIndicesOfHostsByCapacity[j];
                    Host sourceHost = getHostList().get(sourceHostIndex);
//                if (sourceHost.getTotalMips() < destinationHost.getTotalMips())
                    for (Vm vm : sourceHost.getVmList()) {
                        if (isAntHostIsSuitable(hostListInfo.utilVal.get(destinationHostIndex),vm,destinationHost)) {
                            migrationUpdate(migrationMap,destinationHost,vm);
                            numberOfMigrations++;
                            int[] indexOfVmAndItsHostInAntHost = findIndexOfVmInAntHost(hostListInfo,vm);
                            int indexOfVm = indexOfVmAndItsHostInAntHost[0];
                            int indexOfSourceHost = indexOfVmAndItsHostInAntHost[1];
                            double vmMips = vm.getMips();//getCurrentRequestedTotalMips();//
                            double sourceUtilization = hostListInfo.utilVal.get(indexOfSourceHost) - vmMips;
                            double destinationUtilization = hostListInfo.utilVal.get(destinationHostIndex) + vmMips;
                            //update vm information
                            hostListInfo.Vm.get(indexOfSourceHost).remove(indexOfVm);
                            hostListInfo.Vm.get(destinationHostIndex).add(vm.getId());
                            //update userId information
                            hostListInfo.UserId.get(indexOfSourceHost).remove(indexOfVm);
                            hostListInfo.UserId.get(destinationHostIndex).add(vm.getUserId());
                            //update utilization information
                            hostListInfo.utilVal.set(indexOfSourceHost,sourceUtilization);
                            hostListInfo.utilVal.set(destinationHostIndex,destinationUtilization);

                            destinationHostEmptySlot = destinationHost.getTotalMips()-hostListInfo.utilVal.get(destinationHostIndex);
                        }
                        if (destinationHostEmptySlot < minVMCapacity) {
                            break;
                        }
                    }
                    if (destinationHostEmptySlot < minVMCapacity) {
                        break;
                    }
                }
            }
        }
        return (numberOfMigrations>0) ? migrationMap : null;
    }

    public boolean isAntHostIsSuitable(Double hostUtilization, Vm vm, Host h) {
        double newAvailableMips =  h.getTotalMips() - hostUtilization;
        double vmMips = vm.getMips();//vm.getCurrentRequestedTotalMips();//
        return (h.getVmScheduler().getPeCapacity() >= vmMips
                && newAvailableMips >= vmMips
                && (hostUtilization + vmMips) < h.getTotalMips()
                && h.getRamProvisioner().isSuitableForVm(vm, vm.getCurrentRequestedRam()) && h.getBwProvisioner()
                .isSuitableForVm(vm, vm.getCurrentRequestedBw()));
    }

}
