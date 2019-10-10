import argparse
import importlib
import pickle

from matplotlib import pyplot as plt

from election.g6.src.map import Map
from election.g6.src.plothelper import draw_polygons, draw_voters


def main(options):
    generator_module = importlib.import_module(options.module)
    m = Map.from_file(options.input)
    triangles = generator_module.get_triangles(m.voters, options.representatives, options.seed)
    with open(options.output + '.pickle', 'wb') as file:
        pickle.dump(triangles, file)
    draw_voters(m.voters)
    polygons = [t['polygon'] for t in triangles]
    draw_polygons(polygons)
    plt.savefig(options.output + '.png')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--module', '-m', type=str, default="election.g6.src.trianglegenerator")
    parser.add_argument('--input', '-i', type=str, default="maps/g6/2_real_data_map.map")
    parser.add_argument('--output', '-o', type=str, default="maps/g6/0_real_data_triangles")
    parser.add_argument('--seed', type=int, default=42)
    parser.add_argument('--representatives', type=int, default=3)
    args = parser.parse_args()
    main(args)
