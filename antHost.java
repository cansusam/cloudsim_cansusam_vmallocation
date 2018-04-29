package org.cloudbus.cloudsim.cloudsim_cansusam_vmallocation;

import java.util.ArrayList;

/**
 * @author canssm
 */

public class antHost {
    ArrayList<Integer> Host;
    ArrayList<ArrayList<Integer>> Vm;
    ArrayList<ArrayList<Integer>> UserId;
    ArrayList<Double> totalRequestedMIPS;

    public antHost(){
        this.Host = new ArrayList<Integer>();
        this.Vm = new ArrayList<ArrayList<Integer>>();
        this.UserId = new ArrayList<ArrayList<Integer>>();
        this.totalRequestedMIPS = new ArrayList<Double>();
    }

    public antHost(antHost toClone){
        this.Host = toClone.Host;
        this.Vm = toClone.Vm;
        this.UserId = toClone.UserId;
        this.totalRequestedMIPS = toClone.totalRequestedMIPS;
    }

}
