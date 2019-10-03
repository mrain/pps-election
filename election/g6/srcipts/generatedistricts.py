import argparse
import importlib

from election.g6.src.map import Map


def main(options):
    generator_module = importlib.import_module(options.module)
    m = Map.from_file(options.input)
    m.districts = generator_module.get_districts(m.voters, m.parties, options.seed)
    m.to_file(options.output)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--module', '-m', type=str, default="election.g6.src.districtgenerator")
    parser.add_argument('--input', '-i', type=str, default="maps/g6/3.map")
    parser.add_argument('--output', '-o', type=str, default="maps/g6/3_d.map")
    parser.add_argument('--seed', type=int, default=42)
    parser.add_argument('--representatives', type=int, default=3)
    args = parser.parse_args()
    main(args)
