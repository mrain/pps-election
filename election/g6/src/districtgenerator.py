import random
from collections import defaultdict
from itertools import combinations, product
from typing import List

import metis
import networkx as nx
from shapely.geometry import Polygon
from shapely.ops import cascaded_union

from election.g6.src.voter import Voter
METIS_DBG_ALL = sum(2**i for i in list(range(9))+[11])


def is_in_polygon(voter: Voter, polygon: Polygon) -> bool:
    return polygon.contains(voter.location)


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


def check_if_node_is_near_part_boundary(graph, node):
    return True


def get_districts_from_triangles(
        voters: List[Voter], raw_triangles: List[Polygon], n_districts: int, seed: int
) -> List[Polygon]:
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
    print('Forming graph')
    graph = nx.Graph()
    graph.graph['node_weight_attr'] = 'population'
    graph.add_nodes_from(list(range(len(raw_triangles))))
    for index, triangle in enumerate(raw_triangles):
        print(str(index+1) + '/' + str(len(raw_triangles)), flush=True)
        graph.nodes[index]['population'] = triangle.population
        adj_trs = find_adjacent_triangle(index, raw_triangles)
        for tr in adj_trs:
            graph.add_edge(index, tr)
    # Extension
    print('Making initial partition')
    (edgecuts, parts) = metis.part_graph(
        graph,
        n_districts,
        ncuts=2,  # number of different cuts to try
        niter=20,  # number of iterations of an algorithm
        contig=True,  # force partitions to be contiguous
        ubvec=1.1,  # allowed constraint imbalance
        dbglvl=METIS_DBG_ALL
    )
    print('Forming districts')
    districts = []
    for i in range(n_districts):
        districts.append(District())
    for index, part in enumerate(parts):
        graph.nodes[index]['part'] = part
        districts[part].append_triangle(raw_triangles[index])

    # niters = 1000
    # nsample = 50
    # for i in range(niters):
    #     # sample random vertices pairs
    #     sampled_nodes = random.sample(graph.nodes, nsample)
    #     candidate_nodes = []
    #     for node in sampled_nodes:
    #         is_near_boundary = check_if_node_is_near_part_boundary(graph, node)
    #         if is_near_boundary:
    #             candidate_nodes.append(node)
    #     parts_nodes = defaultdict(list)
    #     swap_proposals = []
    #     for node in candidate_nodes:
    #         parts_nodes[node['part']].append(node)
    #     for a, b in combinations(parts_nodes.keys(), 2):
    #         for node1, node2 in product(parts_nodes[a], parts_nodes[b]):
    #             node1_has_where_to_flip = False
    #             node2_has_where_to_flip = False
    #             for n in graph.neighbors(node1):
    #                 if n != node2:
    #                     node1_has_where_to_flip = True
    #                     break
    #             for n in graph.neighbors(node2):
    #                 if n != node1:
    #                     node2_has_where_to_flip = True
    #                     break
    #             if node1_has_where_to_flip and node2_has_where_to_flip:
    #                 swap_proposals.append([
    #                     [a, node1],
    #                     [b, node2]
    #                 ])
    #     # check if flipping each pair is better for metric
    #     # check if flip is valid
    #     # flip
    #     pass
    print('Returning polygons')
    return [d.get_one_polygon() for d in districts]


def get_districts(
        voters: List[Voter], triangles: List[Polygon], representatives_per_district: int, seed: int
) -> List[Polygon]:
    n_districts = int(81. / representatives_per_district * 3.)
    districts = get_districts_from_triangles(voters, triangles, n_districts, seed)
    return districts
