#import shapely
import math
import numpy as np
import pandas as pd
from scipy.stats import norm
from shapely.geometry import Point, Polygon

lat_range = (90.0, 0)  # overall range of latitudes (90 deg N to 0 deg)
long_range = (-180.0, -90.0)  # overall range of longitudes (180 deg to 90 deg W)
res = 120.0  # resolution (30 sec, 120 grid squares in one degree)
threeland = Polygon([(0, 0), (1000, 0), (500, 500 * math.sqrt(3))])  # shape of Threeland
threeland_pop = 333333  # total population of Threeland

def read_pop(file):
    # Read population by grid square from ASCII file
    arr = np.loadtxt(file, skiprows = 6)
    return arr

def get_triangle_pop(arr, min_long, max_long, min_lat, max_lat):
    # Get population distribution in a triangular area
    # Vertices: (min_long, min_lat), (max_long, min_lat), ((min_long + max_long) / 2, max_lat)
    triangle = Polygon([(min_long, min_lat), (max_long, min_lat), ((min_long + max_long) / 2, max_lat)])
    pop = {}
    # Determine which grid squares are needed
    min_x = int((lat_range[0] - max_lat) * res)
    max_x = int((lat_range[0] - min_lat) * res)
    min_y = int((min_long - long_range[0]) * res)
    max_y = int((max_long - long_range[0]) * res)
    # Add population square by square
    for i in range(min_x, max_x):
        for j in range(min_y, max_y):
            # Calculate coordinates and population of the grid square
            this_long = j / res + long_range[0]
            this_lat = lat_range[0] - i / res
            this_pop = max(arr[i, j], 0)  # error code is -9999
            # Add population if at least one vertex of the square lies in the big triangle
            ul_in = triangle.contains(Point(this_long, this_lat))
            ur_in = triangle.contains(Point(this_long + 1 / res, this_lat))
            ll_in = triangle.contains(Point(this_long, this_lat - 1 / res))
            lr_in = triangle.contains(Point(this_long + 1 / res, this_lat - 1 / res))
            if ul_in or ur_in or ll_in or lr_in:
                pop[(this_long, this_lat)] = this_pop
    return pop

def get_threeland_pop(raw_pop, min_long, max_long, min_lat, max_lat):
    # Get population distribution in Threeland based on raw population data
    raw_total = sum(list(raw_pop.values()))
    #r_x = 1000 / res / (max_long - min_long)
    #r_y = 500 * math.sqrt(3) / res / (max_lat - min_lat)
    pop = {}
    # Add population square by square
    for (long, lat) in raw_pop:
        # Calculate corresponding coordinates in Threeland
        x = 1000 * (long - min_long) / (max_long - min_long)
        y = 500 * math.sqrt(3) * (lat - min_lat) / (max_lat - min_lat)
        # Calculate corresponding (Threeland) population in the square
        pop[(x, y)] = (raw_pop[(long, lat)] / raw_total) * threeland_pop
    return pop

def get_threeland_pop_list(pop_dist):
    # Get a list of squares in Threeland and their population
    pop_list = []
    for (x, y) in pop_dist:
        pop_list.append((x, y, pop_dist[(x, y)]))
    return pop_list

def sim_threeland_voters(pop_dist, r_x, r_y, seed = 1234):
    # Simulate voters in Threeland based on distribution
    np.random.seed(seed)
    voters = []
    # Step 1: If a square should contain p voters, add int(p) voters
    for (x, y) in pop_dist:
        base_pop = int(pop_dist[(x, y)])
        for i in range(base_pop):
            # Randomly get a voter in the square
            voter_x = np.random.uniform(low = x, high = x + r_x)
            voter_y = np.random.uniform(low = y - r_y, high = y)
            # Only add the voter if it is actually in Threeland
            if threeland.contains(Point(voter_x, voter_y)):
                voters.append((voter_x, voter_y))
    # Step 2: Allocate remaining voters based on fractional parts of intended square population
    # e.g. Square 1 should have 3.72 voters, Square 2 should have 2.85 voters, add a voter in Square 2 first
    # Add at most one voter from each square
    pop_list = get_threeland_pop_list(pop_dist)
    # Sort the squares based on fractional parts of intended population
    pop_list.sort(key = lambda x: int(x[2]) - x[2])
    for i in range(len(pop_list)):
        x = pop_list[i][0]
        y = pop_list[i][1]
        # Randomly get a voter in the square
        voter_x = np.random.uniform(low = x, high = x + r_x)
        voter_y = np.random.uniform(low = y - r_y, high = y)
        # Only add the voter if it is actually in Threeland
        if threeland.contains(Point(voter_x, voter_y)):
            voters.append((voter_x, voter_y))
        # Stop if enough voters have been added
        if len(voters) >= threeland_pop:
            break
    # Step 3: If there are still more voters to be added, add them
    # Usually this step is not needed, but just in case
    additional_pop = threeland_pop - len(voters)
    for i in range(additional_pop):
        while True:
            # Randomly get a voter in the whole Threeland
            voter_x = np.random.uniform(low = 0, high = 1000)
            voter_y = np.random.uniform(low = 0, high = 500 * math.sqrt(3))
            if threeland.contains(Point(voter_x, voter_y)):
                voters.append((voter_x, voter_y))
                break
    return voters

def get_counties(file, min_long, max_long, min_lat, max_lat):
    # Read county boundaries in a triangular area
    # Assume counties are shaped like polygons
    triangle = Polygon([(min_long, min_lat), (max_long, min_lat), ((min_long + max_long) / 2, max_lat)])
    df = pd.read_csv(file, dtype = {"id": str})
    # Step 1: Add boundaries of all counties
    bounds = {}
    for i in range(len(df)):
        # Extract (x, y) = (long, lat) of each row
        x = df.iloc[i]["x"]
        y = df.iloc[i]["y"]
        # Extract state and county IDs
        county_id = df.iloc[i]["id"].split(".")
        state, county = int(county_id[0]), int(county_id[1])
        # For each (state, county) build a list of border vertices
        if (state, county) in bounds:
            bounds[(state, county)].append((x, y))
        else:
            bounds[(state, county)] = [(x, y)]
    # Step 2: For all counties at least partially in the triangular area, return their boundaries
    counties = {}
    for (state, county) in bounds:
        if len(bounds[(state, county)]) >= 3:
            if triangle.intersects(Polygon(bounds[(state, county)])):
                counties[(state, county)] = bounds[(state, county)][:]
    return counties

def get_threeland_counties(raw_counties, min_long, max_long, min_lat, max_lat):
    # Get the counties in Threeland
    counties = {}
    for (state, county) in raw_counties:
        # For each (state, county) build a list of border vertices
        counties[(state, county)] = []
        for i in range(len(raw_counties[(state, county)])):
            # Calculate corresponding coordinates in Threeland
            long, lat = raw_counties[(state, county)][i]
            x = 1000 * (long - min_long) / (max_long - min_long)
            y = 500 * math.sqrt(3) * (lat - min_lat) / (max_lat - min_lat)
            counties[(state, county)].append((x, y))
    return counties

def get_counties_voting(file, counties):
    # Read the voting records of counties
    voting = {}
    df = pd.read_csv(file, header = 1)
    for i in range(len(df)):
        # Extract state and county IDs
        county_id = df.iloc[i]["fips"]
        state, county = county_id // 1000, county_id % 1000
        # Calculate percentages of votes for each party
        vote_d = df.iloc[i]["vote1"] / (df.iloc[i]["vote1"] + df.iloc[i]["vote2"])
        vote_r = df.iloc[i]["vote2"] / (df.iloc[i]["vote1"] + df.iloc[i]["vote2"])
        # Add the record if the county is in the list
        if (state, county) in counties:
            voting[(state, county)] = (vote_r, vote_d)
    return voting

def get_counties_pref(voting, sd = 0.3):
    # Get the voting preferences in each county
    prefs = {}
    for (state, county) in voting:
        # Extract percentages of votes for each party
        vote_r, vote_d = voting[(state, county)]
        # Calculate voting preference based on normal distribution
        pref_sum = 1
        pref_diff_dr = math.sqrt(2) * sd * norm.ppf(vote_d)
        pref_r = (pref_sum - pref_diff_dr) / 2
        pref_d = (pref_sum + pref_diff_dr) / 2
        prefs[(state, county)] = (pref_r, pref_d)
    return prefs

def sim_threeland_pref(voters, counties, prefs, sd = 0.3, seed = 1234):
    # Simulate voters with preferences in Threeland
    np.random.seed(seed)
    pref_threeland = {}
    for i in range(len(voters)):
        # Get the location of a voter
        v = voters[i]
        for (state, county) in counties:
            # See which county the voter is in
            if Polygon(counties[(state, county)]).contains(Point(v)):
                # Generate preference based on normal distribution
                pref_r, pref_d = prefs[(state, county)]
                pr = np.random.normal(pref_r, sd)
                pd = np.random.normal(pref_d, sd)
                # Truncate preference at 0, 1
                pr = max(min(pr, 1), 0)
                pd = max(min(pd, 1), 0)
                pref_threeland[i] = (v[0], v[1], pr, pd)
                break
        # If the voter is not in any county, generate random preference
        # This should not occur, but just in case
        if i not in pref_threeland:
            # Assume the mean is 0.5 for both parties
            pr = np.random.normal(0.5, sd)
            pd = np.random.normal(0.5, sd)
            pr = max(min(pr, 1), 0)
            pd = max(min(pd, 1), 0)
            pref_threeland[i] = (v[0], v[1], pr, pd)
    return pref_threeland

def save_threeland_pref(file, pref_threeland):
    # Save Threeland voters with preferences
    f = open(file, "w")
    f.write(str(threeland_pop) + " 2\n" )
    for i in pref_threeland:
        this_pref = pref_threeland[i]
        for j in range(len(this_pref)):
            f.write(str(this_pref[j]) + " ")
        f.write("\n")
    f.write("0")
    f.close()

if __name__ == "__main__":
    arr = read_pop("gpw_v4_population_count_rev11_2015_30_sec_1.asc")
    # Vertices: (29 deg N, 100 deg W), (29 deg N, 94 deg W), (33.5 deg N, 97 deg W)
    # Texas Triangle: San Antonio, Houston/Galveston, Dallas/Fort Worth
    pop_triangle = get_triangle_pop(arr, -100, -94, 29, 33.5)
    pop_threeland = get_threeland_pop(pop_triangle, -100, -94, 29, 33.5)
    r_x = 1000 / res / 6
    r_y = 500 * math.sqrt(3) / res / 4.5
    voters_threeland = sim_threeland_voters(pop_threeland, r_x, r_y)
    counties = get_counties("county-boundaries.csv", -100, -94, 29, 33.5)
    counties_threeland = get_threeland_counties(counties, -100, -94, 29, 33.5)
    voting_counties = get_counties_voting("2016_countysheet_v1-0.csv", counties_threeland)
    pref_counties = get_counties_pref(voting_counties)
    pref_threeland = sim_threeland_pref(voters_threeland, counties_threeland, pref_counties)
    save_threeland_pref("pref_threeland.txt", pref_threeland)