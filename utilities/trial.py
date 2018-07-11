import matplotlib.pyplot as plt
from io import StringIO
from collections import OrderedDict
import pandas as pd
import numpy as np
import os
import time

cwd = os.getcwd()
timestr = time.strftime("%Y%m%d-%H%M%S")

newDir = cwd+ "/" + timestr

if not os.path.exists(newDir):
    os.makedirs(newDir)

newDir = newDir + "/"

content = ""
# x=np.loadtxt('../results.txt')
with open(cwd+'/results.txt', 'r') as myfile:
#    datastring=myfile
# print(datastring)
    for line in myfile.readlines():
        content += line


content = StringIO(content)
out = OrderedDict()
final = []

for line in content:
    if line[0].isalpha():
        header = line.strip('\n#')
        out[header] = []
    elif line not in out[header]:
        # to prevent getting units as a number
        if(header == "SLA:"):
            out[header].append(line[0:7].strip('\n'))
        elif(header == "Energy consumption:"):
            out[header].append(line[0:7].strip('\n'))
        else:
            out[header].append(line.strip('\n'))

sub = pd.DataFrame()

for k, v in out.items():
    sub[k] = np.asarray(v)


sub.columns = ['SLA',
             'Energy consumption (kWh)',
             'Number of VM migrations',
             'Number of host shutdowns',
             'Elapsed real time',
             'iterationLimit',
             'antNumber',
             'antVMNumber',
             'pheromoneDecay']
sub.to_csv(newDir + '/results.csv', float_format='%.8f', index=False)

sub['SLA'] = sub['SLA'].astype('float')
sub['Energy consumption (kWh)'] = sub['Energy consumption (kWh)'].astype('float')
sub['Number of VM migrations'] = sub['Number of VM migrations'].astype('int')
sub['Number of host shutdowns'] = sub['Number of host shutdowns'].astype('int')
# elapsed real time missing, format as time
sub['iterationLimit'] = sub['iterationLimit'].astype('int')
sub['antNumber'] = sub['antNumber'].astype('int')
sub['antVMNumber'] = sub['antVMNumber'].astype('int')
sub['pheromoneDecay'] = sub['pheromoneDecay'].astype('float')

plotTitles = ['Number of VM migrations', 'Energy consumption (kWh)']
for title in plotTitles:
    VMNumber_array = sub['antVMNumber'].unique()
    VMNumber_len = len(VMNumber_array)
    for i in range(0,VMNumber_len):
        vmNumVal = VMNumber_array[i]
        VMNumberXLoc = np.where(np.array(sub['antVMNumber']) == vmNumVal)
        sub_VMNumber = sub.iloc[VMNumberXLoc]
        antNumber_array = sub_VMNumber['pheromoneDecay'].unique()
        antNumber_len = len(antNumber_array)
        ticks = np.array([])
        for j in range(0,antNumber_len):
            antNumVal = antNumber_array[j]
            antNumberXLoc = np.where(np.array(sub_VMNumber['pheromoneDecay']) == antNumVal)
            sub_antNumber = sub_VMNumber.iloc[antNumberXLoc]
            iteration_array = sub_antNumber['iterationLimit'].unique()
            ticks = np.array(iteration_array)
            # iteration_len = len(VMNumber_array)
            label_ = "Pheromone Evaporation: "  + str(antNumVal)[0:4]
            plt.plot(np.array(iteration_array),sub_antNumber[title],label = label_)
        name = title + " for antVMcap = " + str(vmNumVal)
        fileName = timestr + "_" + name
        plt.ylabel(title)
        plt.xlabel('Iteration Number')
        plt.title(name)
        plt.grid(True)
        plt.xticks(ticks)
        plt.legend()
        plt.savefig(newDir + name)
        plt.clf()
