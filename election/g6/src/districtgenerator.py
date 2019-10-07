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
    x_diff = 1 / 2 * edge_length
    y_diff = math.sqrt(3) / 2 * edge_length
    points_by_level = [[(500, 500 * math.sqrt(3))]]
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
        next_level = points_by_level[level + 1]
        temp = [next_level[0]]
        for p1, p2 in zip(curr_level, next_level[1:]):
            temp.append(p1)
            temp.append(p2)

        for i in range(len(temp) - 2):
            p1, p2, p3 = temp[i], temp[i + 1], temp[i + 2]
            result.append(Polygon([p1, p2, p3]))

    return result


def height_adjustable_partition(n: int) -> List[Polygon]:
    # TODO
    edge_length = 1000 / n
    x_diff = 1 / 2 * edge_length
    y_diff = math.sqrt(3) / 2 * edge_length
    points_by_level = [[(500, 500 * math.sqrt(3))]]
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
        next_level = points_by_level[level + 1]
        temp = [next_level[0]]
        for p1, p2 in zip(curr_level, next_level[1:]):
            temp.append(p1)
            temp.append(p2)

        for i in range(len(temp) - 2):
            p1, p2, p3 = temp[i], temp[i + 1], temp[i + 2]
            result.append(Polygon([p1, p2, p3]))

    return result


# partition into three smaller triangles recursively
def recursive_partition(triangle: Polygon, voters: List[Voter], threshold,
                        tolerance=2.2) -> List[Polygon]:
    new_voters = get_voters_in_polygon(triangle, voters)
    if len(new_voters) <= tolerance * threshold:
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


def combined_partition(n: int, threshold, voters: List[Voter]) -> List[Polygon]:
    result = []
    polygons = naive_partition(n)
    print("Naive done", flush=True)
    for polygon in polygons:
        result += recursive_partition(polygon, voters, threshold)
        print(".", end='', flush=True)
    return result


def k_means_clustering():
    # TODO: Derek
    pass


def get_initial_triangles(voters: List[Voter], threshold, n_levels: int, seed: int = 1234) -> List[Polygon]:
    np.random.seed(seed)
    return combined_partition(n_levels, threshold, voters)


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
