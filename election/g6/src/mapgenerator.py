import math
import random
from typing import List

import numpy as np

from election.g6.src.voter import Voter, Point

num_parties = 3


def is_in_triangle(x: int, y: int):
    from shapely.geometry.polygon import Polygon
    from shapely.geometry import Point as P
    triangle = Polygon([(0, 0), (1000, 0), (500, 500 * math.sqrt(3))])
    return triangle.contains(P(x, y))


def get_normal(num_voters: int, mean_x, mean_y, std_x, std_y, seed: int = 1234, num_parties: int = 3,
               batch_size: int = 64) -> List[Voter]:
    # @TODO try generate distribution with numpy multivairate normal distribution
    np.random.seed(seed)
    voters = []
    while True:
        x_batch = np.random.normal(loc=mean_x, scale=std_x, size=(batch_size,))
        y_batch = np.random.normal(loc=mean_y, scale=std_y, size=(batch_size,))
        for x, y in zip(x_batch, y_batch):
            if is_in_triangle(x, y):
                loc = Point(x, y)
                pref = [random.random() for _ in range(num_parties)]
                voter = Voter(location=loc, preference=pref)
                voters.append(voter)
        if len(voters) >= num_voters:
            return voters[:num_voters]


def get_uniform(num_voters: int, seed: int = 1234, num_parties: int = 3, batch_size: int = 64) -> List[Voter]:
    np.random.seed(seed)
    voters = []
    while True:
        x_batch = np.random.uniform(low=0, high=1000, size=(batch_size,))
        y_batch = np.random.uniform(low=0, high=500 * math.sqrt(3), size=(batch_size,))
        for x, y in zip(x_batch, y_batch):
            if is_in_triangle(x, y):
                loc = Point(x, y)
                pref = [random.random() for _ in range(num_parties)]
                voter = Voter(location=loc, preference=pref)
                voters.append(voter)
        if len(voters) >= num_voters:
            return voters[:num_voters]


def dist(p1: Point, p2: Point):
    # Distance between p1 (x1, y1) and p2 (x2, y2)
    d = math.sqrt((p2.x - p1.x) ** 2 + (p2.y - p1.y) ** 2)
    return d


def coord_transform(triangle_coord):
    # d = distance to left side, y = distance to lower side
    d, y = triangle_coord[0], triangle_coord[1]
    x = (math.sqrt(3) / 3) * y + (2 * math.sqrt(3) / 3) * d
    return (x, y)


def get_p1_score(location: Point):
    # Get the preference score for party 1, can be changed in the future
    center = Point(500, 500 * math.sqrt(3) / 3)  # center of the triangle
    d = dist(location, center)
    # max_d = 1000 * math.sqrt(3) / 3, min_d = 0
    # Linear transform from d into score
    s = 1 - d / (1000 * math.sqrt(3) / 3)
    return s


def get_p2_score(location):
    # Get the preference score for party 2, can be changed in the future
    return 0.5


def get_p3_score(location):
    # Get the preference score for party 3, can be changed in the future
    if location.y < 100 * math.sqrt(3):
        return 0.75
    return -5


def get_party_score(voter: Voter, num_parties: int):
    # Get the vector of party preference scores
    scores = []
    p1_score = get_p1_score(voter.location)
    scores.append(p1_score)
    if num_parties >= 2:
        p2_score = get_p2_score(voter.location)
        scores.append(p2_score)
        if num_parties == 3:
            p3_score = get_p3_score(voter.location)
            scores.append(p3_score)
    return scores


def get_party_preference(voters: List[Voter], num_parties: int, seed: int) -> List[Voter]:
    # @TODO Patrick
    np.random.seed(seed)
    new_voters = []
    for voter in voters:
        scores = get_party_score(voter, num_parties)
        prefs_raw = [np.random.normal(score, 0.3) for score in scores]
        prefs = [max(min(pref, 1), 0) for pref in prefs_raw]
        new_voter = Voter(location=voter.location, preference=prefs)
        new_voters.append(new_voter)
    return new_voters


def get_coast(num_voters: int, seed: int, scale=500) -> List[Voter]:
    # @TODO Patrick
    # Assuming the left side of the triangle is the coast
    np.random.seed(seed)
    voters = []
    while len(voters) < num_voters:
        d = np.random.exponential(scale)
        y = np.random.uniform(low=0, high=500 * math.sqrt(3))
        triangle_coord = (d, y)
        x, y = coord_transform(triangle_coord)
        if is_in_triangle(x, y):
            loc = Point(x, y)
            voter = Voter(location=loc)
            voters.append(voter)
    return voters


def get_voters(num_voters: int, num_parties: int, seed: int) -> List[Voter]:
    # Define population distribution
    params = [
        {
            'type': 'coast',
            'percentage': 0.65
        },
        {
            'type': 'uniform',
            'percentage': 0.15
        },
        {
            'type': 'normal',
            'params': {'mean_x': 500, 'mean_y': 650, 'std_x': 85, 'std_y': 40},
            'percentage': 0.08
        },
        {
            'type': 'normal',
            'params': {'mean_x': 400, 'mean_y': 250, 'std_x': 30, 'std_y': 30},
            'percentage': 0.02
        },
        {
            'type': 'normal',
            'params': {'mean_x': 460, 'mean_y': 200, 'std_x': 24, 'std_y': 44},
            'percentage': 0.03
        },
        {
            'type': 'normal',
            'params': {'mean_x': 400, 'mean_y': 150, 'std_x': 60, 'std_y': 30},
            'percentage': 0.07
        }
    ]
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
            voters += get_normal(
                number_to_generate,
                param['params']['mean_x'],
                param['params']['mean_y'],
                param['params']['std_x'],
                param['params']['std_y'],
                seed=seed
            )
        print('.', end='', flush=True)
    # Generate party preference
    voters = get_party_preference(voters, num_parties, seed)
    return voters
