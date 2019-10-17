import argparse
import importlib
import pickle

from election.g6.src.map import Map


def main(options):
    generator_module = importlib.import_module(options.module)
    m = Map.from_file(options.input_map)
    with open(options.input, 'rb') as file:
        o = pickle.load(file)
    m.districts = generator_module.get_districts(
        m.voters,
        o['triangles'],
        o['graph'],
        options.representatives,
        options.n_iters,
        options.gerrymander_for,
        options.seed
    )
    m.to_file(options.output)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--module', '-m', type=str, default="election.g6.src.districtgenerator",
        help='Python module to use as district generator'
    )
    parser.add_argument(
        '--input', '-i', type=str, default="maps/g6/0_randomg1_3.pickle",
        help='Path to pickle with graph and triangles'
    )
    parser.add_argument(
        '--input-map', '-im', type=str, default="maps/g1/randomg1_3.map",
        help='Path to map file to gerrymander'
    )
    parser.add_argument(
        '--n-iters', type=int, default=500,
        help='Number of iteration to run gerrymandering for'
    )
    parser.add_argument(
        '--gerrymander-for', type=int, default=2,
        help='Party index for which to gerrymander [0, 1, 2]'
    )
    parser.add_argument(
        '--output', '-o', type=str, default="maps/g6/6_randomg1_3_districts.map",
        help='A path where to save the final map'
    )
    parser.add_argument(
        '--seed', type=int, default=42,
        help='Random seed'
    )
    parser.add_argument(
        '--representatives', type=int, default=3,
        help='Number of representatives per district [1, 3]'
    )
    args = parser.parse_args()
    main(args)
