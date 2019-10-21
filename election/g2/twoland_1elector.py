import os
import argparse
import importlib
import map_util
import numpy as np
import matplotlib.pyplot as plt

import shapely
from shapely.ops import split
from shapely.geometry import Polygon, LineString, Point

BASE_DIR="/home/ps2958/pps-election"
print(f"BASE DIR = {BASE_DIR}")
os.chdir(BASE_DIR)

# Utility function
def get_rotation_matrix(theta):
    """
    Set theta>0 for counter-clockwise rotation.
    Set theta<0 for clockwise rotation.
    Transpose rotation matrix to reverse rotation.
    """
    c, s = np.cos(theta), np.sin(theta)
    return np.array([[c, -s], [s, c]])

def visualize_map(_map, ax=None, color_scheme='argmax'):
    if ax is None:
        fig, ax = plt.subplots(1, 1, figsize=(10,10))
    
    if color_scheme == 'argmax':
        colors = np.argmax(_map[:, 2:], 1)
    else:
        colors = [tuple(dist) for dist in _map[:, 2:]]

    plot = ax.scatter(_map[:,0], _map[:,1], s=0.01, c=colors, alpha=0.5, cmap='rainbow')

# 2 party 1 elector with voter count tolerance
def find_two_split_with_tolerance(location, winners, target, voter_tolerance=25, resolution=300, MYPARTY=0, return_top=True, debug=False):
    
    if target is None:
        target = (winners==MYPARTY).mean()

    # Calculate voter numbers required
    num_voters = len(location) // 2
    min_voters = num_voters - voter_tolerance
    max_voters = num_voters + voter_tolerance
    
    # Initial number of winners
    init_wins = (winners==MYPARTY).sum()
    
    # Output statistics
    thetas  = np.linspace(0, np.pi, resolution)
    ratios1 = np.zeros_like(thetas)
    ratios2 = np.zeros_like(thetas)
    idxes   = np.zeros_like(thetas, dtype=int)
    
    if target < 0.5:
        # If it is less than 50%, split such that one district has 50% or more votes
        for i, theta in enumerate(thetas):
            R = get_rotation_matrix(theta)
            new_locs = location @ R.T
            
            # Boundary line is x=x'
            sortidx = new_locs[:, 0].argsort()
            
            winner_counts = np.arange(min_voters, max_voters)
            poly1_winners = (winners[sortidx] == MYPARTY).cumsum()[min_voters: max_voters]
            poly2_winners = init_wins - poly1_winners
            
            poly1_ratio = poly1_winners / winner_counts
            poly2_ratio = poly2_winners / winner_counts[::-1]
            
            p1_idx, p2_idx = poly1_ratio.argmax(), poly2_ratio.argmax()
            p1_max, p2_max = poly1_ratio[p1_idx], poly2_ratio[p2_idx]
            
            idxes[i]   = min_voters + (p1_idx if p1_max > p2_max else p2_idx)
            ratios1[i] = poly1_ratio[idxes[i] - min_voters]
            ratios2[i] = poly2_ratio[idxes[i] - min_voters]
            
        # print(ratios1.max(), ratios2.max())

        # Calculate threshold for heuristic function
        thresh = min(max(ratios1.max(), ratios2.max()), 0.51)
    
    else:
        # Else try hard to maintain district proportion
        for i, theta in enumerate(thetas):
            R = get_rotation_matrix(theta)
            new_locs = location @ R.T
            
            # Boundary line is x = x'
            sortidx = new_locs[:, 0].argsort()
            
            winner_counts = np.arange(min_voters, max_voters)
            poly1_winners = (winners[sortidx] == MYPARTY).cumsum()[min_voters: max_voters]
            poly2_winners = init_wins - poly1_winners
            
            poly1_ratio = poly1_winners / winner_counts
            poly2_ratio = poly2_winners / winner_counts[::-1]
            
            thresh = 0.50
            delta = 10. * (poly1_ratio < thresh) + 10. * (poly2_ratio < thresh)
            delta = ((poly1_ratio < target) + 1) * np.abs(poly1_ratio - target)
            delta = ((poly2_ratio < target) + 1) * np.abs(poly2_ratio - target)
            
            idxes[i]   = delta.argmin()
            ratios1[i] = poly1_ratio[idxes[i]]
            ratios2[i] = poly2_ratio[idxes[i]]
            idxes[i]  += min_voters
        
        # Threshold for heuristic function is simply majority
        thresh = 0.50

    deltas = 10. * (ratios1 < thresh) + 10. * (ratios2 < thresh)

    # Penalties for differences to target
    deltas += ((ratios1<target) + 1) * np.abs(ratios1-target)
    deltas += ((ratios2<target) + 1) * np.abs(ratios2-target)

    if debug:
        return thetas, deltas, ratios1, ratios2

    if return_top:
        min_idx = deltas.argmin()
        
#         print(f"Found Δ={deltas[min_idx]:0.5f} at θ={thetas[min_idx]:0.5f} "
#               f"xp_idx={idxes[min_idx]}. Split=[{ratios1[min_idx]:0.3f} {ratios2[min_idx]:0.3f}]")

        return thetas[min_idx], int(idxes[min_idx]) # TODO: Verify types

    top_deltas = deltas.argsort()[:10]
    return thetas[top_deltas], deltas[top_deltas], ratios1[top_deltas], ratios2[top_deltas]

def split_polygon(xp, theta, polygon):
    """
    Helper function.
    Given a polygon and a theta, divides the polygon into two districts based on theta
    """
    R = get_rotation_matrix(theta)
    
    rp = Polygon(np.dstack(polygon.exterior.xy)[0] @ R.T)
    x_min, y_min, x_max, y_max = rp.bounds
    
    """ Has form x = xp """
    liney = LineString([[xp, y_min], [xp, y_max]] @ R)

    poly1, poly2 = shapely.ops.split(polygon, liney)
    
    return poly1, poly2

def recursively_split_twos(location, winners, polygon, voter_tolerance=25, num_splits=8, resolution=1000, MYPARTY=0):
    """
    Splits a district into two equal halves with same proportion with a penalty for < 0.50
    Outperforms recursively_split_threes
    """
    target = (winners==MYPARTY).mean()
    if num_splits==0:
        return int(target >= 0.50), [polygon]
    
    # print()
    # print("Target = ", target)
    
    # Finds the best-split line x=xp rotated about theta
    theta, xp_idx = find_two_split_with_tolerance(location, winners, target,
            voter_tolerance=voter_tolerance, resolution=resolution, MYPARTY=MYPARTY)
    
    # Calculate xp and split voter locations/preferences
    R = get_rotation_matrix(theta)
    new_locs = location @ R.T
    sorted_idx = new_locs[:, 0].argsort()
    xp = new_locs[sorted_idx[xp_idx], 0]
    
    poly1, poly2 = split_polygon(xp, theta, polygon)
    
    location1, winners1 = location[sorted_idx[:xp_idx+1]], winners[sorted_idx[:xp_idx+1]]
    location2, winners2 = location[sorted_idx[xp_idx+1:]], winners[sorted_idx[xp_idx+1:]]
    
    if poly1.contains(Point(*location2[0])):
        poly1, poly2 = poly2, poly1

    if num_splits == 1:
        print(f"Found split θ={np.degrees(theta):0.2f} splits={[(winners1==MYPARTY).mean(), (winners2==MYPARTY).mean()]}")
    
    left_wins,  left_polygons  = recursively_split_twos(location1, winners1, poly1, voter_tolerance, num_splits-1, resolution, MYPARTY)
    right_wins, right_polygons = recursively_split_twos(location2, winners2, poly2, voter_tolerance, num_splits-1, resolution, MYPARTY)
    
    return left_wins + right_wins, left_polygons + right_polygons

if __name__ == '__main__':
    MAP_LIST    = map_util.list_maps("/home/ps2958/pps-election/tournaments/maps")
    LOADED_MAPS = [map_util.load_map(map_file) for map_file in MAP_LIST]

    for mmap, name in zip(LOADED_MAPS, [name.split("/")[-1].split(".")[0] for name in MAP_LIST]):

        # Code to generate 256 districts with 1 elector scheme
        location, winners = mmap[:,:2], mmap[:,2:].argmax(1)
        polygon   = Polygon([[0,0], [1000,0], [500,500*np.sqrt(3)]])

        num_voters, num_parties = mmap[:, 2:].shape
        
        for MYPARTY in range(num_parties):
            num_splits = 6
            num_districts = 2**num_splits
            output_name = f"results/{name}_{num_districts}districts_party{MYPARTY+1}.dat"
            if os.path.exists(output_name):
                print(output_name, " exists")
                continue

            voter_tolerance = np.floor(0.1 * 333333/(2**num_splits) / num_splits).astype(int)
            win_counts, districts = recursively_split_twos(
                location, winners, polygon, voter_tolerance, num_splits, resolution=1000, MYPARTY=MYPARTY)
            
            print(name, MYPARTY, win_counts)

            district_boundaries = [
                [len(district.boundary.coords)] + [p for x in district.boundary.coords for p in x] 
                for district in districts
            ]
            num_districts = len(district_boundaries)

            fmt_boundary = []
            for boundary in district_boundaries:
                fmt_boundary.append(str(boundary[0]) + " " + "".join(map(lambda x:f"{x:f} ", boundary[1:])))

            with open(f"results/64districts_1elector/{name}_{num_districts}districts_party{MYPARTY+1}.dat", "w") as handle:
                np.savetxt(handle, mmap, fmt="%f", delimiter=' ', header=f"{num_voters} {num_parties}", comments='')
                np.savetxt(handle, np.array(fmt_boundary), fmt="%s", header=f"{num_districts}", comments='')
