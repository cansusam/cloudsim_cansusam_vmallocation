#!/usr/bin/env bash

#@canssm
#For cloud studies.
#Cloudsim generates result files, which are hard to examine when there are more than one. To arange the SLA, Energy Consumption, VM Migration and Host Shutdown values in the files in the same folder, this scrip can be used.

cd "$(dirname "$BASH_SOURCE")" || {
    echo "Error getting script directory" >&2
    exit 1
}

path="$PWD"

functionPrint (){
  echo $1 # echo first parameters value
  for f in $path/*.txt;
  do  string="$(awk NR==$2 $f)"; # find the line that is equal to the second parameters' value
  new_f=${string#$1} # remove first parameters' value from the string
  echo $new_f
  done
}

functionPrint "SLA: " "7" > $path/results
functionPrint "Energy consumption: " "5" >> $path/results
functionPrint "Number of VM migrations: " "6" >> $path/results
functionPrint "Number of host shutdowns: " "12" >> $path/results
functionPrint "Elapsed real time = " "18" >> $path/results
functionPrint "iterationLimit: " "25" >> $path/results
functionPrint "antNumber: " "26" >> $path/results
functionPrint "antVMNumber: " "27" >> $path/results
functionPrint "pheromoneDecayGlobal: " "29" >> $path/results
mv $path/results $path/results.txt
python trial.py