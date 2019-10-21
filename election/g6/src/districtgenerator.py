import random
from collections import defaultdict
from itertools import combinations, product
from typing import List, Dict

import metis
import nxmetis
from shapely.geometry import Polygon

from election.g6.src import dist_analysis
from election.g6.src.voter import Voter
from election.g6.src.district import District
from election.g6.src.metrics import wasted_vote_metric, get_metric, wasted_percentage_difference
from election.g6.src.utils import (
    check_if_node_is_near_part_boundary,
    check_if_node_is_near_part,
    find_adjacent_district_with_most_triangles,
    check_if_removing_polygon_is_okay
)


def get_districts_from_triangles(
        voters: List[Voter],
        representatives_per_district: int,
        triangles: List[Dict],
        graph,
        n_districts: int,
        n_iterations: int,
        gerrymander_for: int,
        seed: int
) -> List[Polygon]:
    n_parties = 2
    # print('Making initial partition')
    # (edgecuts, parts) = metis.part_graph(
    #     graph,
    #     n_districts,
    #     ncuts=100,  # number of different cuts to try
    #     niter=2000,  # number of iterations of an algorithm
    #     contig=True,  # force partitions to be contiguous
    #     recursive=True
    #     # ubvec=[1.09],  # allowed constraint imbalance
    # )
    # print('Forming districts')
    # districts = []
    # for i in range(n_districts):
    #     districts.append(District(representatives_per_district, n_parties))
    # for index, part in enumerate(parts):
    #     graph.nodes[index]['part'] = part
    #     districts[part].append_triangle((index, triangles[index]))
    print('Making initial partition')
    (edgecuts, parts) = nxmetis.partition(
        graph,
        n_districts,
        # ctype='shem',
        # rtype='sep2sided',
        # node_weight='population',
        options=nxmetis.types.MetisOptions(
            # ctype=nxmetis.enums.MetisCType.shem,
            iptype=nxmetis.enums.MetisIPType.node,
            ncuts=100,  # number of different cuts to try
            niter=4000,  # number of iterations of an algorithm
            contig=True
        ),
        # ubvec=[1.09],  # allowed constraint imbalance
    )
    print('Forming districts')
    districts = []
    for i in range(n_districts):
        districts.append(District(representatives_per_district, n_parties))
    for part, part_triangles in enumerate(parts):
        for index in part_triangles:
            graph.nodes[index]['part'] = part
            districts[part].append_triangle((index, triangles[index]))

    print(len(districts))
    pre = [(len(d.polygons), d.get_population(), 3703 < d.get_population() <= 4526) for d in districts]
    done = []
    not_all_good = True
    num_it = 0
    total_num_it = 0
    reset_each = 100
    while not_all_good:
        total_num_it += 1
        num_it += 1
        if num_it > reset_each:
            num_it = 0
            done = []
        if total_num_it > 3000:
            reset_each = 50
        pre = [(len(d.polygons), d.get_population(), 3703 < d.get_population() <= 4526) for d in districts]
        print('|', end='')
        not_all_good = False
        for i, d in enumerate(districts):
            if len(d.polygons) < 6:
                not_all_good = True
                made_flip = False
                closest_districts = find_adjacent_district_with_most_triangles(i, d, districts, graph)
                for potential_district_id, potential_district in closest_districts:
                    if potential_district_id in done:
                        continue
                    if made_flip:
                        break
                    possible_triangles = []
                    for index, polygon in potential_district.polygons:
                        is_adj = check_if_node_is_near_part(graph, index, i)
                        if is_adj and check_if_removing_polygon_is_okay(potential_district, index):
                            possible_triangles.append((index, polygon))
                    if len(possible_triangles) > 0:
                        index, triangle_to_swap = random.sample(possible_triangles, 1)[0]
                        made_flip = True
                        potential_district.drop_triangle_by_id(index)
                        d.append_triangle((index, triangle_to_swap))
                        done.append(potential_district_id)
                        graph.nodes[index]['part'] = i
                if made_flip:
                    print('.' + str(len(possible_triangles)), end='')
                else:
                    print(',' + str(len(possible_triangles)), end='')


    pre = [(len(d.polygons), d.get_population(), 3703 < d.get_population() <= 4526) for d in districts]

    if n_parties == 3:
        pre_wasted = wasted_percentage_difference(districts, n_parties)
    else:
        pre_wasted = wasted_vote_metric(districts, n_parties)
    # Iterative Gerrymandering
    s = sum([len(d.polygons) for d in districts])
    print(s, end='')
    nsample = 50
    for i in range(n_iterations):
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
            if (swap_a[1] in swapped_nodes) or (swap_b[1] in swapped_nodes) or (
                    swap_a[0] in swapped_districts) or (swap_b[0] in swapped_districts):
                continue
            district_a = districts[swap_a[0]]
            district_b = districts[swap_b[0]]
            if n_parties == 3:
                before_swap = wasted_percentage_difference([district_a, district_b], n_parties)
            else:
                before_swap = wasted_vote_metric([district_a, district_b], n_parties)
            triangle_a = triangles[swap_a[1]]
            triangle_b = triangles[swap_b[1]]
            district_a_after = District(representatives_per_district, n_parties)
            for q, polygon in district_a.polygons:
                if polygon != triangle_a:
                    district_a_after.append_triangle((q, polygon))
            district_a_after.append_triangle((swap_b[1], triangle_b))
            district_b_after = District(representatives_per_district, n_parties)
            for q, polygon in district_b.polygons:
                if polygon != triangle_b:
                    district_b_after.append_triangle((q, polygon))
            district_b_after.append_triangle((swap_a[1], triangle_a))
            if district_a_after.is_invalid() or district_b_after.is_invalid():
                continue
            if district_a_after.is_population_invalid() or district_b_after.is_population_invalid():
                print("UPsss")
                continue
            if n_parties == 3:
                after_swap = wasted_percentage_difference([district_a_after, district_b_after], n_parties)
            else:
                after_swap = wasted_vote_metric([district_a_after, district_b_after], n_parties)
            # Gerrymandering for party 0 => decrease wasted votes for 0, increase for party 1
            metric = get_metric(before_swap, after_swap, gerrymander_for, n_parties)
            # metric = after_swap[1] - before_swap[1]
            # metric = before_swap[0] - after_swap[0]
            if metric > 0:
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
            # district_a.drop_triangle(triangle_a)
            district_a.drop_triangle_by_id(swap_a[1])
            district_a.append_triangle((swap_b[1], triangle_b))
            # district_b.drop_triangle(triangle_b)
            district_b.drop_triangle_by_id(swap_b[1])
            district_b.append_triangle((swap_a[1], triangle_a))
            graph.nodes[swap_a[1]]['part'] = swap_b[0]
            graph.nodes[swap_b[1]]['part'] = swap_a[0]
            print('s', end='')
        s = sum([len(d.polygons) for d in districts])
        # print(s, end='')
        # if s > 486:
        #     print(s)
        print('.', end='')
    post = [(len(d.polygons), d.get_population(), 3703 < d.get_population() <= 4526) for d in districts]
    if n_parties == 3:
        post_wasted = wasted_percentage_difference(districts, n_parties)
    else:
        post_wasted = wasted_vote_metric(districts, n_parties)
    print('Returning polygons')
    print(pre_wasted, post_wasted)
    print(post)
    print(len(districts))
    return [d.get_one_polygon() for d in districts]


def get_districts(
        voters: List[Voter],
        triangles: List[Dict],
        graph,
        representatives_per_district: int,
        n_iterations: int,
        gerrymander_for: int,
        seed: int
) -> List[Polygon]:
    n_districts = int(81. / representatives_per_district * 3.)
    districts = get_districts_from_triangles(
        voters,
        representatives_per_district,
        triangles,
        graph,
        n_districts,
        n_iterations,
        gerrymander_for,
        seed
    )
    return districts
