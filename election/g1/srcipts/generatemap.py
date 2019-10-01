import argparse
import importlib


def save_map_to_file(filepath, voters, districts):
    pass


def main(options):
    generator_module = importlib.import_module(options['class'])
    voters = generator_module.get_voters(options['voters'], options['parties'], options['seed'])
    save_map_to_file(options['output'], voters, [])


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--class', '-c', type=str, default="election.g1.src.mapgenerator")
    parser.add_argument('--output', '-o', type=str, default="maps/g1/1.map")
    parser.add_argument('--seed', type=int, default=42)
    parser.add_argument('--voters', type=int, default=333333)
    parser.add_argument('--parties', type=int, default=3)
    args = parser.parse_args()
    main(args)