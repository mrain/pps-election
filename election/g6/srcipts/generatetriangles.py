import argparse
import importlib
import pickle

import networkx as nx
from matplotlib import pyplot as plt

from election.g6.src.map import Map
from election.g6.src.plothelper import draw_polygons, draw_voters
from election.g6.src.utils import find_adjacent_triangle


def main(options):
    generator_module = importlib.import_module(options.module)
    m = Map.from_file(options.input)
    triangles = generator_module.get_triangles(m.voters, options.representatives, options.seed)
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
    parser.add_argument('--module', '-m', type=str, default="election.g6.src.trianglegenerator")
    parser.add_argument('--input', '-i', type=str, default="maps/g6/1_2.map")
    parser.add_argument('--output', '-o', type=str, default="maps/g6/0_1_2")
    parser.add_argument('--seed', type=int, default=42)
    parser.add_argument('--representatives', type=int, default=3)
    args = parser.parse_args()
    main(args)
