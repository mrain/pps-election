import argparse
import importlib


def save_map_to_file(filepath, voters, districts, parties):
    file = open(filepath, 'w')
    file.write(f'{str(len(voters))} {str(parties)}\n')
    for voter in voters:
        file.write(voter.get_file_line())
        file.write('\n')
    file.write(f'{str(len(districts))}\n')
    for district in districts:
        file.write(district.get_file_line())
        file.write('\n')
    file.close()


def main(options):
    generator_module = importlib.import_module(options.module)
    voters = generator_module.get_voters(options.voters, options.parties, options.seed)
    save_map_to_file(options.output, voters, [], options.parties)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--module', '-m', type=str, default="election.g1.src.mapgenerator")
    parser.add_argument('--output', '-o', type=str, default="maps/g1/1.map")
    parser.add_argument('--seed', type=int, default=42)
    parser.add_argument('--voters', type=int, default=333333)
    parser.add_argument('--parties', type=int, default=3)
    args = parser.parse_args()
    main(args)