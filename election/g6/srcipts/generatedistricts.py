import argparse
import importlib
import pickle

from election.g6.src.map import Map


def main(options):
    generator_module = importlib.import_module(options.module)
    m = Map.from_file(options.input_map)
    with open(options.input, 'rb') as file:
        o = pickle.load(file)
    m.districts = generator_module.get_districts(m.voters, o['triangles'], o['graph'], options.representatives, options.seed)
    m.to_file(options.output)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--module', '-m', type=str, default="election.g6.src.districtgenerator")
    parser.add_argument('--input', '-i', type=str, default="maps/g6/0_1_2.pickle")
    parser.add_argument('--input-map', '-im', type=str, default="maps/g6/1_2.map")
    parser.add_argument('--output', '-o', type=str, default="maps/g6/5_1_2.map")
    parser.add_argument('--seed', type=int, default=42)
    parser.add_argument('--representatives', type=int, default=3)
    args = parser.parse_args()
    main(args)
