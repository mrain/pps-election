import math
import random
from typing import List
from shapely.ops import cascaded_union

import networkx as nx
import metis
import numpy as np
from shapely.geometry import Polygon

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

def _find_level_points(voters, population_per_triangle):
    voters = sorted(voters, key = lambda voter: voter.location.y, reverse = True)
    line1_x = lambda y: math.sqrt(3)/3 * y
    line2_x = lambda y: -math.sqrt(3)/3 * y + 1000
    def find_level(curr_voter_num, population_per_level, level):
        if curr_voter_num + population_per_level <= len(voters):
            split_voter = voters[curr_voter_num + population_per_level - 1]
            y = max(split_voter.location.y - 0.00001, 0)
        else:
            y = 0
        x_left, x_right = line1_x(y), line2_x(y)
        x_diff = (x_right - x_left) / (level - 1)
        level_points = [(x_left, y)] + [(x_left + i*x_diff, y) for i in range(1, level-1)] + \
                       [(x_right, y)]
        return level_points 
    
    level_num = math.floor(math.sqrt(len(voters) / population_per_triangle))
    population_per_triangle = math.ceil(len(voters) / (level_num**2))
    curr_voter_num = 0
    points_by_level = [[(500, 500*math.sqrt(3))]]
    for level in range(2, level_num + 1):
        curr_level_population = population_per_triangle * 2 * (level - 2) + 1
        curr_level = find_level(curr_voter_num, curr_level_population, level)
        curr_voter_num += curr_level_population
        points_by_level.append(curr_level)
    # manually add all remaining voters to last level
    curr_level = find_level(curr_voter_num, len(voters) - curr_voter_num + 1, level + 1)
    points_by_level.append(curr_level)
    return points_by_level

def adaptive_partition(voters: List[Voter], population_per_triangle = None) -> List[Polygon]:
    if not population_per_triangle: 
        population_per_triangle = len(voters) // (81*7)
    points_by_level = _find_level_points(voters, population_per_triangle)

    result = []
    for level in range(len(points_by_level)-1):
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
def recursive_partition(triangle: Polygon, voters: List[Voter], threshold,
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
    
def combined_partition(voters: List[Voter], population_per_triangle = None) -> List[Polygon]:
    if not population_per_triangle:
        population_per_triangle = len(voters) // (81*7)
    result = []
    polygons = adaptive_partition(voters, population_per_triangle = population_per_triangle)
    for polygon in polygons:
        result += recursive_partition(polygon, voters, threshold = population_per_triangle)
    return result
    
def k_means_clustering():
    # TODO
    pass

def get_initial_triangles(voters: List[Voter], threshold = 333333//(81*7), seed: int = 1234) -> List[Polygon]:
    np.random.seed(seed)
    return combined_partition(voters, population_per_triangle = threshold)


class District:
    def __init__(self):
        self.n_voters = 0
        self.polygons = []

    def append_triangle(self, triangle):
        self.polygons.append(triangle)

    def get_one_polygon(self):
        # f = self.polygons[0]
        # for p in self.polygons[1:]:
        #     f = f.union(p)
        return cascaded_union(self.polygons)


class Triangle:
    def __init__(self, n_voters, polygon):
        self.n_voters = n_voters
        self.polygon = polygon


def find_adjacent_triangle(index, triangles: List[Polygon]):
    inds = []
    tr = triangles[index]
    for i, t in enumerate(triangles):
        if i == index:
            continue
        x1, y1 = t.exterior.coords.xy
        x2, y2 = tr.exterior.coords.xy
        n_p = 0
        for l in range(3):
            for j in range(3):
                if x1[l] == x2[j] and y1[l] == y2[j]:
                    n_p += 1
        if n_p >= 2:
            inds.append(i)
    return inds


def get_districts_from_triangles(voters: List[Voter], raw_triangles: List[Polygon], n_districts: int, seed: int) -> List[Polygon]:
    # triangles = []
    # for rt in raw_triangles:
    #     triangles.append(Triangle(get_voters_in_polygon(rt, voters), rt))
    # triangles = sorted(triangles, key=lambda x: x.n_voters)
    # free_triangles = list(range(len(triangles)))
    # districts = []
    # for i in range(n_districts):
    #     index = random.random.sample(free_triangles, 1)
    #     d = District()
    #     d.append_triangle(triangles[index])
    #     districts.append(d)
    #     free_triangles.pop(index)
    graph = nx.Graph()
    graph.add_nodes_from(list(range(len(raw_triangles))))
    for index, triangle in enumerate(raw_triangles):
        adj_trs = find_adjacent_triangle(index, raw_triangles)
        for tr in adj_trs:
            graph.add_edge(index, tr)
    # Extension
    (edgecuts, parts) = metis.part_graph(graph, n_districts, ncuts=2, niter=20, contig=True)
    districts = []
    for i in range(n_districts):
        districts.append(District())
    for index, part in enumerate(parts):
        districts[part].append_triangle(raw_triangles[index])
    return [d.get_one_polygon() for d in districts]


def get_districts(voters: List[Voter], representatives_per_district: int, seed: int) -> List[Polygon]:
    n_districts = 81
    n_triangles = n_districts * 7
    threshold = len(voters) / n_triangles
    n_levels = 22
    triangles = get_initial_triangles(voters, threshold, n_levels, seed)
    districts = get_districts_from_triangles(voters, triangles, n_districts, seed)
    return districts
