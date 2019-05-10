#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri Jul 13 13:22:43 2018

@author: seilhan3
"""
# import the java stuff
import csv
import jpype as jp
import jpype.imports # this allows for importing of the gov directories 
import glob
import os
import pandas as pd
import numpy as np
import matplotlib as mpl
import matplotlib.pyplot as plt
import math
from math import *
import utm
import itertools
import glob
import time as tm
import datetime

# add dependencies to class path
classpath = os.pathsep.join(glob.glob(os.path.join("jars", "*.jar")))
# start up the JVM pointing to classpath (java dependencies)
if not jp.isJVMStarted():
  print("Starting up JVM")
  jp.startJVM(jp.getDefaultJVMPath() , "-Djava.class.path=%s" % classpath)

# load in java tools
from org.sigma.thrift import ThriftStreamReader
from org.sigma.thrift import MessageUtilities
from java.nio.file import Paths

# I know global variables are bad but this is the last working version. Converting to python objects broke the code so this is what is being documented

#For One File
cps = []
extrastuff = []
everything = []
date = []
time = []
lat = []
lon = []
utmeasting = []
utmnorthing = []
deltas = []
speeds_metersperminute = []
speeds_milesperhour = []
active_deltas = []

#For Everything in a folder
reported_sensors = ['Sensors Reporting']
sensordata_size = []
sensordata_percentage = ['Percent of Total Data']
a_cps = []
a_extrastuff = []
a_everything = []
a_date = []
a_time = []
a_lat = []
a_lon = []
a_utmeasting = []
a_utmnorthing = []
a_deltas = []
a_speeds_metersperminute = []
a_speeds_milesperhour = []
a_active_deltas = []
a_allthestuffs = []
foldername = []
instances = []

#GPS Correction
scan_time = []
thrift_time = []
scan_stop_number = []
scan_lat = []
scan_lon = []
scan_thrift_lat = []
scan_thrift_lon = []
scan_tau_utmeasting = []
scan_tau_utmnorthing = []
thrift_utmeasting = []
thrift_utmnorthing = []
scan_stop_easting = [435008.72,435009.66,435010.51,435010.55,435010.44]
scan_stop_northing = [3774165.71,3774153.06,3774138.74,3774124.79,3774115.49]
scan_stop_easting3 = [434397.90,434409.43,434424.18,434437.92]
scan_stop_northing3 = [3774151.46,3774152.60,3774153.72,3774154.18]
scan_stop_eastingFinal = [434407.23,434294.06,434480.89,434796.49,434807.32,434921.55,434945.61,434644.65,434381.77,434381.15,434576.89,434806.26,434758.54,434897.5,434862.82]
scan_stop_northingFinal = [3774161.25,3773765.72,3773571.48,3773548.44,3773701.28,3773758.71,3774103.96,3774186.77,3774054.86,3773780.42,3773633.35,3773641.63,3773769.59,3773803.23,3774096.55]

def readallThrift(thrift_folder,stepsize,saturation_level,inculde_QR):
    """
        This Function takes in a folder name of thrift files located at the same level as the python file,
        a stepsize for determining the resolution of the heat map as a single number in meters,
        a saturation level for capping the upper value of the heat map as a single number
        and a boolean vale for include_QR to determine whether QR locations are plotted over top
            The function parses the thrift file for:
                date
                time
                latitude
                longitude
            The function then takes these values and converts them to UTM coordinates and plots them as a path, determines how much of the total each sensor was
            The heat map portion of this function takes the data and distributes across the map in a grid based off of the step size in meters. This is then displayed as a heat map that has its upper level windowed by the saturation level

    """

    del reported_sensors[:]
    del sensordata_size[:]
    del sensordata_percentage[:]
    del a_cps[:]
    del a_extrastuff[:]
    del a_everything[:]
    del a_date[:]
    del a_time[:]
    del a_lat[:]
    del a_lon[:]
    del a_utmeasting[:]
    del a_utmnorthing[:]
    del a_deltas[:]
    del a_speeds_metersperminute[:]
    del a_speeds_milesperhour[:]
    del a_active_deltas[:]
    del a_allthestuffs[:]
    del foldername[:]
    reported_sensors.append('Sensors Reporting')
    sensordata_percentage.append('Percent of Total Data')
    foldername.append(thrift_folder)
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), thrift_folder)
    for thriftfile in glob.glob(os.path.join(path, '*.thrift')):    
        b = []
        a_lat_internal = []
        a_lon_internal = []
        with ThriftStreamReader(Paths.get(thriftfile)) as istream:
            print(istream.getDetectorInfo().getSerialNumber())
            reported_sensors.append(istream.getDetectorInfo().getSerialNumber())
            for n,message in enumerate(istream.iterator()):
                report = MessageUtilities.loadReport(message)
                now = MessageUtilities.getTimestamp(report)
                instances.append(now)
                location = MessageUtilities.getLocation(report)
                gc = MessageUtilities.getTotalCounts(report)
                spectrum = MessageUtilities.getSpectrum(report)
                a = '{:s} {:9.6f},{:9.6f} {:d}'.format(now.toString(),location.getLatitude(),location.getLongitude(),gc)
                a_everything.append(a[0:46])
                a_extrastuff.append(a[0:])
        
                b.append(a[0:])
        sensordata_size.append(len(b))
        y = range(len(b))
        for i in y:
            if len(b[i]) == 49:
                a_date.append(b[i][0:10])
                a_time.append(b[i][11:23])
                a_lat.append(float(b[i][25:34]))
                a_lon.append(float(b[i][35:46]))
                a_lat_internal.append(float(b[i][25:34]))
                a_lon_internal.append(float(b[i][35:46]))                
                a_cps.append(float(b[i][47:49]))
                x = [b[i][0:10],b[i][11:23],float(b[i][25:34]),float(b[i][35:46]),float(b[i][47:49])]
                a_allthestuffs.append([b[i][0:10],b[i][11:23],float(b[i][25:34]),float(b[i][35:46]),float(b[i][47:49])])
        c = a_lat_internal
        d = a_lon_internal
        x = range(len(c))
        for i in x:
            a_utmeasting.append(utm.from_latlon(c[i],d[i])[0])
            a_utmnorthing.append(utm.from_latlon(c[i],d[i])[1])
    for i in range(len(sensordata_size)):
        sensordata_percentage.append(sensordata_size[i]/(len(a_utmeasting))*100)
    utmeasting_delta = [j-i for i, j in zip(a_utmeasting[:-1], a_utmeasting[1:])]
    utmnorthing_delta = [j-i for i, j in zip(a_utmnorthing[:-1], a_utmnorthing[1:])] 
    utm_iterator = range(len(utmeasting_delta))
    for i in utm_iterator:
        a_deltas.append(
            sqrt(
                (utmeasting_delta[i])**2+(utmnorthing_delta[i])**2
                )
        )
    delta_iterator = range(len(a_deltas))
    for i in delta_iterator:
        a_speeds_metersperminute.append(a_deltas[i]/0.00416)
        a_speeds_milesperhour.append(a_speeds_metersperminute[i]/26.8224)
    filtered_deltas = list(filter((0.0).__ne__, a_deltas))
    for i in range(len(filtered_deltas)):
        active_deltas.append(filtered_deltas[i])
    active_points = len(filtered_deltas)
    active_minutes = active_points/5
    active_hours = active_minutes/60
    print('active minutes = ', active_minutes)
    print('active hours = ', active_hours)


    utmdataframe = pd.DataFrame({'Easting' : a_utmeasting, 'Northing' : a_utmnorthing})
    max_value_easting = max(utmdataframe['Easting'])
    min_value_easting = min(utmdataframe['Easting'])
    easting_delta = max_value_easting-min_value_easting
    max_value_easting = max_value_easting + easting_delta*.025
    min_value_easting = min_value_easting - easting_delta*.025
    max_value_northing = max(utmdataframe['Northing'])
    min_value_northing = min(utmdataframe['Northing'])
    northing_delta = max_value_northing-min_value_northing
    max_value_northing = max_value_northing + northing_delta*.025
    min_value_northing = min_value_northing - northing_delta*.025
    # img = plt.imread("map.jpg")
    # fig, ax = plt.subplots()
    # ax.imshow(img, extent = [434265.61381463625,434973.49016928545,3773376.8694098727,3774197.5671255533])
    # scatter_plot =ax.scatter(a_utmeasting, a_utmnorthing, s=2, c='#824CD4')
    # for i in range(len(reported_sensors)):
    #     plt.text(0.02, 0.5-.02*i, reported_sensors[i]+'   '+str(sensordata_percentage[i]), fontsize=10, transform=plt.gcf().transFigure)
    # ax.set_xlim(xmin=434265.61381463625 , xmax=434973.49016928545)
    # ax.set_ylim(ymin=3773376.8694098727 , ymax=3774197.5671255533)
    # plt.xlabel('UTM Easting')
    # plt.ylabel('UTM Northing')
    # plt.title('User path for ' +str(thrift_folder))
    # plt.show()
    
    
    print(len(a_utmeasting))
    heat = []
    max_heat = 0
    max_heat_2 = 0
    easting_range = range(int(min_value_easting), int(max_value_easting))
    northing_range = range(int(min_value_northing), int(max_value_northing))
    for j in range(int(min_value_northing), int(max_value_northing), stepsize):
        row = []
        for i in range(int(min_value_easting), int(max_value_easting),stepsize):  
            row.append(0)  
        heat.append(row)

    for i in range(len(a_utmeasting)):
        heat[int((int(a_utmnorthing[i])-int(min_value_northing))/stepsize)][int((int(a_utmeasting[i])-int(min_value_easting))/stepsize)] += 1
    for i in range(len(heat)):
        for j in range(len(heat[i])):
            if heat[i][j] > max_heat:
                max_heat = heat[i][j]
    
    for i in range(len(heat)):
        for j in range(len(heat[i])):
            if heat[i][j] > saturation_level:
                heat[i][j] = saturation_level

    for i in range(len(heat)):
        for j in range(len(heat[i])):
            if heat[i][j] > max_heat_2:
                max_heat_2 = heat[i][j]    
    
    heat = heat[::-1]
    img = plt.imread("map.jpg")
    fig, ax = plt.subplots()
    ax.imshow(img, extent = [434265.61381463625,434973.49016928545,3773376.8694098727,3774197.5671255533])
    plt.imshow(heat, cmap='ocean_r', interpolation='none', extent = [min_value_easting,max_value_easting,min_value_northing,max_value_northing], alpha = .6)

    if inculde_QR == True:
        print(thrift_folder)     
        qr_easting = [434656.83164434927, 434703.0671030256, 434729.39589894173, 434784.9161682739, 434875.77309062914, 434894.89286255947, 434875.0718818688, 434795.2259855086, 434694.2390751023, 434631.20345609944, 434519.7429454065, 434358.89848690946, 434363.23734350037, 434350.63314087567, 434415.02337086666, 434550.08229921735, 434593.8586904025]
        qr_northing = [3774157.2110547, 3774179.212427485, 3774166.416478734, 3774155.9246310145, 3774162.7036760813, 3774134.4447325226, 3774095.937084754, 3774080.892735427, 3774129.8617153177, 3774130.0436967206, 3774130.3961061738, 3774137.5806525033, 3774097.0373155484, 3774161.3240135917, 3774158.3252447797, 3774159.84247252, 3774157.093456563]
        number_of_scans = [46,10,42,45,15,22,26,43,33,25,39,25,21,32,47,68,56]
        scatter_plot =ax.scatter(qr_easting, qr_northing, s=number_of_scans, c='#E1FF00')
        for i, number_of_scans in enumerate(number_of_scans):
            ax.annotate(number_of_scans, (qr_easting[i], qr_northing[i]))
    ax.set_xlim(xmin=434265.61381463625 , xmax=434973.49016928545)
    ax.set_ylim(ymin=3773376.8694098727 , ymax=3774197.5671255533)
    colorbar = plt.colorbar()
    colorbar.set_label('Sensor Reports per Unit Area')    
    plt.xlabel('UTM Easting')
    plt.ylabel('UTM Northing')
    plt.title('Coverage heat map for '+str(thrift_folder)+' with area unit size of '+str(stepsize)+ ' and a saturation level of '+str(saturation_level))
    plt.show()
    print(len(a_utmeasting))
    
def radiationmapAll(stepsize,inculde_QR):
    """
        This function takes in:
        a stepsize for determining the resolution of the heat map as a single number in meters,
        and a boolean vale for include_QR to determine whether QR locations are plotted over top

            The function uses the location from readallThrift, which must be run first, and plots a heat map using these values and the radiation data associated
    """
    utmdataframe = pd.DataFrame({'Easting' : a_utmeasting, 'Northing' : a_utmnorthing})
    max_value_easting = max(utmdataframe['Easting'])
    min_value_easting = min(utmdataframe['Easting'])
    easting_delta = max_value_easting-min_value_easting
    max_value_easting = max_value_easting + easting_delta*.025
    min_value_easting = min_value_easting - easting_delta*.025
    max_value_northing = max(utmdataframe['Northing'])
    min_value_northing = min(utmdataframe['Northing'])
    northing_delta = max_value_northing-min_value_northing
    max_value_northing = max_value_northing + northing_delta*.025
    min_value_northing = min_value_northing - northing_delta*.025
    
    heat = []
    count = []
    max_heat = 0

    
    for j in range(int(min_value_northing), int(max_value_northing), stepsize):
        row = []
        for i in range(int(min_value_easting), int(max_value_easting),stepsize):  
            row.append(0)  
        heat.append(row)
        count.append(row)
    
    for i in range(len(a_utmeasting)):
        if heat[int((int(a_utmnorthing[i])-int(min_value_northing))/stepsize)][int((int(a_utmeasting[i])-int(min_value_easting))/stepsize)] <= a_cps[i]:
            heat[int((int(a_utmnorthing[i])-int(min_value_northing))/stepsize)][int((int(a_utmeasting[i])-int(min_value_easting))/stepsize)] = a_cps[i]
    
    for i in range(len(heat)):
        for j in range(len(heat[i])):
            if heat[i][j] > max_heat:
                max_heat = heat[i][j]
    heat = heat[::-1]
    print(max_heat)
    img = plt.imread("map.jpg")
    fig, ax = plt.subplots()
    ax.imshow(img, extent = [434265.61381463625,434973.49016928545,3773376.8694098727,3774197.5671255533])
    plt.imshow(heat, cmap='hot_r', interpolation='none', extent = [min_value_easting,max_value_easting,min_value_northing,max_value_northing], alpha=.6)
    # plt.imshow(heat, cmap='OrRd', interpolation='none', extent = [min_value_easting,max_value_easting,min_value_northing,max_value_northing], alpha=.6)
    colorbar = plt.colorbar()
    colorbar.set_label('Counts per Second')
    if inculde_QR == True:
        source_easting = [434615.89,434550.83,434379.59,434873.98,434858.65,434669.4]
        source_northing =[3774163.57,3774158.48,3774125.74,3774109.56,3774147.25,3774125.05]
        # source_easting = [434537.67,434491.17,434736.20,434869.82,434938.07,434609.69,434404.27,434666.92]
        # source_northing =[3774162.67,3774135.50,3774167.95,3774108.25,3774108.12,3774159.96,3774160.05,3774125.74]
        scatter_plot_truelocation = plt.scatter(source_easting, source_northing, s=20, c='#18FF00', marker='X', label='Source Location')   
        plt.legend(handles = [scatter_plot_truelocation])
    ax.set_xlim(xmin=434265.61381463625 , xmax=434973.49016928545)
    ax.set_ylim(ymin=3773376.8694098727 , ymax=3774197.5671255533)
    plt.xlabel('UTM Easting')
    plt.ylabel('UTM Northing')
    plt.title('Count rate heat map for '+str(foldername[0])+' with area unit size of '+str(stepsize)+ ' and a max CPS of '+str(max_heat)) 

    plt.show()
    
    """
    source_lats = [34.106236,34.105988,34.106296,34.105766,34.105769,34.106216,34.106204,34.105911]
    source_lon = [-117.709745,-117.710247,-117.707593,-117.70614,-117.7054,-117.708964,-117.711191,-117.708341,]
    source_easting = [434537.67,434491.17,434736.20,434869.82,434938.07,434609.69,434404.27,434666.92]
    source_northing =[3774162.67,3774135.50,3774167.95,3774108.25,3774108.12,3774159.96,3774160.05,3774125.74]
    # for i in range(len(source_lats)):
    #     source_lats[i] = utm.from_latlon(source_lats[i],source_lon[i])[0]
    #     source_lon[i] =utm.from_latlon(source_lats[i],source_lon[i])[1]
    scatter_plot_truelocation = ax.scatter(source_easting, source_northing, s=10, c='#FF0000')
    plt.xlabel('UTM Easting')
    plt.ylabel('UTM Northing')
    plt.title('User Path with Counts per Second')
    plt.show()
    """  

def qrscans():
    """
        This function plots all of the given QR locations with a weight based off of how many times they were scanned
    """
    qr_easting = [434656.83164434927, 434703.0671030256, 434729.39589894173, 434784.9161682739, 434875.77309062914, 434894.89286255947, 434875.0718818688, 434795.2259855086, 434694.2390751023, 434631.20345609944, 434519.7429454065, 434358.89848690946, 434363.23734350037, 434350.63314087567, 434415.02337086666, 434550.08229921735, 434593.8586904025]
    qr_northing = [3774157.2110547, 3774179.212427485, 3774166.416478734, 3774155.9246310145, 3774162.7036760813, 3774134.4447325226, 3774095.937084754, 3774080.892735427, 3774129.8617153177, 3774130.0436967206, 3774130.3961061738, 3774137.5806525033, 3774097.0373155484, 3774161.3240135917, 3774158.3252447797, 3774159.84247252, 3774157.093456563]
    number_of_scans = [46,10,42,45,15,22,26,43,33,25,39,25,21,32,47,68,56]
    colors = ['#1f77b4','#ff7f0e','#2ca02c','#d62728','#9467bd','#8c564b','#e377c2','#7f7f7f','#bcbd22','#17becf','#1f77b4','#ff7f0e','#2ca02c','#d62728','#9467bd','#8c564b','#e377c2']
    img = plt.imread("map.jpg")
    fig, ax = plt.subplots()
    ax.imshow(img, extent = [434265.61381463625,434973.49016928545,3773376.8694098727,3774197.5671255533])
    for i in range(len(number_of_scans)):
        ax.scatter(qr_easting[i], qr_northing[i], s=(number_of_scans[i]*10), c=colors[i])
        ax.annotate(number_of_scans[i], (qr_easting[i], qr_northing[i]))
    plt.xlabel('UTM Easting')
    plt.ylabel('UTM Northing')
    plt.title('QR Scans on HMC Campus') 
    plt.show()   


def qrhisto(csvfile, bins,bigfont):
    """
       This function reads in a csv file, a bin range, and a boolean for larger or smaller font and outputs a histogram showing all the qr codes scanned over time
    """
    time = []
    stop_number = []
    stop0=[]
    stop1=[]
    stop2=[]
    stop3=[]
    stop4=[]
    stop5=[]
    stop6=[]
    stop7=[]
    stop8=[]
    stop9=[]
    stop10=[]
    stop11=[]
    stop12=[]
    stop13=[]
    stop14=[]
    stop15=[]
    stop16=[]
    with open(csvfile) as csvfile:
        readCSV = csv.reader(csvfile, delimiter=',')
        for row in readCSV:
            time.append(row[0])
            stop_number.append(row[1])
    del time[0]
    del stop_number[0]
    for i in range(len(time)):
        time[i] = float(time[i])
        stop_number[i] = float(stop_number[i])
    fig, ax = plt.subplots()
    ax.hist(time, bins=bins)
    plt.xlabel('Day of Event')
    plt.ylabel('Number of Scans')
    plt.title('QR Scans from 11/27-12/2') 
    plt.show()
    for i in range(len(time)):
        if stop_number[i] == 0:
            stop0.append(time[i])
        if stop_number[i] == 1:
            stop1.append(time[i])
        if stop_number[i] == 2:
            stop2.append(time[i])
        if stop_number[i] == 3:
            stop3.append(time[i])
        if stop_number[i] == 4:
            stop4.append(time[i])
        if stop_number[i] == 5:
            stop5.append(time[i])
        if stop_number[i] == 6:
            stop6.append(time[i])
        if stop_number[i] == 7:
            stop7.append(time[i])
        if stop_number[i] == 8:
            stop8.append(time[i])
        if stop_number[i] == 9:
            stop9.append(time[i])
        if stop_number[i] == 10:
            stop10.append(time[i])
        if stop_number[i] == 11:
            stop11.append(time[i])
        if stop_number[i] == 12:
            stop12.append(time[i])
        if stop_number[i] == 13:
            stop13.append(time[i])
        if stop_number[i] == 14:
            stop14.append(time[i])
        if stop_number[i] == 15:
            stop15.append(time[i])
        if stop_number[i] == 16:
            stop16.append(time[i])         
    


    stuffs = [stop0,stop1,stop2,stop3,stop4,stop5,stop6,stop7,stop8,stop9,stop10,stop11,stop12,stop13,stop14,stop15,stop16]
    if bigfont == True:
        font = {'family' : 'normal',
            'weight' : 'normal',
            'size'   : 20}
        plt.rc('font', **font)
    bin_range = np.arange(bins[0],bins[1],bins[2])
    plt.rc('xtick', labelsize=20) 
    plt.rc('ytick', labelsize=20) 
    fig, ax2 = plt.subplots()
    ax2.hist(stuffs, bins=bins, stacked=True,label=range(len(stuffs)))
    plt.xlabel('Day of Event')
    plt.ylabel('Number of Scans')
    plt.title('QR Scans from 11/27-12/2')
    plt.legend() 
    plt.show() 
    # for i in range(len(stuffs)):
    #     fig, ax2 = plt.subplots()
    #     ax2.hist(stuffs[i], bins=bins)
    #     plt.xlabel('Day of Event')
    #     plt.ylabel('Number of Scans')
    #     plt.title('QR '+str(i)+ ' Scans from 11/27-12/2') 
    #     plt.show()

    # stuffs = [stop0,stop1,stop2,stop3,stop4,stop5,stop6,stop7,stop8,stop9,stop10,stop11,stop12,stop13,stop14,stop15,stop16]   
    # fig, ax2 = plt.subplots()   
    # # ax2.hist(stuffs[0], bins=bins, stacked=True,label=str(0))
    # # ax2.hist(stuffs[1], bins=bins, stacked=True,label=str(1))
    # # ax2.hist(stuffs[2], bins=bins, stacked=True,label=str(2))
    # # ax2.hist(stuffs[3], bins=bins, stacked=True,label=str(3))
    # # ax2.hist(stuffs[4], bins=bins, stacked=True,label=str(4))
    # # ax2.hist(stuffs[5], bins=bins, stacked=True,label=str(5))
    # # ax2.hist(stuffs[6], bins=bins, stacked=True,label=str(6))
    # # ax2.hist(stuffs[7], bins=bins, stacked=True,label=str(7))
    # # ax2.hist(stuffs[8], bins=bins, stacked=True,label=str(8))
    # # ax2.hist(stuffs[9], bins=bins, stacked=True,label=str(9))
    # # ax2.hist(stuffs[10], bins=bins, stacked=True,label=str(10))
    # # ax2.hist(stuffs[11], bins=bins, stacked=True,label=str(11))
    # # ax2.hist(stuffs[12], bins=bins, stacked=True,label=str(12))
    # # ax2.hist(stuffs[13], bins=bins, stacked=True,label=str(13))
    # # ax2.hist(stuffs[14], bins=bins, stacked=True,label=str(14))
    # # ax2.hist(stuffs[15], bins=bins, stacked=True,label=str(15))
    # # ax2.hist(stuffs[16], bins=bins, stacked=True,label=str(16))
    # for i in range(len(stuffs)):
    #     ax2.hist(stuffs[i], bins=bins, stacked=True,label=str(i))


def locationhist(thrift_folder,stepsize, inculde_QR,bins,bigfont):
    """
        This function takes in
            a thrift folder
            a step size number in meters
            a boolean as to whether QR should be included
            a bin range
            and a boolean as to whether the font should be larger

            This function does the same thing that readallThrift does, but additionally generates a histogram representing the distribution of users across different densities of area
            (i know doing the same thing twice is bad practice, sorry I did not have time to clean this up)

    """

    del reported_sensors[:]
    del sensordata_size[:]
    del sensordata_percentage[:]
    del a_cps[:]
    del a_extrastuff[:]
    del a_everything[:]
    del a_date[:]
    del a_time[:]
    del a_lat[:]
    del a_lon[:]
    del a_utmeasting[:]
    del a_utmnorthing[:]
    del a_deltas[:]
    del a_speeds_metersperminute[:]
    del a_speeds_milesperhour[:]
    del a_active_deltas[:]
    del a_allthestuffs[:]
    del foldername[:]
    reported_sensors.append('Sensors Reporting')
    sensordata_percentage.append('Percent of Total Data')
    foldername.append(thrift_folder)
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), thrift_folder)
    for thriftfile in glob.glob(os.path.join(path, '*.thrift')):    
        b = []
        a_lat_internal = []
        a_lon_internal = []
        with ThriftStreamReader(Paths.get(thriftfile)) as istream:
            print(istream.getDetectorInfo().getSerialNumber())
            reported_sensors.append(istream.getDetectorInfo().getSerialNumber())
            for n,message in enumerate(istream.iterator()):
                report = MessageUtilities.loadReport(message)
                now = MessageUtilities.getTimestamp(report)
                instances.append(now)
                location = MessageUtilities.getLocation(report)
                gc = MessageUtilities.getTotalCounts(report)
                spectrum = MessageUtilities.getSpectrum(report)
                a = '{:s} {:9.6f},{:9.6f} {:d}'.format(now.toString(),location.getLatitude(),location.getLongitude(),gc)
                a_everything.append(a[0:46])
                a_extrastuff.append(a[0:])
        
                b.append(a[0:])
        sensordata_size.append(len(b))
        y = range(len(b))
        for i in y:
            if len(b[i]) == 49:
                a_date.append(b[i][0:10])
                a_time.append(b[i][11:23])
                a_lat.append(float(b[i][25:34]))
                a_lon.append(float(b[i][35:46]))
                a_lat_internal.append(float(b[i][25:34]))
                a_lon_internal.append(float(b[i][35:46]))                
                a_cps.append(float(b[i][47:49]))
                x = [b[i][0:10],b[i][11:23],float(b[i][25:34]),float(b[i][35:46]),float(b[i][47:49])]
                a_allthestuffs.append([b[i][0:10],b[i][11:23],float(b[i][25:34]),float(b[i][35:46]),float(b[i][47:49])])
        c = a_lat_internal
        d = a_lon_internal
        x = range(len(c))
        for i in x:
            a_utmeasting.append(utm.from_latlon(c[i],d[i])[0])
            a_utmnorthing.append(utm.from_latlon(c[i],d[i])[1])
    for i in range(len(sensordata_size)):
        sensordata_percentage.append(sensordata_size[i]/(len(a_utmeasting))*100)
    utmdataframe = pd.DataFrame({'Easting' : a_utmeasting, 'Northing' : a_utmnorthing})
    max_value_easting = max(utmdataframe['Easting'])
    min_value_easting = min(utmdataframe['Easting'])
    easting_delta = max_value_easting-min_value_easting
    max_value_easting = max_value_easting + easting_delta*.025
    min_value_easting = min_value_easting - easting_delta*.025
    max_value_northing = max(utmdataframe['Northing'])
    min_value_northing = min(utmdataframe['Northing'])
    northing_delta = max_value_northing-min_value_northing
    max_value_northing = max_value_northing + northing_delta*.025
    min_value_northing = min_value_northing - northing_delta*.025  
    heat = []
    max_heat = 0
    max_heat_2 = 0
    easting_range = range(int(min_value_easting), int(max_value_easting))
    northing_range = range(int(min_value_northing), int(max_value_northing))
    for j in range(int(min_value_northing), int(max_value_northing), stepsize):
        row = []
        for i in range(int(min_value_easting), int(max_value_easting),stepsize):  
            row.append(0)  
        heat.append(row)

    for i in range(len(a_utmeasting)):
        heat[int((int(a_utmnorthing[i])-int(min_value_northing))/stepsize)][int((int(a_utmeasting[i])-int(min_value_easting))/stepsize)] += 1
    for i in range(len(heat)):
        for j in range(len(heat[i])):
            if heat[i][j] > max_heat:
                max_heat = heat[i][j]
    
    flattened_heat = []
    for i in range(len(heat)):
        for j in range(len(heat[i])):
            if heat[i][j] != 0:
                flattened_heat.append(heat[i][j])
    if bigfont == True:
        font = {'family' : 'normal',
            'weight' : 'bold',
            'size'   : 22}
        plt.rc('font', **font)
    bin_range = np.arange(bins[0],bins[1],bins[2])
    plt.rc('xtick', labelsize=20) 
    plt.rc('ytick', labelsize=20) 
    fig, ax2 = plt.subplots()

    ax2.hist(np.clip(flattened_heat, bin_range[0], bin_range[-1]), bins=bin_range)
    ax2.set_yscale('log')
    plt.xlabel('Total Time Spent in Location Tile')
    plt.ylabel('Number of '+str(stepsize)+' Meter Square Tiles')
    plt.title('Histogram of Location Distribution')
    plt.legend() 
    plt.show()

    
    
    #below here will handle filtering the data identified above
    saturation_level = 10
    for i in range(len(heat)):
        for j in range(len(heat[i])):
            if heat[i][j] > saturation_level:
                heat[i][j] = saturation_level

    for i in range(len(heat)):
        for j in range(len(heat[i])):
            if heat[i][j] > max_heat_2:
                max_heat_2 = heat[i][j]    
    
    heat = heat[::-1]
    img = plt.imread("map.jpg")
    fig, ax = plt.subplots()
    ax.imshow(img, extent = [434265.61381463625,434973.49016928545,3773376.8694098727,3774197.5671255533])
    plt.imshow(heat, cmap='ocean_r', interpolation='none', extent = [min_value_easting,max_value_easting,min_value_northing,max_value_northing], alpha = 0.6)
    if inculde_QR == True:
        print(thrift_folder)     
        qr_easting = [434656.83164434927, 434703.0671030256, 434729.39589894173, 434784.9161682739, 434875.77309062914, 434894.89286255947, 434875.0718818688, 434795.2259855086, 434694.2390751023, 434631.20345609944, 434519.7429454065, 434358.89848690946, 434363.23734350037, 434350.63314087567, 434415.02337086666, 434550.08229921735, 434593.8586904025]
        qr_northing = [3774157.2110547, 3774179.212427485, 3774166.416478734, 3774155.9246310145, 3774162.7036760813, 3774134.4447325226, 3774095.937084754, 3774080.892735427, 3774129.8617153177, 3774130.0436967206, 3774130.3961061738, 3774137.5806525033, 3774097.0373155484, 3774161.3240135917, 3774158.3252447797, 3774159.84247252, 3774157.093456563]
        number_of_scans = [46,10,42,45,15,22,26,43,33,25,39,25,21,32,47,68,56]
        scatter_plot =ax.scatter(qr_easting, qr_northing, s=number_of_scans, c='#E1FF00')
        for i, number_of_scans in enumerate(number_of_scans):
            ax.annotate(number_of_scans, (qr_easting[i], qr_northing[i]))
    ax.set_xlim(xmin=434265.61381463625 , xmax=434973.49016928545)
    ax.set_ylim(ymin=3773376.8694098727 , ymax=3774197.5671255533)
    colorbar = plt.colorbar()
    colorbar.set_label('Sensor Reports per Tile')    
    plt.xlabel('UTM Easting')
    plt.ylabel('UTM Northing')
    plt.title('Coverage heat map for '+str(thrift_folder)+' with area unit size of '+str(stepsize)+ ' and a saturation level of '+str(saturation_level))
    plt.show()


def gpscorrection(thrift_folder, csvfile,trange):
    """
        This function takes in a thrift folder, a csv file, and a trange (unimplemented)
        it plots all user location over area with each QR scan location as reported by Tau compared to the true location of the QR code itself for ground truth
        (this is a work in progress and worth investing more time if we had had it)
    """
    del scan_tau_utmeasting[:]
    del scan_tau_utmnorthing[:]
    del thrift_utmeasting[:]
    del thrift_utmnorthing[:]
    del reported_sensors[:]
    del sensordata_size[:]
    del sensordata_percentage[:]
    del a_cps[:]
    del a_extrastuff[:]
    del a_everything[:]
    del a_date[:]
    del a_time[:]
    del a_lat[:]
    del a_lon[:]
    del a_utmeasting[:]
    del a_utmnorthing[:]
    del a_deltas[:]
    del a_speeds_metersperminute[:]
    del a_speeds_milesperhour[:]
    del a_active_deltas[:]
    del a_allthestuffs[:]
    del foldername[:]
    del scan_time[:]
    del thrift_time[:]
    del scan_stop_number[:]
    del scan_lat[:]
    del scan_lon[:]
    del scan_thrift_lat[:]
    del scan_thrift_lon[:]
    reported_sensors.append('Sensors Reporting')
    sensordata_percentage.append('Percent of Total Data')
    foldername.append(thrift_folder)
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), thrift_folder)
    for thriftfile in glob.glob(os.path.join(path, '*.thrift')):    
        b = []
        a_lat_internal = []
        a_lon_internal = []
        with ThriftStreamReader(Paths.get(thriftfile)) as istream:
            print(istream.getDetectorInfo().getSerialNumber())
            reported_sensors.append(istream.getDetectorInfo().getSerialNumber())
            for n,message in enumerate(istream.iterator()):
                report = MessageUtilities.loadReport(message)
                now = MessageUtilities.getTimestamp(report)
                instances.append(now)
                location = MessageUtilities.getLocation(report)
                gc = MessageUtilities.getTotalCounts(report)
                spectrum = MessageUtilities.getSpectrum(report)
                a = '{:s} {:9.6f},{:9.6f} {:d}'.format(now.toString(),location.getLatitude(),location.getLongitude(),gc)
                a_everything.append(a[0:46])
                a_extrastuff.append(a[0:])
        
                b.append(a[0:])
        sensordata_size.append(len(b))
        y = range(len(b))
        for i in y:
            if len(b[i]) == 49:
                a_date.append(b[i][0:10])
                a_time.append(b[i][11:23])
                a_lat.append(float(b[i][25:34]))
                a_lon.append(float(b[i][35:46]))
                a_lat_internal.append(float(b[i][25:34]))
                a_lon_internal.append(float(b[i][35:46]))                
                a_cps.append(float(b[i][47:49]))
                x = [b[i][0:10],b[i][11:23],float(b[i][25:34]),float(b[i][35:46]),float(b[i][47:49])]
                a_allthestuffs.append([b[i][0:10],b[i][11:23],float(b[i][25:34]),float(b[i][35:46]),float(b[i][47:49])])
        c = a_lat_internal
        d = a_lon_internal
        x = range(len(c))
        for i in x:
            thrift_utmeasting.append(utm.from_latlon(c[i],d[i])[0])
            thrift_utmnorthing.append(utm.from_latlon(c[i],d[i])[1])
    
    with open(csvfile) as csvfile:
        readCSV = csv.reader(csvfile, delimiter=',')
        for row in readCSV:
            scan_time.append(row[1])
            scan_stop_number.append(row[2])
            scan_lat.append(row[5])
            scan_lon.append(row[6])
    del scan_time[0]
    del scan_stop_number[0]
    del scan_lat[0]
    del scan_lon[0]
    # This is the 'i cant figure out what is weird with the time stamps' section
    # for i in range(len(scan_time)):
    #     scan_time[i] = tm.mktime(datetime.datetime.strptime(scan_time[i], "%b %d, %Y %I:%M:%S %p").timetuple())
    # for i in range(len(a_date)):
    #     thrift_time.append(tm.mktime(datetime.datetime.strptime(a_date[i]+a_time[i][0:8], "%Y-%m-%d%H:%M:%S").timetuple()))
    # for i in range(len(scan_time)):
    #     average_lat = []
    #     average_lon = []
    #     for j in range(len(thrift_time)):
    #         if scan_time[i]-trange <= thrift_time[j] <= scan_time[i] + trange:
    #             average_lat.append(a_lat[j])
    #             average_lon.append(a_lon[j])
    #     if len(average_lat)!=0 and len(average_lon)!=0: 
    #         scan_thrift_lat.append(sum(average_lat)/len(average_lat))
    #         scan_thrift_lon.append(sum(average_lon)/len(average_lon))
    #     else:
    #         scan_thrift_lat.append('N/A')
    #         scan_thrift_lon.append('N/A')
    for i in range(len(scan_lat)):
        scan_tau_utmeasting.append(utm.from_latlon(float(scan_lat[i]),float(scan_lon[i]))[0])
        scan_tau_utmnorthing.append(utm.from_latlon(float(scan_lat[i]),float(scan_lon[i]))[1])
    img = plt.imread("map.jpg")
    fig, ax = plt.subplots()
    # 34.105675, -117.711687
    # 34.106603, -117.710056
    ax.imshow(img, extent = [434265.61381463625,434973.49016928545,3773376.8694098727,3774197.5671255533])
    ax.scatter(thrift_utmeasting, thrift_utmnorthing, s=4, c='#824CD4')
    ax.scatter(scan_tau_utmeasting, scan_tau_utmnorthing, s=10, c='#71FF00')
    ax.scatter(scan_stop_eastingFinal,scan_stop_northingFinal,s=20,c='#FF0036')
    plt.xlabel('UTM Easting')
    plt.ylabel('UTM Northing')
    plt.title('GPS path')
    plt.show()                                                                                                                                                                      

