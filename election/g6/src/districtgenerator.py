import math
import random
from typing import List

import numpy as np
from shapely.geometry import Point, Polygon

from election.g6.src.voter import Voter

num_parties = 3
triangle = Polygon([(0, 0), (1000, 0), (500, 500 * math.sqrt(3))])


def is_in_polygon(voter: Voter, polygon: Polygon) -> bool:
    return polygon.contains(voter.location)

def get_voters_in_polygon(polygon: Polygon, voters: List[Voter]) -> List[Polygon]:
    return list(filter(lambda x: is_in_polygon(x, polygon), voters))

def naive_partition(n: int) -> List[Polygon]:
    edge_length = 1000 / n
    x_diff = 1/2 * edge_length
    y_diff = math.sqrt(3)/2 * edge_length
    points_by_level = [[(500, 500*math.sqrt(3))]]
    for level in range(n):
        new_level = []
        for point in points_by_level[-1]:
            new_point = (point[0] - x_diff, point[1] - y_diff)
            new_level.append(new_point)
        new_point = (point[0] + x_diff, point[1] - y_diff)
        new_level.append(new_point)
        points_by_level.append(new_level)
    
    result = []
    for level in range(n):
        curr_level = points_by_level[level]
        next_level = points_by_level[level+1]
        temp = [next_level[0]]
        for p1, p2 in zip(curr_level, next_level[1:]):
            temp.append(p1)
            temp.append(p2)
        
        for i in range(len(temp)-2):
            p1, p2, p3 = temp[i], temp[i+1], temp[i+2]
            result.append(Polygon([p1, p2, p3]))
        
    return result

def height_adjustable_partition(n: int) -> List[Polygon]:
    # TODO
    edge_length = 1000 / n
    x_diff = 1/2 * edge_length
    y_diff = math.sqrt(3)/2 * edge_length
    points_by_level = [[(500, 500*math.sqrt(3))]]
    for level in range(n):
        new_level = []
        for point in points_by_level[-1]:
            new_point = (point[0] - x_diff, point[1] - y_diff)
            new_level.append(new_point)
        new_point = (point[0] + x_diff, point[1] - y_diff)
        new_level.append(new_point)
        points_by_level.append(new_level)
    
    result = []
    for level in range(n):
        curr_level = points_by_level[level]
        next_level = points_by_level[level+1]
        temp = [next_level[0]]
        for p1, p2 in zip(curr_level, next_level[1:]):
            temp.append(p1)
            temp.append(p2)
        
        for i in range(len(temp)-2):
            p1, p2, p3 = temp[i], temp[i+1], temp[i+2]
            result.append(Polygon([p1, p2, p3]))
        
    return result

# partition into three smaller triangles recursively
def recursive_partition(triangle: Polygon, voters: List[Voter], threshold = threshold,
                        tolerance = 2.2) -> List[Polygon]:
    new_voters = get_voters_in_polygon(triangle, voters)
    if len(new_voters) <= tolerance*threshold: 
        return [triangle]
    
    # TODO: better centroid finding algorithm, e.g fast median
    x, y = 0, 0
    for voter in new_voters:
        x += voter.location.x
        y += voter.location.y
    x /= len(new_voters)
    y /= len(new_voters)
    
    centroid = (x, y)
    temp = list(triangle.exterior.coords)
    p1, p2, p3 = temp[0], temp[1], temp[2]
    new_triangle1 = Polygon([p1, p2, centroid])
    new_triangle2 = Polygon([p1, p3, centroid])
    new_triangle3 = Polygon([p2, p3, centroid])
    result = recursive_partition(new_triangle1, new_voters, threshold, tolerance) + \
             recursive_partition(new_triangle2, new_voters, threshold, tolerance) + \
             recursive_partition(new_triangle3, new_voters, threshold, tolerance)
    return result
    
def combined_partition(n: int, voters: List[Voter]) -> List[Polygon]:
    result = []
    polygons = naive_partition(n)
    for polygon in polygons:
        result += recursive_partition(polygon, voters)
    return result
    
def k_means_clustering():
    # TODO: Derek
    pass

def get_initial_triangles(voters: List[Voter], n_levels: int, seed: int = 1234) -> List[Polygon]:
    np.random.seed(seed)
    return combined_partition(n_levels, voters)


def get_districts_from_triangles(triangles: List[Polygon], n_districts: int, seed: int) -> List[Polygon]:
    # TODO: Adam
    pass


def get_districts(voters: List[Voter], representatives_per_district: int, seed: int) -> List[Polygon]:
    n_districts = 81
    n_triangles = n_districts * 7
    triangles = get_initial_triangles(voters, n_triangles, seed)
    districts = get_districts_from_triangles(triangles, n_districts, seed)
    return districts
