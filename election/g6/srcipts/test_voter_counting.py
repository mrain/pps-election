import argparse
import pickle
import time

from election.g6.src.map import Map
from election.g6.src.utils import get_population_in_polygons, get_population_in_polygons_basic


def main(options):
    m = Map.from_file(options.input_map)
    with open(options.input, 'rb') as file:
        o = pickle.load(file)
    polygons = [t['polygon'] for t in o['triangles']]
    print('Counting better')
    start_time = time.time()
    populations = get_population_in_polygons(m.voters, polygons[:10])
    print(populations)
    print('Done in: ' + str(time.time() - start_time))
    print('Counting basic')
    start_time = time.time()
    populations = get_population_in_polygons_basic(m.voters, polygons[:10])
    print(populations)
    print('Done in: ' + str(time.time() - start_time))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--input', '-i', type=str, default="maps/g6/0_g2_2party.pickle")
    parser.add_argument('--input-map', '-im', type=str, default="maps/g2/2party.map")
    args = parser.parse_args()
    main(args)
