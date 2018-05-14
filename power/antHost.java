package org.cloudbus.cloudsim.cloudsim_cansusam_vmallocation.power;

import java.util.ArrayList;

/**
 * @author canssm
 */

public class antHost {

    /**
     * * * Add these in first place
     * Migration map
     * Sleeping hosts
     * * * Maybe
     * Energy Info
     * VM Migration Info
     */

    ArrayList<Integer> Host;
    ArrayList<ArrayList<Integer>> Vm;
    ArrayList<ArrayList<Integer>> UserId;
    ArrayList<Double> utilVal; // keeps total requested mips, TODO: confusing, meaning of utilization, totalReqMips/totalMips

    public antHost(){
        this.Host = new ArrayList<Integer>();
        this.Vm = new ArrayList<ArrayList<Integer>>();
        this.UserId = new ArrayList<ArrayList<Integer>>();
        this.utilVal = new ArrayList<Double>();
    }

    public antHost(antHost toClone){
        this.Host = toClone.Host;
        this.Vm = toClone.Vm;
        this.UserId = toClone.UserId;
        this.utilVal = toClone.utilVal;
    }

}
