import argparse
import importlib
import pickle
import random
from collections import defaultdict

import numpy as np
import networkx as nx
from matplotlib import pyplot as plt

from election.g6.src.map import Map
from election.g6.src.plothelper import draw_polygons, draw_voters
from election.g6.src.utils import find_adjacent_triangle, get_voters_in_polygons


def main(options):
    generator_module = importlib.import_module(options.module)
    m = Map.from_file(options.input)
    triangles = generator_module.get_triangles(m.voters, options.representatives, options.seed)
    populations = get_voters_in_polygons(m.voters, triangles)
    print('Forming graph')
    graph = nx.Graph()
    graph.graph['node_weight_attr'] = 'population'
    graph.add_nodes_from(list(range(len(triangles))))
    triangles_dicts = []
    for index, triangle in enumerate(triangles):
        print(str(index + 1) + '/' + str(len(triangles)), flush=True)
        graph.nodes[index]['population'] = len(populations[index])
        party_distribution = defaultdict(int)
        for voter in populations[index]:
            this_pref = voter.preference  # party preferences for this voter
            this_party = np.argmax(this_pref)  # the party most preferred by this voter
            party_distribution[this_party] += 1
        triangles_dicts.append({
            "polygon": triangle,
            "population": len(populations[index]),
            "party_distribution": party_distribution
        })
        adj_trs = find_adjacent_triangle(index, triangles)
        for tr in adj_trs:
            graph.add_edge(index, tr)
    o = {
        'graph': graph,
        'triangles': triangles_dicts
    }
    with open(options.output + '.pickle', 'wb') as file:
        pickle.dump(o, file)
    draw_voters(m.voters)
    polygons = [t for t in triangles]
    draw_polygons(polygons)
    plt.savefig(options.output + '.png')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--module', '-m', type=str, default="election.g6.src.trianglegenerator")
    parser.add_argument('--input', '-i', type=str, default="maps/tournaments/tour3.map")
    parser.add_argument('--output', '-o', type=str, default="maps/final/26_1rep_new")
    parser.add_argument('--seed', type=int, default=42)
    parser.add_argument('--representatives', type=int, default=1)
    args = parser.parse_args()
    main(args)
