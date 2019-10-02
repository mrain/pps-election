from typing import List

from election.g1.src.voter import Voter, Point
import numpy as np
import math
import random

num_of_party = 3

def is_in_triangle(x: int, y: int):
    from shapely.geometry.polygon import Polygon
    from shapely.geometry import Point as P
    triangle = Polygon([(0, 0), (0, 1000), (500, 500*math.sqrt(3))])
    return triangle.contains(P(x, y))

def get_normal(num_voters: int, mean_x, mean_y, std_x, std_y, seed: int=1234) -> List[Voter]:
    # @TODO try generate distribution with numpy multivairate normal distribution
    np.random.seed(seed)
    voters = []
    while len(voters) < num_voters:
        x = np.random.normal(loc=mean_x, scale=std_x)
        y = np.random.normal(loc=mean_y, scale=std_y)
        if is_in_triangle(x, y):
            loc = Point(x, y)
            pref = [random.random() for _ in range(num_of_party)]
            voter = Voter(location=loc, preference=pref)
            voters.append(voter)
    return voters


def get_uniform(num_voters: int, seed: int=1234) -> List[Voter]:
    np.random.seed(seed)
    voters = []
    while len(voters) < num_voters:
        x = np.random.uniform(low=0, high=1000)
        y = np.random.uniform(loc=0, high=500*math.sqrt(3))
        if is_in_triangle(x, y):
            loc = Point(x, y)
            pref = [random.random() for _ in range(num_of_party)]
            voter = Voter(location=loc, preference=pref)
            voters.append(voter)
    return voters


def get_party_preference(voters: List[Voter], num_parties: int, seed: int) -> List[Voter]:
    # @TODO Patrick
    pass


def get_coast(num_voters: int, seed: int) -> List[Voter]:
    # @TODO Patrick
    pass


def get_voters(num_voters: int, num_parties: int, seed: int) -> List[Voter]:
    # Define population distribution
    params = [{
        'type': 'coast',
        'percentage': 0.6
    }, {
        'type': 'uniform',
        'percentage': 0.2
    }, {
        'type': 'normal',
        'params': {'mean': (), 'sigma': ()},  # TODO: Add proper city 1 params
        'percentage': 0.1
    }, {
        'type': 'normal',
        'params': {'mean': (), 'sigma': ()},  # TODO: Add proper city 2 params
        'percentage': 0.1
    }]
    # Sample the population
    voters = []
    for index, param in enumerate(params):
        number_to_generate = round(param['percentage'] * num_voters)
        if index == len(params) - 1:
            number_to_generate = num_voters - len(voters)
        if param['type'] == 'coast':
            voters += get_coast(number_to_generate, seed=seed)
        elif param['type'] == 'uniform':
            voters += get_uniform(number_to_generate, seed=seed)
        elif param['type'] == 'normal':
            voters += get_normal(number_to_generate, param['params']['mean'], param['params']['sigma'], seed=seed)
    # Generate party preference
    voters = get_party_preference(voters, num_parties, seed)
    return voters
