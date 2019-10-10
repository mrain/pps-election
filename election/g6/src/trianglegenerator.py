import math
from typing import List, Tuple, Dict
from collections import defaultdict

import numpy as np
from shapely.geometry import Polygon

from election.g6.src.voter import Voter

num_parties = 3
triangle = Polygon([(0, 0), (1000, 0), (500, 500 * math.sqrt(3))])


def is_in_polygon(voter: Voter, polygon: Polygon) -> bool:
    return polygon.contains(voter.location)


def get_voters_in_polygon(polygon: Polygon, voters: List[Voter]) -> List[Voter]:
    return list(filter(lambda x: is_in_polygon(x, polygon), voters))


def naive_partition(n: int) -> List[Polygon]:
    edge_length = 1000 / n
    x_diff = 1 / 2 * edge_length
    y_diff = math.sqrt(3) / 2 * edge_length
    points_by_level = [[(500, 500 * math.sqrt(3))]]
    for level in range(n):
        new_level = []
        for point in points_by_level[-1]:
            new_point = (point[0] - x_diff, point[1] - y_diff)
            new_level.append(new_point)
        new_point = (point[0] + x_diff, point[1] - y_diff)
        new_level.append(new_point)
        points_by_level.append(new_level)

    result = []
    for level in range(n):
        curr_level = points_by_level[level]
        next_level = points_by_level[level + 1]
        temp = [next_level[0]]
        for p1, p2 in zip(curr_level, next_level[1:]):
            temp.append(p1)
            temp.append(p2)

        for i in range(len(temp) - 2):
            p1, p2, p3 = temp[i], temp[i + 1], temp[i + 2]
            result.append(Polygon([p1, p2, p3]))

    return result


def _find_level_points(voters, population_per_triangle):
    voters = sorted(voters, key=lambda voter: voter.location.y, reverse=True)
    line1_x = lambda y: math.sqrt(3) / 3 * y
    line2_x = lambda y: -math.sqrt(3) / 3 * y + 1000

    def find_level(curr_voter_num, population_per_level, level):
        if curr_voter_num + population_per_level <= len(voters):
            split_voter = voters[curr_voter_num + population_per_level - 1]
            y = max(split_voter.location.y - 0.00001, 0)
        else:
            y = 0
        x_left, x_right = line1_x(y), line2_x(y)
        x_diff = (x_right - x_left) / (level - 1)
        level_points = [(x_left, y)] + [(x_left + i * x_diff, y) for i in range(1, level - 1)] + \
                       [(x_right, y)]
        return level_points

    level_num = math.floor(math.sqrt(len(voters) / population_per_triangle))
    population_per_triangle = math.ceil(len(voters) / (level_num ** 2))
    curr_voter_num = 0
    points_by_level = [[(500, 500 * math.sqrt(3))]]
    for level in range(2, level_num + 1):
        curr_level_population = population_per_triangle * 2 * (level - 2) + 1
        curr_level = find_level(curr_voter_num, curr_level_population, level)
        curr_voter_num += curr_level_population
        points_by_level.append(curr_level)
    # manually add all remaining voters to last level
    curr_level = find_level(curr_voter_num, len(voters) - curr_voter_num + 1, level + 1)
    points_by_level.append(curr_level)
    return points_by_level


def _horizontal_adjustment(voters, points_by_level):
    num_levels = len(points_by_level)
    result = points_by_level[:2]
    for i in range(2, num_levels):
        prev_level, curr_level = points_by_level[i - 1], points_by_level[i]
        polygon = Polygon([prev_level[0], prev_level[-1], curr_level[0], curr_level[-1]])
        curr_voters = get_voters_in_polygon(polygon, voters)
        curr_voters.sort(key=lambda voter: voter.location.x)
        num_voters = len(curr_voters)

        curr_level_points = []
        for j in range(1, len(curr_level) - 1):
            index = math.ceil(j * (num_voters / (len(curr_level) - 1)))
            split_voter = curr_voters[index]
            split_point = (split_voter.location.x - 0.00001, curr_level[j][1])
            curr_level_points.append(split_point)
        curr_level_points = [curr_level[0]] + curr_level_points + [curr_level[-1]]
        result.append(curr_level_points)
    return result


def adaptive_partition(voters: List[Voter], population_per_triangle=None) -> List[Polygon]:
    if not population_per_triangle:
        population_per_triangle = len(voters) // (81 * 7)
    points_by_level = _find_level_points(voters, population_per_triangle)
    points_by_level = _horizontal_adjustment(voters, points_by_level)  # comment out this to do naive

    result = []
    for level in range(len(points_by_level) - 1):
        curr_level = points_by_level[level]
        next_level = points_by_level[level + 1]
        temp = [next_level[0]]
        for p1, p2 in zip(curr_level, next_level[1:]):
            temp.append(p1)
            temp.append(p2)

        for i in range(len(temp) - 2):
            p1, p2, p3 = temp[i], temp[i + 1], temp[i + 2]
            result.append(Polygon([p1, p2, p3]))

    return result


# partition into three smaller triangles recursively
def recursive_partition(triangle: Polygon, voters: List[Voter], threshold,
                        tolerance=2.7, return_population=True):
    new_voters = get_voters_in_polygon(triangle, voters)
    if len(new_voters) <= tolerance * threshold:
        if return_population:
            party_distribution = defaultdict(int)
            for voter in new_voters:
                this_pref = voter.preference  # party preferences for this voter
                this_party = np.argmax(this_pref)  # the party most preferred by this voter
                party_distribution[this_party] += 1
            return [{
                "polygon": triangle,
                "population": len(new_voters),
                "party_distribution": party_distribution
            }]
        else:
            return [triangle]

    # TODO: better centroid finding algorithm, e.g fast median
    x, y = 0, 0
    for voter in new_voters:
        x += voter.location.x
        y += voter.location.y
    x /= len(new_voters)
    y /= len(new_voters)

    centroid = (x, y)
    temp = list(triangle.exterior.coords)
    p1, p2, p3 = temp[0], temp[1], temp[2]
    new_triangle1 = Polygon([p1, p2, centroid])
    new_triangle2 = Polygon([p1, p3, centroid])
    new_triangle3 = Polygon([p2, p3, centroid])
    result = recursive_partition(new_triangle1, new_voters, threshold, tolerance, return_population) + \
             recursive_partition(new_triangle2, new_voters, threshold, tolerance, return_population) + \
             recursive_partition(new_triangle3, new_voters, threshold, tolerance, return_population)
    return result


def combined_partition(voters: List[Voter], population_per_triangle=None,
                       return_population=True) -> List[Dict]:
    if not population_per_triangle:
        population_per_triangle = len(voters) // (81 * 7)
    result = []
    polygons = adaptive_partition(voters, population_per_triangle=1.8 * population_per_triangle)
    print('Naive done', flush=True)
    for i, polygon in enumerate(polygons):
        result += recursive_partition(polygon, voters, threshold=population_per_triangle,
                                      return_population=return_population)
        print(str(i + 1) + '/' + str(len(polygons)), flush=True)
    return result


def k_means_clustering():
    # TODO
    pass


def get_initial_triangles(voters: List[Voter], threshold: float = 333333. // (81 * 7),
                          seed: int = 1234) -> List[Dict]:
    np.random.seed(seed)
    return combined_partition(voters, population_per_triangle=threshold)


def get_triangles(voters: List[Voter], representatives_per_district: int, seed: int) -> List[Dict]:
    n_districts = 81.
    n_triangles = n_districts * 7.
    threshold = len(voters) / n_triangles
    triangles = get_initial_triangles(voters, threshold, seed)
    return triangles
