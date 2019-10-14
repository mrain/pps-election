import random
from collections import defaultdict
from itertools import combinations, product
from typing import List, Dict

import metis
import shapely
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

    def drop_triangle(self, triangle):
        p = []
        for polygon in self.polygons:
            if polygon != triangle:
                p.append(polygon)
        self.polygons = p

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
        party_distribution = [0, 0]
        for polygon in self.polygons:
            for party in polygon['party_distribution'].keys():
                party_distribution[party] += polygon['party_distribution'][party]
        return party_distribution

    def get_party_seats(self):
        party_distribution = self.get_party_distribution()
        return dist_analysis.get_one_dist_seats(party_distribution, 3)

    def is_invalid(self):
        s = self.get_one_polygon()
        return type(s) == shapely.geometry.MultiPolygon


def check_if_node_is_near_part_boundary(graph, node: int) -> bool:
    part = graph.nodes[node]['part']
    for n in graph.neighbors(node):
        if graph.nodes[n]['part'] != part:
            return True
    return False


def wasted_vote_metric(given_districts: List[District]) -> List[int]:
    party_distribution = {-1: [0, 0]}
    party_seats = {-1: [0, 0]}
    for index, d in enumerate(given_districts):
        p = d.get_party_distribution()
        s = d.get_party_seats()
        for i, pp in enumerate(p):
            party_distribution[-1][i] += pp
        for i, ss in enumerate(s):
            party_seats[-1][i] += ss
        party_distribution[index] = p
        party_seats[index] = s
    wasted_votes = dist_analysis.get_wasted_votes(party_distribution, party_seats)
    return wasted_votes[-1]


def get_districts_from_triangles(
        voters: List[Voter], triangles: List[Dict], graph, n_districts: int, seed: int
) -> List[Polygon]:
    print('Making initial partition')
    (edgecuts, parts) = metis.part_graph(
        graph,
        n_districts,
        # ctype='shem',
        # rtype='sep2sided',
        ncuts=100,  # number of different cuts to try
        niter=2000,  # number of iterations of an algorithm
        contig=True,  # force partitions to be contiguous
        ubvec=[1.09],  # allowed constraint imbalance
    )
    print('Forming districts')
    districts = []
    for i in range(n_districts):
        districts.append(District())
    for index, part in enumerate(parts):
        graph.nodes[index]['part'] = part
        districts[part].append_triangle(triangles[index])

    pre = [(len(d.polygons), d.get_population(), 3703 < d.get_population() <= 4526) for d in districts]
    pre_wasted = wasted_vote_metric(districts)
    # Iterative Gerrymandering
    niters = 1000
    nsample = 50
    for i in range(niters):
        # sample random vertices pairs
        sampled_nodes = random.sample(graph.nodes, nsample)
        candidate_nodes = []
        for node in sampled_nodes:
            is_near_boundary = check_if_node_is_near_part_boundary(graph, node)
            if is_near_boundary:
                candidate_nodes.append(node)
        parts_nodes = defaultdict(list)
        swap_proposals = []
        for node in candidate_nodes:
            parts_nodes[graph.nodes[node]['part']].append(node)
        for a, b in combinations(parts_nodes.keys(), 2):
            for node1, node2 in product(parts_nodes[a], parts_nodes[b]):
                node1_has_where_to_flip = False
                node2_has_where_to_flip = False
                for n in graph.neighbors(node1):
                    if n != node2 and graph.nodes[n]['part'] == b:
                        node1_has_where_to_flip = True
                        break
                for n in graph.neighbors(node2):
                    if n != node1 and graph.nodes[n]['part'] == a:
                        node2_has_where_to_flip = True
                        break
                if node1_has_where_to_flip and node2_has_where_to_flip:
                    swap_proposals.append([
                        [a, node1],
                        [b, node2]
                    ])
        final_swaps = []
        swapped_nodes = []
        swapped_districts = []
        for swap_a, swap_b in swap_proposals:
            if swap_a[1] in swapped_nodes or swap_b[1] in swapped_nodes or swap_a[0] in swapped_districts or swap_b[0] in swapped_districts:
                continue
            district_a = districts[swap_a[0]]
            district_b = districts[swap_b[0]]
            before_swap = wasted_vote_metric([district_a, district_b])
            triangle_a = triangles[swap_a[1]]
            triangle_b = triangles[swap_b[1]]
            district_a_after = District()
            for polygon in district_a.polygons:
                if polygon != triangle_a:
                    district_a_after.append_triangle(polygon)
            district_a_after.append_triangle(triangle_b)
            district_b_after = District()
            for polygon in district_b.polygons:
                if polygon != triangle_b:
                    district_b_after.append_triangle(polygon)
            district_b_after.append_triangle(triangle_a)
            if district_a_after.is_invalid() or district_b_after.is_invalid():
                continue
            after_swap = wasted_vote_metric([district_a_after, district_b_after])
            # Gerrymandering for party 0 => decrease wasted votes for 0, increase for party 1
            metric = (before_swap[0] - after_swap[0]) + (after_swap[1] - before_swap[1])
            # metric = after_swap[1] - before_swap[1]
            # metric = before_swap[0] - after_swap[0]
            if metric > 0:
                # before_swap = wasted_vote_metric([district_a, district_b])
                # after_swap = wasted_vote_metric([district_a_after, district_b_after])
                swapped_nodes.append(swap_a[1])
                swapped_nodes.append(swap_b[1])
                swapped_districts.append(swap_a[0])
                swapped_districts.append(swap_b[0])
                final_swaps.append([swap_a, swap_b])
        for swap_a, swap_b in final_swaps:
            district_a = districts[swap_a[0]]
            district_b = districts[swap_b[0]]
            triangle_a = triangles[swap_a[1]]
            triangle_b = triangles[swap_b[1]]
            district_a.drop_triangle(triangle_a)
            district_a.append_triangle(triangle_b)
            district_b.drop_triangle(triangle_b)
            district_b.append_triangle(triangle_a)
            graph.nodes[swap_a[1]]['part'] = swap_b[0]
            graph.nodes[swap_b[1]]['part'] = swap_a[0]
            print('s', end='')
        # print(final_swaps)
        # post = [(len(d.polygons), d.get_population(), 3703 < d.get_population() <= 4526) for d in districts]
        print('.', end='')
    post = [(len(d.polygons), d.get_population(), 3703 < d.get_population() <= 4526) for d in districts]
    post_wasted = wasted_vote_metric(districts)
    print('Returning polygons')
    print(pre_wasted, post_wasted)
    return [d.get_one_polygon() for d in districts]


def get_districts(
        voters: List[Voter], triangles: List[Dict], graph, representatives_per_district: int, seed: int
) -> List[Polygon]:
    n_districts = int(81. / representatives_per_district * 3.)
    districts = get_districts_from_triangles(voters, triangles, graph, n_districts, seed)
    return districts
