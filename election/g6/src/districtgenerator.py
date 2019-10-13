import random
from collections import defaultdict
from itertools import combinations, product
from typing import List, Tuple, Dict

import metis
import networkx as nx
import nxmetis
from shapely.geometry import Polygon
from shapely.ops import cascaded_union

from election.g6.src import dist_analysis
from election.g6.src.voter import Voter


class District:
    def __init__(self):
        self.n_voters = 0
        self.polygons = []

    def append_triangle(self, triangle):
        self.polygons.append(triangle)

    def get_population(self):
        population = 0
        for polygon in self.polygons:
            population += polygon['population']
        return population

    def get_one_polygon(self):
        f = []
        for p in self.polygons:
            f.append(p['polygon'])
        return cascaded_union(f)

    def get_party_distribution(self):
        party_distribution = defaultdict(int)
        for polygon in self.polygons:
            for party in polygon['party_distribution'].keys():
                party_distribution[party] += polygon['party_distribution'][party]
        return party_distribution

    def get_party_seats(self):
        party_distribution = self.get_party_distribution()
        return dist_analysis.get_one_dist_seats(party_distribution, 3)


class Triangle:
    def __init__(self, n_voters, polygon):
        self.n_voters = n_voters
        self.polygon = polygon


def check_if_node_is_near_part_boundary(graph, node):
    return True


def wasted_vote_metric(districts: List[District]) -> Dict[str, float]:
    party_distribution = []
    party_seats = []
    for district in districts:
        pass
    return {}


def get_districts_from_triangles(
        voters: List[Voter], triangles: List[Dict], graph, n_districts: int, seed: int
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

    # Extension
    # print('Making initial partition')
    # (edgecuts, parts) = metis.part_graph(
    #     graph,
    #     n_districts,
    #     ctype='shem',
    #     rtype='sep2sided',
    #     ncuts=100,  # number of different cuts to try
    #     niter=2000,  # number of iterations of an algorithm
    #     contig=True,  # force partitions to be contiguous
    #     ubvec=[1.09],  # allowed constraint imbalance
    # )
    # print('Forming districts')
    # districts = []
    # for i in range(n_districts):
    #     districts.append(District())
    # for index, part in enumerate(parts):
    #     graph.nodes[index]['part'] = part
    #     districts[part].append_triangle(triangles[index])
    print('Making initial partition')
    (edgecuts, parts) = nxmetis.partition(
        graph,
        n_districts,
        # ctype='shem',
        # rtype='sep2sided',
        node_weight='population',
        options=nxmetis.types.MetisOptions(
            ncuts=100,  # number of different cuts to try
            niter=2000,  # number of iterations of an algorithm
            contig=True
        ),
        ubvec=[1.09],  # allowed constraint imbalance
    )
    print('Forming districts')
    districts = []
    for i in range(n_districts):
        districts.append(District())
    for part, part_triangles in enumerate(parts):
        for index in part_triangles:
            graph.nodes[index]['part'] = part
            districts[part].append_triangle(triangles[index])

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
    s = [(d.get_population(), 3703 < d.get_population() <= 4526) for d in districts]
    print('Returning polygons')
    return [d.get_one_polygon() for d in districts]


def get_districts(
        voters: List[Voter], triangles: List[Dict], graph, representatives_per_district: int, seed: int
) -> List[Polygon]:
    n_districts = int(81. / representatives_per_district * 3.)
    districts = get_districts_from_triangles(voters, triangles, graph, n_districts, seed)
    return districts
