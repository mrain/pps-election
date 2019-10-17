import argparse
import pickle
from collections import defaultdict

import numpy as np
import networkx as nx
from matplotlib import pyplot as plt

from election.g6.src.map import Map
from election.g6.src.plothelper import draw_polygons, draw_voters
from election.g6.src.utils import find_adjacent_triangle
from election.g6.src.utils import read_triangles_from_file, get_voters_in_polygons


def main(options):
    m = Map.from_file(options.input_map)
    raw_triangles = read_triangles_from_file(options.input)
    populations = get_voters_in_polygons(m.voters, raw_triangles)
    print('Counting triangles')
    triangles = []
    for i, t in enumerate(raw_triangles):
        party_distribution = defaultdict(int)
        new_voters = populations[i]
        for voter in new_voters:
            this_pref = voter.preference  # party preferences for this voter
            this_party = np.argmax(this_pref)  # the party most preferred by this voter
            party_distribution[this_party] += 1
        triangles.append({
            "polygon": t,
            "population": len(new_voters),
            "party_distribution": party_distribution
        })
    print('Forming graph')
    graph = nx.Graph()
    graph.graph['node_weight_attr'] = 'population'
    graph.add_nodes_from(list(range(len(triangles))))
    for index, triangle in enumerate(triangles):
        print(str(index + 1) + '/' + str(len(triangles)), flush=True)
        graph.nodes[index]['population'] = triangle['population']
        adj_trs = find_adjacent_triangle(index, triangles)
        for tr in adj_trs:
            graph.add_edge(index, tr)
    o = {
        'graph': graph,
        'triangles': triangles
    }
    with open(options.output + '.pickle', 'wb') as file:
        pickle.dump(o, file)
    draw_voters(m.voters)
    polygons = [t['polygon'] for t in triangles]
    draw_polygons(polygons)
    plt.savefig(options.output + '.png')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--input', '-i', type=str, default="maps/g6/saved_triangles.dat",
        help='Path to triangles from physical simulation'
    )
    parser.add_argument(
        '--input-map', '-im', type=str, default="maps/g1/randomg1_3.map",
        help='Path to input map for which the triangles where generated'
    )
    parser.add_argument(
        '--output', '-o', type=str, default="maps/g6/0_saved_triangles",
        help='Path to where to save the pickle with graph and triangles'
    )
    args = parser.parse_args()
    main(args)
