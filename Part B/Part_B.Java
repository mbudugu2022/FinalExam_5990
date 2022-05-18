import os.path
import os
import csv
import shutil
import pandas as pd
import random
import numpy as np
import matplotlib.pyplot as plt
from pandas import DataFrame
import re
import shutil
import subprocess
########### Inputs
MileStone_Day = 27
Folder = 'MileS'
Social_Distancing = .2811
Proportion_of_people_using_Mask = 0.0
Proportion_of_people_using_Mask_who_use_Mask_at_Home = 0.0

Daily_Count_the_infected_cases_reach_to_use_Mask = {10:10,100:100,50:500,1000:1000}
Daily_Count_the_infected_cases_reach_to_use_SD = Daily_Count_the_infected_cases_reach_to_use_Mask
Reduction_in_transmission_DueTo_Mask_OoH = {1:0.67, 0:0}
Reduction_in_transmission_DueTo_Mask_IH = {1:0.79, 0:0}
# if os.path.exists('{0}\M_MileStone_Day.csv'.format(Folder)):
#     df_ = pd.read_csv('{0}\M_MileStone_Day.csv'.format(Folder),delimiter=',')
#     MileStone_Day = df_.MileStone_Day[0]
#     Following = True
#     Mile_Stone_day_reached = True
# else:
#     MileStone_Day = 10000
#     Following = False # Activate the other line
#     Mile_Stone_day_reached = False
    
Infection_rate_Act = {'Work':0.044,'Market':0.044,'School':0.044,'Other':0.044, 'Home':0}
Inf_rate_Home = 0.052
Adjustmentـfactor_for_Infection_rate = {'M':0.8,'O':0.8,'P':0.8,'S':1,'G':1} 
Number_of_contacts_Act = {'Work':2,'Market':2,'School':2,'Home':2,'Other':2} 
Obligatory_hidden_period = 5 # The number of days (after the start of infection) that the propability of Quarantine is 0.0
Quarantine_prob = 0.12 
Infected_to_Dead_prob = 0.0003
Quarantined_to_Dead_prob = 0.003
Infection_rate_in_Transit = 0.08
N0_of_passengers_in_Contact_in_Transit = 10
ArrivalDeviation = 5
LagDay = 4
ClosingSchools_startday = 34 - LagDay
MarketRestriction_startday = 40  - LagDay
AllReduction_startday = 34 - LagDay
DistantWorking_startday = 40 - LagDay
Number_of_contacts_forMarket_afterRestriction = Social_Distancing
Number_of_contacts_forWork_afterRestriction = Social_Distancing
Number_of_contacts_forOther_afterRestriction = Social_Distancing
Number_of_contacts_forMarket_MidRestriction = 2 * Social_Distancing
Number_of_contacts_forWork_MidRestriction = 2 * Social_Distancing
Number_of_contacts_forOther_MidRestriction = 2 * Social_Distancing
N0_of_passengers_in_Contact_in_Transit_afterRestriction = 10*.3/2
PropofTime = 1/5
Duration_of_sickness_Infection_to_Recovery = 14 # Sickness period. After these number of days, the infected persons are automatically recovered.
Number_of_days_for_simulation = 500
AllowedInfection_InterActivities = {
                                    'Work':['Work','Market','School'],
                                    'Market':['Market','Work','Other'],
                                    'School':['School','Work'],
                                    'Home':['Home'],
                                    'Other':['Other'],
                                    }
Coef = 1
Coef_ = .95
GenerationRates = {'IncreasingRate':0.95*Coef, 'PrimaryWork':0.1*Coef,'SecondaryWork':0.1*Coef,'WorkBasedBusiness':0.1*Coef,'WorkAtHomeBusiness':4*Coef,'ReturnFromWork':0.1*Coef,'MarketJointMarket':0.5*Coef,'School':0*Coef}
GenerationRates_Base = {'IncreasingRate':3.55*Coef, 'PrimaryWork':3.55*Coef_,'SecondaryWork':3.55*Coef_,'WorkBasedBusiness':3.55*Coef_,'WorkAtHomeBusiness':3.55*Coef_,'ReturnFromWork':3.55*Coef_,'MarketJointMarket':3.55*Coef_,'School':3.55*Coef_}
############################
Count_Infections = {'Work':0,'School':0,'Market':0,'Other':0,'Home':0,'Transit':0}
Daily_Infected = {}
Cumulative_Infected = {1:0}
Infected_list = pd.read_csv('Initial_Infected_Pop.csv',delimiter=',')
Infected_list['HH_P'] = Infected_list.household_id.map(str) + "_" + Infected_list.person_id.map(str)
persons = pd.read_csv('C:/Users/z5057821/Desktop/Runs/persons.csv',delimiter=',')
persons = persons.astype({"household_id": str, "person_id": str})
persons['HH_P'] = persons.household_id + "_" + persons.person_id
np.random.seed(150)
persons['Mask_used?'] = np.random.randint(1, 100, persons.shape[0])
persons['Mask_used?'] = np.where(persons['Mask_used?'] <= Proportion_of_people_using_Mask*100,1,0)
persons['Coeff_Of_Infectious_prob_because_of_Mask_Main'] = np.where(persons['Mask_used?'] == 1,1-Reduction_in_transmission_DueTo_Mask_OoH[1],1)
np.random.seed(100)
persons['Mask_used_at_Home?'] = np.random.randint(1, 100, persons.shape[0])
persons['Mask_used_at_Home?'] = np.where(persons['Mask_used_at_Home?'] <= Proportion_of_people_using_Mask_who_use_Mask_at_Home*100,1,0)
persons['Coeff_Of_Infectious_prob_because_of_Mask_at_Home_Main'] = np.where(persons['Mask_used_at_Home?'] == 1,1-Reduction_in_transmission_DueTo_Mask_IH[1],1)
persons_copy = persons.copy()
df = persons[['HH_P','Coeff_Of_Infectious_prob_because_of_Mask_at_Home_Main','Coeff_Of_Infectious_prob_because_of_Mask_Main']]
if not os.path.isfile('Quarantine.csv'):
    with open('Quarantine.csv','a', newline='') as csvfile:
        readCSV = csv.writer(csvfile,delimiter = ',')
        data = ['household_id','person_id','age','sex','license','transit_pass','employment_status','occupation','free_parking','student_status','work_zone','school_zone','weight','Infection_date','HH_P','Sent_to_quarantine_date']
        readCSV.writerows([data])
        
    with open('Dead.csv','a', newline='') as csvfile:
        readCSV = csv.writer(csvfile,delimiter = ',')
        readCSV.writerows([data])        
    with open('Recovered.csv','a', newline='') as csvfile:
        readCSV = csv.writer(csvfile,delimiter = ',')
        readCSV.writerows([data])   
    with open('Quarantined_Family.csv','a', newline='') as csvfile:
        readCSV = csv.writer(csvfile,delimiter = ',')
        readCSV.writerows([data])   
        
Isolated_list = pd.read_csv('Quarantine.csv',delimiter=',')
Dead_list = pd.read_csv('Dead.csv',delimiter=',')
Recovered_list = pd.read_csv('Recovered.csv',delimiter=',')
Quarantined_Family = pd.read_csv('Quarantined_Family.csv',delimiter=',')
Y = {}
for i in ['Susceptibled','Infected','Quarantined','Dead','Recovered','Quarantined_Family','Total_infected']:
    Y[i] = []
def Run_TASHA(Day):
    os.remove("E:/madhu/Sydney TASHA Model/V1InputV1.0/TASHAforSydneyModel_PandemicBatch4.xml")
    
    shutil.copy('E:/madhu/Sydney TASHA Model/V1InputV1.0/TASHAforSydneyModel_PandemicBatch4 - Backup.xml', 'E:/madhu/Sydney TASHA Model/V1InputV1.0/TASHAforSydneyModel_PandemicBatch4.xml')
#  
    for i in GenerationRates:
        f = open("E:/madhu/Sydney TASHA Model/V1InputV1.0/TASHAforSydneyModel_PandemicBatch4.xml", "r")
        contents = f.readlines()
        f.close()
        f = open("E:/madhu/Sydney TASHA Model/V1InputV1.0/TASHAforSydneyModel_PandemicBatch4.xml", "w")
        
        if i == 'School':
            if Day >= ClosingSchools_startday:                                                                                                         
                contents.insert(4, ' ' * 12 + '<changeParameter ParameterPath=' + '"Scheduler.Generation Rate Adjustments.Generation Adjustment-{0}.Factor"'.format(i) + ' Value=' + '"{0}"'.format(GenerationRates[i]) + ' />' + "\n")
            else:
                contents.insert(4, ' ' * 12 + '<changeParameter ParameterPath=' + '"Scheduler.Generation Rate Adjustments.Generation Adjustment-{0}.Factor"'.format(i) + ' Value=' + '"{0}"'.format(GenerationRates_Base[i]) + ' />' + "\n")
        elif i == 'MarketJointMarket':
            if Day >= MarketRestriction_startday:                                                                                                         
                contents.insert(4, ' ' * 12 + '<changeParameter ParameterPath=' + '"Scheduler.Generation Rate Adjustments.Generation Adjustment-{0}.Factor"'.format(i) + ' Value=' + '"{0}"'.format(GenerationRates[i]) + ' />' + "\n")
            else:
                contents.insert(4, ' ' * 12 + '<changeParameter ParameterPath=' + '"Scheduler.Generation Rate Adjustments.Generation Adjustment-{0}.Factor"'.format(i) + ' Value=' + '"{0}"'.format(GenerationRates_Base[i]) + ' />' + "\n")
        else:
            if Day >= DistantWorking_startday:
                contents.insert(4, ' ' * 12 + '<changeParameter ParameterPath=' + '"Scheduler.Generation Rate Adjustments.Generation Adjustment-{0}.Factor"'.format(i) + ' Value=' + '"{0}"'.format(GenerationRates[i]) + ' />' + "\n")
            else:
                contents.insert(4, ' ' * 12 + '<changeParameter ParameterPath=' + '"Scheduler.Generation Rate Adjustments.Generation Adjustment-{0}.Factor"'.format(i) + ' Value=' + '"{0}"'.format(GenerationRates_Base[i]) + ' />' + "\n")
        
        if i == 'IncreasingRate':
            if Day >= AllReduction_startday:                                                                                                         
                contents.insert(4, ' ' * 12 + '<changeParameter ParameterPath=' + '"Scheduler.Generation Rate Adjustments.Generation Adjustment-{0}.Factor"'.format(i) + ' Value=' + '"{0}"'.format(GenerationRates[i]) + ' />' + "\n")
#                contents.insert(4, ' ' * 12 + '<changeParameter ParameterPath=' + '"Scheduler.Generation Rate Adjustments.Generation Adjustment-{0}.Factor"'.format(i) + ' Value=' + '"{0}"'.format(GenerationRates[i]) + ' />' + "\n")
            else:
                contents.insert(4, ' ' * 12 + '<changeParameter ParameterPath=' + '"Scheduler.Generation Rate Adjustments.Generation Adjustment-{0}.Factor"'.format(i) + ' Value=' + '"{0}"'.format(GenerationRates_Base[i]) + ' />' + "\n")
            
        contents = "".join(contents)
        f.write(contents)
        f.close()
    subprocess.call(["E:/madhu/XTMF/XTMF.Run.exe", "Sydney TASHA for Pandemic", "Sydney TASHA for Pandemic-1 iteration", "Day{0}".format(Day)])
if os.path.exists('Quarantine.csv'):
    os.remove('Quarantine.csv')
    os.remove('Quarantined_Family.csv')
    os.remove('Recovered.csv')
    os.remove('Dead.csv')
if os.path.exists('Infected.csv'):
    os.remove('Infected.csv')
if not os.path.isfile('Quarantine.csv'):
    with open('Quarantine.csv','a', newline='') as csvfile:
        readCSV = csv.writer(csvfile,delimiter = ',')
        data = ['household_id','person_id','age','sex','license','transit_pass','employment_status','occupation','free_parking','student_status','work_zone','school_zone','weight','Infection_date','HH_P','Sent_to_quarantine_date']
        readCSV.writerows([data])
        
    with open('Dead.csv','a', newline='') as csvfile:
        readCSV = csv.writer(csvfile,delimiter = ',')
        readCSV.writerows([data])        
    with open('Recovered.csv','a', newline='') as csvfile:
        readCSV = csv.writer(csvfile,delimiter = ',')
        readCSV.writerows([data])   
    with open('Quarantined_Family.csv','a', newline='') as csvfile:
        readCSV = csv.writer(csvfile,delimiter = ',')
        readCSV.writerows([data])   
        
Quarantined_list = pd.read_csv('Quarantine.csv',delimiter=',')
Dead_list = pd.read_csv('Dead.csv',delimiter=',')
Recovered_list = pd.read_csv('Recovered.csv',delimiter=',')
Quarantined_Family = pd.read_csv('Quarantined_Family.csv',delimiter=',')

Number_of_days_for_simulation = 3000
## Running the model over days
# for day in range(MileStone_Day+1,Number_of_days_for_simulation+1):
for day in range(1,Number_of_days_for_simulation+1):
    
    if day == MileStone_Day+1 and Following == True:
        Infected_list = pd.read_csv('{0}\M_Infected_list.csv'.format(Folder),delimiter=',')
        Infected_list.drop(['Coeff_Of_Infectious_prob_because_of_Mask_Main','Coeff_Of_Infectious_prob_because_of_Mask'], axis=1,inplace = True)
        Isolated_list = pd.read_csv('{0}\M_Quarantine.csv'.format(Folder),delimiter=',')
        Isolated_list.drop(['Coeff_Of_Infectious_prob_because_of_Mask_Main','Coeff_Of_Infectious_prob_because_of_Mask'], axis=1,inplace = True)
        Quarantined_Family = pd.read_csv('{0}\M_Quarantined_Family.csv'.format(Folder),delimiter=',')
        Quarantined_Family.drop(['Coeff_Of_Infectious_prob_because_of_Mask_Main','Coeff_Of_Infectious_prob_because_of_Mask'], axis=1,inplace = True)
        Recovered_list = pd.read_csv('{0}\M_Recovered.csv'.format(Folder),delimiter=',')
        Recovered_list.drop(['Coeff_Of_Infectious_prob_because_of_Mask_Main','Coeff_Of_Infectious_prob_because_of_Mask'], axis=1,inplace = True)
        Dead_list = pd.read_csv('{0}\M_Dead.csv'.format(Folder),delimiter=',')
        Dead_list.drop(['Coeff_Of_Infectious_prob_because_of_Mask_Main','Coeff_Of_Infectious_prob_because_of_Mask'], axis=1,inplace = True)
        persons = pd.read_csv('{0}\M_persons.csv'.format(Folder),delimiter=',')
        persons.drop(['Coeff_Of_Infectious_prob_because_of_Mask_Main','Coeff_Of_Infectious_prob_because_of_Mask'], axis=1,inplace = True)
        persons = pd.merge(persons, df, how='inner', on='HH_P')
        Infected_list = pd.merge(Infected_list, df, how='inner', on='HH_P')
        Isolated_list = pd.merge(Isolated_list, df, how='inner', on='HH_P')
        Quarantined_Family = pd.merge(Quarantined_Family, df, how='inner', on='HH_P')
        Recovered_list = pd.merge(Recovered_list, df, how='inner', on='HH_P')
        Dead_list = pd.merge(Dead_list, df, how='inner', on='HH_P')
        #*************** End
        
        Daily_Infected_M = pd.read_csv('{0}\M_Daily_Infected.csv'.format(Folder),delimiter=',')
        Daily_Infected = dict(zip(Daily_Infected_M.Day, Daily_Infected_M.Daily_Infected))
        Cumulative_Infected_M = pd.read_csv('{0}\M_Cumulative_Infected.csv'.format(Folder),delimiter=',')
        Cumulative_Infected = dict(zip(Cumulative_Infected_M.Day, Cumulative_Infected_M.Cumulative_Infected))
        Y_M = pd.read_csv('{0}\M_Y.csv'.format(Folder),delimiter=',')
        Y = dict(zip(Y_M.Group, Y_M.Values))
        for k in Y:
            Y[k] = list(Y[k][1:-1].split(","))
            Y[k] = [int(i) for i in Y[k]]
            
     ######################### Run TASHA
    
    Run_TASHA(day)
    for folder in ['StationAccess','Demand','Validation']:
        shutil.rmtree("E:/madhu/XTMF Projects/Sydney TASHA for Pandemic/Day{0}/2016 - 1 iteration/{1}".format(day,folder))
    print('***************', 'TASHA is run')

    ######################### Run model
       
    Conducted_trips = pd.read_csv('C:/Users/madhu/Documents/XTMF/Projects/Sydney TASHA for Pandemic/Day{0}/2016 - 1 iteration/Microsim Results/trips.csv'.format(day),delimiter=',')
    
    Conducted_trips = pd.read_csv('C:/Users/z5057821/Desktop/Runs/Day{0}/2016 - 1 iteration/Microsim Results/trips.csv'.format(day),delimiter=',')
    Conducted_trips['New_d_act_coding'] = 'Work'
    Conducted_trips['New_d_act_coding'] = np.where(Conducted_trips['d_act'] == 'Market','Market',Conducted_trips['New_d_act_coding'])
    Conducted_trips['New_d_act_coding'] = np.where(Conducted_trips['d_act'] == 'School','School',Conducted_trips['New_d_act_coding'])
    Conducted_trips['New_d_act_coding'] = np.where(Conducted_trips['d_act'] == 'Home','Home',Conducted_trips['New_d_act_coding'])
    Conducted_trips['New_d_act_coding'] = np.where(Conducted_trips['d_act'] == 'JointMarket','Market',Conducted_trips['New_d_act_coding'])
    Conducted_trips['New_d_act_coding'] = np.where(Conducted_trips['d_act'] == 'IndividualOther','Other',Conducted_trips['New_d_act_coding'])
    Conducted_trips['New_d_act_coding'] = np.where(Conducted_trips['d_act'] == 'JointOther','Other',Conducted_trips['New_d_act_coding'])
    Conducted_trips['HH_P'] = Conducted_trips.household_id.map(str) + "_" + Conducted_trips.person_id.map(str)
    Conducted_trips['HH_P_N'] = Conducted_trips.HH_P.map(str) + "_" + Conducted_trips.trip_id.map(str)
    
    if day >= MarketRestriction_startday - 5:
        Number_of_contacts_Act['Market'] = Number_of_contacts_forMarket_MidRestriction
        Number_of_contacts_Act['Other'] = Number_of_contacts_forOther_MidRestriction
        Number_of_contacts_Act['Work'] = Number_of_contacts_forWork_MidRestriction
        
    if day >= ClosingSchools_startday:
        Number_of_contacts_Act['School'] = 0
#        Adjustmentـfactor_for_Infection_rate = Adjustmentـfactor_for_Infection_rate_afterRestriction
        
    if day >= MarketRestriction_startday:
        Number_of_contacts_Act['Market'] = Number_of_contacts_forMarket_afterRestriction
        Number_of_contacts_Act['Other'] = Number_of_contacts_forOther_afterRestriction
        
    if day >= DistantWorking_startday:
        Number_of_contacts_Act['Work'] = Number_of_contacts_forWork_afterRestriction
        N0_of_passengers_in_Contact_in_Transit = N0_of_passengers_in_Contact_in_Transit_afterRestriction
    
    if day >= TransitReduction_startday1 and day < TransitReduction_startday2:
        Infection_rate_in_Transit = Infection_rate_in_Transit_afterRestriction1
    elif day >= TransitReduction_startday2 and day < TransitReduction_startday3:
        Infection_rate_in_Transit = Infection_rate_in_Transit_afterRestriction2   
    elif day >= TransitReduction_startday3:
        Infection_rate_in_Transit = Infection_rate_in_Transit_afterRestriction3
        
#    # Restrictions for MASK paper
    if day-1 >= MileStone_Day:
        persons['Coeff_Of_Infectious_prob_because_of_Mask'] = persons['Coeff_Of_Infectious_prob_because_of_Mask_Main']
        Infected_list['Coeff_Of_Infectious_prob_because_of_Mask'] = Infected_list['Coeff_Of_Infectious_prob_because_of_Mask_Main']
        Isolated_list['Coeff_Of_Infectious_prob_because_of_Mask'] = Isolated_list['Coeff_Of_Infectious_prob_because_of_Mask_Main']
        Quarantined_Family['Coeff_Of_Infectious_prob_because_of_Mask'] = Quarantined_Family['Coeff_Of_Infectious_prob_because_of_Mask_Main']
        Recovered_list['Coeff_Of_Infectious_prob_because_of_Mask'] = Recovered_list['Coeff_Of_Infectious_prob_because_of_Mask_Main']
        Dead_list['Coeff_Of_Infectious_prob_because_of_Mask'] = Dead_list['Coeff_Of_Infectious_prob_because_of_Mask_Main']
        persons['Coeff_Of_Infectious_prob_because_of_Mask_at_Home'] = persons['Coeff_Of_Infectious_prob_because_of_Mask_at_Home_Main']       
        Infected_list['Coeff_Of_Infectious_prob_because_of_Mask_at_Home'] = Infected_list['Coeff_Of_Infectious_prob_because_of_Mask_at_Home_Main']
        Isolated_list['Coeff_Of_Infectious_prob_because_of_Mask_at_Home'] = Isolated_list['Coeff_Of_Infectious_prob_because_of_Mask_at_Home_Main']
        Quarantined_Family['Coeff_Of_Infectious_prob_because_of_Mask_at_Home'] = Quarantined_Family['Coeff_Of_Infectious_prob_because_of_Mask_at_Home_Main']
        Recovered_list['Coeff_Of_Infectious_prob_because_of_Mask_at_Home'] = Recovered_list['Coeff_Of_Infectious_prob_because_of_Mask_at_Home_Main']
        Dead_list['Coeff_Of_Infectious_prob_because_of_Mask_at_Home'] = Dead_list['Coeff_Of_Infectious_prob_because_of_Mask_at_Home_Main']
    else:
        persons['Coeff_Of_Infectious_prob_because_of_Mask'] = 1
        Infected_list['Coeff_Of_Infectious_prob_because_of_Mask'] = 1
        Isolated_list['Coeff_Of_Infectious_prob_because_of_Mask'] = 1
        Quarantined_Family['Coeff_Of_Infectious_prob_because_of_Mask'] = 1
        Recovered_list['Coeff_Of_Infectious_prob_because_of_Mask'] = 1
        Dead_list['Coeff_Of_Infectious_prob_because_of_Mask'] = 1
        persons['Coeff_Of_Infectious_prob_because_of_Mask_at_Home'] = 1
        Infected_list['Coeff_Of_Infectious_prob_because_of_Mask_at_Home'] = 1
        Isolated_list['Coeff_Of_Infectious_prob_because_of_Mask_at_Home'] = 1
        Quarantined_Family['Coeff_Of_Infectious_prob_because_of_Mask_at_Home'] = 1
        Recovered_list['Coeff_Of_Infectious_prob_because_of_Mask_at_Home'] = 1
        Dead_list['Coeff_Of_Infectious_prob_because_of_Mask_at_Home'] = 1
    #######################  
    Conducted_trips_ = pd.read_csv('C:/Users/z5057821/Desktop/Runs//Day{0}/2016 - 1 iteration/Microsim Results/Conducted_trips.csv'.format(day),delimiter=',')
    Trips_by_Infected_Persons = Conducted_trips_[(Conducted_trips_.HH_P.isin(Infected_list.HH_P)) & (Conducted_trips_.New_d_act_coding != 'Home') & (~Conducted_trips_['HH_P'].isin(Isolated_list['HH_P'])) & (~Conducted_trips_['HH_P'].isin(Quarantined_Family['HH_P']))]
    Conducted_trips_ = Conducted_trips_[(Conducted_trips_.d_zone.isin(Trips_by_Infected_Persons.d_zone)) & (Conducted_trips_.New_d_act_coding != 'Home')]
    Conducted_trips_ = pd.merge(Conducted_trips_, persons, how='inner', on='HH_P')
    Conducted_trips_ = Conducted_trips_[['d_zone','HH_P','occupation','New_d_act_coding','HH_P_N','mode','Mask_used?','Coeff_Of_Infectious_prob_because_of_Mask','Coeff_Of_Infectious_prob_because_of_Mask_at_Home']]
    Trips_by_Infected_Persons = Conducted_trips_[Conducted_trips_['HH_P'].isin(Infected_list.HH_P)]
    print('***************', 'Preprocessing is finished')
    
    Daily_Infected[day] = 0
    
    #### Processing of infected cases
    Trips_by_Infected_Persons_ = Trips_by_Infected_Persons.drop_duplicates(subset=['HH_P', 'd_zone'], keep='first')
    counts_InfectedinZone = Trips_by_Infected_Persons_[['d_zone','New_d_act_coding']].groupby(['d_zone','New_d_act_coding']).size().reset_index(name='counts_InfectedinZone')  
    counts_PopinZone = Conducted_trips_[['d_zone','New_d_act_coding']].groupby(['d_zone','New_d_act_coding']).size().reset_index(name='counts_PopinZone')  
    counts_InfectedinZone = pd.merge(counts_InfectedinZone, counts_PopinZone, how='inner', on=['d_zone','New_d_act_coding'])
    Conducted_trips__ = pd.merge(Conducted_trips_, counts_InfectedinZone, how='inner', on=['d_zone','New_d_act_coding'])
    
    Conducted_trips__['Infection_rate_Act'] = Conducted_trips__['New_d_act_coding'].apply(lambda x: Infection_rate_Act[x])
    Conducted_trips__['Number_of_contacts_Act'] = Conducted_trips__['New_d_act_coding'].apply(lambda x: Number_of_contacts_Act[x])
    Conducted_trips__['Adjustmentـfactor_for_Infection_rate'] = Conducted_trips__['occupation'].apply(lambda x: Adjustmentـfactor_for_Infection_rate[x])
    Conducted_trips__['Infection_Probability'] = Conducted_trips__['Infection_rate_Act'] * Conducted_trips__['Number_of_contacts_Act'] * Conducted_trips__['Adjustmentـfactor_for_Infection_rate'] * Conducted_trips__['counts_InfectedinZone'] / Conducted_trips__['counts_PopinZone']
    
    Conducted_trips__['IsInfected?'] = Conducted_trips__['Infection_Probability'].apply(lambda x: np.random.choice([1,0], p=[x, 1 - x]))
    Conducted_trips__ = Conducted_trips__[(Conducted_trips__['IsInfected?'] == 1) & (~Conducted_trips__['HH_P'].isin(Recovered_list['HH_P'])) & (~Conducted_trips__['HH_P'].isin(Infected_list['HH_P']))]
    Selected_New_infected_person_records = persons[persons['HH_P'].isin(Conducted_trips__['HH_P'])]
    Selected_New_infected_person_records['Infection_date'] = day
    Infected_list = pd.concat([Infected_list,Selected_New_infected_person_records])      
    
    Conducted_trips__ = Conducted_trips__[['New_d_act_coding']].groupby(['New_d_act_coding']).size().reset_index(name='counts')  
    for index,row in Conducted_trips__.iterrows():
        Count_Infections[row['New_d_act_coding']] += row['counts']
        Daily_Infected[day] += row['counts']
    print('***************', 'Processing of infected cases is finished')
    
    #### Processing of infections at Home
    persons_ = persons[(persons.household_id.isin(Infected_list['household_id'])) & (~persons.HH_P.isin(Infected_list.HH_P))]
    counts_InfectedinHH = Infected_list[['household_id']].groupby(['household_id']).size().reset_index(name='counts_InfectedinHH')
    
    Selected_New_infected_person_records = pd.merge(persons_, counts_InfectedinHH, how='inner', on='household_id')
    
    Selected_New_infected_person_records['Infection_Probability'] = Selected_New_infected_person_records['counts_InfectedinHH'].apply(lambda x: 1 - (1 - Inf_rate_Home)**x)
    Selected_New_infected_person_records['IsInfected?'] = Selected_New_infected_person_records['Infection_Probability'].apply(lambda x: np.random.choice([1,0], p=[min(1,x), 1 - min(1,x)]))
    Selected_New_infected_person_records = Selected_New_infected_person_records[(Selected_New_infected_person_records['IsInfected?'] == 1) & (~Selected_New_infected_person_records['HH_P'].isin(Recovered_list['HH_P'])) & (~Selected_New_infected_person_records['HH_P'].isin(Infected_list['HH_P']))]
    Selected_New_infected_person_records = persons_[persons_['HH_P'].isin(Selected_New_infected_person_records['HH_P'])]
    Selected_New_infected_person_records['Infection_date'] = day
    Infected_list = pd.concat([Infected_list,Selected_New_infected_person_records])  
    
    Count_Infections['Home'] += len(Selected_New_infected_person_records)
    Daily_Infected[day] += len(Selected_New_infected_person_records)
    
    print('***************', 'Processing of infections at home is finished')
    
    #### Processing of infections on Transit
    
    Infected_Persons_in_Transit = Trips_by_Infected_Persons[Trips_by_Infected_Persons['mode'] == 'Passenger']
    Total_TripsbyTransit = Conducted_trips_[Conducted_trips_['mode'] == 'Passenger']
    counts_InfectedinTransitTowardZone = Infected_Persons_in_Transit[['d_zone']].groupby(['d_zone']).size().reset_index(name='counts_InfectedinTransitTowardZone')  
    counts_PopinTransitTowardZone = Total_TripsbyTransit[['d_zone']].groupby(['d_zone']).size().reset_index(name='counts_PopinTransitTowardZone')  
    counts_InfectedinTransitTowardZone = pd.merge(counts_InfectedinTransitTowardZone, counts_PopinTransitTowardZone, how='inner', on='d_zone')
    Total_TripsbyTransit = pd.merge(Total_TripsbyTransit, counts_InfectedinTransitTowardZone, how='inner', on='d_zone')
    Total_TripsbyTransit['Infection_Probability'] = Infection_rate_in_Transit * PropofTime * Total_TripsbyTransit['counts_InfectedinTransitTowardZone'] / Total_TripsbyTransit['counts_PopinTransitTowardZone']
    
    Total_TripsbyTransit['IsInfected?'] = Total_TripsbyTransit['Infection_Probability'].apply(lambda x: np.random.choice([1,0], p=[min(x,1), 1 - min(x,1)]))
    Selected_New_infected_person_records = Total_TripsbyTransit[(Total_TripsbyTransit['IsInfected?'] == 1) & (~Total_TripsbyTransit['HH_P'].isin(Recovered_list['HH_P'])) & (~Total_TripsbyTransit['HH_P'].isin(Infected_list['HH_P']))]
    Selected_New_infected_person_records = persons[persons['HH_P'].isin(Selected_New_infected_person_records['HH_P'])]
    Selected_New_infected_person_records['Infection_date'] = day
    Infected_list = pd.concat([Infected_list,Selected_New_infected_person_records])   
    
    Count_Infections['Transit'] += len(Selected_New_infected_person_records)
    Daily_Infected[day] += len(Selected_New_infected_person_records)    
    
    print('***************', 'Processing of infections on Transit is finished')
            
    #### Processing of Recovered cases
    New_recovered_records = Infected_list[Infected_list.Infection_date <= day - Duration_of_sickness_Infection_to_Recovery]
    Infected_list = Infected_list[Infected_list.Infection_date > day - Duration_of_sickness_Infection_to_Recovery]
    Recovered_list = pd.concat([Recovered_list,New_recovered_records])
    
    New_recovered_records = Quarantined_list[Quarantined_list.Infection_date <= day - Duration_of_sickness_Infection_to_Recovery]
    Quarantined_list = Quarantined_list[Quarantined_list.Infection_date > day - Duration_of_sickness_Infection_to_Recovery]
    Recovered_list = pd.concat([Recovered_list,New_recovered_records])
   
    print('***************', 'Processing of recovered cases is finished')
    
    #### Processing of quarantined cases
    Selected_New_quarantined_records = Infected_list[Infected_list.Infection_date < day - Obligatory_hidden_period]
    Selected_New_quarantined_records['IsQuarantined?'] = Selected_New_quarantined_records['HH_P'].apply(lambda x: np.random.choice([1,0], p=[Quarantine_prob, 1 - Quarantine_prob]))
    Selected_New_quarantined_records = Selected_New_quarantined_records[(Selected_New_quarantined_records['IsQuarantined?'] == 1)]
    Selected_New_quarantined_records = Infected_list[Infected_list['household_id'].isin(Selected_New_quarantined_records['household_id'])]
    Selected_New_quarantined_records['Sent_to_quarantine_date'] = day
    Quarantined_list = pd.concat([Quarantined_list,Selected_New_quarantined_records])
    
    Infected_list = Infected_list[(~Infected_list['HH_P'].isin(Selected_New_quarantined_records['HH_P']))]
    
    
    print('***************', 'Processing of quarantined cases is finished')
    
    # If an infected person is found and quarantined, his/her whole family should be automatically quarantined
    InfectedinHH = Selected_New_quarantined_records[['household_id']].groupby(['household_id']).size().reset_index(name='counts_InfectedinHH')
    Selected_New_quarantined_Family_records = persons[persons.household_id.isin(InfectedinHH['household_id'])]
    Selected_New_quarantined_Family_records = Selected_New_quarantined_Family_records[(~Selected_New_quarantined_Family_records.HH_P.isin(Selected_New_quarantined_records.HH_P)) & (~Selected_New_quarantined_Family_records['HH_P'].isin(Recovered_list['HH_P'])) & (~Selected_New_quarantined_Family_records['HH_P'].isin(Dead_list['HH_P']))]
    
    Selected_New_quarantined_Family_records['Sent_to_quarantine_date'] = day
    Quarantined_Family = pd.concat([Quarantined_Family,Selected_New_quarantined_Family_records])
    Quarantined_Family = Quarantined_Family[Quarantined_Family.Sent_to_quarantine_date > day - Duration_of_sickness_Infection_to_Recovery]
    
    print('***************', 'Processing of family members quarantining is finished')
    
    #### Processing of Quarantined to Dead cases
    New_dead_persons = Quarantined_list
    New_dead_persons['IsDead?'] = New_dead_persons['HH_P'].apply(lambda x: np.random.choice([1,0], p=[Quarantined_to_Dead_prob, 1 - Quarantined_to_Dead_prob]))
    New_dead_persons = New_dead_persons[(New_dead_persons['IsDead?'] == 1)]
    
    Dead_list = pd.concat([Dead_list,New_dead_persons])
    Quarantined_list = Quarantined_list[~Quarantined_list['HH_P'].isin(New_dead_persons['HH_P'])]  
    
    print('***************', 'Processing of quarantining to dead cases is finished')
    
    #### Processing of Infected to Dead cases
    New_dead_persons = Infected_list
    New_dead_persons['IsDead?'] = New_dead_persons['HH_P'].apply(lambda x: np.random.choice([1,0], p=[Infected_to_Dead_prob, 1 - Infected_to_Dead_prob]))
    New_dead_persons = New_dead_persons[(New_dead_persons['IsDead?'] == 1)]
    
    Dead_list = pd.concat([Dead_list,New_dead_persons])
    Infected_list = Infected_list[~Infected_list['HH_P'].isin(New_dead_persons['HH_P'])]     
    
#    Infected_list.drop_duplicates('HH_P' , keep='first', inplace = True)
    
    print('***************', 'Processing of infected to dead cases is finished')
    
    ### Processing persons dataset
    persons = persons_copy.copy()
    persons = persons[(~persons['HH_P'].isin(Isolated_list['HH_P'])) & (~persons['HH_P'].isin(Dead_list['HH_P'])) & (~persons['HH_P'].isin(Quarantined_Family['HH_P']))]
    
#    Infected_list.to_csv('Infected_list.csv',index = False)
#    Isolated_list.to_csv('Quarantine.csv',index = False)
#    Quarantined_Family.to_csv('Quarantined_Family.csv',index = False)
#    Recovered_list.to_csv('Recovered.csv',index = False)
#    Dead_list.to_csv('Dead.csv',index = False)
    
    Y['Susceptibled'].append(len(persons))
    Y['Infected'].append(len(Infected_list))
    Y['Quarantined'].append(len(Isolated_list))
    Y['Quarantined_Family'].append(len(Quarantined_Family))
    Y['Dead'].append(len(Dead_list))
    Y['Recovered'].append(len(Recovered_list))
    Y['Total_infected'] = [Y['Quarantined'][y]+Y['Infected'][y] for y in range(len(Y['Infected']))]
    
    ## Reports
    print("+++++++++++++++++++++++++++++++++++++++++++++")
    print('Day number = ', day)
    print('Number of Susceptibled = ', Y['Susceptibled'][-1])
    print('Number of Infected = ', Y['Infected'][-1])
    print('Number of Isolated = ', Y['Quarantined'][-1])
    print('Number of Quarantined_Family = ', Y['Quarantined_Family'][-1])
    print('Number of Dead = ', Y['Dead'][-1])
    print('Number of Recovered = ', Y['Recovered'][-1])
    print("_____________________________________________")
    
    if day > 1:
        Cumulative_Infected[day] = Cumulative_Infected[day-1] + Daily_Infected[day]
    else:
        Cumulative_Infected[day] = Y['Infected'][0]
    
    Color = {'Susceptibled':'green','Infected':'red','Quarantined':'darkgoldenrod','Dead':'black','Recovered':'blue','Quarantined_Family':'orange','Total_infected':'red', 'Observed':'black'}
    
    # ObservedData = pd.read_csv('ObservedNumberOfCOVIDcases.csv',delimiter=',')
    
#    for i in ['Infected','Quarantined','Dead','Recovered','Quarantined_Family']:
#        plt.plot(list(range(1,day+1)),Y[i],label = i,color=Color[i])
#    plt.legend(loc=2,prop={'size':8})
#    plt.savefig('States1.png',dpi = 300, bbox_inches='tight')
#    plt.show()    
#
#    plt.clf()
#   
#    for i in ['Total_infected','Dead','Recovered','Quarantined_Family']:
#        plt.plot(list(range(1,day+1)),Y[i],label = i,color=Color[i])
#    plt.legend(loc=2,prop={'size':8})
#    plt.savefig('States2.png',dpi = 300, bbox_inches='tight')
#    plt.show()    
#
#    plt.clf()
    
    LagDay = 4
    plt.scatter(list(range(0,day)),Daily_Infected.values(),label = 'Simulated',color=Color['Infected'], s = 8)   
#    plt.scatter(list(range(0,day)),ObservedData['Count'][LagDay:LagDay+day],label = 'Observed',color=Color['Observed'], s = 8)  
    plt.legend(loc=2,prop={'size':8})
    plt.savefig('Daily_Infected.png',dpi = 300, bbox_inches='tight')
    plt.show()    
    plt.clf()
    
    plt.scatter(list(range(0,day)),Cumulative_Infected.values(),label = 'Simulated',color=Color['Infected'], s = 8)  
#    plt.scatter(list(range(0,day)),ObservedData['CumulativeCases'][LagDay:LagDay+day],label = 'Observed',color=Color['Observed'], s = 8)  
    plt.legend(loc=2,prop={'size':8})
    plt.savefig('Cumulative_Infected.png',dpi = 300, bbox_inches='tight')
    plt.show()    
    plt.clf()    
    
    if day == MileStone_Day:
        df = DataFrame ([day],columns=['MileStone_Day'])
        df.to_csv('M_MileStone_Day.csv',index = False)
        
        Infected_list.to_csv('M_Infected_list.csv',index = False)
        Isolated_list.to_csv('M_Quarantine.csv',index = False)
        Quarantined_Family.to_csv('M_Quarantined_Family.csv',index = False)
        Recovered_list.to_csv('M_Recovered.csv',index = False)
        Dead_list.to_csv('M_Dead.csv',index = False)
        persons.to_csv('M_persons.csv',index = False)
        pd.DataFrame(Daily_Infected.items(),columns=['Day','Daily_Infected']).to_csv('M_Daily_Infected.csv',index = False)
        pd.DataFrame(Cumulative_Infected.items(),columns=['Day','Cumulative_Infected']).to_csv('M_Cumulative_Infected.csv',index = False)
        pd.DataFrame(Y.items(),columns=['Group','Values']).to_csv('M_Y.csv',index = False)
        Mile_Stone_day_reached == True
        
        exit()
      
        
###### Saving results
        
# MakingFolder = '{0}_{1}_{2}_{3}'.format(Folder,Social_Distancing,Proportion_of_people_using_Mask,Proportion_of_people_using_Mask_who_use_Mask_at_Home)
# os.mkdir(MakingFolder)
# Infected_list.to_csv('{0}/R_Infected_list.csv'.format(MakingFolder),index = False)
# Isolated_list.to_csv('{0}/R_Quarantine.csv'.format(MakingFolder),index = False)
# Quarantined_Family.to_csv('{0}/R_Quarantined_Family.csv'.format(MakingFolder),index = False)
# Recovered_list.to_csv('{0}/R_Recovered.csv'.format(MakingFolder),index = False)
# Dead_list.to_csv('{0}/R_Dead.csv'.format(MakingFolder),index = False)
# persons.to_csv('{0}/R_persons.csv'.format(MakingFolder),index = False)
# pd.DataFrame(Daily_Infected.items(),columns=['Day','Daily_Infected']).to_csv('{0}/R_Daily_Infected.csv'.format(MakingFolder),index = False)
# pd.DataFrame(Cumulative_Infected.items(),columns=['Day','Cumulative_Infected']).to_csv('{0}/R_Cumulative_Infected.csv'.format(MakingFolder),index = False)
# pd.DataFrame(Y.items(),columns=['Group','Values']).to_csv('{0}/R_Y.csv'.format(MakingFolder),index = False)
# pd.DataFrame(Count_Infections.items(),columns=['Act','Count']).to_csv('{0}/R_Count_Infections.csv'.format(MakingFolder),index = False)

